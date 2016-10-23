/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

/**
 * A structure holding some performance data, such as rolling averages of draw
 * times, draws per second, etc.
 *
 * @author Dennis
 */
public class LoadStatistics {
    private static final RollingAverage FRAME_ADVANCE_NANOS = new RollingAverage(Settings.STATISTICS_AVERAGE_TIME);
    private static final RollingAverage CANVAS_DRAW_NANOS = new RollingAverage(Settings.STATISTICS_AVERAGE_TIME);
    private static final RollingAverage CUMULATIVE_FPS = new RollingAverage(Settings.STATISTICS_AVERAGE_TIME);

    private static long frameCounter = 0;
    private static long canvasDrawNanosCounter = 0;
    private static long frameAdvanceNanosCounter = 0;

    public static int taskId = 0;

    public static void updateFrameAdvanceNanos(long nanos) {
        frameAdvanceNanosCounter += nanos;
    }

    public static long averageFrameAdvanceNanos() {
        return FRAME_ADVANCE_NANOS.getAverage();
    }

    public static void updateCanvasDrawNanos(long nanos) {
        canvasDrawNanosCounter += nanos;
    }

    public static long averageCanvasDrawNanos() {
        return CANVAS_DRAW_NANOS.getAverage();
    }

    public static void countFrame() {
        frameCounter++;
    }

    public static long averageCumulativeFPS() {
        return CUMULATIVE_FPS.getSum() * 20 / Settings.STATISTICS_AVERAGE_TIME;
    }

    public static void tick() {
        CUMULATIVE_FPS.update(frameCounter);
        FRAME_ADVANCE_NANOS.update(frameAdvanceNanosCounter);
        CANVAS_DRAW_NANOS.update(canvasDrawNanosCounter);
        frameCounter = 0;
        frameAdvanceNanosCounter = 0;
        canvasDrawNanosCounter = 0;
    }

    private static class RollingAverage {

        private final long[] ringBuffer;
        private int ringIndex = 0;

        public RollingAverage(int historyLength) {
            ringBuffer = new long[historyLength];
        }

        public void update(long dataPoint) {
            ringBuffer[ringIndex] = dataPoint;
            ringIndex = (ringIndex + 1) % ringBuffer.length;
        }

        public long getAverage() {
            return getSum() / ringBuffer.length;
        }

        public long getSum() {
            long sum = 0;
            for (int i = 0; i < ringBuffer.length; i++) {
                sum += ringBuffer[i];
            }
            return sum;
        }
    }
}
