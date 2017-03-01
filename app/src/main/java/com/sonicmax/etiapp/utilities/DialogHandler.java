package com.sonicmax.etiapp.utilities;

import android.app.ProgressDialog;
import android.content.Context;

public class DialogHandler {
    private static ProgressDialog mDialog;

    public static void showDialog(Context context, String message) {
        mDialog = new ProgressDialog(context);
        mDialog.setMessage(message);
        mDialog.show();
    }

    public static void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}
