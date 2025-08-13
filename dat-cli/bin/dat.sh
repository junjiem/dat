#!/bin/bash

# Set the LANG and LC_ALL environment variables to UTF-8 encoding
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

CURR_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v java &> /dev/null; then
    echo "❌ Error: Java was not found. Please install Java 17 or a higher version first"
    exit 1
fi

cd "$CURR_DIR"

java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar $CURR_DIR/dat-cli-*.jar "$@"
