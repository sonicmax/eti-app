package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class PostMessageHandler {

    private String LOG_TAG = PostTopicHandler.class.getSimpleName();
    private Context mContext;
    private ProgressDialog mDialog;
    private Topic mTopic;

    public PostMessageHandler(Context context, Topic topic) {

        this.mContext = context;
        this.mDialog = new ProgressDialog(context);
        this.mTopic = topic;
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
                // Create new intent for MessageListActivity using Topic data
                Intent intent = new Intent(mContext, MessageListActivity.class);
                intent.putExtra("topic", mTopic);
                intent.putExtra("title", mTopic.getTitle());
                intent.putExtra("last_page", true);
                mContext.startActivity(intent);

                mDialog.dismiss();
            }

        }.execute();

    }

}
