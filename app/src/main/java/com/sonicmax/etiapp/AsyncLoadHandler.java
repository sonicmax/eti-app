package com.sonicmax.etiapp;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

/**
 * Generic AsyncTaskLoader implementation
 */
public class AsyncLoadHandler extends AsyncTaskLoader<Object> {

    final String LOG_TAG = AsyncLoadHandler.class.getSimpleName();

    private Object mData;

    public AsyncLoadHandler(Context context, Bundle args) {
        super(context);
    }

    @Override
    public Object loadInBackground() {
        // This method should be overridden after instantiating AsyncLoadHandler
        return null;
    }

    @Override public void deliverResult(Object data) {

        if (isReset()) {
            if (data != null) {
                Log.v(LOG_TAG, "releasing data from deliverResult");
                onReleaseResources(data);
            }
        }

        mData = data;

        if (isStarted()) {
            Log.v(LOG_TAG, "delivering data");
            super.deliverResult(data);
        }
    }

    @Override
    protected void onForceLoad() {
        if (mData != null) {
            Log.v(LOG_TAG, "returning cached result");
            deliverResult(mData);
        }
        else {
            Log.v(LOG_TAG, "forcing load");
            super.onForceLoad();
        }
    }

    @Override public void onCanceled(Object data) {
        super.onCanceled(data);
        onReleaseResources(data);
    }

    protected void onReleaseResources(Object data) {
        Log.v(LOG_TAG, "releasing data");
        data = null;
    }

}
