#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

JAVA_HOME_17="${JAVA_HOME_17:-$(/usr/libexec/java_home -v 17 2>/dev/null || true)}"
if [[ ! -x "$JAVA_HOME_17/bin/java" ]]; then
  echo "未找到项目要求的 Java 17，请先在 macOS 安装 JDK 17。" >&2
  exit 1
fi

if ! docker inspect -f '{{.State.Running}}' cxnp-mysql 2>/dev/null | grep -q '^true$'; then
  echo "Docker 容器 cxnp-mysql 未运行，请先启动本地 MySQL。" >&2
  exit 1
fi

MYSQL_ROOT_PASSWORD="$(docker exec cxnp-mysql printenv MYSQL_ROOT_PASSWORD 2>/dev/null || true)"
if [[ -z "$MYSQL_ROOT_PASSWORD" ]]; then
  echo "无法从 cxnp-mysql 读取 MySQL 密码。" >&2
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_17"
export PATH="$JAVA_HOME/bin:$PATH"
export DB_USERNAME="${DB_USERNAME:-root}"
export DB_PASSWORD="$MYSQL_ROOT_PASSWORD"

TEST_CLASS="${1:-UserRoleServiceTest}"
exec mvn "-Dtest=$TEST_CLASS" test
