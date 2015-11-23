package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


public class AccountManager {

    private String LOG_TAG = AccountManager.class.getSimpleName();
    private Context mContext;
    private ProgressDialog mDialog;

    public AccountManager(Context context, ProgressDialog dialog) {
        mContext = context;
        mDialog = dialog;
    }

    public void requestLogout() {

        mDialog = new ProgressDialog(mContext);
        mDialog.setMessage("Logging out...");
        mDialog.show();

        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "logout");

        new WebRequestAsyncTask(mContext, args) {

            @Override
            public void onPostExecute(String response) {
                mDialog.dismiss();
                SharedPreferenceManager.putBoolean(mContext, "is_logged_in", false);
                Intent intent = new Intent(mContext, LoginActivity.class);
                mContext.startActivity(intent);
            }

        }.execute();

    }

}
