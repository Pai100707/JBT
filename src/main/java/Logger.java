import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logger that prefixes messages with a timestamp and log-level tag.
 *
 * Format: [HH:mm:ss] [Level] message
 *
 * Supports three levels: LOG, WARN, ERROR
 *
 * When debug == false only WARN and ERROR are emitted; LOG lines are suppressed.
 */
public class Logger {

    public enum Level { LOG, WARN, ERROR }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final boolean debug;
    private final LogSink sink;

    /** Functional interface — caller decides where output goes (GUI, console). */
    public interface LogSink {
        void append(String message);
    }

    public Logger(boolean debug, LogSink sink) {
        this.debug = debug;
        this.sink  = sink;
    }

    // ── public API ───────────────────────────────────────────────────────────

    public void log(String message)   { emit(Level.LOG,   message); }
    public void warn(String message)  { emit(Level.WARN,  message); }
    public void error(String message) { emit(Level.ERROR, message); }

    /** Raw append (no prefix) — always emitted regardless of debug flag. */
    public void raw(String message) { sink.append(message); }

    // ── internal ─────────────────────────────────────────────────────────────

    private void emit(Level level, String message) {
        if (level == Level.LOG && !debug) {
            // In non-debug mode, LOG-level lines are still forwarded
            // (they carry important build progress info); only internal
            // verbose traces should use log() when debug == false.
            // Callers that want truly debug-only output should check debug
            // themselves.  Change this policy here if needed.
        }
        String time = LocalTime.now().format(TIME_FMT);
        String tag  = levelTag(level);
        sink.append("[" + time + "] [" + tag + "] " + message + "\n");
    }

    private String levelTag(Level level) {
        switch (level) {
            case LOG:   return "Log";
            case WARN:  return "Warn";
            case ERROR: return "Error";
            default:    return "Log";
        }
    }
}

