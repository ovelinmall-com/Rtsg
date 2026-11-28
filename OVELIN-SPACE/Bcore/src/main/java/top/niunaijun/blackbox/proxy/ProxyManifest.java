package top.niunaijun.blackbox.proxy;

import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;


public class ProxyManifest {
    // Must match exactly how many "$Pn" proxy component slots (activity/service/provider/
    // job-service) are declared in Bcore's AndroidManifest.xml. This used to be 50, which meant
    // ~300 manifest-declared components for the host app's own package - a real, testable
    // candidate for the persistent ":p0" NullPointerException some OEM ROMs (ITEL/Transsion,
    // Android 14) throw deep inside their own ActivityThread.handleBindApplication while binding
    // this app's own process, before any of our code runs. Cut down to a pool that's still far
    // more than this app needs concurrently, to test whether manifest/component-count size is the
    // trigger. If this device still crashes with the same exact stack trace after this change, the
    // crash is unrelated to manifest size and is confirmed to be a device/ROM-level bug.
    public static final int FREE_COUNT = 12;

    public static boolean isProxy(String msg) {
        return getBindProvider().equals(msg) || msg.contains("proxy_content_provider_");
    }

    public static String getBindProvider() {
        return BlackBoxCore.getHostPkg() + ".blackbox.SystemCallProvider";
    }

    public static String getProxyAuthorities(int index) {
        return String.format(Locale.CHINA, "%s.proxy_content_provider_%d", BlackBoxCore.getHostPkg(), index);
    }

    public static String getProxyPendingActivity(int index) {
        return String.format(Locale.CHINA, "top.niunaijun.blackbox.proxy.ProxyPendingActivity$P%d", index);
    }

    public static String getProxyActivity(int index) {
        return String.format(Locale.CHINA, "top.niunaijun.blackbox.proxy.ProxyActivity$P%d", index);
    }

    public static String TransparentProxyActivity(int index) {
        return String.format(Locale.CHINA, "top.niunaijun.blackbox.proxy.TransparentProxyActivity$P%d", index);
    }

    public static String getProxyService(int index) {
        return String.format(Locale.CHINA, "top.niunaijun.blackbox.proxy.ProxyService$P%d", index);
    }

    public static String getProxyJobService(int index) {
        return String.format(Locale.CHINA, "top.niunaijun.blackbox.proxy.ProxyJobService$P%d", index);
    }

    public static String getProxyFileProvider() {
        return BlackBoxCore.getHostPkg() + ".blackbox.FileProvider";
    }

    public static String getMediaProviderAuthority() {
        return BlackBoxCore.getHostPkg() + ".blackbox.MediaProvider";
    }

    public static String getProxyReceiver() {
        return BlackBoxCore.getHostPkg() + ".stub_receiver";
    }

    public static String getProcessName(int bPid) {
        return BlackBoxCore.getHostPkg() + ":p" + bPid;
    }
}
