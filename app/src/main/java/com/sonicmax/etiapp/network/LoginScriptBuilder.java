package com.sonicmax.etiapp.network;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

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
                .appendQueryParameter("ip", wifiIpAddress(mContext));

        return builder.build().toString();
    }

    private String wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endian if needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        try {
            return InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("wifiIpAddress", "Unable to get host address.");
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
