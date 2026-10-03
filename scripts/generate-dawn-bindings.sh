#!/usr/bin/env bash
# Build the pinned kextract distribution and regenerate the raw Dawn bindings.
#
# The kextract checkout lives in a dedicated, git-ignored directory; no user
# checkout is ever touched. The tool is built with an explicit, validated
# LLVM home (default: `brew --prefix llvm`, override with LLVM_HOME or
# --llvm-home). The generated output lands in dawn4k-native/build/regenerated/src
# for review; promotion into dawn4k-native/generated/src is a manual step.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd -P)"
version_file="$root/bindings/kextract.version"
commit="$(tr -d '[:space:]' < "$version_file")"
[ -n "$commit" ] || { echo "empty $version_file" >&2; exit 2; }

llvm_home="${LLVM_HOME:-}"
if [ "${1:-}" = "--llvm-home" ]; then
    llvm_home="${2:-}"
    shift 2 || true
fi
if [ -z "$llvm_home" ]; then
    llvm_home="$(brew --prefix llvm 2>/dev/null || true)"
fi
if [ -z "$llvm_home" ] || [ ! -d "$llvm_home/lib/clang" ]; then
    echo "kextract needs LLVM with lib/clang; pass --llvm-home or set LLVM_HOME" >&2
    exit 2
fi

tool_root="$root/dawn4k-native/build/tooling/kextract"
checkout="$tool_root/checkout"
mkdir -p "$tool_root"

if [ ! -d "$checkout/.git" ]; then
    git clone --filter=blob:none https://github.com/klang-toolkit/kextract.git "$checkout"
fi
# The pinned commit is fetched from upstream first; if it is not yet on upstream
# (for example an unmerged PR branch), fall back to the fork.
if ! git -C "$checkout" remote get-url fork >/dev/null 2>&1; then
    git -C "$checkout" remote add fork https://github.com/ygdrasil-io/kextract.git
fi
if ! git -C "$checkout" fetch --quiet origin "$commit" 2>/dev/null; then
    git -C "$checkout" fetch --quiet fork "$commit"
fi
git -C "$checkout" checkout --quiet --detach "$commit"
if [ "$(git -C "$checkout" rev-parse HEAD)" != "$commit" ]; then
    echo "kextract checkout is not at pinned commit $commit" >&2
    exit 1
fi

echo "Building kextract $commit with LLVM at $llvm_home"
"$checkout/gradlew" -p "$checkout" createKextractImage -Pllvm_home="$llvm_home"

launcher="$checkout/build/kextract/bin/kextract"
[ -x "$launcher" ] || { echo "kextract image not produced at $launcher" >&2; exit 1; }

echo "Regenerating Dawn bindings"
"$root/gradlew" -p "$root" :dawn4k-native:generateBindingsFromHeader

echo "Review $root/dawn4k-native/build/regenerated/src, then promote it into $root/dawn4k-native/generated/src."
