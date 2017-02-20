package com.sonicmax.etiapp.network;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Gets current IP address and constructs string for scripts/login.php request.
 * The script tells us whether user is logged in at current IP address - however
 * it doesn't necessarily mean that our session token is valid. (eg. user could've
 * logged in using a different computer)
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
        String username = SharedPreferenceManager.getString(mContext, "username");
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("boards.endoftheinter.net")
                .appendPath("scripts")
                .appendPath("login.php")
                .appendQueryParameter("username", username)
                .appendQueryParameter("ip", getIp());

        return builder.build().toString();
    }

    private String getIp() {
        try {
            Document doc = Jsoup.connect("http://www.checkip.org").get();
            return doc.getElementById("yourip").select("h1").first().select("span").text();

        } catch(IOException error) {
            return null;
        }
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
