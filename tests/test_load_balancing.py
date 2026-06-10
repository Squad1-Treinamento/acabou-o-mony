#!/usr/bin/env python3
"""
Script para testar load balancing do nginx.

Verifica:
- Serviço nginx está rodando no Docker Compose
- Upstreams configurados
- Distribuição de requisições entre upstreams

Uso:
    # Modo interativo
    python tests/test_load_balancing.py
    
    # Modos predefinidos
    python tests/test_load_balancing.py -m quick
    python tests/test_load_balancing.py -m standard
    python tests/test_load_balancing.py -m stress
    python tests/test_load_balancing.py -m concurrent
    
    # Modo concurrent com workers customizados
    python tests/test_load_balancing.py -m concurrent -w 50
    
    # Modo customizado (sequencial)
    python tests/test_load_balancing.py -n 200
    
    # Modo customizado CONCORRENTE (com -w ou -m concurrent)
    python tests/test_load_balancing.py -n 500 -w 30
    python tests/test_load_balancing.py -m concurrent -n 500
    python tests/test_load_balancing.py -m concurrent -n 500 -w 50
    
    # Com URL customizada
    python tests/test_load_balancing.py -u http://localhost:8080 -n 50
"""

import argparse
import os
import re
import subprocess
import sys
import time
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from enum import Enum
from typing import Dict, List, Optional, Tuple

try:
    import requests
except ImportError:
    print("❌ Erro: biblioteca 'requests' não encontrada.")
    print("   Instale com: pip install requests")
    sys.exit(1)


class Colors:
    """Cores ANSI para output colorido no terminal."""
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'


class TestMode(Enum):
    """Modos de teste disponíveis."""
    QUICK = "quick"
    STANDARD = "standard"
    STRESS = "stress"
    CONCURRENT = "concurrent"


@dataclass
class TestProfile:
    """Perfil de configuração para cada modo de teste."""
    name: str
    description: str
    waves: List[Tuple[int, float]]  # [(num_requests, delay_between_waves), ...]
    timeout: int
    concurrent: bool = False  # Indica se executa requisições concorrentemente
    max_workers: int = 10  # Número de workers para modo concurrent
    
    @property
    def total_requests(self) -> int:
        return sum(wave[0] for wave in self.waves)


# Perfis de teste predefinidos
TEST_PROFILES = {
    TestMode.QUICK: TestProfile(
        name="🚀 Quick Test",
        description="Teste rápido com 100 requisições em uma única onda",
        waves=[(100, 0)],
        timeout=5
    ),
    TestMode.STANDARD: TestProfile(
        name="📊 Standard Test",
        description="Teste completo com 300 requisições em 3 ondas (pausa de 2s entre ondas)",
        waves=[(100, 2), (100, 2), (100, 0)],
        timeout=5
    ),
    TestMode.STRESS: TestProfile(
        name="💥 Stress Test",
        description="Teste de estresse com 1000 requisições em 5 ondas (pausa de 1s entre ondas)",
        waves=[(200, 1), (200, 1), (200, 1), (200, 1), (200, 0)],
        timeout=3
    ),
    TestMode.CONCURRENT: TestProfile(
        name="⚡ Concurrent Test",
        description="Teste concorrente com 300 requisições simultâneas (20 workers)",
        waves=[(300, 0)],
        timeout=5,
        concurrent=True,
        max_workers=20
    )
}


def print_header(text: str):
    """Imprime cabeçalho formatado."""
    print(f"\n{Colors.BOLD}{Colors.HEADER}{text}{Colors.ENDC}")


def print_success(text: str):
    """Imprime mensagem de sucesso."""
    print(f"{Colors.OKGREEN}✅ {text}{Colors.ENDC}")


def print_error(text: str):
    """Imprime mensagem de erro."""
    print(f"{Colors.FAIL}❌ {text}{Colors.ENDC}")


def print_info(text: str):
    """Imprime mensagem informativa."""
    print(f"{Colors.OKCYAN}ℹ️  {text}{Colors.ENDC}")


def print_warning(text: str):
    """Imprime mensagem de aviso."""
    print(f"{Colors.WARNING}⚠️  {text}{Colors.ENDC}")


def print_test_modes():
    """Imprime os modos de teste disponíveis."""
    print_header("📋 Modos de Teste Disponíveis")
    print()
    
    for i, (mode, profile) in enumerate(TEST_PROFILES.items(), 1):
        execution_type = "concorrente" if profile.concurrent else "sequencial"
        print(f"{Colors.BOLD}{i}. {profile.name}{Colors.ENDC}")
        print(f"   {profile.description}")
        print(f"   Total: {profile.total_requests} requisições em {len(profile.waves)} onda(s) ({execution_type})")
        if profile.concurrent:
            print(f"   Workers: {profile.max_workers} threads simultâneas")
        print()


def select_test_mode() -> TestProfile:
    """
    Solicita ao usuário que selecione um modo de teste.
    
    Returns:
        TestProfile: Perfil de teste selecionado
    """
    print_test_modes()
    
    modes_list = list(TEST_PROFILES.values())
    
    while True:
        try:
            choice = input(f"{Colors.OKCYAN}Selecione o modo de teste (1-{len(modes_list)}): {Colors.ENDC}").strip()
            
            if not choice:
                print_info("Usando modo padrão: Quick Test")
                return modes_list[0]
            
            choice_num = int(choice)
            
            if 1 <= choice_num <= len(modes_list):
                selected = modes_list[choice_num - 1]
                print_success(f"Modo selecionado: {selected.name}")
                return selected
            else:
                print_error(f"Opção inválida. Escolha entre 1 e {len(modes_list)}.")
        except ValueError:
            print_error("Entrada inválida. Digite um número.")
        except KeyboardInterrupt:
            print("\n")
            print_warning("Operação cancelada pelo usuário")
            sys.exit(130)


def check_nginx_running() -> Tuple[bool, Optional[str]]:
    """
    Verifica se o serviço nginx está rodando no Docker Compose.
    
    Returns:
        Tuple[bool, Optional[str]]: (está_rodando, mensagem_erro)
    """
    try:
        result = subprocess.run(
            ["docker", "compose", "ps", "--services", "--filter", "status=running"],
            capture_output=True,
            text=True,
            check=True
        )
        
        services = result.stdout.strip().split('\n')
        
        if 'nginx' in services:
            # Verificar health check
            result = subprocess.run(
                ["docker", "compose", "ps", "nginx", "--format", "json"],
                capture_output=True,
                text=True,
                check=True
            )
            
            if '"Health":"healthy"' in result.stdout or '"State":"running"' in result.stdout:
                return True, None
            else:
                return False, "Nginx está rodando mas não está healthy"
        else:
            return False, "Serviço nginx não encontrado ou não está rodando"
            
    except subprocess.CalledProcessError as e:
        return False, f"Erro ao executar docker compose: {e}"
    except FileNotFoundError:
        return False, "Docker ou docker compose não encontrado no PATH"


def get_configured_upstreams() -> Tuple[Optional[List[str]], Optional[str]]:
    """
    Obtém a lista de upstreams configurados a partir das variáveis de ambiente.
    
    Returns:
        Tuple[Optional[List[str]], Optional[str]]: (lista_upstreams, mensagem_erro)
    """
    try:
        # Tentar ler UPSTREAMS do ambiente
        upstreams_env = os.environ.get('UPSTREAMS')
        
        if upstreams_env:
            upstreams = [u.strip() for u in upstreams_env.split(',') if u.strip()]
            return upstreams, None
        
        # Fallback: tentar UPSTREAM_HOST e UPSTREAM_PORT
        upstream_host = os.environ.get('UPSTREAM_HOST')
        upstream_port = os.environ.get('UPSTREAM_PORT')
        
        if upstream_host and upstream_port:
            return [f"{upstream_host}:{upstream_port}"], None
        
        # Tentar ler do container nginx
        result = subprocess.run(
            ["docker", "compose", "exec", "-T", "nginx", "sh", "-c", 
             "echo $UPSTREAMS"],
            capture_output=True,
            text=True,
            check=True
        )
        
        upstreams_container = result.stdout.strip()
        if upstreams_container:
            upstreams = [u.strip() for u in upstreams_container.split(',') if u.strip()]
            return upstreams, None
        
        return None, "Não foi possível determinar os upstreams configurados"
        
    except Exception as e:
        return None, f"Erro ao obter upstreams: {e}"


def send_single_request(url: str, request_id: int, timeout: int) -> Tuple[int, Optional[str], Optional[str]]:
    """
    Envia uma única requisição (usado no modo concurrent).
    
    Args:
        url: URL para enviar requisição
        request_id: ID da requisição
        timeout: Timeout em segundos
    
    Returns:
        Tuple[int, Optional[str], Optional[str]]: (request_id, upstream, erro)
    """
    try:
        response = requests.get(url, timeout=timeout)
        
        # Verificar status code primeiro
        if response.status_code != 200:
            return (request_id, None, f"HTTP {response.status_code}")
        
        # Status 200: tentar obter header
        upstream = response.headers.get('X-Upstream-Server')
        
        if upstream:
            return (request_id, upstream, None)
        else:
            return (request_id, None, "Header X-Upstream-Server não encontrado")
            
    except requests.exceptions.Timeout:
        return (request_id, None, "Timeout")
    except requests.exceptions.ConnectionError:
        return (request_id, None, "Erro de conexão")
    except Exception as e:
        return (request_id, None, str(e))


def send_requests_wave_concurrent(
    url: str,
    count: int,
    wave_num: int,
    total_waves: int,
    timeout: int,
    max_workers: int
) -> Tuple[List[str], List[str]]:
    """
    Envia uma onda de requisições CONCORRENTES usando ThreadPoolExecutor.
    
    Args:
        url: URL para enviar requisições
        count: Número de requisições a enviar nesta onda
        wave_num: Número da onda atual (1-indexed)
        total_waves: Total de ondas no teste
        timeout: Timeout em segundos para cada requisição
        max_workers: Número de threads concorrentes
    
    Returns:
        Tuple[List[str], List[str]]: (lista_upstreams_respondidos, lista_erros)
    """
    upstreams = []
    errors = []
    
    wave_label = f"Onda {wave_num}/{total_waves}" if total_waves > 1 else ""
    print(f"\n⚡ {wave_label} Enviando {count} requisições CONCORRENTES ({max_workers} workers) para {url}")
    print("   ", end="", flush=True)
    
    start_time = time.time()
    completed = 0
    
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Submeter todas as requisições
        futures = {
            executor.submit(send_single_request, url, i, timeout): i 
            for i in range(count)
        }
        
        # Coletar resultados conforme completam
        for future in as_completed(futures):
            request_id, upstream, error = future.result()
            
            if upstream:
                upstreams.append(upstream)
            else:
                errors.append(f"Onda {wave_num}, Req {request_id+1}: {error}")
            
            completed += 1
            
            # Mostrar progresso
            if completed % 10 == 0:
                print(".", end="", flush=True)
    
    elapsed = time.time() - start_time
    rps = count / elapsed if elapsed > 0 else 0
    
    print(f" ✓ ({elapsed:.2f}s, {rps:.1f} req/s)", flush=True)
    
    return upstreams, errors


def send_requests_wave(
    url: str,
    count: int,
    wave_num: int,
    total_waves: int,
    timeout: int = 5
) -> Tuple[List[str], List[str]]:
    """
    Envia uma onda de requisições SEQUENCIAIS para o URL e coleta as respostas.
    
    Args:
        url: URL para enviar requisições
        count: Número de requisições a enviar nesta onda
        wave_num: Número da onda atual (1-indexed)
        total_waves: Total de ondas no teste
        timeout: Timeout em segundos para cada requisição
    
    Returns:
        Tuple[List[str], List[str]]: (lista_upstreams_respondidos, lista_erros)
    """
    upstreams = []
    errors = []
    
    wave_label = f"Onda {wave_num}/{total_waves}" if total_waves > 1 else ""
    print(f"\n🚀 {wave_label} Enviando {count} requisições para {url}")
    print("   ", end="", flush=True)
    
    start_time = time.time()
    
    for i in range(count):
        try:
            response = requests.get(url, timeout=timeout)
            
            # Verificar status code primeiro
            if response.status_code != 200:
                errors.append(f"Onda {wave_num}, Req {i+1}: HTTP {response.status_code}")
            else:
                # Status 200: tentar obter header
                upstream = response.headers.get('X-Upstream-Server')
                
                if upstream:
                    upstreams.append(upstream)
                else:
                    errors.append(f"Onda {wave_num}, Req {i+1}: Header X-Upstream-Server não encontrado")
            
            # Mostrar progresso
            if (i + 1) % 10 == 0:
                print(".", end="", flush=True)
                
        except requests.exceptions.Timeout:
            errors.append(f"Onda {wave_num}, Req {i+1}: Timeout")
        except requests.exceptions.ConnectionError:
            errors.append(f"Onda {wave_num}, Req {i+1}: Erro de conexão")
        except Exception as e:
            errors.append(f"Onda {wave_num}, Req {i+1}: {str(e)}")
    
    elapsed = time.time() - start_time
    rps = count / elapsed if elapsed > 0 else 0
    
    print(f" ✓ ({elapsed:.2f}s, {rps:.1f} req/s)", flush=True)
    
    return upstreams, errors


def analyze_distribution(upstreams: List[str]) -> Dict[str, int]:
    """
    Analisa a distribuição de requisições entre upstreams.
    
    Args:
        upstreams: Lista de upstreams que responderam
    
    Returns:
        Dict[str, int]: Dicionário com contagem por upstream
    """
    return dict(Counter(upstreams))


def run_test_profile(url: str, profile: TestProfile) -> Tuple[Dict[str, int], int, List[str]]:
    """
    Executa um perfil de teste completo.
    
    Args:
        url: URL para testar
        profile: Perfil de teste a executar
    
    Returns:
        Tuple[Dict[str, int], int, List[str]]: (distribuição, total_requisições, erros)
    """
    print_header(f"🧪 Executando: {profile.name}")
    print(f"   {profile.description}")
    
    if profile.concurrent:
        print(f"   Modo: CONCORRENTE com {profile.max_workers} workers")
    else:
        print(f"   Modo: SEQUENCIAL")
    
    all_upstreams = []
    all_errors = []
    total_waves = len(profile.waves)
    
    for wave_num, (count, delay) in enumerate(profile.waves, 1):
        if profile.concurrent:
            # Usar função concorrente
            upstreams, errors = send_requests_wave_concurrent(
                url, count, wave_num, total_waves, profile.timeout, profile.max_workers
            )
        else:
            # Usar função sequencial (comportamento original)
            upstreams, errors = send_requests_wave(url, count, wave_num, total_waves, profile.timeout)
        
        all_upstreams.extend(upstreams)
        all_errors.extend(errors)
        
        # Delay entre ondas (exceto na última)
        if delay > 0 and wave_num < total_waves:
            print(f"   ⏸️  Aguardando {delay}s antes da próxima onda...")
            time.sleep(delay)
    
    distribution = analyze_distribution(all_upstreams)
    return distribution, profile.total_requests, all_errors


def print_detailed_report(
    distribution: Dict[str, int],
    total: int,
    errors: List[str],
    profile: TestProfile
):
    """
    Imprime relatório detalhado incluindo informações do perfil de teste.
    
    Args:
        distribution: Dicionário com contagem por upstream
        total: Total de requisições enviadas
        errors: Lista de erros encontrados
        profile: Perfil de teste executado
    """
    print_header("📊 Resultados do Load Balancing")
    print(f"Modo: {profile.name}")
    execution_type = "CONCORRENTE" if profile.concurrent else "SEQUENCIAL"
    print(f"Tipo de execução: {execution_type}")
    print("━" * 60)
    
    if not distribution:
        print_error("Nenhuma requisição foi processada com sucesso!")
        return
    
    # Calcular largura máxima para alinhamento
    max_upstream_len = max(len(upstream) for upstream in distribution.keys())
    
    # Imprimir cada upstream com barra de progresso
    for upstream, count in sorted(distribution.items(), key=lambda x: x[1], reverse=True):
        percentage = (count / total) * 100
        bar_length = int(percentage / 2)  # Barra de até 50 caracteres (100% / 2)
        bar = "█" * bar_length + "░" * (50 - bar_length)
        
        upstream_padded = upstream.ljust(max_upstream_len)
        print(f"{upstream_padded}  {bar}  {count:3d} ({percentage:5.1f}%)")
    
    print("━" * 60)
    
    # Análise da distribuição
    successful = sum(distribution.values())
    failed = total - successful
    success_rate = (successful / total) * 100 if total > 0 else 0
    
    print(f"\n📈 Estatísticas:")
    print(f"   • Total de requisições: {total}")
    print(f"   • Bem-sucedidas: {successful} ({success_rate:.1f}%)")
    print(f"   • Falhadas: {failed} ({100-success_rate:.1f}%)")
    print(f"   • Ondas executadas: {len(profile.waves)}")
    if profile.concurrent:
        print(f"   • Workers concorrentes: {profile.max_workers}")
    
    if errors:
        print_warning(f"\n⚠️  Erros encontrados: {len(errors)}")
        
        # Agrupar erros por tipo
        error_types = {}
        for error in errors:
            if "Timeout" in error:
                error_types["Timeout"] = error_types.get("Timeout", 0) + 1
            elif "HTTP 429" in error:
                error_types["HTTP 429 (Rate Limit)"] = error_types.get("HTTP 429 (Rate Limit)", 0) + 1
            elif "HTTP 502" in error:
                error_types["HTTP 502 (Bad Gateway)"] = error_types.get("HTTP 502 (Bad Gateway)", 0) + 1
            elif "HTTP 503" in error:
                error_types["HTTP 503 (Service Unavailable)"] = error_types.get("HTTP 503 (Service Unavailable)", 0) + 1
            elif "HTTP 504" in error:
                error_types["HTTP 504 (Gateway Timeout)"] = error_types.get("HTTP 504 (Gateway Timeout)", 0) + 1
            elif "HTTP" in error:
                # Outros códigos HTTP
                match = re.search(r'HTTP (\d+)', error)
                if match:
                    code = match.group(1)
                    error_types[f"HTTP {code}"] = error_types.get(f"HTTP {code}", 0) + 1
                else:
                    error_types["HTTP (outros)"] = error_types.get("HTTP (outros)", 0) + 1
            elif "Header X-Upstream-Server não encontrado" in error:
                error_types["Header não encontrado"] = error_types.get("Header não encontrado", 0) + 1
            elif "Erro de conexão" in error:
                error_types["Erro de conexão"] = error_types.get("Erro de conexão", 0) + 1
            else:
                error_types["Outros"] = error_types.get("Outros", 0) + 1
        
        for error_type, count in error_types.items():
            print(f"   • {error_type}: {count}")
        
        if len(errors) <= 5:
            print("\n   Detalhes:")
            for error in errors:
                print(f"   • {error}")
        else:
            print("\n   Primeiros erros:")
            for error in errors[:3]:
                print(f"   • {error}")
            print(f"   • ... e mais {len(errors) - 3} erros")
    
    # Avaliar qualidade do load balancing
    if len(distribution) > 1:
        values = list(distribution.values())
        max_count = max(values)
        min_count = min(values)
        variance = max_count - min_count
        variance_percentage = (variance / successful) * 100 if successful > 0 else 0
        
        print(f"\n🎯 Análise de distribuição:")
        print(f"   • Upstreams ativos: {len(distribution)}")
        print(f"   • Variação: {variance} requisições ({variance_percentage:.1f}%)")
        
        if variance_percentage < 10:
            print_success("   Load balancing está bem equilibrado! ✨")
        elif variance_percentage < 25:
            print_info("   Load balancing está razoavelmente equilibrado.")
        else:
            print_warning("   Load balancing está desbalanceado.")
    else:
        print_warning("\n⚠️  Apenas um upstream respondeu. Load balancing não pode ser avaliado.")


def main():
    """Função principal do script."""
    parser = argparse.ArgumentParser(
        description="Testa load balancing do nginx",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Exemplos de uso:
  # Modo interativo
  python tests/test_load_balancing.py
  
  # Modos predefinidos: quick, standard, stress, concurrent
  python tests/test_load_balancing.py -m quick
  python tests/test_load_balancing.py -m concurrent -w 50
  
  # Customizado (sequencial ou concurrent com -w)
  python tests/test_load_balancing.py -n 500
  python tests/test_load_balancing.py -n 1000 -w 30
  
  # URL customizada
  python tests/test_load_balancing.py -u http://staging:8080 -m stress
        """
    )
    
    parser.add_argument(
        '-u', '--url',
        default='http://localhost:8080/',
        help='URL para testar (padrão: http://localhost:8080/)'
    )
    
    parser.add_argument(
        '-m', '--mode',
        choices=['quick', 'standard', 'stress', 'concurrent'],
        help='Modo de teste (quick, standard, stress, concurrent). Se não especificado, será solicitado interativamente.'
    )
    
    parser.add_argument(
        '-n', '--requests',
        type=int,
        help='Número de requisições (sobrescreve o modo selecionado)'
    )
    
    parser.add_argument(
        '-w', '--workers',
        type=int,
        help='Número de workers concorrentes (apenas para modo concurrent)'
    )
    
    args = parser.parse_args()
    
    print_header("🔍 Testando Load Balancing do Nginx")
    
    # 1. Verificar se nginx está rodando
    print("\n1️⃣  Verificando serviço nginx...")
    is_running, error = check_nginx_running()
    
    if not is_running:
        print_error(f"Nginx não está rodando: {error}")
        print_info("Execute: docker compose up -d")
        sys.exit(1)
    
    print_success("Nginx está rodando e healthy")
    
    # 2. Obter upstreams configurados
    print("\n2️⃣  Obtendo upstreams configurados...")
    upstreams, error = get_configured_upstreams()
    
    if not upstreams:
        print_error(f"Não foi possível obter upstreams: {error}")
        print_info("Configure a variável UPSTREAMS no .env")
        sys.exit(1)
    
    print_success(f"Upstreams configurados: {', '.join(upstreams)}")
    
    if len(upstreams) == 1:
        print_warning("Apenas um upstream configurado. Load balancing não será testado.")
        print_info("Configure múltiplos upstreams para testar load balancing")
        print_info("Exemplo: UPSTREAMS=servico-a:80,servico-b:80")
    
    # 3. Selecionar modo de teste
    print("\n3️⃣  Configurando teste de load balancing...")
    
    # Determinar se deve usar modo concurrent
    use_concurrent = False
    workers = 10  # Padrão
    
    if args.mode == 'concurrent':
        use_concurrent = True
        workers = args.workers if args.workers else 20  # Padrão do modo concurrent
    elif args.workers:
        # Se --workers foi especificado sem modo, assumir concurrent
        use_concurrent = True
        workers = args.workers
        print_info(f"Opção --workers detectada: usando modo concorrente com {workers} workers")
    
    if args.requests:
        # Modo customizado com número específico de requisições
        if use_concurrent:
            profile = TestProfile(
                name="🔧 Custom Concurrent Test",
                description=f"Teste concorrente customizado com {args.requests} requisições ({workers} workers)",
                waves=[(args.requests, 0)],
                timeout=5,
                concurrent=True,
                max_workers=workers
            )
            print_info(f"Usando modo customizado CONCORRENTE: {args.requests} requisições com {workers} workers")
        else:
            profile = TestProfile(
                name="🔧 Custom Test",
                description=f"Teste customizado com {args.requests} requisições",
                waves=[(args.requests, 0)],
                timeout=5,
                concurrent=False
            )
            print_info(f"Usando modo customizado SEQUENCIAL: {args.requests} requisições")
    elif args.mode:
        # Modo especificado via argumento
        mode = TestMode(args.mode)
        profile = TEST_PROFILES[mode]
        
        # Se --requests foi especificado junto com --mode, sobrescrever número de requisições
        if args.requests:
            profile = TestProfile(
                name=profile.name,
                description=f"Teste {mode.value} com {args.requests} requisições customizadas",
                waves=[(args.requests, 0)],
                timeout=profile.timeout,
                concurrent=profile.concurrent,
                max_workers=workers if profile.concurrent else profile.max_workers
            )
        # Se --workers foi especificado e o modo é concurrent, sobrescrever max_workers
        elif args.workers and mode == TestMode.CONCURRENT:
            profile = TestProfile(
                name=profile.name,
                description=f"Teste concorrente com {profile.total_requests} requisições simultâneas ({args.workers} workers)",
                waves=profile.waves,
                timeout=profile.timeout,
                concurrent=True,
                max_workers=args.workers
            )
            print_info(f"Modo selecionado: {profile.name} com {args.workers} workers")
        else:
            print_info(f"Modo selecionado: {profile.name}")
            
        # Avisar se --workers foi usado com modo não-concurrent
        if args.workers and mode != TestMode.CONCURRENT:
            print_warning(f"Opção --workers ignorada (apenas válida para modo concurrent)")
    else:
        # Modo interativo
        profile = select_test_mode()
    
    # 4. Executar teste
    distribution, total, errors = run_test_profile(args.url, profile)
    
    if not distribution and errors:
        print_error("\n❌ Todas as requisições falharam!")
        for error in errors[:5]:
            print(f"   • {error}")
        sys.exit(1)
    
    # 5. Analisar e reportar
    print_detailed_report(distribution, total, errors, profile)
    
    # Verificar se header está presente
    if not distribution:
        print_warning("\n⚠️  Header X-Upstream-Server não foi encontrado nas respostas!")
        print_info("Verifique se o nginx.conf.template contém:")
        print_info("   add_header X-Upstream-Server $upstream_addr always;")
        print_info("E que ENVIRONMENT=development no .env")
        sys.exit(1)
    
    print_success("\n✅ Teste de load balancing concluído!")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️  Teste interrompido pelo usuário")
        sys.exit(130)
    except Exception as e:
        print_error(f"\n❌ Erro inesperado: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
