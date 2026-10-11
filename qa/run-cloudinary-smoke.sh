#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -d "$OUT" "$ROOT/src/main/java/com/jubayer/cholobodlai/Service/CloudinaryMediaGateway.java" "$ROOT/qa/CloudinarySmoke.java"
java -cp "$OUT" com.jubayer.cholobodlai.Service.CloudinarySmoke
