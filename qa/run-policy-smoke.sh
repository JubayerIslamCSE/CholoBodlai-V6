#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  src/main/java/com/jubayer/cholobodlai/EXCEPTION/BusinessRuleException.java \
  src/main/java/com/jubayer/cholobodlai/Model/Emums/Role.java \
  src/main/java/com/jubayer/cholobodlai/Service/InputPolicy.java \
  src/main/java/com/jubayer/cholobodlai/Service/MarketplaceRules.java \
  qa/V5BusinessPolicySmoke.java
java -cp "$out" V5BusinessPolicySmoke
