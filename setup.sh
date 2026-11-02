#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
setup_java=java
if [[ -n ${JAVA_HOME:-} ]]; then setup_java="$JAVA_HOME/bin/java"; fi
exec "$setup_java" tools/Setup.java "$@"
