package top.niunaijun.blackbox.fake.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Everything beyond Android ID / serial / build fingerprint that identifies a "device" to an app:
 * IMEI (both slots), MEID, IMSI, SIM serial (ICCID), Wi-Fi and Bluetooth MAC, and the secondary
 * Build fields (BOARD, HARDWARE, DISPLAY, ID, BOOTLOADER, VERSION.INCREMENTAL).
 *
 * Generated once per virtual user when the clone is created and stored under that user's id.
 * A clone created before this existed has no stored ids (get() returns null) and keeps behaving
 * exactly as it did, so existing clones are never changed under the app's feet.
 *
 * These values are only what the cloned app is told when it asks; nothing on the real phone or
 * SIM is changed.
 */
public final class DeviceSpoofIds {
    private static final String TAG = "DeviceSpoofIds";
    private static final String PREFS_NAME = "DeviceSpoofIds";
    public static final Object NO_MATCH = new Object();

    private static final SecureRandom RANDOM = new SecureRandom();

    public final String imei1;
    public final String imei2;
    public final String meid;
    public final String imsi;
    public final String iccid;
    public final String wifiMac;
    public final String bluetoothMac;
    public final String board;
    public final String hardware;
    public final String buildId;
    public final String bootloader;
    public final String incremental;

    private DeviceSpoofIds(String imei1, String imei2, String meid, String imsi, String iccid,
                           String wifiMac, String bluetoothMac, String board, String hardware,
                           String buildId, String bootloader, String incremental) {
        this.imei1 = imei1;
        this.imei2 = imei2;
        this.meid = meid;
        this.imsi = imsi;
        this.iccid = iccid;
        this.wifiMac = wifiMac;
        this.bluetoothMac = bluetoothMac;
        this.board = board;
        this.hardware = hardware;
        this.buildId = buildId;
        this.bootloader = bootloader;
        this.incremental = incremental;
    }

    @SuppressWarnings("deprecation")
    private static SharedPreferences prefs() {
        return BlackBoxCore.getContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    // ------------------------------------------------------------------ storage

    /** Generates and stores a fresh set for this virtual user, consistent with its build profile. */
    public static DeviceSpoofIds generate(int userId, DeviceSpoofProfile profile) {
        String buildId = "";
        String incremental = "";
        String[] parts = profile.fingerprint.split("/");
        if (parts.length >= 5) {
            buildId = parts[3];
            incremental = parts[4].split(":")[0];
        }
        String codename = TextUtils.isEmpty(profile.device) ? "device" : profile.device;
        DeviceSpoofIds ids = new DeviceSpoofIds(
                randomImei(),
                randomImei(),
                randomMeid(),
                randomImsi(),
                randomIccid(),
                randomMac(),
                randomMac(),
                codename,
                codename,
                buildId,
                codename + "-1." + RANDOM.nextInt(10) + "-" + randomDigits(7),
                incremental);
        prefs().edit()
                .putString(k("imei1", userId), ids.imei1)
                .putString(k("imei2", userId), ids.imei2)
                .putString(k("meid", userId), ids.meid)
                .putString(k("imsi", userId), ids.imsi)
                .putString(k("iccid", userId), ids.iccid)
                .putString(k("wifi", userId), ids.wifiMac)
                .putString(k("bt", userId), ids.bluetoothMac)
                .putString(k("board", userId), ids.board)
                .putString(k("hardware", userId), ids.hardware)
                .putString(k("buildid", userId), ids.buildId)
                .putString(k("bootloader", userId), ids.bootloader)
                .putString(k("incremental", userId), ids.incremental)
                .commit();
        return ids;
    }

    /** Stored ids for this virtual user, or null for clones created before this feature. */
    public static DeviceSpoofIds get(int userId) {
        SharedPreferences p = prefs();
        String imei1 = p.getString(k("imei1", userId), null);
        if (TextUtils.isEmpty(imei1)) {
            return null;
        }
        return new DeviceSpoofIds(
                imei1,
                p.getString(k("imei2", userId), imei1),
                p.getString(k("meid", userId), ""),
                p.getString(k("imsi", userId), ""),
                p.getString(k("iccid", userId), ""),
                p.getString(k("wifi", userId), ""),
                p.getString(k("bt", userId), ""),
                p.getString(k("board", userId), ""),
                p.getString(k("hardware", userId), ""),
                p.getString(k("buildid", userId), ""),
                p.getString(k("bootloader", userId), ""),
                p.getString(k("incremental", userId), ""));
    }

    public static void clear(int userId) {
        String[] names = {"imei1", "imei2", "meid", "imsi", "iccid", "wifi", "bt", "board",
                "hardware", "buildid", "bootloader", "incremental"};
        SharedPreferences.Editor editor = prefs().edit();
        for (String name : names) {
            editor.remove(k(name, userId));
        }
        editor.commit();
    }

    public static DeviceSpoofIds current() {
        try {
            return get(BActivityThread.getUserId());
        } catch (Throwable e) {
            return null;
        }
    }

    private static String k(String name, int userId) {
        return name + "_" + userId;
    }

    // ------------------------------------------------------------------ hook helper

    /**
     * Shared by the telephony binder proxies: returns the stored value for IMEI / MEID / IMSI /
     * SIM-serial queries of the current clone, or NO_MATCH to let the normal hooks handle it.
     */
    public static Object intercept(Method method, Object[] args) {
        DeviceSpoofIds ids = current();
        if (ids == null || method == null) {
            return NO_MATCH;
        }
        String name = method.getName();
        switch (name) {
            case "getImei":
            case "getImeiForSlot":
            case "getImeiForSubscriber":
            case "getDeviceId":
            case "getDeviceIdWithFeature":
            case "getDeviceIdForPhone":
            case "getDeviceIdForSubscriber":
                return ids.imeiForSlot(slotOf(name, args));
            case "getMeid":
            case "getMeidForSlot":
            case "getMeidForSubscriber":
                return ids.meid;
            case "getSubscriberId":
            case "getSubscriberIdForSubscriber":
            case "getSubscriberIdWithFeature":
                return ids.imsi;
            case "getIccSerialNumber":
            case "getIccSerialNumberForSubscriber":
            case "getIccSerialNumberWithFeature":
                return ids.iccid;
            default:
                return NO_MATCH;
        }
    }

    private static int slotOf(String name, Object[] args) {
        if ((name.endsWith("ForSlot") || name.endsWith("ForPhone")) && args != null
                && args.length > 0 && args[0] instanceof Integer) {
            return (Integer) args[0];
        }
        return 0;
    }

    private String imeiForSlot(int slot) {
        return slot >= 1 ? imei2 : imei1;
    }

    // ------------------------------------------------------------------ generators

    /** 15 digits, 8-digit TAC (starts with 35, the common reporting-body prefix) + serial + Luhn. */
    private static String randomImei() {
        String body = "35" + randomDigits(12);
        return body + luhnCheckDigit(body);
    }

    /** 14 hex characters, upper case. */
    private static String randomMeid() {
        StringBuilder sb = new StringBuilder("A0");
        String hex = "0123456789ABCDEF";
        for (int i = 0; i < 12; i++) {
            sb.append(hex.charAt(RANDOM.nextInt(16)));
        }
        return sb.toString();
    }

    /** MCC+MNC of the phone's real SIM operator (so it stays plausible) + random subscriber id. */
    private static String randomImsi() {
        String plmn = "00101";
        try {
            TelephonyManager tm = (TelephonyManager) BlackBoxCore.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String op = tm != null ? tm.getSimOperator() : null;
            if (!TextUtils.isEmpty(op) && op.length() >= 5 && op.matches("\\d+")) {
                plmn = op;
            }
        } catch (Throwable e) {
            Slog.w(TAG, "SIM operator unavailable: " + e.getMessage());
        }
        return plmn + randomDigits(15 - plmn.length());
    }

    /** 19 digits: 89 + 16 random digits + Luhn check digit. */
    private static String randomIccid() {
        String body = "89" + randomDigits(16);
        return body + luhnCheckDigit(body);
    }

    /** Globally-unique unicast style MAC (the two low bits of the first octet cleared). */
    private static String randomMac() {
        byte[] bytes = new byte[6];
        RANDOM.nextBytes(bytes);
        bytes[0] = (byte) (bytes[0] & 0xFC);
        return String.format(Locale.US, "%02x:%02x:%02x:%02x:%02x:%02x",
                bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5]);
    }

    private static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private static int luhnCheckDigit(String digits) {
        int sum = 0;
        boolean doubleIt = true;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return (10 - (sum % 10)) % 10;
    }
}
