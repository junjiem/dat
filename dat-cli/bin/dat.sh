#!/bin/bash

CURR_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v java &> /dev/null; then
    echo "❌ Error: Java was not found. Please install Java 17 or a higher version first"
    exit 1
fi

cd "$CURR_DIR"

java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar $CURR_DIR/dat-cli-*.jar "$@"
