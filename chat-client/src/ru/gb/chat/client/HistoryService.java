package ru.gb.chat.client;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class HistoryService implements Closeable {

    private static HistoryService hs;

    private File file;

    BufferedOutputStream out;

    public HistoryService(String login) {
        this.file = new File(getPrefix() + login + getPostfix());
    }

    public HistoryService(File file) {
        this.file = file;
    }

    public static HistoryService getHistoryService(String login) {
        if (hs == null || !hs.file.getPath().equals(getPrefix() + login + getPostfix())) {
            hs = new HistoryService(login);
        }
        return hs;
    }

    public static HistoryService getHistoryService(File file) {
        if (hs == null || !hs.file.getPath().equals(file.getPath())) {
            hs = new HistoryService(file);
        }
        return hs;
    }

    private BufferedOutputStream getOut() throws IOException {
        if (out == null) {
            file.getParentFile().mkdirs();
            out = new BufferedOutputStream(new FileOutputStream(file, true));
        }
        return out;
    }

    public static String getPrefix() {
        return ".\\logs\\history_";
    }

    public static String getPostfix() {
        return ".txt";
    }



    public void writeLog(String msg) throws IOException {
        getOut().write((msg + "\n").getBytes());
        close();
    }

    public List<String> getHistory() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        LinkedList<String> log = new LinkedList<>();
        String mes = br.readLine();
        while (mes != null) {
            log.addLast(mes);
            if (log.size() > 100)
                log.removeFirst();
            mes = br.readLine();
        }
        return log;
    }

    @Override
    public void close() throws IOException {
        getOut().close();
        out=null;
    }
}
