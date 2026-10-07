#!/usr/bin/env bash
set -euo pipefail
module=$(cd "$(dirname "$0")/.." && pwd)
sysroot=${DAWN_WAYLAND_X64_SYSROOT:-/opt/demo/wayland-sysroot}
test -f "$sysroot/usr/lib/x86_64-linux-gnu/pkgconfig/wayland-client.pc"
export CC=x86_64-linux-gnu-gcc
export CFLAGS="--sysroot=$sysroot"
export DAWN_WAYLAND_TARGET=linuxX64
export PKG_CONFIG_SYSROOT_DIR="$sysroot"
export PKG_CONFIG_LIBDIR="$sysroot/usr/lib/x86_64-linux-gnu/pkgconfig:$sysroot/usr/share/pkgconfig"
exec bash "$module/scripts/build-wayland-bridge.sh"
