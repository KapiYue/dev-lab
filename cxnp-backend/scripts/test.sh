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
export SPRING_PROFILES_ACTIVE="test"
export DB_USERNAME="${DB_USERNAME:-root}"
export DB_PASSWORD="$MYSQL_ROOT_PASSWORD"
export DB_URL="${TEST_DB_URL:-jdbc:mysql://127.0.0.1:3306/cxnp_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&connectTimeout=3000&socketTimeout=5000&sessionVariables=lock_wait_timeout=5,innodb_lock_wait_timeout=5}"

if [[ ! "$DB_URL" =~ ^jdbc:mysql://[^/?]+(:[0-9]+)?/cxnp_test(\?.*)?$ ]]; then
  echo "测试数据库 URL 必须指向 cxnp_test，拒绝执行：$DB_URL" >&2
  exit 1
fi

if ! docker exec cxnp-mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '\''cxnp_test'\''"' \
  | grep -qx 'cxnp_test'; then
  echo "测试数据库 cxnp_test 不存在；为保护开发数据库，拒绝执行测试。" >&2
  exit 1
fi

TEST_CLASS="${1:-UserRoleServiceTest}"
exec ./mvnw "-Dtest=$TEST_CLASS" test
