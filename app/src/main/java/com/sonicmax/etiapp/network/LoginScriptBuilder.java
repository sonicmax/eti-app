package com.sonicmax.etiapp.network;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

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
