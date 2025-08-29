package com;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;



//data struccture = timestamp, type, message
final class LogManager {

    private List<String> logs;
    private final int maxLogs;
    private final String filePath = "";


    public LogManager(int maxLogs){
        this.logs = new ArrayList<>();
        this.maxLogs = maxLogs;


        this.loadLogs();

    }

    public List<String> loadLogs(){

        try{
            Scanner scanner = new Scanner(new java.io.File(filePath));
            while (scanner.hasNextLine()) {
                logs.add(scanner.nextLine());
            }
            scanner.close();
            return logs;
        } catch (Exception e) {
            System.err.println("Error loading logs: " + e.getMessage());
            return Collections.emptyList();
        }
        

    }

    public void addLog(String[] log) {
    if (logs.size() >= maxLogs) {
        logs.remove(0);
    }

    String csvLog = String.join(",", log);
    logs.add(csvLog);
    saveLogs();
}


    public List<String> getLogs() {
    List<String> fileLogs = new ArrayList<>();
    try {
        File file = new File(this.filePath);
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            fileLogs.add(scanner.nextLine());
        }

        scanner.close();
    } catch (Exception e) {
        e.printStackTrace();
    }

    return fileLogs;
}



    private void saveLogs() {
        try {
            java.io.FileWriter writer = new java.io.FileWriter(filePath, false);
            for (String log : logs) {
                writer.write(log + System.lineSeparator());
            }
            writer.close();
        } catch (Exception e) {
            System.err.println("Error saving logs: " + e.getMessage());
        }
    }

  
}

