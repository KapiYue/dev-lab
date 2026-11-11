#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

JAVA_HOME_17="${JAVA_HOME_17:-$(/usr/libexec/java_home -v 17 2>/dev/null || true)}"
if [[ ! -x "$JAVA_HOME_17/bin/java" ]]; then
  echo "未找到项目要求的 Java 17，请先在 macOS 安装 JDK 17。" >&2
  exit 1
fi

start_container() {
  local name="$1"
  local running
  if ! docker inspect "$name" >/dev/null 2>&1; then
    echo "找不到 Docker 容器 $name，请先按项目文档创建它。" >&2
    exit 1
  fi
  running="$(docker inspect -f '{{.State.Running}}' "$name")"
  if [[ "$running" != "true" ]]; then
    echo "正在启动 Docker 容器 $name..."
    docker start "$name" >/dev/null
  fi
}

start_container cxnp-mysql
start_container cxnp-redis

MYSQL_ROOT_PASSWORD="$(docker exec cxnp-mysql printenv MYSQL_ROOT_PASSWORD 2>/dev/null || true)"
if [[ -z "$MYSQL_ROOT_PASSWORD" ]]; then
  echo "无法从 cxnp-mysql 读取 MySQL 密码；未写入或显示任何密码。" >&2
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_17"
export PATH="$JAVA_HOME/bin:$PATH"
export SPRING_PROFILES_ACTIVE="dev"
export DB_URL="${DB_URL:-jdbc:mysql://127.0.0.1:3306/cxnp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}"
export DB_USERNAME="${DB_USERNAME:-root}"
export DB_PASSWORD="$MYSQL_ROOT_PASSWORD"

echo "Java: $($JAVA_HOME/bin/java -version 2>&1 | head -n 1)"
echo "Spring Profile: $SPRING_PROFILES_ACTIVE"
echo "数据库: $DB_USERNAME@$DB_URL"
echo "正在启动 Spring Boot，按 Ctrl-C 停止。"
exec ./mvnw spring-boot:run
