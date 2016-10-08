/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zedly.luma;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Accepts Runnables for execution in the server's main thread.
 *
 * @author Dennis
 */
public class Synchronizer implements Runnable {

    private static final ConcurrentLinkedQueue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Synchronizer INSTANCE = new Synchronizer();
    private static int taskid;

    public static void add(Runnable r) {
        QUEUE.add(r);
    }

    public static void setTaskId(int taskId) {
        taskid = taskId;
    }

    public static int getTaskId() {
        return taskid;
    }

    public static Synchronizer instance() {
        return INSTANCE;
    }

    @Override
    public void run() {
        while (!QUEUE.isEmpty()) {
            QUEUE.poll().run();
        }
    }
}
