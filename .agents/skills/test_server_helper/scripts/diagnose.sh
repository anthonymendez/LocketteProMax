#!/bin/bash

# ANSI Color Codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== LocketteProMax Dev Server Diagnostics ===${NC}"

# Check port 25565 and 25566
echo -e "\n1. Checking Port Status..."
for port in 25565 25566; do
    PID=$(lsof -t -i :$port 2>/dev/null || true)
    if [ ! -z "$PID" ]; then
        PROCESS_NAME=$(ps -p $PID -o comm= 2>/dev/null || echo "Unknown")
        echo -e "${YELLOW}[WARNING] Port $port is in use by PID $PID ($PROCESS_NAME).${NC}"
    else
        echo -e "${GREEN}[OK] Port $port is free.${NC}"
    fi
done

# Check session lock
echo -e "\n2. Checking World Session Locks..."
if [ -f run/world/session.lock ]; then
    echo -e "${YELLOW}[WARNING] Stale world lock found at run/world/session.lock.${NC}"
else
    echo -e "${GREEN}[OK] No stale world locks found.${NC}"
fi

# Check spark symlink
echo -e "\n3. Checking NTFS Spark Symlink..."
if [ -d run/plugins/spark ]; then
    if [ -L run/plugins/spark ]; then
        TARGET=$(readlink run/plugins/spark)
        echo -e "${GREEN}[OK] run/plugins/spark is a symlink pointing to $TARGET.${NC}"
    else
        echo -e "${RED}[ERROR] run/plugins/spark is a standard folder on NTFS. Native async-profiler will fail to load!${NC}"
        echo -e "        -> Run './dev.sh' to automatically fix this."
    fi
else
    echo -e "${BLUE}[INFO] run/plugins/spark does not exist yet (will be configured on server run).${NC}"
fi
