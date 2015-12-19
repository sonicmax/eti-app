package com.sonicmax.etiapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

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

    /**
     *  Serialises List<String> so we can store it in SharedPreferences.
     *  SharedPreferences only accepts Set<String> which isn't always helpful
     */
    public static void putStringList(Context context, String key, List<String> list) {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.sonicmax.etiapp", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();

        // We need to store size of list under separate key so we know how many values to retrieve
        editor.putInt(key + "size", list.size());

        for (int i = 0; i < list.size(); i++) {
            editor.putString(key + i, list.get(i));
        }

        editor.apply();
    }

    public static List<String> getStringList(Context context, String key) {
        int size = SharedPreferenceManager.getInt(context, key + "size");
        List<String> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            list.add(SharedPreferenceManager.getString(context, key + i));
        }

        return list;
    }
}
