package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Handles initial postmsg.php GET request (to retrieve value of hidden token) and subsequent POST requests
 */
public class PostTopicHandler {

    private final String LOG_TAG = PostTopicHandler.class.getSimpleName();
    private Context mContext;
    private ContentValues mValues;
    private ProgressDialog mDialog;

    public PostTopicHandler(Context context, ContentValues values) {

        mContext = context;
        mValues = values;
        mDialog = new ProgressDialog(context);
    }

    public void getHiddenToken() {

        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "newtopic");

        new WebRequestAsyncTask(mContext, args) {

            @Override
            protected void onPreExecute() {
                mDialog.setMessage("Loading...");
                mDialog.show();
            }

            @Override
            protected void onPostExecute(String response) {

                Document document = Jsoup.parse(response);

                String token = scrapeToken(document);
                SharedPreferenceManager.putString(mContext, "h", token);

                String signature = scrapeSignature(document);
                SharedPreferenceManager.putString(mContext, "signature", signature);

                mDialog.dismiss();
            }

        }.execute();

    }

    private String scrapeToken(Document document) {
        Element tokenField = document.select("[name=h]").get(0);
        return tokenField.attr("value");
    }

    private String scrapeSignature(Document document) {
        Element messageInput = document.getElementById("message");
        return messageInput.text().trim();
    }

    public void submitTopic() {

        Bundle args = new Bundle();
        args.putString("method", "POST");
        args.putString("type", "newtopic");
        args.putParcelable("values", mValues);

        new WebRequestAsyncTask(mContext, args) {

            @Override
            protected void onPreExecute() {
                mDialog.setMessage("Creating topic...");
                mDialog.show();
            }

            @Override
            protected void onPostExecute(String response) {
                // Redirect user to topic that was just created
                mDialog.dismiss();
            }

        }.execute();

    }


}
