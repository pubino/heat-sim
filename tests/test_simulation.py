import json
import subprocess
import tempfile
import unittest
from pathlib import Path

import numpy as np

from web.simulation import create

ROOT = Path(__file__).resolve().parents[1]
CASES = [
    [100, 80, 0, 0, 32, 80, 80],
    [-3, -8, -15, -21, -12.5, 7, 4],
    [6, 6, 6, 6, 6, 2, 3],
]


class HeatTest(unittest.TestCase):
    def test_matches_java_entire_grid_and_color_scale(self) -> None:
        with tempfile.TemporaryDirectory() as classes:
            subprocess.run(
                [
                    "javac",
                    "--release",
                    "25",
                    "-d",
                    classes,
                    *sorted((ROOT / "myutil").glob("*.java")),
                    ROOT / "heat/Heat.java",
                    ROOT / "tests/HeatReference.java",
                ],
                check=True,
                timeout=30,
            )
            output = subprocess.check_output(
                [
                    "java",
                    "-Djava.awt.headless=true",
                    "-cp",
                    classes,
                    "heat.HeatReference",
                ],
                timeout=30,
                text=True,
            )
        models = [create(json.dumps(case)) for case in CASES]
        lines = output.splitlines()
        self.assertEqual(len(lines), 15)
        for line in lines:
            grid, palette = line.split(";")
            case, step, minimum, maximum, *cells = grid.split(",")
            model = models[int(case)]
            with self.subTest(case=case, step=step):
                while model.steps < int(step):
                    model.advance(min(int(step) - model.steps, 32))
                expected = np.asarray(cells, dtype=np.float64).reshape(
                    model.values.shape
                )
                np.testing.assert_array_equal(model.values, expected)
                self.assertEqual(model.low, float(minimum))
                self.assertEqual(model.high, float(maximum))
                colors = np.asarray(palette.rstrip(",").split(","), dtype=np.int64)
                clipped = np.clip(colors.reshape(model.values.shape), 0, 255)
                expected_pixels = clipped[:, ::-1].T.astype(np.uint8).tobytes()
                self.assertEqual(model.frame()[-1], expected_pixels)

    def test_frame_is_oriented_top_to_bottom(self) -> None:
        model = create("[100,80,0,0,32,7,4]")
        step, width, height, low, high, center, pixels = model.frame()
        self.assertEqual(
            (step, width, height, low, high, center), (0, 8, 5, 0, 100, 32)
        )
        self.assertEqual(pixels[1], 254)
        self.assertEqual(pixels[-2], 204)
        self.assertEqual(pixels[width * (height // 2) + width // 2], 81)

    def test_validates_grid_and_temperatures(self) -> None:
        for invalid in (
            "[]",
            "[0,0,0,0,0,1,10]",
            "[0,0,0,0,0,257,10]",
            "[1e309,0,0,0,0,5,5]",
        ):
            with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                create(invalid)
