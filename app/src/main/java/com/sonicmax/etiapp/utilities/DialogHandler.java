package com.sonicmax.etiapp.utilities;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class DialogHandler {
    private static final String LOG_TAG = "DialogHandler";
    private static ProgressDialog mDialog;

    public static void showDialog(Context context, String message) {
        if (!((AppCompatActivity) context).isFinishing()) {
        mDialog = new ProgressDialog(context);
        mDialog.setMessage(message);
        mDialog.show();
    }
        else {
            Log.e(LOG_TAG, "Tried to show dialog while activity was finishing. \n" +
                    "Message:" + message);
        }
    }

    public static void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}
