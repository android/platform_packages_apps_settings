package com.android.settings.wifi;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.util.Log;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WifiReflectMethod {

    private static final String TAG = WifiReflectMethod.class.getSimpleName();

    public static final class Settings {
        public static final class System {
            public static String WIFI_VALID_ENABLE = null;
        }
    }

    static {
        try {
            Field on = android.provider.Settings.System.class.getDeclaredField("WIFI_VALID_ENABLE");
            Settings.System.WIFI_VALID_ENABLE = (String) on.get(null);
        } catch (NoSuchFieldException e) {
            Log.d(TAG, "Field WIFI_VALID_ENABLE not found");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        // do nothing, for running static region first.
    }
}