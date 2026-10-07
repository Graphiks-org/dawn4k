#!/usr/bin/env bash
set -euo pipefail
module=$(cd "$(dirname "$0")/.." && pwd)
cd "$module"
CC=${CC:-gcc}
target=${DAWN_WAYLAND_TARGET:-$(case $(uname -m) in aarch64) echo linuxArm64;; x86_64) echo linuxX64;; *) exit 1;; esac)}
output="$module/build/wayland/$target"
generated="$output/generated"
protocol=${WAYLAND_PROTOCOL_XML:-/usr/share/wayland-protocols/stable/xdg-shell/xdg-shell.xml}
mkdir -p "$generated"
wayland-scanner client-header "$protocol" "$generated/xdg-shell-client-protocol.h"
wayland-scanner private-code "$protocol" "$generated/xdg-shell-protocol.c"
read -r -a flags <<< "$(pkg-config --cflags --libs wayland-client xkbcommon)"
read -r -a compiler_flags <<< "${CFLAGS:-}"
"$CC" -std=c11 -fPIC -shared -Wall -Wextra -Werror -Wl,-z,defs \
  "${compiler_flags[@]}" \
  -I"$generated" -Isrc/main/c src/main/c/wayland-host.c \
  "$generated/xdg-shell-protocol.c" "${flags[@]}" -o "$output/libdawn4k_wayland.so"
if [[ ${1:-} == --test ]]; then
  "$CC" -std=c11 -Wall -Wextra -Werror -Isrc/main/c \
    src/test/c/wayland-host-abi.c -o "$output/abi"
  "$output/abi"
  "$CC" -std=c11 -Wall -Wextra -Werror -fsanitize=address,undefined -g \
    -I"$generated" -Isrc/main/c src/test/c/wayland-host-test.c \
    src/main/c/wayland-host.c "$generated/xdg-shell-protocol.c" "${flags[@]}" \
    $(pkg-config --cflags --libs wayland-server) -pthread -o "$output/test"
  "$output/test"
fi
