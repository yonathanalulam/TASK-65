#!/usr/bin/env bash
#
# run_tests.sh — Run ALL tests (backend + frontend) inside Docker.
# Works in a cold environment with only Docker installed.
#
# Usage: ./run_tests.sh
#
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "========================================"
echo "  Culinary Coach — Full Test Suite"
echo "========================================"
echo ""

BACKEND_EXIT=0
FRONTEND_EXIT=0

# ── Backend Tests (Java / Spring Boot / H2 in-memory) ──────────────────
echo -e "${YELLOW}▶ Running backend tests...${NC}"
echo ""

docker build -f backend/Dockerfile.test -t culinary-coach-backend-test backend/ 2>&1 | tail -5
docker run --rm culinary-coach-backend-test 2>&1 | tee /tmp/cc_backend_test.log
BACKEND_EXIT=${PIPESTATUS[0]}

echo ""

# ── Frontend Tests (vitest / happy-dom) ─────────────────────────────────
echo -e "${YELLOW}▶ Running frontend tests...${NC}"
echo ""

docker build -f frontend/Dockerfile.test -t culinary-coach-frontend-test frontend/ 2>&1 | tail -5
docker run --rm culinary-coach-frontend-test 2>&1 | tee /tmp/cc_frontend_test.log
FRONTEND_EXIT=${PIPESTATUS[0]}

echo ""
echo "========================================"
echo "  Test Results"
echo "========================================"

if [ $BACKEND_EXIT -eq 0 ]; then
  echo -e "  Backend:  ${GREEN}PASSED${NC}"
else
  echo -e "  Backend:  ${RED}FAILED${NC}"
fi

if [ $FRONTEND_EXIT -eq 0 ]; then
  echo -e "  Frontend: ${GREEN}PASSED${NC}"
else
  echo -e "  Frontend: ${RED}FAILED${NC}"
fi

echo "========================================"
echo ""

if [ $BACKEND_EXIT -ne 0 ] || [ $FRONTEND_EXIT -ne 0 ]; then
  echo -e "${RED}Some tests failed.${NC}"
  exit 1
fi

echo -e "${GREEN}All tests passed!${NC}"
exit 0
