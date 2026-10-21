#!/usr/bin/env bash
# Prints "<versionName> <versionCode>" for a release tag such as v1.4.2 or v1.4.2-rc1.
#
#   versionName  the tag without its leading "v" (1.4.2, 1.4.2-rc1)
#   versionCode  major * 10000 + minor * 100 + patch (v1.4.2 -> 10402)
#
# The code only ever grows when the version does, so each release is an upgrade for Android.
# Minor and patch must stay below 100. A pre-release shares the code of its final version.
set -euo pipefail

tag="${1:?usage: version-from-tag.sh <tag>}"
pattern='^v([0-9]+)\.([0-9]+)\.([0-9]+)(-[0-9A-Za-z.-]+)?$'

if [[ ! $tag =~ $pattern ]]; then
  echo "Tag '$tag' must look like v1.2.3 or v1.2.3-rc1" >&2
  exit 1
fi

# 10# keeps numbers such as 08 from being read as octal.
major=$((10#${BASH_REMATCH[1]}))
minor=$((10#${BASH_REMATCH[2]}))
patch=$((10#${BASH_REMATCH[3]}))

if (( minor > 99 || patch > 99 )); then
  echo "Tag '$tag': minor and patch must be 99 or less" >&2
  exit 1
fi

echo "${tag#v} $(( major * 10000 + minor * 100 + patch ))"
