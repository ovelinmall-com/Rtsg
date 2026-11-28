package top.niunaijun.blackbox.fake.service;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.app.BRAppOpsManager;
import black.android.os.BRServiceManager;
import black.com.android.internal.app.BRIAppOpsServiceStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Slog;


public class IAppOpsManagerProxy extends BinderInvocationStub {
    public IAppOpsManagerProxy() {
        super(BRServiceManager.get().getService(Context.APP_OPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder call = BRServiceManager.get().getService(Context.APP_OPS_SERVICE);
        return BRIAppOpsServiceStub.get().asInterface(call);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (BRAppOpsManager.get(null)._check_mService() != null) {
            AppOpsManager appOpsManager = (AppOpsManager) BlackBoxCore.getContext().getSystemService(Context.APP_OPS_SERVICE);
            try {
                BRAppOpsManager.get(appOpsManager)._set_mService(getProxyInvocation());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        replaceSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        
        
        
        if (methodName.startsWith("check") || 
            methodName.startsWith("note") || 
            methodName.startsWith("start")) {
            
            
            Slog.d(TAG, "AppOps invoke: Bypassing system for " + methodName + ", allowing operation");
            return allowedResultFor(method);
        }
        
        
        if (methodName.startsWith("finish")) {
            Slog.d(TAG, "AppOps invoke: Bypassing system for " + methodName);
            return null;
        }
        
        
        try {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            
            Slog.w(TAG, "AppOps invoke: SecurityException caught for " + methodName + ", allowing operation", e);
            return AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Slog.e(TAG, "AppOps invoke: Error in method " + methodName, e);
            
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("noteProxyOperation")
    public static class NoteProxyOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkPackage")
    public static class CheckPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOperation")
    public static class CheckOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            
            Slog.d(TAG, "AppOps CheckOperation: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    
    @ProxyMethod("checkOperationForDevice")
    public static class CheckOperationForDevice extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps CheckOperationForDevice: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("noteOperation")
    public static class NoteOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps NoteOperation: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOpNoThrow")
    public static class CheckOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps CheckOpNoThrow: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    
    @ProxyMethod("startOp")
    public static class StartOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps StartOp: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("startOpNoThrow")
    public static class StartOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps StartOpNoThrow: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    
    @ProxyMethod("finishOp")
    public static class FinishOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && isMediaStorageOrAudioOp(name)) {
                    Slog.d(TAG, "AppOps FinishOp: Finishing operation: " + name);
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    
    @ProxyMethod("noteOp")
    public static class NoteOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            
            
            Slog.d(TAG, "AppOps NoteOp: Bypassing system check, allowing operation");
            return allowedResultFor(method);
        }
    }

    
    
    
    
    @ProxyMethod("noteOpNoThrow")
    public static class NoteOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AppOps NoteOpNoThrow: Bypassing system check, allowing operation");
            return allowedResultFor(method);
        }
    }

    
    
    // This proxy fires on every single noteOperation/checkOperation/startOperation call the
    // virtual app's framework code makes - which on a busy, heavily-instrumented app like
    // Facebook is constant, often from paths that block a calling thread on the result. The old
    // code below re-resolved SyncNotedAppOp and its constructor with Class.forName() +
    // reflection, and used a thrown NoSuchMethodException as normal control flow, on EVERY call.
    // Exceptions are expensive on ART (they fill in a stack trace), so doing that on a hot path
    // this frequent is a real, measurable source of UI-thread stutter, not just noise. Resolve
    // everything once, statically, and never throw here in the common case.
    private static final Class<?> SYNC_NOTED_APP_OP_CLASS;
    private static final java.lang.reflect.Constructor<?> SYNC_NOTED_APP_OP_CTOR_2;
    private static final java.lang.reflect.Constructor<?> SYNC_NOTED_APP_OP_CTOR_1;

    static {
        Class<?> cls = null;
        java.lang.reflect.Constructor<?> ctor2 = null;
        java.lang.reflect.Constructor<?> ctor1 = null;
        try {
            cls = Class.forName("android.app.SyncNotedAppOp");
            try {
                ctor2 = cls.getConstructor(int.class, String.class);
            } catch (NoSuchMethodException ignored) {
            }
            if (ctor2 == null) {
                try {
                    ctor1 = cls.getConstructor(int.class);
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        SYNC_NOTED_APP_OP_CLASS = cls;
        SYNC_NOTED_APP_OP_CTOR_2 = ctor2;
        SYNC_NOTED_APP_OP_CTOR_1 = ctor1;
    }

    private static Object allowedResultFor(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == int.class || returnType == Integer.class) {
            return AppOpsManager.MODE_ALLOWED;
        }
        if (SYNC_NOTED_APP_OP_CLASS != null && returnType.isAssignableFrom(SYNC_NOTED_APP_OP_CLASS)) {
            try {
                if (SYNC_NOTED_APP_OP_CTOR_2 != null) {
                    return SYNC_NOTED_APP_OP_CTOR_2.newInstance(AppOpsManager.MODE_ALLOWED, (String) null);
                }
                if (SYNC_NOTED_APP_OP_CTOR_1 != null) {
                    return SYNC_NOTED_APP_OP_CTOR_1.newInstance(AppOpsManager.MODE_ALLOWED);
                }
            } catch (Throwable ignored) {
            }
        }
        return AppOpsManager.MODE_ALLOWED;
    }

    private static boolean isMediaStorageOrAudioOp(String opPublicNameOrStr) {
        if (opPublicNameOrStr == null) return false;
        
        String n = opPublicNameOrStr.toUpperCase();
        return n.contains("READ_MEDIA")
                || n.contains("READ_EXTERNAL_STORAGE")
                || n.contains("RECORD_AUDIO")
                || n.contains("CAPTURE_AUDIO_OUTPUT")
                || n.contains("MODIFY_AUDIO_SETTINGS")
                || n.contains("AUDIO")
                || n.contains("MICROPHONE")
                || n.contains("FOREGROUND_SERVICE")
                || n.contains("SYSTEM_ALERT_WINDOW")
                || n.contains("WRITE_SETTINGS")
                || n.contains("ACCESS_FINE_LOCATION")
                || n.contains("ACCESS_COARSE_LOCATION")
                || n.contains("CAMERA")
                || n.contains("BODY_SENSORS")
                || n.contains("BLUETOOTH_SCAN")
                || n.contains("BLUETOOTH_CONNECT")
                || n.contains("BLUETOOTH_ADVERTISE")
                || n.contains("NEARBY_WIFI_DEVICES")
                || n.contains("POST_NOTIFICATIONS");
    }

    private static String getOpPublicName(int op) {
        try {
            
            java.lang.reflect.Method m = AppOpsManager.class.getMethod("opToPublicName", int.class);
            Object name = m.invoke(null, op);
            return name != null ? name.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
