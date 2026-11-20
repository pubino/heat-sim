import threading
import unittest
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from playwright.sync_api import (
    Browser,
    BrowserContext,
    Page,
    Playwright,
    expect,
    sync_playwright,
)

BUILD = Path(__file__).resolve().parents[1] / "build"


class BrowserTest(unittest.TestCase):
    server: ThreadingHTTPServer
    thread: threading.Thread
    playwright: Playwright
    browser: Browser
    context: BrowserContext
    page: Page

    @classmethod
    def setUpClass(cls) -> None:
        if not (BUILD / "site/index.html").is_file():
            raise RuntimeError("Build the site before running browser tests.")
        # Serve from a subdirectory to match a GitHub project Pages URL.
        cls.server = ThreadingHTTPServer(
            ("127.0.0.1", 0), partial(SimpleHTTPRequestHandler, directory=BUILD)
        )
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.addClassCleanup(cls.server.server_close)
        cls.addClassCleanup(cls.thread.join, 5)
        cls.addClassCleanup(cls.server.shutdown)
        cls.playwright = sync_playwright().start()
        cls.addClassCleanup(cls.playwright.stop)
        cls.browser = cls.playwright.chromium.launch()
        cls.addClassCleanup(cls.browser.close)

    def setUp(self) -> None:
        self.context = self.browser.new_context()
        self.addCleanup(self.context.close)
        self.page = self.context.new_page()
        self.base_url = f"http://127.0.0.1:{self.server.server_port}/site/"

    def test_renders_and_advances_a_plate_with_different_edge_temperatures(
        self,
    ) -> None:
        self.page.goto(self.base_url)
        start = self.page.get_by_role("button", name="Start", exact=True)
        expect(start).to_be_enabled(timeout=45_000)

        x_panels = self.page.get_by_label("X panels")
        x_panels.fill("2")
        x_panels.press("Tab")
        y_panels = self.page.get_by_label("Y panels")
        y_panels.fill("2")
        y_panels.press("Tab")
        sky = self.page.locator("#sky")
        expect(sky).to_have_attribute("width", "3")
        expect(sky).to_have_attribute("height", "3")
        center = self.page.locator("#center")
        expect(center).to_have_text("32.00")
        pixels = sky.evaluate("""(canvas) => {
            const ctx = canvas.getContext("2d");
            return [0, 1, 2].map(y => Array.from(ctx.getImageData(1, y, 1, 1).data));
        }""")
        self.assertEqual(
            pixels, [[254, 0, 1, 255], [81, 0, 174, 255], [204, 0, 51, 255]]
        )

        self.page.get_by_role("button", name="Step").click()
        expect(self.page.locator("#iteration")).to_have_text("1")
        expect(center).to_have_text("45.00")
        self.page.get_by_role("button", name="Resume").click()
        expect(self.page.locator("#iteration")).not_to_have_text("1")
        self.page.get_by_role("button", name="Pause").click()
        expect(self.page.get_by_role("status")).to_have_text("Paused.")
        self.page.get_by_role("button", name="Reset").click()
        expect(center).to_have_text("32.00")
        expect(start).to_be_enabled()
        self.page.set_viewport_size({"width": 390, "height": 844})
        self.assertTrue(
            self.page.evaluate("document.documentElement.scrollWidth <= innerWidth")
        )

    def test_failed_runtime_load_shows_retry(self) -> None:
        self.page.route("**/vendor/pyodide.js", lambda route: route.abort())
        self.page.goto(self.base_url)
        expect(self.page.get_by_role("alert")).to_contain_text("Python could not load")
        expect(self.page.get_by_role("button", name="Reload simulator")).to_be_visible()
        expect(self.page.get_by_role("button", name="Start")).to_be_disabled()
