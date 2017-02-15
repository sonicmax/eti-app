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
    private final int LIVELINKS_TOPICLIST = 1;
    private final int LIVELINKS_PM_THREAD = 2;
    private final int FETCH_MESSAGE = 3;
    private final int SHIFT_CONSTANT = 48;

    private final int mTopicId;
    private final int mUserId;

    private Context mContext;
    private EventInterface mEventInterface;
    private MessageListScraper mScraper;

    private int mTopicSize;
    private int mInboxSize;
    private boolean mIsPmThread = false;

    public LivelinksSubscriber(Context context, EventInterface eventInterface,
                               String topicId, String userId, int topicSize, int inboxSize) {
        this.mContext = context;
        this.mEventInterface = eventInterface;
        this.mTopicId = Integer.parseInt(topicId);
        this.mUserId = Integer.parseInt(userId);
        this.mTopicSize = topicSize;
        this.mInboxSize = inboxSize;

        mScraper = new MessageListScraper(context, "");
    }

    public LivelinksSubscriber(String topicId, String userId, int topicSize, int inboxSize) {
        this.mTopicId = Integer.parseInt(topicId);
        this.mUserId = Integer.parseInt(userId);
        this.mTopicSize = topicSize;
        this.mInboxSize = inboxSize;
    }

    public void setPmThreadFlag(boolean value) {
        mIsPmThread = value;
    }

    public void subscribe() {
        JSONObject payload = buildLivelinksPayload();

        Bundle args = new Bundle(3);
        args.putString("method", "POST");
        args.putString("type", "livelinks");

        ContentValues values = new ContentValues(1);
        values.put("payload", payload.toString());

        args.putParcelable("values", values);

        LoaderManager manager = ((FragmentActivity) mContext).getSupportLoaderManager();

        int loaderId = (mIsPmThread) ? LIVELINKS_PM_THREAD : LIVELINKS_TOPICLIST;

        if (manager.getLoader(loaderId) == null) {
            manager.initLoader(loaderId, args, callbacks).forceLoad();
        }
        else {
            manager.restartLoader(loaderId, args, callbacks).forceLoad();
        }
    }

    public void unsubscribe() {
        LoaderManager manager = ((FragmentActivity) mContext).getSupportLoaderManager();
        if (manager.getLoader(LIVELINKS_TOPICLIST) != null) {
            manager.destroyLoader(LIVELINKS_TOPICLIST);
        }
        else if (manager.getLoader(LIVELINKS_PM_THREAD) != null) {
            manager.destroyLoader(LIVELINKS_PM_THREAD);
        }
    }

    /**
     * After viewing unread PMs, we have to make a request to https://endoftheinter.net/async-update-bookmark.php
     * containing the thread ID and updated count in order to remove them from the unread PM count provided by ETI.
     */
    public void updateBookmarkCount() {
        Bundle args = new Bundle(3);
        args.putString("method", "GET");
        args.putString("type", "async-update-bookmark");

        ContentValues values = new ContentValues(4);
        values.put("pm", mTopicId);
        values.put("count", mTopicSize);

        args.putParcelable("values", values);

        LoaderManager manager = ((FragmentActivity) mContext).getSupportLoaderManager();

        if (manager.getLoader(UPDATE_BOOKMARK_COUNT) == null) {
            manager.initLoader(UPDATE_BOOKMARK_COUNT, args, callbacks).forceLoad();
        }
        else {
            manager.restartLoader(UPDATE_BOOKMARK_COUNT, args, callbacks).forceLoad();
        }
    }

    public interface EventInterface {
        void onReceiveNewPost(MessageList messageList, int position);
        void onReceivePrivateMessage(int oldCount, int newCount);
        void onUpdateBookmarkCount();
    }

    public BigInteger getTopicPayload() {
        final int TOPIC_CHANNEL = 0x0200;
        return BigInteger.valueOf(TOPIC_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(mTopicId));
    }

    public BigInteger getThreadPayload() {
        final int THREAD_CHANNEL = 0x0500;
        return BigInteger.valueOf(THREAD_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(mTopicId));
    }

    public BigInteger getThreadChannel(long id) {
        return BigInteger.valueOf(id)
                .xor(BigInteger.valueOf(mTopicId))
                .shiftRight(SHIFT_CONSTANT);
    }

    public BigInteger getInboxPayload() {
        final int INBOX_CHANNEL = 0x0100;
        return BigInteger.valueOf(INBOX_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(mUserId));
    }

    private JSONObject buildLivelinksPayload() {
        BigInteger topic;

        if (mIsPmThread) {
            topic = getThreadPayload();
        }
        else {
            topic = getTopicPayload();
        }

        BigInteger inbox = getInboxPayload();

        JSONObject payload = new JSONObject();

        try {
            payload.put(topic.toString(), mTopicSize);
            payload.put(inbox.toString(), mInboxSize);
            Log.v(LOG_TAG, "Payload: " + payload.toString());

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error creating livelinks payload", e);
        }

        return payload;
    }

    public JSONObject parseLivelinksResponse(String response) {
        if (response.indexOf("}") == 0) {
            response = response.replace("}{", "{");
        }

        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error parsing response from server: " + response, e);
            return null;
        }
    }

    private void handleLivelinksResponse(String response) {
        int newTopicSize = 0;
        int newInboxSize = 0;
        int newThreadSize = 0;

        JSONObject parsedResponse = parseLivelinksResponse(response);
        Log.v(LOG_TAG, "Response: " + parsedResponse.toString());

        if (parsedResponse.length() == 0) {
            // Empty response means that request timed out.
            subscribe();
        }

        else {
            try {
                newTopicSize = parsedResponse.getInt(getTopicPayload().toString());
            } catch (JSONException noNewPosts) {
                // Do nothing
            }

            try {
                newThreadSize = parsedResponse.getInt(getThreadPayload().toString());
            } catch (JSONException noNewInboxReplies) {

            }

            try {
                newInboxSize = parsedResponse.getInt(getInboxPayload().toString());
            } catch (JSONException noNewPms) {
                // Do nothing
            }

            if (newTopicSize > mTopicSize || newThreadSize > mTopicSize) {
                // Prepare bundle of args so we can load new messages using moremessages.php
                Bundle args = new Bundle(3);
                args.putString("method", "GET");
                args.putString("type", "moremessages");

                ContentValues values = new ContentValues(4);
                values.put("old", mTopicSize);

                if (mIsPmThread) {
                    values.put("pm", mTopicId);
                    values.put("new", newThreadSize);
                    mTopicSize = newThreadSize;
                }
                else {
                    values.put("topic", mTopicId);
                    values.put("new", newTopicSize);
                    mTopicSize = newTopicSize;
                }


                values.put("filter", 0);

                args.putParcelable("values", values);

                LoaderManager loaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
                if (loaderManager.getLoader(FETCH_MESSAGE) == null) {
                    loaderManager.initLoader(FETCH_MESSAGE, args, callbacks).forceLoad();
                }
                else {
                    loaderManager.restartLoader(FETCH_MESSAGE, args, callbacks).forceLoad();
                }
            }

            if (newInboxSize > mInboxSize) {
                mEventInterface.onReceivePrivateMessage(mInboxSize, newInboxSize);
                mInboxSize = newInboxSize;
                subscribe();
            }
        }
    }

    private LoaderManager.LoaderCallbacks<Object> callbacks = new LoaderManager.LoaderCallbacks<Object>() {

        @Override
        public Loader<Object> onCreateLoader(int id, final Bundle args) {

            switch (id) {
                case LIVELINKS_PM_THREAD:
                case LIVELINKS_TOPICLIST:
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
            switch (loader.getId()) {
                case LIVELINKS_PM_THREAD:
                case LIVELINKS_TOPICLIST:
                    if (data != null) {
                        String response = (String) data;
                        handleLivelinksResponse(response);
                    }
                    else {
                        subscribe();
                    }
                    break;

                case FETCH_MESSAGE:
                    if (data != null) {
                        // data will safely cast to MessageList
                        MessageList messageList = (MessageList) data;
                        List<Message> messages = messageList.getMessages();

                        for (Message message : messages) {
                            message.setAnimationFlag(true);
                        }

                        mEventInterface.onReceiveNewPost(messageList, mTopicSize);
                    }

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
