import { ApiKeyForm } from "@/components/dashboard/ApiKeyForm";

export default function LoginPage() {
  return (
    <main className="min-h-screen flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-bold text-text-primary">Acabou o Mony</h1>
          <p className="text-sm text-text-secondary mt-2">
            Insira sua chave de API para acessar o painel
          </p>
        </div>
        <ApiKeyForm />
      </div>
    </main>
  );
}
