package com.sonicmax.etiapp.utilities;

import android.content.Context;
import android.widget.Toast;

public class Toaster {
    private static Toast mToast;

    public static void makeToast(Context context, String text) {
        if (mToast != null) {
            mToast.cancel();
            mToast.setText(text);

        } else {
            mToast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        }

        mToast.show();
    }
}
