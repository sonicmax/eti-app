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
import java.io.FileNotFoundException;
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
    private final String FILE_NOT_FOUND = "404";

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
            mUri = new EtiUriBuilder().build(mRequestType, mValues);
        }
        else {
            mUri = Uri.parse(args.getString("url"));
        }

        mCookieManager = new CookieManager();
    }

    public String sendRequest() {
        final String FORM_DATA = "application/x-www-form-urlencoded";
        final String PLAIN_TEXT = "text/plain;charset=UTF-8";
        final int HTTP_INTERNAL_SERVER_ERROR = 500;

        HttpsURLConnection connection = null;
        PrintWriter writer;
        BufferedReader reader = null;
        String response;
        String formData;

        try {
            if (mUrl == null) {
                mUrl = new URL(mUri.toString());
            }

            Log.v(LOG_TAG, mUrl.toString());
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

            // Handle POST requests
            if (mMethod.equals("POST")) {

                if (mValues == null) {
                    Log.e(LOG_TAG, "Cannot make POST request without mValues");
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
                    // For livelinks requests we want to wait for actual response
                    return Integer.toString(connection.getResponseCode());
                }
            }

            // Return response code as string if we encounter internal server error
            if (mRequestType.matches("home|topiclist")
                    && connection.getResponseCode() == HTTP_INTERNAL_SERVER_ERROR) {
                return Integer.toString(connection.getResponseCode());
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

        } catch (FileNotFoundException fileNotFound) {
            if (mRequestType.equals("livelinks")) {
                // Livelinks server returns null as part of normal operation, so we should
                // return a specific string and check for it in LivelinksSubscriber.
                return FILE_NOT_FOUND;
            }
            else {
                return null;
            }

        } catch (IOException e){
            Log.e(LOG_TAG, "Error requesting data:", e);
            return null;

        } finally {
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

        return response;
    }

    private void getCookies() {
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
