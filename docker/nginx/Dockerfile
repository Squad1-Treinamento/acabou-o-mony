FROM nginx:alpine

# Instalar gettext para envsubst
RUN apk add --no-cache gettext

# Copiar scripts e templates
COPY entrypoint.sh /docker-entrypoint.d/40-generate-config.sh
COPY nginx.conf.template /etc/nginx/nginx.conf.template

# Dar permissão de execução
RUN chmod +x /docker-entrypoint.d/40-generate-config.sh

# Usar o entrypoint padrão do nginx
