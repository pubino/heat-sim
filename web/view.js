export const stepsPerFrame = 2;

/** @param {import("./protocol").Frame} frame */
export function draw([step, width, height, low, high, center, pixels]) {
  const canvas = document.querySelector("canvas");
  const context = canvas?.getContext("2d");
  const iteration = document.querySelector("#iteration");
  const minimum = document.querySelector("#low");
  const maximum = document.querySelector("#high");
  const mid = document.querySelector("#center");
  if (!canvas || !context || !iteration || !minimum || !maximum || !mid) return;

  if (canvas.width !== width) canvas.width = width;
  if (canvas.height !== height) canvas.height = height;
  const image = context.createImageData(width, height);
  const rgba = image.data;
  for (let index = 0; index < pixels.length; index++) {
    rgba[4 * index] = pixels[index];
    rgba[4 * index + 2] = 255 - pixels[index];
    rgba[4 * index + 3] = 255;
  }
  for (const index of [0, width - 1, (height - 1) * width, height * width - 1]) {
    rgba[4 * index] = 0;
    rgba[4 * index + 2] = 0;
  }
  context.putImageData(image, 0, 0);
  iteration.textContent = String(step);
  minimum.textContent = String(low);
  maximum.textContent = String(high);
  mid.textContent = center.toFixed(2);
}
