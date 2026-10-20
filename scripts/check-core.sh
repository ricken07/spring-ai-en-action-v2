#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT
javac --release 21 -d "$OUT" $(find refund-domain/src/main/java refund-application/src/main/java refund-domain/src/test/java refund-application/src/test/java -name '*.java' | sort)
java -cp "$OUT" com.rickenbazolo.refund.domain.DomainSmokeTest
java -cp "$OUT" com.rickenbazolo.refund.application.ApplicationSmokeTest
