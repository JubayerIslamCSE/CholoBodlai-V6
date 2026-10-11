#!/bin/sh
# Safely reuses the Atlas connection URI from your previous working IntelliJ project.
# Nothing is uploaded or printed. The generated file is gitignored.
set -eu
OLD_DIR="${1:-$HOME/IdeaProjects/CholoBodlai}"
ORIGINAL="$OLD_DIR/src/main/resources/application.properties"
DEST="$(dirname "$0")/src/main/resources/application-local.properties"
if [ ! -f "$ORIGINAL" ]; then
  printf 'Cannot find old application.properties at %s\n' "$ORIGINAL" >&2
  printf 'Usage: ./setup-from-existing.sh /path/to/OLD/CholoBodlai\n' >&2
  exit 1
fi
if [ -e "$DEST" ]; then
  printf 'Your application-local.properties already exists; refusing to overwrite it.\n' >&2
  exit 1
fi
umask 077
awk '/^spring\.mongodb\.uri=/{print; found=1; exit} END {if(!found) exit 2}' "$ORIGINAL" > "$DEST" || {
  rm -f "$DEST"
  echo 'Original properties file has no spring.mongodb.uri value.' >&2
  exit 1
}
chmod 600 "$DEST"
printf 'Local MongoDB configuration copied securely. Open the new project in IntelliJ.\n'
