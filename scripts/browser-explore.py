#!/usr/bin/env python3
"""Visual exploration — Playwright screenshot → Ollama vision (qwen3.6).

Takes a real screenshot of the page, sends it to qwen3.6 which supports image input.
Direct approach: no browser-use Agent overhead, no CDP timeouts.

Usage:
  python browser-explore.py "Check if the navbar is aligned correctly" --url http://localhost:8080/
  python browser-explore.py "Describe the card layout on this page"
"""

import argparse
import asyncio
import base64
import json
import urllib.request


async def take_screenshot(url: str, path: str, width: int = 1440, height: int = 900):
    """Take screenshot via Playwright Async API."""
    from playwright.async_api import async_playwright

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_context(viewport={"width": width, "height": height})
        pg = await page.new_page()
        await pg.goto(url)
        await pg.wait_for_load_state("networkidle")
        await pg.screenshot(path=path, full_page=False)
        await browser.close()


async def ask_ollama_vision(question: str, image_path: str, model: str = "qwen3.6:latest"):
    """Send screenshot to Ollama multimodal model and return the analysis."""
    with open(image_path, "rb") as f:
        img_b64 = base64.b64encode(f.read()).decode()

    payload = {
        "model": model,
        "stream": False,
        "messages": [
            {
                "role": "user",
                "content": question,
                "images": [img_b64],
            }
        ],
    }

    req = urllib.request.Request(
        "http://localhost:11434/api/chat",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
    )

    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.loads(resp.read().decode())


async def main(task: str, url: str, model: str):
    screenshot_path = "/tmp/browser-explore-screenshot.png"

    print(f"[browser-explore] Opening {url} and taking screenshot...")
    await take_screenshot(url, screenshot_path)
    print(f"[browser-explore] Screenshot saved to {screenshot_path}")

    print(f"[browser-explore] Asking qwen3.6 to analyze pixels ({model})...")
    response = await ask_ollama_vision(task, screenshot_path, model)

    answer = response.get("message", {}).get("content", "No response from LLM")
    print("\n[browser-explore] === Vision Analysis ===")
    print(answer)
    return answer


def parse_args():
    parser = argparse.ArgumentParser(description="Visual page analysis via Ollama vision")
    parser.add_argument("--url", default="http://localhost:8080", help="Target URL")
    parser.add_argument(
        "--task",
        default=(
            "Look at this page very carefully. Describe the visual appearance in detail: "
            "colors, layout (grid/list/sidebar), typography, spacing between elements, card designs "
            "(shadows, borders, gradients), image placement, overall polish level. "
            "Tell me if it looks visually appealing or broken."
        ),
    )
    parser.add_argument("--model", default="qwen3.6:latest", help="Ollama model (must support vision)")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    asyncio.run(main(args.task, args.url, args.model))
