#!/bin/bash
# Stop infrastructure and clean up ports

# Change to the script's directory
cd "$(dirname "$0")"

echo "=== Stopping Docker Containers & Volumes ==="
./docker-compose down -v || true

echo "=== Cleaning Up Remaining Port Clashes ==="
PORTS=(1414 2181 9094 19093 27017 88 749)

for port in "${PORTS[@]}"; do
    echo "Checking port $port..."
    PID=$(lsof -t -i :$port || true)
    if [ -n "$PID" ]; then
        echo "Found process $PID running on port $port. Terminating..."
        kill -9 $PID || true
    else
        echo "Port $port is free."
    fi
done

echo "=== Clean Up Complete! ==="
