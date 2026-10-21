#!/usr/bin/env bash
# Checks version-from-tag.sh. Run from anywhere: bash scripts/test-version-from-tag.sh
set -uo pipefail

script="$(dirname "${BASH_SOURCE[0]}")/version-from-tag.sh"
failures=0

expect() {
  local tag="$1" want="$2" got
  got="$(bash "$script" "$tag" 2>/dev/null)" || got="error"
  if [[ "$got" == "$want" ]]; then
    echo "ok    $tag -> $want"
  else
    echo "FAIL  $tag -> got '$got', wanted '$want'"
    failures=$((failures + 1))
  fi
}

expect v1.0.0      "1.0.0 10000"
expect v1.4.2      "1.4.2 10402"
expect v0.1.0      "0.1.0 100"
expect v2.10.15    "2.10.15 21015"
expect v1.4.2-rc1  "1.4.2-rc1 10402"
expect v01.08.09   "01.08.09 10809"
expect v3.99.99    "3.99.99 39999"

# These must be rejected.
expect main        error
expect 1.2.3       error
expect v1.2        error
expect v1.2.3.4    error
expect v1.100.0    error
expect v1.0.100    error
expect "v1.2.3 x"  error
expect ""          error

if (( failures > 0 )); then
  echo "$failures check(s) failed"
  exit 1
fi
echo "all checks passed"
