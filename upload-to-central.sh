#!/usr/bin/env bash
set -euo pipefail

BUNDLE_PATH="build/mavenCentralBundle/bundle.zip"

if [[ ! -f "$BUNDLE_PATH" ]]; then
  echo "Bundle not found at $BUNDLE_PATH. Run './gradlew bundleForMavenCentral' first."
  exit 1
fi

if [[ -z "${CENTRAL_USERNAME:-}" || -z "${CENTRAL_PASSWORD:-}" ]]; then
  echo "Please export CENTRAL_USERNAME and CENTRAL_PASSWORD environment variables."
  exit 1
fi

echo "Uploading bundle to Maven Central Portal..."

TOKEN=$(printf "%s:%s" "$CENTRAL_USERNAME" "$CENTRAL_PASSWORD" | base64 -w 0)

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -F "bundle=@${BUNDLE_PATH}" \
  https://central.sonatype.com/api/v1/publisher/upload)

HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
DEPLOYMENT_ID=$(echo "$RESPONSE" | head -n1)

if [[ "$HTTP_STATUS" != "201" ]]; then
  echo "Upload failed (HTTP $HTTP_STATUS): $DEPLOYMENT_ID"
  exit 1
fi

echo "Upload successful. Deployment ID: $DEPLOYMENT_ID"
echo "Visit https://central.sonatype.com/publishing to review and publish."
