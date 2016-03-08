package com.sonicmax.etiapp.network;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.PopupWindow;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.utilities.AsyncLoader;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

public class QuickpostHandler {

    private final FloatingActionButton mQuickpostButton;
    private final Context mContext;
    private final Topic mTopic;

    public final PopupWindow.OnDismissListener dismissListener;

    public QuickpostHandler(Context context, FloatingActionButton quickpostButton, Topic topic) {
        this.mContext = context;
        this.mQuickpostButton = quickpostButton;
        this.mTopic = topic;
        this.dismissListener = new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setVisible();
            }
        };
    }

    public void onPreLoad() {}

    public void onSuccess() {}

    public void onError(String message) {}

    public void postMessage(String message) {
        final int POST_MESSAGE = 0;

        if (message.length() >= 5) {
            String token = SharedPreferenceManager.getString(mContext, "h");

            // Get input from editText elements
            ContentValues values = new ContentValues();
            values.put("id", mTopic.getId());
            values.put("message", message);
            values.put("h", token);
            values.put("submit", "Post Message");

            Bundle args = new Bundle();
            args.putString("method", "POST");
            args.putString("type", "newmessage");
            args.putParcelable("values", values);

            LoaderManager loaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();

            if (loaderManager.getLoader(POST_MESSAGE) == null) {
                loaderManager.initLoader(POST_MESSAGE, args, loaderCallbacks).forceLoad();
            }
            else {
                loaderManager.restartLoader(POST_MESSAGE, args, loaderCallbacks).forceLoad();
            }
        }

        else {
            onError(mContext.getResources().getString(R.string.error_5_chars_or_more));
        }
    }

    public void setInvisible() {
        mQuickpostButton.setVisibility(View.INVISIBLE);
    }

    public void setVisible() {
        mQuickpostButton.setVisibility(View.VISIBLE);
    }

    private LoaderManager.LoaderCallbacks<Object> loaderCallbacks = new LoaderManager.LoaderCallbacks<Object>() {

        @Override
        public Loader<Object> onCreateLoader(final int id, final Bundle args) {

            onPreLoad();

            return new AsyncLoader(mContext, args) {
                @Override
                public String loadInBackground() {
                    return new WebRequest(mContext, args).sendRequest();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Object> loader, Object data) {
            final String HTTP_STATUS_OK = "200";

            if (data.equals(HTTP_STATUS_OK)) {
                onSuccess();
            }
            else {
                // TODO: Display different error depending on which status code is returned
                onError((mContext.getResources().getString(R.string.error_message_failed)));
            }
        }

        @Override
        public void onLoaderReset(android.support.v4.content.Loader<Object> loader) {
            loader.reset();
        }
    };
}
