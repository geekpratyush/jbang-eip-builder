#!/usr/bin/env bash
# Determine script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JBANG_EXE="$SCRIPT_DIR/route-builder/jbang"

if [ ! -f "$JBANG_EXE" ]; then
    JBANG_EXE="$SCRIPT_DIR/jbang"
fi

if [ ! -f "$JBANG_EXE" ]; then
    echo "Error: Could not find JBang wrapper script in route-builder/jbang or ./jbang"
    exit 1
fi

echo "Pre-caching Camel JBang CLI locally..."
echo "Executing: $JBANG_EXE --main=main.CamelJBang camel --help"
"$JBANG_EXE" --main=main.CamelJBang camel --help

echo "Camel dependencies pre-cached successfully!"
