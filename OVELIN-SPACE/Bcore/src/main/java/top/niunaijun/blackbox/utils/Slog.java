

package top.niunaijun.blackbox.utils;

import android.util.Log;


public final class Slog {
     public static final int LOG_ID_SYSTEM = 3;

    private Slog() {
    }

    // Every hooked system-service call in the engine (ActivityManager, PackageManager, AppOps,
    // ContentResolver, WebView, ...) used to call Slog.d()/v() unconditionally. For a chatty app
    // like Facebook that's thousands of calls a minute, each one doing a synchronous logcat write
    // (and string-building for the Throwable overloads) on the calling thread — often the UI or a
    // binder thread — which is a well-known, real source of jank, not just noise. Debug/verbose
    // lines now only print when the tag has debug logging explicitly turned on for it
    // (standard Android convention: `adb shell setprop log.tag.<TAG> DEBUG`), which is off by
    // default on virtually every real device. Warnings and errors (used by the in-app diagnostics
    // screen) are untouched and always logged.

    public static int v(String tag, String msg) {
        if (!Log.isLoggable(tag, Log.VERBOSE)) return 0;
        return Log.println(Log.VERBOSE, tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        if (!Log.isLoggable(tag, Log.VERBOSE)) return 0;
        return Log.println(Log.VERBOSE, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }


    public static int d(String tag, String msg) {
        if (!Log.isLoggable(tag, Log.DEBUG)) return 0;
        return Log.println(Log.DEBUG, tag, msg);
    }


    public static int d(String tag, String msg, Throwable tr) {
        if (!Log.isLoggable(tag, Log.DEBUG)) return 0;
        return Log.println(Log.DEBUG, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }


    public static int i(String tag, String msg) {
        return Log.println(Log.INFO, tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return Log.println(Log.INFO, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }


    public static int w(String tag, String msg) {
        return Log.println(Log.WARN, tag, msg);
    }


    public static int w(String tag, String msg, Throwable tr) {
        return Log.println(Log.WARN, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int w(String tag, Throwable tr) {
        return Log.println(Log.WARN, tag, Log.getStackTraceString(tr));
    }


    public static int e(String tag, String msg) {
        return Log.println(Log.ERROR, tag, msg);
    }


    public static int e(String tag, String msg, Throwable tr) {
        return Log.println(Log.ERROR, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int println(int priority, String tag, String msg) {
        return Log.println(priority, tag, msg);
    }
}

