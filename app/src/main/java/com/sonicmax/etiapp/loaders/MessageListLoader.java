package com.sonicmax.etiapp.loaders;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.KeyEvent;

import com.sonicmax.etiapp.activities.BaseActivity;
import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.objects.MessageList;
import com.sonicmax.etiapp.scrapers.MessageListScraper;
import com.sonicmax.etiapp.scrapers.UserInfoScraper;
import com.sonicmax.etiapp.utilities.AsyncLoader;

import java.util.List;

/**
 * Class which loads message list data and scrapes content to be displayed in adapter.
 */

public class MessageListLoader implements LoaderManager.LoaderCallbacks<Object> {
    private Context mContext;
    private EventInterface mEventInterface;
    private LoaderManager mLoaderManager;
    private MessageListScraper mScraper;
    private Bundle mArgs;

    public MessageListLoader (Context context, MessageListLoader.EventInterface eventInterface) {
        mContext = context;
        mEventInterface = eventInterface;
        mLoaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
        mScraper = new MessageListScraper(context);
    }

    public void load(Bundle args, int id) {
        if (mLoaderManager.getLoader(id) == null) {
            mLoaderManager.initLoader(id, args, this).forceLoad();
        }
        else {
            mLoaderManager.restartLoader(id, args, this).forceLoad();
        }
    }

    private MessageList processHtml(Bundle loaderArgs, String html) {
        if (html == null) {
            return null;

        } else {
            mArgs = loaderArgs;
            String url = loaderArgs.getString("url");
            boolean isFiltered = loaderArgs.getBoolean("filter");

            try {
                mScraper.setUrl(url);
                return mScraper.scrapeMessages(html, isFiltered);

            } catch (IllegalArgumentException outOfBounds) {
                return null;
            }
        }
    }

    public interface EventInterface {
        void onLoadMessageList(Bundle args, MessageList messageList);
        void onLoadError();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public Loader<Object> onCreateLoader(final int id, final Bundle args) {
        final int LOAD_MESSAGES = 0;
        final int REFRESH = 1;
        final int LOAD_FROM_CACHE = 2;

        switch (id) {
            case LOAD_MESSAGES:
            case REFRESH:
                return new AsyncLoader(mContext, args) {

                    @Override
                    public MessageList loadInBackground() {
                        String html = new WebRequest(mContext, args).sendRequest();
                        return processHtml(args, html);
                    }
                };

            case LOAD_FROM_CACHE:
                return new AsyncLoader(mContext, args) {

                    @Override
                    public MessageList loadInBackground() {
                        String html = args.getString("html");
                        return processHtml(args, html);
                    }
                };

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data != null) {
            // We can be sure that data will safely cast to MessageList.
            MessageList messageList = (MessageList) data;
            mEventInterface.onLoadMessageList(mArgs, messageList);
        }

        else {
            // Handle the error in fragment
            mEventInterface.onLoadError();
        }
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }

}
