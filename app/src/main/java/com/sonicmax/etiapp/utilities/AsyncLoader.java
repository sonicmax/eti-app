package com.sonicmax.etiapp.utilities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Generic AsyncTaskLoader implementation
 */
public class AsyncLoader extends AsyncTaskLoader<Object> {
    private final String LOG_TAG = AsyncLoader.class.getSimpleName();
    private final Bundle mArgs;

    private Object mData;

    public AsyncLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    public Bundle getArgs() {
        return mArgs;
    }

    @Override
    public Object loadInBackground() {
        // This method should be overridden after instantiating AsyncLoader
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
