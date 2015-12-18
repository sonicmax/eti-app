package com.sonicmax.etiapp.network;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class WebRequest {

    private final String LOG_TAG = WebRequest.class.getSimpleName();
    private Context mContext;
    private String httpMethod;
    private String requestType;
    private ContentValues values;
    private URL url;
    private Uri uri;
    private String mPayload;
    private CookieManager mCookieManager;

    /**
     * Handles GET/POST requests. Call from background thread.
     * @param context Context that WebRequest was instantiated in.
     * @param args Values for HTTPSUrlConnection
     */
    public WebRequest(Context context, Bundle args) {
        this.mContext = context;
        this.httpMethod = args.getString("method");
        this.requestType = args.getString("type");
        this.values = args.getParcelable("values");
        this.mPayload = args.getString("payload");

        if (args.getString("url") == null) {
            this.uri = createUriForRequest(requestType);
        }
        else {
            this.uri = Uri.parse(args.getString("url"));
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
            if (url == null) {
                url = new URL(uri.toString());
            }

            Log.v(LOG_TAG, url.toString());
            connection = (HttpsURLConnection) url.openConnection();

            if (mCookieManager.getCookieStore().getCookies().size() > 0) {
                connection.setRequestProperty("Cookie",
                        TextUtils.join(";", mCookieManager.getCookieStore().getCookies()));
            } else {
                // Get stored cookies from sharedPreferences
                getCookies();
                connection.setRequestProperty("Cookie",
                        TextUtils.join(";", mCookieManager.getCookieStore().getCookies()));
            }

            if (httpMethod.equals("POST")) {

                formData = createFormDataForRequest(requestType, values);

                if (formData == null) {
                    // Cancel request
                    return null;
                }

                // Set request headers
                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(true);
                connection.setFixedLengthStreamingMode(formData.getBytes().length);

                if (requestType.equals("livelinks")) {
                    connection.setRequestProperty("Content-Type", PLAIN_TEXT);
                }
                else {
                    connection.setRequestProperty("Content-Type", FORM_DATA);
                }

                // Send data to ETI
                writer = new PrintWriter(connection.getOutputStream());
                writer.print(formData);
                writer.close();

                // Log.v(LOG_TAG, connection.getHeaderFields().toString());

                if (requestType.equals("login")) {
                    // Get cookies from response
                    Map<String, List<String>> headerFields = connection.getHeaderFields();
                    storeCookies(headerFields);
                }

                if (!requestType.equals("livelinks")) {
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
            Log.e(LOG_TAG, "Error ", e);
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
            String cookie = SharedPreferenceManager.getString(mContext, "cookie_array_" + i);
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

            if (!SharedPreferenceManager.getBoolean(mContext, "logged_in")) {

                SharedPreferenceManager.putBoolean(mContext, "logged_in", true);
                SharedPreferenceManager.putInt(mContext, "cookie_array_size", cookiesHeader.size());

                for (int i = 0; i < cookiesHeader.size(); i++) {
                    SharedPreferenceManager.putString(mContext,
                            "cookie_array_" + i, cookiesHeader.get(i));
                }
            }
        }
    }

    private Uri createUriForRequest(String requestType) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https");

        switch (requestType) {
            case "topiclist":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("topics")
                        .appendPath(values.get("tags").toString());
                break;

            case "newtopic":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("postmsg.php");
                break;

            case "newmessage":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("postmsg.php")
                        .appendQueryParameter("topic", values.get("id").toString());
                break;

            case "livelinks":
                builder.authority("evt0.endoftheinter.net")
                        .appendPath("subscribe");
                break;

            case "moremessages":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("moremessages.php")
                        .appendQueryParameter("topic", values.get("topic").toString())
                        .appendQueryParameter("old", values.get("old").toString())
                        .appendQueryParameter("new", values.get("new").toString())
                        .appendQueryParameter("filter", values.get("filter").toString());
                break;
            
            case "home":
                builder.authority("endoftheinter.net")
                        .appendPath("main.php");
                break;

            case "login":
                builder.authority("iphone.endoftheinter.net");
                break;

            case "logout":
                builder.authority("endoftheinter.net")
                        .appendPath("logout.php");
                break;

            default:
                break;
        }

        return builder.build();
    }

    private String createFormDataForRequest(String requestType, ContentValues values) {
        String topic, formData, message, h, submit;
        try {
            switch (requestType) {
                case "newmessage":
                    topic = values.get("topic").toString();
                    message = values.get("message").toString();
                    h = values.get("h").toString();
                    submit = "Post Message";
                    formData = "topic=" + topic + "&message=" + message + "&h=" + h
                            + "&submit=" + submit;
                    break;

                case "newtopic":
                    String title = values.get("title").toString();
                    String tag = values.get("tag").toString();
                    message = values.get("message").toString();
                    h = values.get("h").toString();
                    submit = "Post Message";
                    formData = "title=" + title + "&tag=" + tag
                            + "&message=" + message + "&h=" + h
                            + "&submit=" + submit;
                    break;

                case "login":
                    String username = values.get("username").toString();
                    String password = values.get("password").toString();
                    formData = "username=" + username + "&password=" + password;
                    break;

                case "livelinks":
                    formData = mPayload;
                    break;

                default:
                    return null;
            }

            URLEncoder.encode(formData, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Error creating form data: ", e);
            return null;
        }

        return formData;
    }
}
