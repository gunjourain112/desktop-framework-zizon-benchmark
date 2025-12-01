package com.monitor.viewmodel;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemViewModel {
    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final CentralProcessor processor;
    private final GlobalMemory memory;

    private final DoubleProperty memoryUsage = new SimpleDoubleProperty(0.0);
    private final double[] cpuHistory = new double[60]; // 60 seconds history
    private int cpuHistoryIndex = 0;

    // Using a property to notify when new CPU data is available might be good,
    // or just exposing the array. For simplicity, let's expose the array reference
    // (since it's fixed size) and use a property to signal update.
    private final DoubleProperty currentCpuLoad = new SimpleDoubleProperty(0.0);

    private final ScheduledExecutorService scheduler;
    private long[] prevTicks;

    public SystemViewModel() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.processor = hardware.getProcessor();
        this.memory = hardware.getMemory();
        this.prevTicks = processor.getSystemCpuLoadTicks();

        // Initialize history with 0
        for (int i = 0; i < cpuHistory.length; i++) {
            cpuHistory[i] = 0.0;
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SystemMonitor-Data-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::updateMetrics, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void updateMetrics() {
        // CPU Load
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        prevTicks = processor.getSystemCpuLoadTicks();

        // Memory Usage
        long total = memory.getTotal();
        long available = memory.getAvailable();
        double usedRatio = (double) (total - available) / total;

        Platform.runLater(() -> {
            // Update CPU History Ring Buffer style or Shift?
            // Shifting is easier for drawing: [0] is oldest, [59] is newest.
            System.arraycopy(cpuHistory, 1, cpuHistory, 0, cpuHistory.length - 1);
            cpuHistory[cpuHistory.length - 1] = load;

            currentCpuLoad.set(load);
            memoryUsage.set(usedRatio);
        });
    }

    public DoubleProperty memoryUsageProperty() {
        return memoryUsage;
    }

    public double[] getCpuHistory() {
        return cpuHistory;
    }

    public DoubleProperty currentCpuLoadProperty() {
        return currentCpuLoad;
    }
}
