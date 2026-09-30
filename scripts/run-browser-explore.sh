#!/usr/bin/env bash
# Visual page analysis: Playwright screenshot → Ollama qwen3.6 vision mode.
# Uses real screenshots sent to qwen3.6 which supports image input.
# No browser-use Agent overhead, no CDP timeouts.
#
# Usage:
#   ./run-browser-explore.sh "Check the layout"                           # headless (public pages)
#   DISPLAY=:1 ./run-browser-explore.sh "Check the layout"                # headed (visible browser)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VENV_DIR="$SCRIPT_DIR/.venv"

python3 -m venv "$VENV_DIR" 2>/dev/null || true
source "$VENV_DIR/bin/activate"

pip install playwright --break-system-packages 2>/dev/null || pip3 install browser-use[core] langchain-ollama playwright --break-system-packages 2>/dev/null || \
    uv pip install browser-use[core] langchain-ollama playwright

python3 -m playwright install chromium 2>/dev/null || true

python3 "$SCRIPT_DIR/browser-explore.py" "$@"
