package com.sonicmax.etiapp.utilities;

import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * Helper class which creates Snackbar messages
 */

public class Snacker {
    public static void showSnackBar(View root, String message) {
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    public static void showSnackBar(View root, String message, int duration) {
        Snackbar snackbar = Snackbar.make(root, message, duration);
        snackbar.show();
    }
}
