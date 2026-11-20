package heat;

import java.awt.Graphics;
import java.lang.reflect.Field;

/** Runs the original Java initializer and heat stencil headlessly. */
public final class HeatReference extends HeatCanvas {
    private int remaining;

    @Override
    public void paint(Graphics graphics) {
        if (--remaining == 0) {
            stop();
        }
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        Field running = HeatCanvas.class.getDeclaredField("running");
        running.setAccessible(true);
        Heat parent = new Heat() {
            @Override void appendOutput(String message) {}
        };
        double[][] inputs = {
            {100, 80, 0, 0, 32, 80, 80},
            {-3, -8, -15, -21, -12.5, 7, 4},
            {6, 6, 6, 6, 6, 2, 3},
        };
        for (int sample = 0; sample < inputs.length; sample++) {
            double[] v = inputs[sample];
            HeatReference model = new HeatReference();
            model.setData(v[0], v[1], v[3], v[2], v[4], (int) v[5], (int) v[6], parent);
            int previous = 0;
            for (int step : new int[]{0, 1, 2, 16, 100}) {
                if (step > previous) {
                    model.remaining = step - previous;
                    running.setBoolean(model, true);
                    model.run();
                }
                StringBuilder row = new StringBuilder();
                row.append(sample).append(',').append(model.time).append(',');
                row.append(model.mintemp).append(',').append(model.maxtemp);
                for (double[] column : model.u) {
                    for (double value : column) {
                        row.append(',').append(value);
                    }
                }
                row.append(';');
                for (int x = 0; x <= model.xp; x++) {
                    for (int y = 0; y <= model.yp; y++) {
                        row.append(model.reltemp(x, y)).append(',');
                    }
                }
                System.out.println(row);
                previous = step;
            }
        }
    }
}
