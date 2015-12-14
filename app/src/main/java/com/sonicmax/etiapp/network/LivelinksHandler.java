package com.sonicmax.etiapp.network;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.sonicmax.etiapp.AsyncLoadHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;

public class LivelinksHandler {
    private final String LOG_TAG = LivelinksHandler.class.getSimpleName();
    private final int LIVELINKS = 1;
    private final int FETCH_MESSAGE = 2;
    private final int SHIFT_CONSTANT = 48;

    private final Context mContext;
    private final int mTopicId;
    private final int mUserId;

    private int mTopicSize;
    private int mInboxSize;

    public LivelinksHandler(Context context, String topicId, String userId, int topicSize, int inboxSize) {
        this.mContext = context;
        this.mTopicId = Integer.parseInt(topicId);
        this.mUserId = Integer.parseInt(userId);
        this.mTopicSize = topicSize;
        this.mInboxSize = inboxSize;
    }

    public void subscribeToUpdates() {
        JSONObject payload = buildLivelinksPayload();

        Bundle args = new Bundle(1);
        args.putString("method", "POST");
        args.putString("type", "livelinks");
        args.putString("payload", payload.toString());

        ((FragmentActivity) mContext).getSupportLoaderManager()
                .initLoader(LIVELINKS, args, callbacks)
                .forceLoad();
    }

    public void onReceiveUpdate(String message) {
        // Override this when instantiating LivelinksHandler
    }

    public BigInteger getTopicPayload() {
        final int TOPIC_CHANNEL = 0x0200;
        return BigInteger.valueOf(TOPIC_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(mTopicId));
    }

    public BigInteger getInboxPayload() {
        final int INBOX_CHANNEL = 0x0100;
        return BigInteger.valueOf(INBOX_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(mUserId));
    }

    private JSONObject buildLivelinksPayload() {
        BigInteger topic = getTopicPayload();
        BigInteger inbox = getInboxPayload();

        JSONObject payload = new JSONObject();

        try {
            payload.put(topic.toString(), mTopicSize);
            payload.put(inbox.toString(), 0);
            Log.v(LOG_TAG, "Payload: " + payload.toString());
        } catch (JSONException e) {
            throw new AssertionError(e);
        }

        return payload;
    }

    public JSONObject parseLivelinksResponse(String response) {
        // TODO: This will probably break if user has unread PMs
        String trimmedResponse = response.replace("}{", "{");

        JSONObject parsedResponse;

        try {
            parsedResponse = new JSONObject(trimmedResponse);
        } catch (JSONException e) {
            throw new AssertionError(e);
        }

        return parsedResponse;
    }

    private void handleLivelinksResponse(String response) {
        int newTopicSize = -1;
        int newInboxSize = -1;

        JSONObject parsedResponse = parseLivelinksResponse(response);

        try {
            newTopicSize = parsedResponse.getInt(getTopicPayload().toString());
            newInboxSize = parsedResponse.getInt(getInboxPayload().toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error in livelinks: ", e);
        }

        if (newTopicSize > mTopicSize) {
            // Load new messages using moremessages.php
            Bundle args = new Bundle(6);
            args.putInt("topic", mTopicId);
            args.putInt("old", mTopicSize);
            args.putInt("new", newTopicSize);
            args.putString("method", "POST");
            args.putString("type", "moremessages");

            ((FragmentActivity) mContext).getSupportLoaderManager()
                    .initLoader(FETCH_MESSAGE, args, callbacks)
                    .forceLoad();

            // Update mTopicSize for subsequent requests
            mTopicSize = newTopicSize;
        }

        if (newInboxSize > mInboxSize) {
            // Notify user that they have new PMs. We should then store this locally
            // To avoid having to make unnecessary web requests.
        }
    }

    private LoaderManager.LoaderCallbacks<Object> callbacks = new LoaderManager.LoaderCallbacks<Object>() {

        public Loader<Object> onCreateLoader(int id, final Bundle args) {
            return new AsyncLoadHandler(mContext, args) {

                @Override
                public String loadInBackground() {
                    return new WebRequest(mContext, args).sendRequest();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Object> loader, Object data) {
            String response = (String) data;
            switch (loader.getId()) {
                case LIVELINKS:
                    handleLivelinksResponse(response);
                    break;

                case FETCH_MESSAGE:
                    onReceiveUpdate(response);
                    subscribeToUpdates();
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Object> loader) {
            loader.reset();
        }
    };
}
