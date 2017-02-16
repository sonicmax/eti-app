package com.sonicmax.etiapp.utilities;

import android.content.ContentValues;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FormDataBuilder {
    private final String LOG_TAG = FormDataBuilder.class.getSimpleName();

    private String mRequestType;
    private ContentValues mValues;

    public FormDataBuilder (String requestType, ContentValues values) {
        mRequestType = requestType;
        mValues = values;
    }

    public String build() {
        String topic, formData, message, h, submit;
        try {
            switch (mRequestType) {
                case "newmessage":
                    topic = mValues.get("id").toString();
                    message = mValues.get("message").toString();
                    h = mValues.get("h").toString();
                    submit = "Post Message";
                    formData = "topic=" + topic + "&message=" + message + "&h=" + h
                            + "&submit=" + submit;
                    break;

                case "newpm":
                    topic = mValues.get("pm").toString();
                    message = mValues.get("message").toString();
                    h = mValues.get("h").toString();
                    submit = "Post Message";
                    formData = "pm=" + topic + "&message=" + message + "&h=" + h
                            + "&submit=" + submit;
                    break;

                case "newtopic":
                    String title = mValues.get("title").toString();
                    String tag = mValues.get("tag").toString();
                    message = mValues.get("message").toString();
                    h = mValues.get("h").toString();
                    submit = "Post Message";
                    formData = "title=" + title + "&tag=" + tag
                            + "&message=" + message + "&h=" + h
                            + "&submit=" + submit;
                    break;

                case "login":
                    String username = mValues.get("username").toString();
                    String password = mValues.get("password").toString();
                    formData = "username=" + username + "&password=" + password;
                    break;

                case "livelinks":
                    formData = mValues.get("payload").toString();
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
