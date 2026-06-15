---
id: adr-001
status: Aceita
links:
  - spec/adrs/index.md
  - spec/specs/spec-003-entry-point-with-ngrok-and-nginx.md
  - spec/tech-plans/plan-003-entry-point-with-ngrok-and-nginx.md
---
# ADR-001 - Usar ngrok como API Gateway e Nginx como Reverse Proxy e Load Balancer

## Status
Aceita

## Contexto
Precisamos de um ponto de entrada publico para testes locais que simule um ambiente de producao, antes da existencia da API Java. E necessario expor HTTPS publicamente e encaminhar o trafego para um reverse proxy interno com balanceamento.

## Decisao
Adotar o ngrok como API Gateway publico (terminacao TLS e exposicao HTTPS) e o Nginx como Reverse Proxy e Load Balancer interno. O ngrok encaminha HTTP para o Nginx, que distribui para instancias da Backend API definidas por variaveis de ambiente.

## Consequencias
Positivas:
- Prove um endpoint HTTPS publico sem infraestrutura externa.
- Simplifica testes locais com comportamento proximo de producao.
- Mantem o Nginx como ponto unico de balanceamento interno.

Negativas:
- Dependencia do ngrok e de token de autenticacao.
- Trafego interno sem TLS entre ngrok e Nginx.
