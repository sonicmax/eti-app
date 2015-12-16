package com.sonicmax.etiapp.utilities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

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

    @Override
    public void deliverResult(Object data) {

        if (isReset()) {
            if (data != null) {
                onReleaseResources(data);
            }
        }

        mData = data;

        if (isStarted()) {
            super.deliverResult(data);
        }
    }

    @Override
    protected void onForceLoad() {
        if (mData != null) {
            deliverResult(mData);
        }
        else {
            super.onForceLoad();
        }
    }

    @Override
    public void onCanceled(Object data) {
        super.onCanceled(data);
        onReleaseResources(data);
    }

    protected void onReleaseResources(Object data) {
        data = null;
    }

}
