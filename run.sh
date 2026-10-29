#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v mvn >/dev/null 2>&1; then
    echo "Для запуска установите Maven и JDK 17 или новее." >&2
    exit 1
fi
mvn -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime >&2
exec java -cp "target/classes:target/dependency/*" dev.npmvisualizer.Main "$@"
