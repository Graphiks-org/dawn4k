#!/bin/bash
# Verification script for dawn4k-webgpu-toolkit module coordinates
# This script checks if the module is published to Maven Local with correct coordinates

set -e

echo "=== Verifying dawn4k-webgpu-toolkit coordinates ==="

MAVEN_REPO="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}"
GROUP_ID="org/graphiks"
ARTIFACT_ID="dawn4k-webgpu-toolkit"
VERSION="1.0.0-SNAPSHOT"

POM_PATH="$MAVEN_REPO/$GROUP_ID/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION.pom"

echo "Checking POM at: $POM_PATH"

if [ ! -f "$POM_PATH" ]; then
    echo "ERROR: POM file not found at $POM_PATH"
    echo "Module dawn4k-webgpu-toolkit is not published to Maven Local"
    exit 1
fi

echo "POM file found. Verifying coordinates..."

# Extract and verify groupId
GROUP=$(grep -o '<groupId>.*</groupId>' "$POM_PATH" | head -1 | sed 's/<groupId>\(.*\)<\/groupId>/\1/')
if [ "$GROUP" != "org.graphiks" ]; then
    echo "ERROR: groupId is '$GROUP', expected 'org.graphiks'"
    exit 1
fi
echo "✓ groupId: $GROUP"

# Extract and verify artifactId
ARTIFACT=$(grep -o '<artifactId>.*</artifactId>' "$POM_PATH" | head -1 | sed 's/<artifactId>\(.*\)<\/artifactId>/\1/')
if [ "$ARTIFACT" != "dawn4k-webgpu-toolkit" ]; then
    echo "ERROR: artifactId is '$ARTIFACT', expected 'dawn4k-webgpu-toolkit'"
    exit 1
fi
echo "✓ artifactId: $ARTIFACT"

# Extract and verify version
VERSION_CHECK=$(grep -o '<version>.*</version>' "$POM_PATH" | head -1 | sed 's/<version>\(.*\)<\/version>/\1/')
if [ "$VERSION_CHECK" != "1.0.0-SNAPSHOT" ]; then
    echo "ERROR: version is '$VERSION_CHECK', expected '1.0.0-SNAPSHOT'"
    exit 1
fi
echo "✓ version: $VERSION_CHECK"

echo ""
echo "=== All coordinates verified successfully ==="
echo "groupId: org.graphiks"
echo "artifactId: dawn4k-webgpu-toolkit"
echo "version: 1.0.0-SNAPSHOT"
exit 0
