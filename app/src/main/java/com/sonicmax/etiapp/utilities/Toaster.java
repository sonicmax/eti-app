package com.sonicmax.etiapp.utilities;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

public class Toaster {

    private static Toast mToast;

    public static void makeToast(Context context, String text) {

        if (mToast != null)
            mToast.cancel();

        boolean condition = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;

        if ((mToast == null && condition) || !condition) {
            mToast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        }
        else if ((mToast != null && condition)) {
            mToast.setText(text);
        }

        mToast.show();
    }
}
