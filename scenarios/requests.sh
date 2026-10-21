#!/usr/bin/env bash
set -euo pipefail
base="${BASE_URL:-http://localhost:8080}"
echo 'Alice: own order (200)';curl -i -u alice:alice-demo "$base/api/orders/CMD-1042"
echo 'Alice: Bob order (403)';curl -i -u alice:alice-demo "$base/api/orders/CMD-2099"
echo 'Missing proof';curl -i -u alice:alice-demo "$base/api/orders/CMD-1050/eligibility"
echo 'Draft, no payment';curl -i -X POST -u alice:alice-demo "$base/api/orders/CMD-1042/refund-drafts"
