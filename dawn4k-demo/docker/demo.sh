#!/usr/bin/env bash
set -euo pipefail
if [ "$(id -u)" = 0 ]; then
    exec gosu demo "$0" "$@"
fi
exec python3 /opt/demo/launch-demo.py "$@"
