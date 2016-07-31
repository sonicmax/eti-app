package com.sonicmax.etiapp.network;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.sonicmax.etiapp.utilities.FormDataBuilder;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;
import com.sonicmax.etiapp.utilities.EtiUriBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class WebRequest {
    private final String LOG_TAG = WebRequest.class.getSimpleName();
    private Context mContext;
    private String mMethod;
    private String mRequestType;
    private ContentValues mValues;
    private URL mUrl;
    private Uri mUri;
    private CookieManager mCookieManager;

    /**
     * Handles GET/POST requests. Call from background thread.
     * @param context Context that WebRequest was instantiated in.
     * @param args Values for HTTPSUrlConnection
     */
    public WebRequest(Context context, Bundle args) {
        mContext = context;
        mMethod = args.getString("method");
        mRequestType = args.getString("type");
        mValues = args.getParcelable("values");

        if (args.getString("url") == null) {
            mUri = new EtiUriBuilder(mRequestType, mValues).build();
        }
        else {
            mUri = Uri.parse(args.getString("url"));
        }

        mCookieManager = new CookieManager();
    }

    public String sendRequest() {

        final String FORM_DATA = "application/x-www-form-urlencoded";
        final String PLAIN_TEXT = "text/plain;charset=UTF-8";

        HttpsURLConnection connection = null;
        PrintWriter writer;
        BufferedReader reader = null;
        String response;
        String formData;

        try {
            if (mUrl == null) {
                mUrl = new URL(mUri.toString());
            }

            // Log.v(LOG_TAG, url.toString());
            connection = (HttpsURLConnection) mUrl.openConnection();

            if (mCookieManager.getCookieStore().getCookies().size() > 0) {
                connection.setRequestProperty("Cookie",
                        TextUtils.join(";", mCookieManager.getCookieStore().getCookies()));
            } else {
                // Use stored cookies from sharedPreferences
                getCookies();
                connection.setRequestProperty("Cookie",
                        TextUtils.join(";", mCookieManager.getCookieStore().getCookies()));
            }

            if (mMethod.equals("POST")) {

                if (mValues == null) {
                    Log.e(LOG_TAG, "Cannot make POST request without ContentValues");
                    return null;
                }

                formData = new FormDataBuilder(mRequestType, mValues).build();

                // Set request headers
                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(true);
                connection.setFixedLengthStreamingMode(formData.getBytes().length);

                if (mRequestType.equals("livelinks")) {
                    connection.setRequestProperty("Content-Type", PLAIN_TEXT);
                }
                else {
                    connection.setRequestProperty("Content-Type", FORM_DATA);
                }

                // Send data to ETI
                writer = new PrintWriter(connection.getOutputStream());
                writer.print(formData);
                writer.close();

                if (mRequestType.equals("login")) {
                    // Get cookies from response
                    Map<String, List<String>> headerFields = connection.getHeaderFields();
                    storeCookies(headerFields);
                }

                if (!mRequestType.equals("livelinks")) {
                    // Return response code so we know whether POST was successful or not.
                    // For livelinks requests we want to return the response itself.
                    return Integer.toString(connection.getResponseCode());
                }
            }

            // Read input stream into a String
            InputStream inputStream = connection.getInputStream();
            StringBuilder builder = new StringBuilder();
            if (inputStream == null) {
                // No data to read.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }

            if (builder.length() == 0) {
                // Stream was empty.
                return null;
            }

            response = builder.toString();

        } catch (IOException e){
            Log.e(LOG_TAG, "Error requesting data ", e);
            // Error requesting data - no need to continue.
            return null;

        } finally {
            // Do some cleanup
            if (connection != null) {
                connection.disconnect();
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        // Log.v(LOG_TAG, response);
        return response;
    }

    public void getCookies() {
        int size = SharedPreferenceManager.getInt(mContext, "cookie_array_size");
        for (int i = 0; i < size; i++) {
            String cookie = SharedPreferenceManager.getString(mContext, "cookie_array" + i);
            if (cookie != null) {
                mCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
            }
        }
    }

    private void storeCookies(Map<String, List<String>> headerFields) {
        final String COOKIES_HEADER = "Set-Cookie";

        List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);

        if (cookiesHeader != null) {

            for (String cookie : cookiesHeader) {
                mCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
            }

            SharedPreferenceManager.putStringList(mContext, "cookie_array", cookiesHeader);

            if (!SharedPreferenceManager.getBoolean(mContext, "logged_in")) {
                SharedPreferenceManager.putBoolean(mContext, "logged_in", true);
            }
        }
    }
}
