package com.sonicmax.etiapp.loaders;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.scrapers.MessageListScraper;
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
    private String mUrl;

    public MessageListLoader (Context context, MessageListLoader.EventInterface eventInterface, String url) {
        mContext = context;
        mEventInterface = eventInterface;
        mLoaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
        mScraper = new MessageListScraper(context, url);
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public void load(Bundle args, int id) {
        if (mLoaderManager.getLoader(id) == null) {
            mLoaderManager.initLoader(id, args, this).forceLoad();
        }
        else {
            mLoaderManager.restartLoader(id, args, this).forceLoad();
        }
    }

    public interface EventInterface {
        void onLoadMessageList(List<Message> messages);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public Loader<Object> onCreateLoader(final int id, final Bundle args) {

        return new AsyncLoader(mContext, args) {
            @Override
            public List<Message> loadInBackground() {

                String html = new WebRequest(mContext, args).sendRequest();
                return mScraper.scrapeMessages(html, args.getBoolean("filter"));
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data != null) {
            // We can be sure that data will safely cast to List<Message>.
            List<Message> messages = (List<Message>) data;
            mEventInterface.onLoadMessageList(messages);
        }

        else {
            // Handle error
        }
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }

}
