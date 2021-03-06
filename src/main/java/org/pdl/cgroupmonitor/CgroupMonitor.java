package org.pdl.cgroupmonitor;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CgroupMonitor {
    private String groupName;
    private String cpuUsageFilePath;
    private long interval;

    private long[] cpuUsages;
    private double[] cpuPercents;
    private long cpuLastUpdate;
    private int numCpuCores;

    private Runnable updateRunner;
    private ScheduledExecutorService scheduler;

    public boolean isValid;

    private double currentMaxCpuPercent;
    private double smoothedMaxCpuPercent;
    private double sumCpuPercent;
    private static double alpha = 0.5; //EWMA smoothed factor

    public CgroupMonitor(String mountPoint, final String groupName, long interval) throws IOException {
        this.groupName = groupName;
        cpuUsageFilePath = mountPoint + "/cpu/" + groupName + "/cpuacct.usage_percpu";
        this.interval = interval;
        cpuLastUpdate = 0;
        numCpuCores = Runtime.getRuntime().availableProcessors();
        cpuUsages = new long[numCpuCores];
        cpuPercents = new double[numCpuCores];
        updateAll();
        isValid = true;
        currentMaxCpuPercent = 0.0;
        smoothedMaxCpuPercent = 0.0;
        sumCpuPercent = 0.0;
        updateRunner = new Runnable() {
            public void run() {
                try {
                    updateAll();
                } catch (IOException ioException) {
                    isValid = false;
                    System.out.println("Error: " + groupName);
                    resetAll();
                }
            }
        };
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(updateRunner, 0, this.interval, TimeUnit.MILLISECONDS);
    }

    public String getGroupName() {
        return groupName;
    }

    public void stop() {
        scheduler.shutdown();
    }

    public String toString() {
        return groupName + " " + cpuLastUpdate + " " + currentMaxCpuPercent + " " +
                smoothedMaxCpuPercent + " " + sumCpuPercent;
    }

    private void resetAll() {
        resetCpuUsage();
    }

    private void resetCpuUsage() {
        for (int i = 0; i < numCpuCores; i++) {
            cpuUsages[i] = 0;
            cpuPercents[i] = 0;
        }
    }

    private void updateAll() throws IOException {
        updateCpuUsage();
    }

    private void updateCpuUsage() throws IOException {
        FileReader cpuUsageFileReader = new FileReader(cpuUsageFilePath);
        BufferedReader cpuUsageBufferedReader = new BufferedReader(cpuUsageFileReader);
        String cpuUsageStr = cpuUsageBufferedReader.readLine();
        cpuUsageBufferedReader.close();
        cpuUsageFileReader.close();

        long currentTime = System.currentTimeMillis();
        String[] currentCpuUsageStrings = cpuUsageStr.split(" ");
        for (int i = 0; i < currentCpuUsageStrings.length; i++) {
            long currentCpuUsage = Long.parseLong(currentCpuUsageStrings[i]);
            cpuPercents[i] = (double)(currentCpuUsage - cpuUsages[i]) * 1000 /
                    (currentTime - cpuLastUpdate) / 10000000;
            cpuUsages[i] = currentCpuUsage;
        }
        cpuLastUpdate = currentTime;

        double maxPercent = 0.0;
        double sumPercent = 0.0;
        for (double cpuPercent : cpuPercents) {
            maxPercent = Math.max(cpuPercent, maxPercent);
            sumPercent += cpuPercent;
        }
        this.currentMaxCpuPercent = maxPercent;
        this.smoothedMaxCpuPercent = this.smoothedMaxCpuPercent * alpha + maxPercent * (1 - alpha);
        this.sumCpuPercent = this.sumCpuPercent * alpha + sumPercent * (1 - alpha);
    }
}
