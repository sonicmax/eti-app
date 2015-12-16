package com.sonicmax.etiapp.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.sonicmax.etiapp.utilities.AsyncLoadHandler;
import com.sonicmax.etiapp.LoginActivity;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;


public class AccountManager {

    private final String LOG_TAG = AccountManager.class.getSimpleName();
    private final int LOGOUT = 1;

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

        ((FragmentActivity) mContext).getSupportLoaderManager()
                .initLoader(LOGOUT, args, callbacks)
                .forceLoad();
    }

    private LoaderManager.LoaderCallbacks<Object> callbacks = new LoaderManager.LoaderCallbacks<Object>() {

        public Loader<Object> onCreateLoader(int id, final Bundle args) {
            return new AsyncLoadHandler(mContext, args) {

                @Override
                public String loadInBackground() {
                    return new WebRequest(mContext, args).sendRequest();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Object> loader, Object data) {
            switch (loader.getId()) {
                case LOGOUT:
                    mDialog.dismiss();
                    SharedPreferenceManager.putBoolean(mContext, "is_logged_in", false);
                    Intent intent = new Intent(mContext, LoginActivity.class);
                    mContext.startActivity(intent);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Object> loader) {
            loader.reset();
        }
    };

}
