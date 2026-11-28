package top.niunaijun.blackbox.fake.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.security.SecureRandom;
import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.NativeCore;
import top.niunaijun.blackbox.utils.Md5Utils;
import top.niunaijun.blackbox.utils.Slog;

public final class DeviceSpoofManager {
    private static final String TAG = "DeviceSpoofManager";
    private static final String PREFS_NAME = "DeviceSpoofProfiles";

    private static final String KEY_MANUFACTURER = "manufacturer_";
    private static final String KEY_MODEL = "model_";
    private static final String KEY_BRAND = "brand_";
    private static final String KEY_DEVICE = "device_";
    private static final String KEY_PRODUCT = "product_";
    private static final String KEY_FINGERPRINT = "fingerprint_";
    private static final String KEY_SERIAL = "serial_";
    private static final String KEY_ANDROID_ID = "android_id_";

    private DeviceSpoofManager() {
    }

    @SuppressWarnings("deprecation")
    private static SharedPreferences prefs() {
        return BlackBoxCore.getContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    public static DeviceSpoofProfile getProfile(int userId) {
        DeviceSpoofProfile defaults = getDefaultProfile(userId);
        SharedPreferences prefs = prefs();
        return new DeviceSpoofProfile(
                valueOrDefault(prefs.getString(KEY_MANUFACTURER + userId, null), defaults.manufacturer),
                valueOrDefault(prefs.getString(KEY_BRAND + userId, null), defaults.brand),
                valueOrDefault(prefs.getString(KEY_MODEL + userId, null), defaults.model),
                valueOrDefault(prefs.getString(KEY_DEVICE + userId, null), defaults.device),
                valueOrDefault(prefs.getString(KEY_PRODUCT + userId, null), defaults.product),
                valueOrDefault(prefs.getString(KEY_FINGERPRINT + userId, null), defaults.fingerprint),
                valueOrDefault(prefs.getString(KEY_SERIAL + userId, null), defaults.serial),
                valueOrDefault(prefs.getString(KEY_ANDROID_ID + userId, null), defaults.androidId)
        );
    }

    public static DeviceSpoofProfile getDefaultProfile(int userId) {
        return new DeviceSpoofProfile(
                "Google",
                "google",
                "Pixel 6",
                "oriole",
                "oriole",
                "google/oriole/oriole:12/SP1A.210812.015/7679548:user/release-keys",
                "1A2B3C4D5E6F",
                stableAndroidId("bbb_default_" + userId)
        );
    }

    public static DeviceSpoofProfile getSamsungPreset(int userId) {
        return new DeviceSpoofProfile(
                "Samsung",
                "samsung",
                "Galaxy S26+",
                "s26plus",
                "s26plus",
                "samsung/s26plus/s26plus:16/BBX1.260329.001/S26PLUS:user/release-keys",
                "S26PBBX260329",
                stableAndroidId("bbb_samsung_" + userId)
        );
    }

    public static void saveUserProfile(int userId, String manufacturer, String model, String androidId) {
        DeviceSpoofProfile profile = buildCustomProfile(userId, manufacturer, model, androidId);
        prefs().edit()
                .putString(KEY_MANUFACTURER + userId, profile.manufacturer)
                .putString(KEY_BRAND + userId, profile.brand)
                .putString(KEY_MODEL + userId, profile.model)
                .putString(KEY_DEVICE + userId, profile.device)
                .putString(KEY_PRODUCT + userId, profile.product)
                .putString(KEY_FINGERPRINT + userId, profile.fingerprint)
                .putString(KEY_SERIAL + userId, profile.serial)
                .putString(KEY_ANDROID_ID + userId, profile.androidId)
                .commit();
    }


    // ---------------------------------------------------------------------------------------
    // Per-clone random identity. Generated ONCE when the clone is created and stored under that
    // virtual user's own keys, so no two clones share Android ID / serial / build fingerprint,
    // and deleting + re-creating a clone never brings an old identity back.
    // ---------------------------------------------------------------------------------------

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final char[] SERIAL_CHARS = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final String[] BUILD_PREFIXES = {"SP1A", "SQ3A", "TP1A", "TQ3A", "UP1A", "UQ1A", "AP3A", "AP4A", "BP1A"};

    /**
     * Creates and saves a fully random identity for this virtual user. manufacturer/model are
     * optional (blank falls back to the default Pixel 6 look); everything that identifies the
     * "device" - Android ID, serial, build fingerprint - is random and independent per call.
     */
    public static DeviceSpoofProfile saveRandomProfile(int userId, String manufacturer, String model) {
        DeviceSpoofProfile base = TextUtils.isEmpty(model) && TextUtils.isEmpty(manufacturer)
                ? getDefaultProfile(userId)
                : buildCustomProfile(userId, manufacturer, model, null);
        String androidId = randomString(HEX, 16);
        String serial = randomSerial();
        String fingerprint = randomFingerprint(base.brand, base.product, base.device);
        DeviceSpoofProfile profile = new DeviceSpoofProfile(
                base.manufacturer, base.brand, base.model, base.device, base.product,
                fingerprint, serial, androidId);
        prefs().edit()
                .putString(KEY_MANUFACTURER + userId, profile.manufacturer)
                .putString(KEY_BRAND + userId, profile.brand)
                .putString(KEY_MODEL + userId, profile.model)
                .putString(KEY_DEVICE + userId, profile.device)
                .putString(KEY_PRODUCT + userId, profile.product)
                .putString(KEY_FINGERPRINT + userId, profile.fingerprint)
                .putString(KEY_SERIAL + userId, profile.serial)
                .putString(KEY_ANDROID_ID + userId, profile.androidId)
                .commit();
        DeviceSpoofIds.generate(userId, profile);
        Slog.d(TAG, "Generated random identity for user " + userId);
        return profile;
    }

    private static String randomString(char[] alphabet, int length) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = alphabet[RANDOM.nextInt(alphabet.length)];
        }
        return new String(out);
    }

    private static String randomSerial() {
        // Same look as a real retail serial: 10-12 upper-case alphanumerics.
        return randomString(SERIAL_CHARS, 10 + RANDOM.nextInt(3));
    }

    private static String randomFingerprint(String brand, String product, String device) {
        // brand/product/device:RELEASE/BUILD_ID/INCREMENTAL:user/release-keys
        // RELEASE is the host's real Android version so the fingerprint stays consistent with
        // Build.VERSION.* that the guest also sees.
        String release = android.os.Build.VERSION.RELEASE;
        if (TextUtils.isEmpty(release)) {
            release = "13";
        }
        String prefix = BUILD_PREFIXES[RANDOM.nextInt(BUILD_PREFIXES.length)];
        int year = 22 + RANDOM.nextInt(4);                 // 22..25
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28);
        String buildId = String.format(Locale.US, "%s.%02d%02d%02d.%03d",
                prefix, year, month, day, 1 + RANDOM.nextInt(60));
        String incremental = String.valueOf(1000000L + (long) (RANDOM.nextDouble() * 8999999L));
        return brand + "/" + product + "/" + device + ":" + release + "/" + buildId + "/" + incremental
                + ":user/release-keys";
    }

    public static void resetToDefaults(int userId) {
        DeviceSpoofIds.clear(userId);
        prefs().edit()
                .remove(KEY_MANUFACTURER + userId)
                .remove(KEY_BRAND + userId)
                .remove(KEY_MODEL + userId)
                .remove(KEY_DEVICE + userId)
                .remove(KEY_PRODUCT + userId)
                .remove(KEY_FINGERPRINT + userId)
                .remove(KEY_SERIAL + userId)
                .remove(KEY_ANDROID_ID + userId)
                .commit();
    }

    /** Spoofing applies to every virtual user: the default Pixel 6 profile, or a custom one if saved. */
    public static boolean isEnabled(int userId) {
        return true;
    }

    /** True when the user saved a custom profile for this virtual user. */
    public static boolean hasCustomProfile(int userId) {
        return prefs().contains(KEY_MODEL + userId);
    }

    public static void applyToCurrentProcess(int userId) {
        try {
            if (!isEnabled(userId)) {
                return;
            }
            DeviceSpoofProfile profile = getProfile(userId);
            DeviceBuildSpoofer.apply(profile);
            DeviceBuildSpoofer.applyExtras(DeviceSpoofIds.get(userId));
            NativeCore.setDeviceSpoof(
                    profile.manufacturer,
                    profile.brand,
                    profile.model,
                    profile.device,
                    profile.product,
                    profile.fingerprint,
                    profile.serial
            );
            Slog.d(TAG, "Applied device spoof for user " + userId + ": " + profile.manufacturer + " " + profile.model);
        } catch (Throwable e) {
            Slog.w(TAG, "Failed to apply device spoof for user " + userId + ": " + e.getMessage());
        }
    }

    public static String getAndroidIdForCurrentUser() {
        int userId;
        try {
            userId = BActivityThread.getUserId();
        } catch (Throwable e) {
            userId = 0;
        }
        return getProfile(userId).androidId;
    }

    /** True only when the user enabled spoofing for the virtual user of the current process. */
    public static boolean isCurrentUserEnabled() {
        try {
            return isEnabled(BActivityThread.getUserId());
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isValidAndroidId(String value) {
        return !TextUtils.isEmpty(value) && value.matches("(?i)[0-9a-f]{16}");
    }

    private static DeviceSpoofProfile buildCustomProfile(int userId, String manufacturer, String model, String androidId) {
        String safeManufacturer = normalizeManufacturer(manufacturer);
        String safeBrand = normalizeBrand(safeManufacturer);
        String safeModel = normalizeModel(model);
        String safeDevice = slugify(safeModel);
        String safeProduct = safeDevice;
        String safeFingerprint = safeBrand + "/" + safeProduct + "/" + safeDevice + ":16/BBX1.260329.001/BBB001:user/release-keys";
        String safeSerial = stableSerial(safeManufacturer + "_" + safeModel + "_" + userId);
        String safeAndroidId = normalizeAndroidId(androidId, "bbb_custom_" + userId + "_" + safeDevice);
        return new DeviceSpoofProfile(
                safeManufacturer,
                safeBrand,
                safeModel,
                safeDevice,
                safeProduct,
                safeFingerprint,
                safeSerial,
                safeAndroidId
        );
    }

    private static String normalizeManufacturer(String manufacturer) {
        if (TextUtils.isEmpty(manufacturer)) {
            return "Google";
        }
        String trimmed = manufacturer.trim();
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase(Locale.US);
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.US) + trimmed.substring(1);
    }

    private static String normalizeBrand(String manufacturer) {
        return slugify(manufacturer);
    }

    private static String normalizeModel(String model) {
        if (TextUtils.isEmpty(model)) {
            return "Pixel 6";
        }
        return model.trim();
    }

    private static String normalizeAndroidId(String androidId, String seed) {
        if (!TextUtils.isEmpty(androidId)) {
            String normalized = androidId.trim().toLowerCase(Locale.US);
            if (isValidAndroidId(normalized)) {
                return normalized;
            }
        }
        return stableAndroidId(seed);
    }

    private static String stableAndroidId(String seed) {
        String hash = Md5Utils.md5(seed);
        if (TextUtils.isEmpty(hash)) {
            return "0f0f0f0f0f0f0f0f";
        }
        String normalized = hash.toLowerCase(Locale.US);
        return normalized.length() >= 16 ? normalized.substring(0, 16) : String.format(Locale.US, "%1$-16s", normalized).replace(' ', '0');
    }

    private static String stableSerial(String seed) {
        String hash = Md5Utils.md5(seed);
        if (TextUtils.isEmpty(hash)) {
            return "BBB000000000";
        }
        String normalized = hash.toUpperCase(Locale.US);
        return normalized.length() >= 12 ? normalized.substring(0, 12) : String.format(Locale.US, "%1$-12s", normalized).replace(' ', '0');
    }

    private static String valueOrDefault(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private static String slugify(String input) {
        if (TextUtils.isEmpty(input)) {
            return "device";
        }
        String normalized = input.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
        return TextUtils.isEmpty(normalized) ? "device" : normalized;
    }
}
