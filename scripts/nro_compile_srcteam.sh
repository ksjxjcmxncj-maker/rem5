#!/bin/bash
# =============================================================================
# NRO Compile SRC-Team Source → JAR
# =============================================================================
# Dùng: bash scripts/nro_compile_srcteam.sh
# Yêu cầu: Java 11+, Maven 3.x
# Source: /tmp/nro_extracted/SRC-Team/Soucre/
# =============================================================================

set -e

SRC_DIR="/tmp/nro_extracted/SRC-Team/Soucre"
OUTPUT_DIR="$(pwd)/build"
NRO_SERVER_JAR="${NRO_SERVER_JAR:-/opt/nro/nro.jar}"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_err()  { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# Check source exists
if [ ! -d "$SRC_DIR" ]; then
    log_err "Source not found: $SRC_DIR"
    echo "Please extract the SRC-Team RAR file first:"
    echo "  1. Download: https://drive.google.com/file/d/1xq-9ZLkgziBdD01OJ-0WNUnttBxxifco"
    echo "  2. Extract: /tmp/rar/unrar x SRC-Team.rar /tmp/nro_extracted/"
    exit 1
fi

# Check Java
if ! command -v java &>/dev/null; then
    log_err "Java not found. Install: nix-env -iA nixpkgs.jdk11"
fi

# Check Maven
if ! command -v mvn &>/dev/null; then
    log_warn "Maven not found. Attempting to install..."
    nix-env -iA nixpkgs.maven 2>/dev/null || {
        log_err "Cannot install Maven. Please install manually."
    }
fi

log_info "Java version: $(java -version 2>&1 | head -1)"
log_info "Maven version: $(mvn -version 2>&1 | head -1)"

# Go to source
cd "$SRC_DIR"

# Check pom.xml
if [ ! -f "pom.xml" ]; then
    log_err "pom.xml not found in $SRC_DIR"
fi

log_info "Building from: $SRC_DIR"
log_info "pom.xml found: OK"

# Count source files
java_count=$(find src -name "*.java" | wc -l)
log_info "Java files: $java_count"

# Clean build
log_info "Running: mvn clean package -DskipTests..."
mvn clean package -DskipTests 2>&1 | tee /tmp/nro_build.log

# Find JAR
jar_file=$(find target -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -type f 2>/dev/null | head -1)

if [ -z "$jar_file" ]; then
    log_err "Build failed! Check /tmp/nro_build.log"
fi

jar_size=$(du -sh "$jar_file" | cut -f1)
log_ok "Build SUCCESS: $jar_file ($jar_size)"

# Create output dir
mkdir -p "$OUTPUT_DIR"

# Copy to output
cp "$jar_file" "$OUTPUT_DIR/nro-srcteam-$(date +%Y%m%d).jar"
cp "$jar_file" "$OUTPUT_DIR/nro-latest.jar"
log_ok "Saved to: $OUTPUT_DIR/nro-latest.jar"

# Ask to deploy
echo ""
echo "Deploy to server?"
echo "  Target: $NRO_SERVER_JAR"
echo -n "Deploy now? (y/N): "
read -r confirm

if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
    if [ -f "$NRO_SERVER_JAR" ]; then
        log_info "Backing up current JAR..."
        cp "$NRO_SERVER_JAR" "${NRO_SERVER_JAR%.jar}_backup_$(date +%Y%m%d_%H%M%S).jar"
    fi
    
    log_info "Deploying..."
    cp "$OUTPUT_DIR/nro-latest.jar" "$NRO_SERVER_JAR"
    log_ok "Deployed to: $NRO_SERVER_JAR"
    log_warn "Remember to restart NRO server!"
else
    log_info "Not deployed. JAR ready at: $OUTPUT_DIR/nro-latest.jar"
    log_info "To deploy manually: cp $OUTPUT_DIR/nro-latest.jar $NRO_SERVER_JAR"
fi
