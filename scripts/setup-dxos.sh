#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
RELEASE_FILE="$ROOT_DIR/scripts/dxos-release.env"
MODEL_FILE="$ROOT_DIR/dxos.yaml"
LOCAL_DIR="$ROOT_DIR/.local"
DXOS_HOME="$LOCAL_DIR/dxos"
CACHE_DIR="$LOCAL_DIR/cache/dxos"

if [[ -n "${MSYSTEM:-}" ]]; then
  export MSYS_NO_PATHCONV=1
fi

die() {
  echo "setup-dxos: $*" >&2
  exit 1
}

load_env_file() {
  local file="$1" raw_key raw_value key value
  [[ -f "$file" ]] || die "missing configuration file: $file"
  while IFS='=' read -r raw_key raw_value || [[ -n "$raw_key" ]]; do
    key="${raw_key%$'\r'}"
    value="${raw_value%$'\r'}"
    [[ -z "$key" || "$key" == \#* ]] && continue
    [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || die "invalid key in $file: $key"
    printf -v "$key" '%s' "$value"
    export "$key"
  done <"$file"
}

set_env_value() {
  local file="$1" key="$2" value="$3" temp
  temp="$(mktemp)"
  awk -v key="$key" -v value="$value" '
    BEGIN { found = 0 }
    index($0, key "=") == 1 { print key "=" value; found = 1; next }
    { print }
    END { if (!found) print key "=" value }
  ' "$file" >"$temp"
  mv "$temp" "$file"
}

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print tolower($1)}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print tolower($1)}'
  else
    die "sha256sum or shasum is required"
  fi
}

[[ -f "$ENV_FILE" ]] || die "missing .env; copy .env.dxos.example to .env and fill JWT_SECRET first"
load_env_file "$ENV_FILE"
load_env_file "$RELEASE_FILE"

DXOS_ACCEPT_LEVEL="${DXOS_ACCEPT_LEVEL:-SAFE}"
DXOS_APP_NAME="${DXOS_APP_NAME:-}"
DXOS_HOST_PORT="${DXOS_HOST_PORT:-19080}"
DXOS_POSTGRES_HOST_PORT="${DXOS_POSTGRES_HOST_PORT:-127.0.0.1:55432}"
DXOS_MODEL="${DXOS_MODEL:-}"
DXOS_ENTITY="${DXOS_ENTITY:-}"
TTCSN_COMPOSE_PROJECT="${TTCSN_COMPOSE_PROJECT:-$DXOS_APP_NAME-app}"

[[ -n "${JWT_SECRET:-}" ]] || die "JWT_SECRET is empty in .env"
[[ -n "${MYSQL_PASSWORD:-}" ]] || die "MYSQL_PASSWORD is empty in .env"
[[ -n "${MYSQL_ROOT_PASSWORD:-}" ]] || die "MYSQL_ROOT_PASSWORD is empty in .env"
[[ "${APPROVAL_PROVIDER:-}" == "dxos" ]] || die "APPROVAL_PROVIDER must be dxos"
[[ -n "$DXOS_MODEL" ]] || die "DXOS_MODEL is empty in .env"
[[ -n "$DXOS_ENTITY" ]] || die "DXOS_ENTITY is empty in .env"
[[ "$DXOS_MODEL" =~ ^[a-z][a-z0-9_]*$ ]] || die "DXOS_MODEL has an invalid name"
[[ "$DXOS_ENTITY" =~ ^[a-z][a-z0-9_]*$ ]] || die "DXOS_ENTITY has an invalid name"
[[ "$DXOS_MODEL" != replace_with_customer_model ]] || die "replace the example DXOS_MODEL with the customer model name"
[[ "$DXOS_ENTITY" != replace_with_customer_entity ]] || die "replace the example DXOS_ENTITY with the customer entity name"
[[ "$DXOS_ACCEPT_LEVEL" =~ ^(SAFE|MIGRATION|LOSS)$ ]] \
  || die "DXOS_ACCEPT_LEVEL must be SAFE, MIGRATION, or LOSS"
[[ "$DXOS_APP_NAME" =~ ^[a-z][a-z0-9_-]{0,29}$ ]] \
  || die "DXOS_APP_NAME must use lower-case letters, digits, - or _ and be at most 30 characters"
[[ "$DXOS_HOST_PORT" =~ ^[0-9]{2,5}$ ]] || die "DXOS_HOST_PORT must be a TCP port"
(( DXOS_HOST_PORT <= 65535 )) || die "DXOS_HOST_PORT must be at most 65535"
[[ -f "$MODEL_FILE" ]] || die "model file not found: $MODEL_FILE"

declared_model="$(sed -n 's/^name:[[:space:]]*//p' "$MODEL_FILE" | head -n 1 | tr -d '\r')"
[[ "$declared_model" == "$DXOS_MODEL" ]] \
  || die "DXOS_MODEL ($DXOS_MODEL) does not match model name ($declared_model)"
awk -v entity="$DXOS_ENTITY" '
  /^entities:[[:space:]]*$/ { in_entities = 1; next }
  in_entities && /^[^[:space:]]/ { exit }
  in_entities && $0 == "  " entity ":" { found = 1 }
  END { exit(found ? 0 : 1) }
' "$MODEL_FILE" || die "DXOS_ENTITY ($DXOS_ENTITY) is not declared in dxos.yaml"

command -v docker >/dev/null 2>&1 || die "Docker is not available"
docker info >/dev/null 2>&1 || die "Docker Engine is not running"
docker compose version >/dev/null 2>&1 || die "Docker Compose v2 is required"
command -v curl >/dev/null 2>&1 || die "curl is required"
command -v tar >/dev/null 2>&1 || die "tar is required"

archive_name="dx-os-${DXOS_RELEASE_VERSION#v}.tar.gz"
archive="$CACHE_DIR/$archive_name"

download_release() {
  local temp curl_output actual
  mkdir -p "$CACHE_DIR"
  if [[ -f "$archive" ]]; then
    actual="$(sha256_file "$archive")"
    if [[ "$actual" == "$DXOS_RELEASE_SHA256" ]]; then
      echo "Using cached DX-OS $DXOS_RELEASE_VERSION release."
      return
    fi
  fi

  temp="$(mktemp "$CACHE_DIR/$archive_name.download.XXXXXX")"
  curl_output="$temp"
  if [[ -n "${MSYSTEM:-}" ]] && command -v cygpath >/dev/null 2>&1; then
    curl_output="$(cygpath -w "$temp")"
  fi
  echo "Downloading DX-OS $DXOS_RELEASE_VERSION..."
  curl -fL --progress-bar -o "$curl_output" "$DXOS_RELEASE_URL" \
    || { rm -f "$temp"; die "cannot download $DXOS_RELEASE_URL"; }
  actual="$(sha256_file "$temp")"
  if [[ "$actual" != "$DXOS_RELEASE_SHA256" ]]; then
    rm -f "$temp"
    die "DX-OS release checksum mismatch"
  fi
  mv "$temp" "$archive"
}

install_release() {
  local installed="" work source install_args=()
  if [[ -f "$DXOS_HOME/deploy/.env" ]]; then
    installed="$(grep -E '^DXOS_IMAGE_TAG=' "$DXOS_HOME/deploy/.env" | tail -n 1 | cut -d= -f2- || true)"
  fi
  if [[ "$installed" == "$DXOS_RELEASE_VERSION" && -x "$DXOS_HOME/deploy/dxos" ]]; then
    echo "DX-OS $DXOS_RELEASE_VERSION deployment is already installed."
    return
  fi
  if [[ -f "$DXOS_HOME/deploy/.env" && -z "$installed" ]]; then
    die "the existing DX-OS deployment has no version marker: $DXOS_HOME"
  fi

  download_release
  work="$(mktemp -d "$CACHE_DIR/extract.XXXXXX")"
  tar -xzf "$archive" -C "$work"
  source="$work/dx-os-${DXOS_RELEASE_VERSION#v}"
  [[ -f "$source/deploy/install.sh" ]] || die "release archive has no deployment installer"
  [[ -n "$installed" ]] && install_args=(--upgrade)
  mkdir -p "$LOCAL_DIR/bin"
  DXOS_BIN_DIR="$LOCAL_DIR/bin" bash "$source/deploy/install.sh" \
    "${install_args[@]}" "$DXOS_HOME" "$DXOS_RELEASE_VERSION"
  case "$work" in
    "$CACHE_DIR"/extract.*) rm -rf -- "$work" ;;
    *) die "refusing to remove unexpected temporary directory: $work" ;;
  esac
}

provision_client() {
  local name="$1" role="$2" id_key="$3" secret_key="$4"
  local current_id="${!id_key:-}" current_secret="${!secret_key:-}"
  local output client_id client_secret

  if [[ -n "$current_id" && -n "$current_secret" ]]; then
    echo "Using existing DX-OS client for $role."
    return
  fi
  [[ -z "$current_id" && -z "$current_secret" ]] \
    || die "$id_key and $secret_key must either both be set or both be empty"

  if ! output="$("$DXOS_COMMAND" client add "$name" --model "$DXOS_MODEL" --role "$role" 2>&1)"; then
    echo "$output" >&2
    die "cannot provision $role client; if it already exists, rotate it and put its new credentials in .env"
  fi

  client_id="$(sed -n 's/^[[:space:]]*client id[[:space:]]*//p' <<<"$output" | tail -n 1)"
  client_secret="$(sed -n 's/^[[:space:]]*secret[[:space:]]*//p' <<<"$output" | tail -n 1)"
  [[ -n "$client_id" && -n "$client_secret" ]] \
    || die "DX-OS created the client but returned an unreadable credential response"

  set_env_value "$ENV_FILE" "$id_key" "$client_id"
  set_env_value "$ENV_FILE" "$secret_key" "$client_secret"
  printf -v "$id_key" '%s' "$client_id"
  printf -v "$secret_key" '%s' "$client_secret"
  export "$id_key" "$secret_key"
  echo "Provisioned DX-OS client for $role."
}

install_release
DXOS_COMMAND="$DXOS_HOME/deploy/dxos"
DXOS_DEPLOY_ENV="$DXOS_HOME/deploy/.env"
[[ -x "$DXOS_COMMAND" && -f "$DXOS_DEPLOY_ENV" ]] || die "DX-OS deployment is incomplete"

set_env_value "$DXOS_DEPLOY_ENV" COMPOSE_PROJECT_NAME "$DXOS_APP_NAME-dxos"
set_env_value "$DXOS_DEPLOY_ENV" APISIX_HOST_PORT "$DXOS_HOST_PORT"
set_env_value "$DXOS_DEPLOY_ENV" POSTGRES_HOST_PORT "$DXOS_POSTGRES_HOST_PORT"
set_env_value "$DXOS_DEPLOY_ENV" PUBLIC_HOST localhost
set_env_value "$DXOS_DEPLOY_ENV" GATEWAY_PUBLIC_URL "http://localhost:$DXOS_HOST_PORT"
set_env_value "$ENV_FILE" DXOS_BASE_URL "http://host.docker.internal:$DXOS_HOST_PORT"
export DXOS_BASE_URL="http://host.docker.internal:$DXOS_HOST_PORT"

echo "Starting the complete DX-OS platform stack..."
(
  cd "$DXOS_HOME/deploy"
  docker compose up -d --no-build --pull missing --wait --wait-timeout 600
)
"$DXOS_COMMAND" apply "$MODEL_FILE" --accept "$DXOS_ACCEPT_LEVEL"

provision_client "$DXOS_APP_NAME-requester" "REQUESTER" \
  DXOS_REQUESTER_CLIENT_ID DXOS_REQUESTER_CLIENT_SECRET
provision_client "$DXOS_APP_NAME-approver" "APPROVER" \
  DXOS_APPROVER_CLIENT_ID DXOS_APPROVER_CLIENT_SECRET

echo "Starting TTCSN..."
cd "$ROOT_DIR"
docker compose --env-file .env -p "$TTCSN_COMPOSE_PROJECT" up -d --build --wait

echo "TTCSN is ready at http://localhost:${FRONTEND_HOST_PORT:-3000}"
echo "DX-OS $DXOS_RELEASE_VERSION is ready at http://localhost:$DXOS_HOST_PORT"
