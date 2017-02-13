package com.sonicmax.etiapp.loaders;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.objects.MessageList;
import com.sonicmax.etiapp.scrapers.MessageListScraper;
import com.sonicmax.etiapp.utilities.AsyncLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.List;

public class LivelinksSubscriber {
    private final String LOG_TAG = LivelinksSubscriber.class.getSimpleName();
    private final int LIVELINKS = 2;
    private final int FETCH_MESSAGE = 3;
    private final int SHIFT_CONSTANT = 48;

    private final EventInterface mEventInterface;
    private final MessageListScraper mScraper;
    private final Context mContext;
    private final int mTopicId;
    private final int mUserId;

    private int mTopicSize;
    private int mInboxSize;

    public LivelinksSubscriber(Context context, EventInterface eventInterface,
                               String topicId, String userId, int topicSize, int inboxSize) {
        this.mContext = context;
        this.mEventInterface = eventInterface;
        this.mTopicId = Integer.parseInt(topicId);
        this.mUserId = Integer.parseInt(userId);
        this.mTopicSize = topicSize;
        this.mInboxSize = inboxSize;

        mScraper = new MessageListScraper(context, "");

        subscribe();
    }

    private void subscribe() {
        JSONObject payload = buildLivelinksPayload();

        Bundle args = new Bundle(3);
        args.putString("method", "POST");
        args.putString("type", "livelinks");

        ContentValues values = new ContentValues(1);
        values.put("payload", payload.toString());

        args.putParcelable("values", values);


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

    public interface EventInterface {
        void onReceiveNewPost(MessageList messageList, int position);
        void onReceivePrivateMessage(int unreadMessages);
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
            Log.e(LOG_TAG, "Error creating livelinks payload", e);
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
            // Empty response means that request timed out.
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
                mEventInterface.onReceivePrivateMessage(newInboxSize);
            }
        }
    }

    private LoaderManager.LoaderCallbacks<Object> callbacks = new LoaderManager.LoaderCallbacks<Object>() {

        @Override
        public Loader<Object> onCreateLoader(int id, final Bundle args) {

            switch (id) {
                case LIVELINKS:
                    return new AsyncLoader(mContext, args) {

                        @Override
                        public String loadInBackground() {
                            return new WebRequest(mContext, args).sendRequest();
                        }
                    };

                case FETCH_MESSAGE:
                    return new AsyncLoader(mContext, args) {

                        @Override
                        public MessageList loadInBackground() {
                            String response = new WebRequest(mContext, args).sendRequest();
                            // Can't parse HTML unless we remove these characters
                            String escapedResponse = response.replace("\\/", "/")
                                    .replace("\\\"", "\"")
                                    .replace("\\n", "");

                            return mScraper.scrapeMessages(escapedResponse, false);
                        }
                    };

                default:
                    Log.e(LOG_TAG, "Invalid id: " + id);
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Object> loader, Object data) {
            if (data != null) {
                switch (loader.getId()) {
                    case LIVELINKS:
                        String response = (String) data;
                        handleLivelinksResponse(response);
                        break;

                    case FETCH_MESSAGE:
                        // data will safely cast to MessageList
                        MessageList messageList = (MessageList) data;
                        List<Message> messages = messageList.getMessages();

                        for (Message message : messages) {
                            message.setAnimationFlag(true);
                        }

                        mEventInterface.onReceiveNewPost(messageList, mTopicSize);
                        subscribe();
                        break;
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Object> loader) {
            loader.reset();
        }
    };
}
