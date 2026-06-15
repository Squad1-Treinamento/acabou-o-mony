#!/bin/sh
set -e

fail() {
  echo "$1" >&2
  exit 1
}

: "${NGINX_TEMPLATE:=/etc/nginx/nginx.conf.template}"
: "${NGINX_CONF:=/etc/nginx/conf.d/default.conf}"

if [ -z "${UPSTREAMS}" ]; then
  if [ -n "${UPSTREAM_HOST}" ] && [ -n "${UPSTREAM_PORT}" ]; then
    UPSTREAMS="${UPSTREAM_HOST}:${UPSTREAM_PORT}"
  else
    fail "UPSTREAMS is required (or UPSTREAM_HOST and UPSTREAM_PORT)"
  fi
fi

UPSTREAM_SERVERS=""
OLD_IFS=$IFS
IFS=','
for item in $UPSTREAMS; do
  IFS=$OLD_IFS
  if [ -z "$item" ]; then
    fail "UPSTREAMS contains an empty entry"
  fi
  case "$item" in
    *:*)
      host=${item%:*}
      port=${item##*:}
      ;;
    *)
      fail "UPSTREAMS entry must be host:port: $item"
      ;;
  esac

  if [ -z "$host" ] || [ -z "$port" ]; then
    fail "UPSTREAMS entry must be host:port: $item"
  fi

  case "$host" in
    *[!A-Za-z0-9.-]*)
      fail "UPSTREAMS host has invalid characters: $host"
      ;;
  esac

  case "$port" in
    *[!0-9]*|"")
      fail "UPSTREAMS port must be numeric: $port"
      ;;
  esac

  if [ -z "$UPSTREAM_SERVERS" ]; then
    UPSTREAM_SERVERS="server ${item};"
  else
    UPSTREAM_SERVERS="$UPSTREAM_SERVERS
    server ${item};"
  fi
done
IFS=$OLD_IFS

if ! command -v envsubst >/dev/null 2>&1; then
  apk add --no-cache gettext >/dev/null 2>&1
fi

# Determine environment-specific configurations
# ENVIRONMENT can be: production, staging, development (default: production)
: "${ENVIRONMENT:=production}"

if [ "$ENVIRONMENT" = "production" ]; then
  DEBUG_HEADER=""
  RATE_LIMIT="10r/s"
  RATE_BURST="20"
  echo "INFO: Running in PRODUCTION mode"
  echo "  - Debug headers: disabled"
  echo "  - Rate limit: ${RATE_LIMIT} (burst: ${RATE_BURST})"
elif [ "$ENVIRONMENT" = "staging" ]; then
  DEBUG_HEADER=""
  RATE_LIMIT="50r/s"
  RATE_BURST="50"
  echo "INFO: Running in STAGING mode"
  echo "  - Debug headers: disabled"
  echo "  - Rate limit: ${RATE_LIMIT} (burst: ${RATE_BURST})"
else
  DEBUG_HEADER="add_header X-Upstream-Server \$upstream_addr always;"
  RATE_LIMIT="200r/s"
  RATE_BURST="200"
  echo "INFO: Running in DEVELOPMENT mode"
  echo "  - Debug headers: enabled"
  echo "  - Rate limit: ${RATE_LIMIT} (burst: ${RATE_BURST})"
fi

export UPSTREAM_SERVERS
export DEBUG_HEADER
export RATE_LIMIT
export RATE_BURST
envsubst '${UPSTREAM_SERVERS} ${DEBUG_HEADER} ${RATE_LIMIT} ${RATE_BURST}' < "$NGINX_TEMPLATE" > "$NGINX_CONF"

exec nginx -g "daemon off;"
