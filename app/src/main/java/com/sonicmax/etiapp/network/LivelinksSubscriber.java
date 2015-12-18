package com.sonicmax.etiapp.network;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.sonicmax.etiapp.utilities.AsyncLoadHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;

public class LivelinksSubscriber {
    private final String LOG_TAG = LivelinksSubscriber.class.getSimpleName();
    private final int LIVELINKS = 1;
    private final int FETCH_MESSAGE = 2;
    private final int SHIFT_CONSTANT = 48;

    private final Context mContext;
    private final int mTopicId;
    private final int mUserId;

    private int mTopicSize;
    private int mInboxSize;

    public LivelinksSubscriber(Context context, String topicId, String userId, int topicSize, int inboxSize) {
        this.mContext = context;
        this.mTopicId = Integer.parseInt(topicId);
        this.mUserId = Integer.parseInt(userId);
        this.mTopicSize = topicSize;
        this.mInboxSize = inboxSize;
        subscribe();
    }

    private void subscribe() {
        JSONObject payload = buildLivelinksPayload();

        Bundle args = new Bundle(3);
        args.putString("method", "POST");
        args.putString("type", "livelinks");
        args.putString("payload", payload.toString());

        LoaderManager manager = ((FragmentActivity) mContext).getSupportLoaderManager();

        if (manager.getLoader(LIVELINKS) == null) {
            manager.initLoader(LIVELINKS, args, callbacks).forceLoad();
        }
        else {
            manager.restartLoader(LIVELINKS, args, callbacks).forceLoad();
        }
    }

    public void unsubscribe() {
        LoaderManager manager = ((FragmentActivity) mContext).getSupportLoaderManager();
        if (manager.getLoader(LIVELINKS) != null) {
            manager.destroyLoader(LIVELINKS);
        }
    }

    public void onReceiveUpdate(String message, int position) {
        // Override this when instantiating LivelinksSubscriber
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
        /* Response is key-value pair formatted like this:

                }{"144115188085085408":42}

                    Key is either topic payload or inbox payload
                    Value is either index of new post, or number of unread PMs
         */

        String trimmedResponse = response.replace("}{", "{");

        try {
            return new JSONObject(trimmedResponse);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error parsing response from server: " + response, e);
            return null;
        }
    }

    private void handleLivelinksResponse(String response) {
        int newTopicSize = -1;
        int newInboxSize = -1;

        JSONObject parsedResponse = parseLivelinksResponse(response);

        if (parsedResponse.length() == 0) {
            // Empty response means that request timed out. Happens every 70 seconds
            subscribe();
        }

        else {
            try {
                newTopicSize = parsedResponse.getInt(getTopicPayload().toString());

                if (newTopicSize == -1) {
                    newInboxSize = parsedResponse.getInt(getInboxPayload().toString());
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error in livelinks: ", e);
            }

            if (newTopicSize > mTopicSize) {
                // Prepare bundle of args so we can load new messages using moremessages.php
                Bundle args = new Bundle(3);
                args.putString("method", "GET");
                args.putString("type", "moremessages");
                ContentValues values = new ContentValues(4);
                values.put("topic", mTopicId);
                values.put("old", mTopicSize);
                values.put("new", newTopicSize);
                values.put("filter", 0);
                args.putParcelable("values", values);

                // Update mTopicSize for subsequent requests
                mTopicSize = newTopicSize;

                LoaderManager loaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
                if (loaderManager.getLoader(FETCH_MESSAGE) == null) {
                    loaderManager.initLoader(FETCH_MESSAGE, args, callbacks).forceLoad();
                }
                else {
                    loaderManager.restartLoader(FETCH_MESSAGE, args, callbacks).forceLoad();
                }
            }

            if (newInboxSize > mInboxSize) {
                // Notify user that they have new PMs. We should then store this locally
                // To avoid having to make unnecessary web requests.
            }
        }
    }

    private LoaderManager.LoaderCallbacks<Object> callbacks = new LoaderManager.LoaderCallbacks<Object>() {

        @Override
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
                    onReceiveUpdate(response, mTopicSize);
                    subscribe();
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Object> loader) {
            loader.reset();
        }
    };
}
