#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Run with the pinned bundle installed and Ruby 3.4 on PATH.
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s scripts/tests -p 'test_*.py'
# Stub-based Fastlane tests must not share a Ruby process with real Spaceship tests.
for test_file in scripts/tests/test_*.rb; do
  bundle exec ruby "$test_file"
done
