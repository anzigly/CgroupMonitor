package org.pdl.cgroupmonitor;

import java.io.*;
import java.util.*;

public class CgroupMonitor {
    private String cpuUsageFilePath;
    private long interval;

    private long[] cpuUsages;
    private double[] cpuPercents;
    private long cpuLastUpdate;
    private int numCpuCores;

    private Timer sampleTimer;
    private TimerTask sampleTimerTask;

    public CgroupMonitor(String mountPoint, String groupName, long interval) throws IOException {
        cpuUsageFilePath = mountPoint + "/cpu/" + groupName + "/cpuacct.usage_percpu";
        this.interval = interval;
        cpuLastUpdate = 0;
        numCpuCores = Runtime.getRuntime().availableProcessors();
        cpuUsages = new long[numCpuCores];
        cpuPercents = new double[numCpuCores];
        updateCpuUsage();
        sampleTimer = new Timer(true);
        sampleTimerTask = new TimerTask() {
            public void run() {
                try {
                    updateCpuUsage();
                } catch (IOException ioException) {
                    resetCpuUsage();
                    sampleTimer.cancel();
                }
            };
        };
    }

    public void run() {
        sampleTimer.schedule(sampleTimerTask, 0, interval);
    }

    public String toString() {
        String thisToString = "";
        for (double cpuPercent : cpuPercents) {
            thisToString += String.valueOf(cpuPercent) + " ";
        }
        return thisToString;
    }

    private void resetCpuUsage() {
        for (int i = 0; i < numCpuCores; i++) {
            cpuUsages[i] = 0;
            cpuPercents[i] = 0;
        }
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
            cpuPercents[i] = (double)(currentCpuUsage - cpuUsages[i]) /
                    (currentTime - cpuLastUpdate) / 1000000000;
            cpuUsages[i] = currentCpuUsage;
        }
        cpuLastUpdate = currentTime;
    }
}
