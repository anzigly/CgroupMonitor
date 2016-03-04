package org.pdl.cgroupmonitor;

import org.apache.commons.cli.*;

import java.io.*;

public class CgroupMonitorMain {
    private static final String GROUP_OPTION = "group";
    private static final String HELP_OPTION = "help";
    private static final String INTERVAL_OPTION = "interval";
    private static final String MOUNT_OPTION = "mount";
    private static final String NUMBER_OPTION = "number";
    private static final String WRITE_OPTION = "write";

    private String cgroupMountPoint;
    private String groupName;
    private long interval;
    private int number;
    private String writeFile;

    public CgroupMonitorMain() {
        cgroupMountPoint = "/sys/fs/cgroup/";
        groupName = "";
        interval = 1000;
        number = -1;
        writeFile = "";
    }

    public void run(String[] args) throws Exception {
        handleOpts(args);
        CgroupMonitor monitor = new CgroupMonitor(cgroupMountPoint, groupName, interval);
        monitor.run();
        Writer writer = null;
        if (!writeFile.equals("")) {
            writer = new BufferedWriter(new FileWriter(new File(writeFile)));
        }
        if (number == -1) {
            while(true) {
                Thread.sleep(interval);
                System.out.println(monitor);
                if (writer != null) {
                    writer.write(monitor.toString() + "\n");
                    writer.flush();
                }
            }
        } else {
            for (int i = 0; i < number; i++) {
                Thread.sleep(interval);
                System.out.println(monitor);
                if (writer != null) {
                    writer.write(monitor.toString() + "\n");
                    writer.flush();
                }
            }
        }
        if (writer != null) {
            writer.close();
        }
    }

    private void handleOpts(String[] args) throws ParseException {
        Options opts = new Options();
        opts.addOption("g", GROUP_OPTION, true, "Cgroup path to monitor (default /)");
        opts.addOption("h", HELP_OPTION, false, "Prints this message");
        opts.addOption("i", INTERVAL_OPTION, true, "The interval between samplings (default 1s)");
        opts.addOption("m", MOUNT_OPTION, true, "Cgroup mount point (default /sys/fs/cgroup/)");
        opts.addOption("n", NUMBER_OPTION, true, "Terminate after monitored n times (default -1)");
        opts.addOption("w", WRITE_OPTION, true, "Write results to file");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(opts, args);
            if (commandLine.hasOption(HELP_OPTION) || commandLine.getArgList().size() != 0) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("cgroup-monitor", opts);
                System.exit(0);
            }
            if (commandLine.hasOption(GROUP_OPTION)) {
                groupName = commandLine.getOptionValue(GROUP_OPTION);
            }
            if (commandLine.hasOption(INTERVAL_OPTION)) {
                interval = Long.parseLong(commandLine.getOptionValue(INTERVAL_OPTION)) * 1000;
            }
            if (commandLine.hasOption(MOUNT_OPTION)) {
                cgroupMountPoint = commandLine.getOptionValue(MOUNT_OPTION);
            }
            if (commandLine.hasOption(NUMBER_OPTION)) {
                number = Integer.parseInt(commandLine.getOptionValue(NUMBER_OPTION));
            }
            if (commandLine.hasOption(WRITE_OPTION)) {
                writeFile = commandLine.getOptionValue(WRITE_OPTION);
            }
        } catch (ParseException pe) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("cgroup-monitor", opts);
            throw pe;
        }
    }

    public static void main(String[] args) throws Exception {
        CgroupMonitorMain monitorMain = new CgroupMonitorMain();
        monitorMain.run(args);
    }
}
