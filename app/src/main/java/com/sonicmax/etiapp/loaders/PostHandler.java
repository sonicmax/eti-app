package com.sonicmax.etiapp.loaders;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.utilities.AsyncLoader;

public class PostHandler implements LoaderManager.LoaderCallbacks<Object> {
    private final int POST_MESSAGE = 0;
    private final int POST_TOPIC = 1;

    private Context mContext;
    private EventInterface mEventInterface;
    private LoaderManager mLoaderManager;

    public PostHandler(Context context, EventInterface eventInterface) {
        mContext = context;
        mEventInterface = eventInterface;
        mLoaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
    }

    public interface EventInterface {
        void onPostComplete();
        void onPostFail();
    }

    public void postMessage(Bundle args) {
        mLoaderManager.initLoader(POST_MESSAGE, args, this).forceLoad();
    }

    public void postTopic(Bundle args) {
        mLoaderManager.initLoader(POST_TOPIC, args, this).forceLoad();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {
        return new AsyncLoader(mContext, args) {

            @Override
            public String loadInBackground() {
                return new WebRequest(mContext, args).sendRequest();
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        // TODO: Check for errors
        if (data != null) {
            mEventInterface.onPostComplete();
        }
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }
}