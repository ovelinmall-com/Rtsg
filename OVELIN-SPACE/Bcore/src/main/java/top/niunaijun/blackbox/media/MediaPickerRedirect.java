package top.niunaijun.blackbox.media;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import top.niunaijun.blackbox.BlackBoxCore;

/**
 * When a cloned app asks Android to pick a photo/video (GET_CONTENT / PICK / OPEN_DOCUMENT /
 * PICK_IMAGES), send it to the host's shared-gallery picker instead of the phone's real gallery,
 * so every clone picks from the same unified gallery. Non-media pickers ("*&#47;*", documents,
 * audio, ...) are left alone.
 */
public final class MediaPickerRedirect {
    /** The host app declares an activity with this action (see the app manifest). */
    public static final String ACTION_PICK_SHARED_GALLERY = "top.niunaijun.blackbox.action.PICK_SHARED_GALLERY";
    public static final String EXTRA_ALLOW_MULTIPLE = "bbx_allow_multiple";
    public static final String EXTRA_WANT_IMAGES = "bbx_want_images";
    public static final String EXTRA_WANT_VIDEOS = "bbx_want_videos";
    public static final String EXTRA_ORIGINAL_ACTION = "bbx_original_action";

    private static final String ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES";

    private MediaPickerRedirect() {
    }

    /** Returns the replacement intent, or null when this intent is not a photo/video pick. */
    public static Intent redirect(Intent original) {
        if (original == null || original.getComponent() != null) {
            return null;
        }
        String action = original.getAction();
        if (!Intent.ACTION_GET_CONTENT.equals(action)
                && !Intent.ACTION_PICK.equals(action)
                && !Intent.ACTION_OPEN_DOCUMENT.equals(action)
                && !ACTION_PICK_IMAGES.equals(action)) {
            return null;
        }

        boolean images = false;
        boolean videos = false;
        boolean other = false;

        if (ACTION_PICK_IMAGES.equals(action)) {
            images = true;
            videos = true;
        }

        String[] extraTypes = original.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);
        if (extraTypes != null && extraTypes.length > 0) {
            for (String type : extraTypes) {
                if (isImage(type)) images = true;
                else if (isVideo(type)) videos = true;
                else other = true;
            }
        } else {
            String type = original.getType();
            if (!TextUtils.isEmpty(type)) {
                if (isImage(type)) images = true;
                else if (isVideo(type)) videos = true;
                else other = true;
            } else if (!ACTION_PICK_IMAGES.equals(action)) {
                // ACTION_PICK with a MediaStore collection URI and no explicit type.
                Uri data = original.getData();
                String text = data == null ? "" : data.toString();
                if (text.startsWith("content://media/") && text.contains("/images/")) images = true;
                else if (text.startsWith("content://media/") && text.contains("/video/")) videos = true;
                else other = true;
            }
        }

        if (other || (!images && !videos)) {
            return null;
        }

        Intent redirected = new Intent(ACTION_PICK_SHARED_GALLERY);
        redirected.setPackage(BlackBoxCore.getHostPkg());
        redirected.putExtra(EXTRA_WANT_IMAGES, images);
        redirected.putExtra(EXTRA_WANT_VIDEOS, videos);
        redirected.putExtra(EXTRA_ORIGINAL_ACTION, action);
        redirected.putExtra(EXTRA_ALLOW_MULTIPLE,
                original.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                        || ACTION_PICK_IMAGES.equals(action) && original.hasExtra("android.provider.extra.PICK_IMAGES_MAX")
                        && original.getIntExtra("android.provider.extra.PICK_IMAGES_MAX", 1) > 1);
        redirected.addFlags(original.getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));

        // Make it explicit: the caller's resolvedType (e.g. "image/*") is passed along with the
        // intent and would stop a type-less implicit filter from matching.
        android.content.pm.ResolveInfo info = BlackBoxCore.getContext().getPackageManager().resolveActivity(redirected, 0);
        if (info == null || info.activityInfo == null) {
            return null; // host has no shared-gallery picker: behave as before
        }
        redirected.setClassName(info.activityInfo.packageName, info.activityInfo.name);
        return redirected;
    }

    private static boolean isImage(String type) {
        return type != null && type.toLowerCase().startsWith("image/");
    }

    private static boolean isVideo(String type) {
        return type != null && type.toLowerCase().startsWith("video/");
    }
}
