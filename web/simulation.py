"""The original HeatCanvas initializer and four-neighbor heat stencil."""

import json
from math import isfinite
from typing import cast

import numpy as np
from numpy.typing import NDArray


class Simulation:
    def __init__(
        self,
        top: float,
        bottom: float,
        left: float,
        right: float,
        plate: float,
        xp: int,
        yp: int,
    ) -> None:
        self.values = np.zeros((xp + 1, yp + 1), dtype=np.float64)
        self.values[1:xp, 1:yp] = plate
        self.values[:xp, 0] = bottom
        self.values[:xp, yp] = top
        self.values[xp, :yp] = right
        self.values[0, :yp] = left
        # The original initialization leaves the upper-right corner at zero.
        self.low = min(100000.0, top, bottom, left, right, plate)
        self.high = max(-100000.0, top, bottom, left, right, plate)
        self.steps = 0

    def advance(self, steps: int) -> None:
        if type(steps) is not int or not 1 <= steps <= 32:
            raise ValueError("Use 1 to 32 steps per update.")
        u = self.values
        for _ in range(steps):
            # NumPy evaluates the entire right side before writing the interior.
            u[1:-1, 1:-1] = (
                u[2:, 1:-1] + u[:-2, 1:-1] + u[1:-1, 2:] + u[1:-1, :-2]
            ) / 4
            self.steps += 1

    def frame(self) -> tuple[int, int, int, float, float, float, bytes]:
        width, height = self.values.shape
        pixels: NDArray[np.uint8]
        if self.high == self.low:
            pixels = np.zeros((height, width), dtype=np.uint8)
        else:
            scale = 255 / (self.high - self.low)
            color_levels = np.clip((self.values - self.low) * scale, 0, 255)
            pixels = color_levels[:, ::-1].T.astype(np.uint8)
        center = self.values[(width - 1) // 2, (height - 1) // 2]
        return (
            self.steps,
            width,
            height,
            self.low,
            self.high,
            float(center),
            pixels.tobytes(),
        )


def create(parameters: str) -> Simulation:
    values = json.loads(parameters, parse_int=float)
    if (
        not isinstance(values, list)
        or len(values) != 7
        or not all(type(v) is float and isfinite(v) for v in values)
    ):
        raise ValueError("Enter finite temperatures and grid dimensions.")
    top, bottom, left, right, plate, xp, yp = cast(list[float], values)
    if not all(-1e6 <= v <= 1e6 for v in (top, bottom, left, right, plate)):
        raise ValueError("Use temperatures between −1,000,000 and 1,000,000.")
    if not all(v.is_integer() and 2 <= v <= 256 for v in (xp, yp)):
        raise ValueError("Use whole numbers from 2 to 256 for each panel count.")
    return Simulation(top, bottom, left, right, plate, int(xp), int(yp))
