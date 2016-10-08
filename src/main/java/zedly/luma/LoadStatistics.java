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

    private static final LoadStatistics INSTANCE = new LoadStatistics();

    private final RollingAverage frameAdvanceAverage = new RollingAverage(100);
    private final RollingAverage canvasDrawAverage = new RollingAverage(100);
    private final RollingAverage cumulativeFPSAverage = new RollingAverage(100);
    private long frameCounter = 0;

    public static LoadStatistics instance() {
        return INSTANCE;
    }

    private LoadStatistics() {
    }

    public void updateFrameAdvanceNanos(long nanos) {
        frameAdvanceAverage.update(nanos);
    }

    public long averageFrameAdvanceNanos() {
        return frameAdvanceAverage.getAverage();
    }

    public void updateCanvasDrawNanos(long nanos) {
        canvasDrawAverage.update(nanos);
    }

    public long averageCanvasDrawNanos() {
        return canvasDrawAverage.getAverage();
    }

    public void countFrame() {
        frameCounter++;
    }

    public long averageCumulativeFPS() {
        return cumulativeFPSAverage.getAverage();
    }

    public void tick() {
        cumulativeFPSAverage.update(frameCounter);
        frameCounter = 0;
    }

    private class RollingAverage {

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
            long average = 0;
            for (int i = 0; i < ringBuffer.length; i++) {
                average += ringBuffer[i];
            }
            return average / ringBuffer.length;
        }
    }
}
