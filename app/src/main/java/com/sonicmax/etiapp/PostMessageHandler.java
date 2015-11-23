package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class PostMessageHandler {

    private String LOG_TAG = PostTopicHandler.class.getSimpleName();
    private Context mContext;
    private ProgressDialog mDialog;

    public PostMessageHandler(Context context) {

        this.mContext = context;
        this.mDialog = new ProgressDialog(context);
    }


    public void getHiddenToken(final ContentValues values) {

        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "newmessage");
        args.putParcelable("values", values);

        new WebRequestAsyncTask(mContext, args) {

            @Override
            protected void onPreExecute() {
                mDialog.setMessage("Loading...");
                mDialog.show();
            }

            @Override
            protected void onPostExecute(String response) {

                // Retrieve token and store it for future requests
                String token = parseTokenField(response);
                SharedPreferenceManager.putString(mContext, "h", token);
                mDialog.dismiss();
            }

        }.execute();

    }

    private String parseTokenField(String response) {
        Document document = Jsoup.parse(response);
        Element tokenField = document.select("[name=h]").get(0);
        return tokenField.attr("value");
    }


    public void submitMessage(final ContentValues values) {

        Bundle args = new Bundle();
        args.putString("method", "POST");
        args.putString("type", "newmessage");
        args.putParcelable("values", values);

        new WebRequestAsyncTask(mContext, args) {

            @Override
            protected void onPreExecute() {
                mDialog.setMessage("Posting message...");
                mDialog.show();
            }

            @Override
            protected void onPostExecute(String response) {
                final int lastPage = Integer.parseInt(values.get("lastpage").toString());

                // Redirect user to last page of topic (so they can see their post in context).
                String newUrl = "https://boards.endoftheinter.net/showmessages.php?"
                        + "topic=" + values.get("topic")
                        + "&page=" + lastPage;

                // TODO: Create new intent for MessageListActivity here
            }

        }.execute();

    }

}
