package top.niunaijun.blackbox.fake.service;



import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class IAttributionSourceProxy extends ClassInvocationStub {
    public static final String TAG = "IAttributionSourceProxy";

    // AttributionSource crosses process boundaries on nearly every permission-attributed Binder
    // call a modern Android app makes (location, camera, mic, media, notifications...), so for a
    // busy app like Facebook this constructor/hook fires constantly. Re-resolving the class, the
    // right constructor (via chained NoSuchMethodException fallbacks - exceptions are expensive
    // on ART) and the setUid method via reflection on every single call was a real, continuous
    // tax on whatever thread triggered it, which is very often the UI thread. Resolve all of it
    // once, statically, instead.
    private static final Class<?> ATTR_SOURCE_CLASS;
    private static final Constructor<?> ATTR_SOURCE_CTOR;
    private static final int ATTR_SOURCE_CTOR_ARITY;
    private static final Method ATTR_SOURCE_SET_UID;

    static {
        Class<?> cls = null;
        Constructor<?> ctor = null;
        int arity = 0;
        Method setUid = null;
        try {
            cls = Class.forName("android.content.AttributionSource");
            try {
                ctor = cls.getDeclaredConstructor(int.class, String.class, String.class);
                arity = 3;
            } catch (NoSuchMethodException e1) {
                try {
                    ctor = cls.getDeclaredConstructor(int.class, String.class);
                    arity = 2;
                } catch (NoSuchMethodException e2) {
                    try {
                        ctor = cls.getDeclaredConstructor();
                        arity = 0;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }
            if (ctor != null) {
                ctor.setAccessible(true);
            }
            try {
                setUid = cls.getDeclaredMethod("setUid", int.class);
                setUid.setAccessible(true);
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Throwable ignored) {
        }
        ATTR_SOURCE_CLASS = cls;
        ATTR_SOURCE_CTOR = ctor;
        ATTR_SOURCE_CTOR_ARITY = arity;
        ATTR_SOURCE_SET_UID = setUid;
    }

    private static Object createSafeAttributionSourceStatic(int uid, String packageName) {
        if (ATTR_SOURCE_CTOR == null) {
            return null;
        }
        try {
            switch (ATTR_SOURCE_CTOR_ARITY) {
                case 3:
                    return ATTR_SOURCE_CTOR.newInstance(uid, packageName, null);
                case 2:
                    return ATTR_SOURCE_CTOR.newInstance(uid, packageName);
                default:
                    return ATTR_SOURCE_CTOR.newInstance();
            }
        } catch (Throwable e) {
            Slog.e(TAG, "Failed to create safe AttributionSource: " + e.getMessage());
            return null;
        }
    }

    public IAttributionSourceProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        
        Slog.d(TAG, "AttributionSource proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("AttributionSource")
    public static class AttributionSourceConstructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                int uid = BlackBoxCore.getHostUid();
                String packageName = BlackBoxCore.getHostPkg();
                
                Slog.d(TAG, "Creating AttributionSource with UID: " + uid + ", package: " + packageName);
                
                
                Object attributionSource = createSafeAttributionSource(uid, packageName);
                if (attributionSource != null) {
                    return attributionSource;
                }
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error creating AttributionSource, using fallback: " + e.getMessage());
                return createSafeAttributionSource(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
            }
        }
        
        private Object createSafeAttributionSource(int uid, String packageName) {
            return createSafeAttributionSourceStatic(uid, packageName);
        }
    }

    
    @ProxyMethod("enforceCallingUid")
    public static class EnforceCallingUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                Slog.d(TAG, "Intercepting enforceCallingUid to prevent UID mismatch crashes");
                return null; 
            } catch (Exception e) {
                Slog.w(TAG, "Error in enforceCallingUid hook: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("enforceCallingUidAndPid")
    public static class EnforceCallingUidAndPid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                Slog.d(TAG, "Intercepting enforceCallingUidAndPid to prevent UID mismatch crashes");
                return null; 
            } catch (Exception e) {
                Slog.w(TAG, "Error in enforceCallingUidAndPid hook: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("fromParcel")
    public static class FromParcel extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    
                    fixAttributionSourceUid(result);
                    return result;
                }
                
                
                return createSafeAttributionSource(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
            } catch (Exception e) {
                Slog.w(TAG, "Error in fromParcel, using fallback: " + e.getMessage());
                return createSafeAttributionSource(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
            }
        }
        
        private void fixAttributionSourceUid(Object attributionSource) {
            if (ATTR_SOURCE_SET_UID == null) {
                return;
            }
            try {
                ATTR_SOURCE_SET_UID.invoke(attributionSource, BlackBoxCore.getHostUid());
            } catch (Exception e) {
                Slog.w(TAG, "Could not fix AttributionSource UID: " + e.getMessage());
            }
        }

        private Object createSafeAttributionSource(int uid, String packageName) {
            return createSafeAttributionSourceStatic(uid, packageName);
        }
    }
}
