#!/bin/bash

# Exit on error
set -e

# Use Linux tmp dir for executing temporary binaries (fixes NTFS execution mapping issues)
export TMPDIR=/tmp

# Base directory of the project
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# ANSI Color Codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Error handler
failure_handler() {
    echo ""
    log_error "Execution failed! Please check the output above for details."
}
trap 'failure_handler' ERR

# Print usage instructions
usage() {
    echo -e "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  (no args)   Clean, build, and run the Paper server (default)"
    echo "  fabric      Build and launch Fabric modded environment with 2-in-1 JAR loaded"
    echo "  client      Alias for fabric"
    echo "  build       Clean and compile the plugin JAR"
    echo "  test        Run the automated test suite (JUnit 5)"
    echo "  report      Open the Gradle compilation/deprecation report in browser"
    echo "  run         Run the Paper testing server without rebuilding"
    echo "  clean       Clean Gradle build caches and temporary server files"
    echo "  help        Show this help message"
}

# Ensure the spark plugins directory is symlinked to /tmp/spark
# This works around the NTFS mount executable mapping issue for async-profiler.so
if [ -d run/plugins ]; then
    if [ ! -L run/plugins/spark ] && [ -d run/plugins/spark ]; then
        log_info "Migrating existing spark directory to /tmp/spark..."
        rm -rf run/plugins/spark
    fi
fi
mkdir -p /tmp/spark
mkdir -p run/plugins
mkdir -p run/mods
if [ ! -L run/plugins/spark ]; then
    ln -sf /tmp/spark run/plugins/spark
fi

# Print test coverage summary from JaCoCo XML report if available
print_coverage() {
    local xml_path="build/reports/jacoco/test/jacocoTestReport.xml"
    if [ -f "$xml_path" ]; then
        python3 -c "
import xml.etree.ElementTree as ET
try:
    tree = ET.parse('$xml_path')
    root = tree.getroot()
    stats = {c.attrib['type']: (int(c.attrib['covered']), int(c.attrib['covered']) + int(c.attrib['missed'])) for c in root.findall('counter')}
    if 'LINE' in stats:
        l_cov, l_tot = stats['LINE']
        l_pct = (l_cov / l_tot * 100) if l_tot > 0 else 0
        m_cov, m_tot = stats.get('METHOD', (0, 0))
        m_pct = (m_cov / m_tot * 100) if m_tot > 0 else 0
        print(f'\033[0;34m[INFO]\033[0m Test Coverage: \033[1;33m{l_pct:.1f}%\033[0m lines ({l_cov}/{l_tot}), \033[1;33m{m_pct:.1f}%\033[0m methods ({m_cov}/{m_tot})')
except Exception:
    pass
" 2>/dev/null || true
    fi
}

# Run tasks based on the argument
case "$1" in
    ""|default)
        log_info "=== [1/2] Cleaning and Building LocketteProMax ==="
        log_info "Removing old plugin JAR and data folder from run/plugins/..."
        rm -rf run/plugins/LocketteProMax*
        ./gradlew clean build
        print_coverage
        mkdir -p run/mods
        cp build/libs/LocketteProMax*.jar run/mods/ 2>/dev/null || true
        log_success "Build completed successfully!"
        log_info "=== [2/2] Launching Paper Server ==="
        ./gradlew runServer
        log_success "Paper server closed successfully."
        ;;
    fabric|client)
        log_info "=== [1/2] Building LocketteProMax 2-in-1 JAR ==="
        ./gradlew clean build
        print_coverage
        log_success "Build completed successfully!"

        log_info "=== [2/2] Setting up Fabric Modded environment ==="
        mkdir -p run-fabric/mods
        rm -f run-fabric/mods/LocketteProMax*.jar
        cp build/libs/LocketteProMax*.jar run-fabric/mods/
        mkdir -p run/mods
        cp build/libs/LocketteProMax*.jar run/mods/
        log_success "Copied LocketteProMax 2-in-1 JAR to run-fabric/mods/ and run/mods/"

        FABRIC_JAR="run-fabric/fabric-server-launch.jar"
        if [ ! -f "$FABRIC_JAR" ]; then
            log_info "Downloading Fabric loader launcher..."
            curl -sSL "https://meta.fabricmc.net/v2/versions/loader/1.21.4/0.16.10/1.0.1/server/jar" -o "$FABRIC_JAR"
            echo "eula=true" > run-fabric/eula.txt
        fi

        log_info "=== Launching Fabric Modded Minecraft Environment ==="
        cd run-fabric
        java -Xms1G -Xmx2G -Djava.io.tmpdir=/tmp -jar fabric-server-launch.jar nogui
        cd "$PROJECT_DIR"
        log_success "Fabric modded environment closed successfully."
        ;;
    build)
        log_info "=== Cleaning and Building LocketteProMax ==="
        ./gradlew clean build
        print_coverage
        log_success "Build completed successfully!"
        ;;
    test)
        log_info "=== Running Automated Tests ==="
        ./gradlew test
        print_coverage
        log_success "Tests completed successfully!"
        ;;
    report)
        log_info "=== Opening Gradle Problems Report ==="
        REPORT_PATH="build/reports/problems/problems-report.html"
        if [ -f "$REPORT_PATH" ]; then
            log_info "Opening report in browser: $REPORT_PATH"
            if command -v xdg-open > /dev/null; then
                xdg-open "$REPORT_PATH"
            elif command -v open > /dev/null; then
                open "$REPORT_PATH"
            else
                log_error "Could not find xdg-open or open to launch the browser. You can open it manually at: file://\$(pwd)/\$REPORT_PATH"
            fi
            log_success "Report opened!"
        else
            log_error "No problems report found at \$REPORT_PATH. Try running a build or tests first."
        fi
        ;;
    run)
        log_info "=== Launching Paper Server (no build) ==="
        ./gradlew runServer
        log_success "Paper server closed successfully."
        ;;
    clean)
        log_info "=== Cleaning Gradle Build & Temporary Files ==="
        ./gradlew clean
        log_info "Removing plugin JAR and data folder from run/plugins/..."
        rm -rf run/plugins/LocketteProMax*
        log_success "Plugin files removed."
        # Optional: Ask if they want to clean server files
        read -p "Do you also want to clear local server data (world, logs, cache)? [y/N]: " clean_server
        if [[ $clean_server =~ ^[Yy]$ ]]; then
            log_info "Clearing server data..."
            rm -rf run/world run/world_nether run/world_the_end run/logs run/cache
            log_success "Server data cleared."
        fi
        log_success "Clean completed successfully!"
        ;;
    help|-h|--help)
        usage
        ;;
    *)
        log_error "Unknown command: $1"
        usage
        trap - ERR
        exit 1
        ;;
esac
