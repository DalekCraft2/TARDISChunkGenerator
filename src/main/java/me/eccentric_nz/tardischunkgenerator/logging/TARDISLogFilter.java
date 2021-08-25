package me.eccentric_nz.tardischunkgenerator.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.bukkit.ChatColor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TARDISLogFilter implements Filter {

    private final String path;
    private final List<String> filters = new ArrayList<>();
    private boolean clean = true;

    public TARDISLogFilter(String path) {
        this.path = path;
        filters.add("TARDIS");
        filters.add("tardis");
        filters.add("me.eccentric_nz");
        filters.add("Caused by:");
    }

    public Result checkMessage(String message) {
        for (String filter : filters) {
            if (message.contains(filter)) {
                writeToFile(ChatColor.stripColor(message));
                break;
            }
        }
        return Result.NEUTRAL;
    }

    private void writeToFile(String message) {
        FileWriter fileWriter;
        BufferedWriter bufferedWriter;
        try {
            if (clean) {
                fileWriter = new FileWriter(path); // overwrite
                clean = false;
            } else {
                fileWriter = new FileWriter(path, true); // true to append
            }
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }

    @Override
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return checkMessage(TextUtils.getStacktrace(t, true, "me.eccentric_nz."));
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return checkMessage(TextUtils.getStacktrace(t, true, "me.eccentric_nz."));
    }

    @Override
    public Result filter(LogEvent event) {
        if (event.getThrown() != null) {
            return checkMessage(TextUtils.getStacktrace(event.getThrown(), true, "me.eccentric_nz."));
        }
        return checkMessage(event.getMessage().getFormattedMessage());
    }

    @Override
    public State getState() {
        try {
            return State.STARTED;
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public void initialize() {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean isStopped() {
        return false;
    }
}
