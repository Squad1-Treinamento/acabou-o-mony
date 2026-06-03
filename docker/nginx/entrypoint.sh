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

export UPSTREAM_SERVERS
envsubst '${UPSTREAM_SERVERS}' < "$NGINX_TEMPLATE" > "$NGINX_CONF"

exec nginx -g "daemon off;"
