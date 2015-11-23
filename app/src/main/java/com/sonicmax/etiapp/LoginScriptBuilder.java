package com.sonicmax.etiapp;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Gets current IP address and constructs string for scripts/login.php request.
 */

public class LoginScriptBuilder extends AsyncTaskLoader<Object> {

    private final String LOG_TAG = LoginScriptBuilder.class.getSimpleName();
    private Context mContext;

    public LoginScriptBuilder(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public String loadInBackground() {
        String url;
        String username = SharedPreferenceManager.getString(mContext, "username");

        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("boards.endoftheinter.net")
                    .appendPath("scripts")
                    .appendPath("login.php")
                    .appendQueryParameter("username", username)
                    .appendQueryParameter("ip", ipAddress);

            url = builder.build().toString();

        } catch (UnknownHostException e) {

            Log.e(LOG_TAG, "Error in loadInBackground", e);
            return null;

        }

        return url;
    }

    @Override
    protected void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
