#!/usr/bin/env bash
set -euo pipefail
# Only mount roots are touched; never change permissions on source directories.
for directory in /home/demo/.gradle /workspace/.gradle /workspace/.kotlin \
    /workspace/build /workspace/buildSrc/.gradle /workspace/buildSrc/build \
    /workspace/dawn4k-native/build /workspace/dawn4k/build \
    /workspace/dawn4k-demo/build /workspace/docs/build; do
    mkdir -p "$directory"
    chown demo:demo "$directory"
done
install -d -m 0700 -o demo -g demo /run/user/1000
exec gosu demo python3 /opt/demo/start-desktop.py
