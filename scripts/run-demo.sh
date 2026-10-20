#!/usr/bin/env bash
set -euo pipefail
export LAB_DEMO_IDENTITIES_ENABLED=true
mvn -pl refund-api -am spring-boot:run
