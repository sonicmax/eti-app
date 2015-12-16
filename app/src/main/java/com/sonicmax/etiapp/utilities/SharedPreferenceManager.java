package com.sonicmax.etiapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceManager {

    public static void putString(Context context, String key, String value) {

        SharedPreferences prefs = context.getSharedPreferences(
                "com.sonicmax.etiapp", Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    public static String getString(Context context, String key) {

        SharedPreferences prefs = context.getSharedPreferences(
                "com.sonicmax.etiapp", Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    public static void putBoolean(Context context, String key, boolean value) {

        SharedPreferences prefs = context.getSharedPreferences(
                "com.sonicmax.etiapp", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key) {

        SharedPreferences prefs = context.getSharedPreferences(
                "com.sonicmax.etiapp", Context.MODE_PRIVATE);
        // Return false if boolean doesn't exist, and make sure that keys imply truthy values
        return prefs.getBoolean(key, false);
    }

    public static void putInt(Context context, String key, int value) {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.sonicmax.etiapp", Context.MODE_PRIVATE);
        prefs.edit().putInt(key, value).apply();
    }

    public static int getInt(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.sonicmax.etiapp", Context.MODE_PRIVATE);
        // Return 0 if int is not present
        return prefs.getInt(key, 0);
    }

}
