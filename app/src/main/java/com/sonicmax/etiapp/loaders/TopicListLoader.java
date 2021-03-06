package com.sonicmax.etiapp.loaders;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.sonicmax.etiapp.activities.PostTopicActivity;
import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.TopicList;
import com.sonicmax.etiapp.scrapers.PostmsgScraper;
import com.sonicmax.etiapp.scrapers.TopicListScraper;
import com.sonicmax.etiapp.scrapers.UserInfoScraper;
import com.sonicmax.etiapp.utilities.AsyncLoader;

public class TopicListLoader implements LoaderManager.LoaderCallbacks<Object> {
    private final String LOG_TAG = this.getClass().getSimpleName();
    private final int LOAD_TOPIC_LIST = 0;
    private final int POST_TOPIC = 1;
    private final int LOAD_FROM_CACHE = 2;

    private final Context mContext;
    private LoaderManager mLoaderManager;
    private EventInterface mEventInterface;

    private boolean mInternalServerError = false;

    public TopicListLoader (Context context, EventInterface eventInterface) {
        mContext = context;
        mEventInterface = eventInterface;
        mLoaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods
    ///////////////////////////////////////////////////////////////////////////

    public void load(String url) {
        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "topiclist");
        args.putString("url", url);

        if (mLoaderManager.getLoader(LOAD_TOPIC_LIST) == null) {
            mLoaderManager.initLoader(LOAD_TOPIC_LIST, args, this).forceLoad();
        }
        else {
            mLoaderManager.restartLoader(LOAD_TOPIC_LIST, args, this).forceLoad();
        }
    }

    public void loadFromCache(String url, String cachedHtml) {
        Bundle args = new Bundle();
        args.putString("url", url);
        args.putString("html", cachedHtml);

        if (mLoaderManager.getLoader(LOAD_FROM_CACHE) == null) {
            mLoaderManager.initLoader(LOAD_FROM_CACHE, args, this).forceLoad();
        }
        else {
            mLoaderManager.restartLoader(LOAD_FROM_CACHE, args, this).forceLoad();
        }
    }

    public void openPostTopicActivity() {
        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "newtopic");

        if (mLoaderManager.getLoader(POST_TOPIC) == null) {
            mLoaderManager.initLoader(POST_TOPIC, args, this).forceLoad();

        } else {
            mLoaderManager.restartLoader(POST_TOPIC, args, this).forceLoad();
        }
    }

    private TopicList processHtml(Bundle args, String html) {
        final String HTTP_INTERNAL_SERVER_ERROR = "500";

        if (html == null) {
            return null;
        }

        else if (html.equals(HTTP_INTERNAL_SERVER_ERROR)) {
            mInternalServerError = true;
            return null;

        } else {
            mInternalServerError = false;

            try {
                UserInfoScraper userInfoScraper = new UserInfoScraper(mContext);
                userInfoScraper.setInput(html);
                userInfoScraper.scrapeUserInfo();

                return new TopicListScraper(mContext, args.getString("url")).scrapeTopics(html);

            } catch (IllegalArgumentException outOfBounds) {
                Log.e(LOG_TAG, outOfBounds.getMessage());
                return null;
            }
        }
    }

    public interface EventInterface {
        void onLoadTopicList(TopicList topicList);
        void onInternalServerError();
        void onCreateTopic(Intent intent);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {

        switch (id) {
            case LOAD_TOPIC_LIST:
                return new AsyncLoader(mContext, args) {

                    @Override
                    public TopicList loadInBackground() {
                       return processHtml(args, new WebRequest(mContext, args).sendRequest());
                    }
                };

            case LOAD_FROM_CACHE:
                return new AsyncLoader(mContext, args) {

                    @Override
                    public TopicList loadInBackground() {
                        String html = args.getString("html");
                        return processHtml(args, html);
                    }
                };

            case POST_TOPIC:
                return new AsyncLoader(mContext, args) {

                    @Override
                    public String loadInBackground() {
                        return new WebRequest(mContext, args).sendRequest();
                    }
                };

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data != null) {
            switch (loader.getId()) {
                case LOAD_TOPIC_LIST:
                case LOAD_FROM_CACHE:
                    // We can be sure that data will safely cast to List<Topic>.
                    TopicList topicList = (TopicList) data;
                    mEventInterface.onLoadTopicList(topicList);
                    break;

                case POST_TOPIC:
                    String response = (String) data;
                    new PostmsgScraper(mContext).parseResponse(response);
                    Intent intent = new Intent(mContext, PostTopicActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    mEventInterface.onCreateTopic(intent);
                    break;
            }

        } else {
            // There was some kind of error while requesting HTML.

            if (mInternalServerError) {
                // We can try to load message history:
                mEventInterface.onInternalServerError();
            }

            else {
                // Log the error and let it fail
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }

}
