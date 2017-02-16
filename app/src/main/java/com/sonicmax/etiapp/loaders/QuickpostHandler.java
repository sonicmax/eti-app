package com.sonicmax.etiapp.loaders;

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
import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.utilities.AsyncLoader;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class QuickpostHandler implements LoaderManager.LoaderCallbacks<Object> {

    private final FloatingActionButton mQuickpostButton;
    private final Context mContext;
    private final Topic mTopic;

    private boolean mIsInboxThread = false;

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

        if (this.mTopic.getUrl().contains("inboxthread.php")) {
            mIsInboxThread = true;
        }
    }

    // TODO: Should be interface
    public void onPreload() {}
    public void onSuccess(String message) {}
    public void onError(String message) {}

    public void postMessage(String message) {
        final int POST_MESSAGE = 0;

        if (message.length() >= 5) {
            String token = SharedPreferenceManager.getString(mContext, "h");
            String urlEncodedMessage = getUrlEncodedString(message);

            ContentValues values = new ContentValues();

            if (!mIsInboxThread) {
                values.put("id", mTopic.getId());
            } else {
                values.put("pm", mTopic.getId());
            }

            values.put("message", urlEncodedMessage);
            values.put("h", token);
            values.put("submit", "Post Message");

            Bundle args = new Bundle();
            args.putString("method", "POST");

            if (!mIsInboxThread) {
                args.putString("type", "newmessage");
            } else {
                args.putString("type", "newpm");
            }

            args.putParcelable("values", values);

            LoaderManager loaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();

            if (loaderManager.getLoader(POST_MESSAGE) == null) {
                loaderManager.initLoader(POST_MESSAGE, args, this).forceLoad();
            }
            else {
                loaderManager.restartLoader(POST_MESSAGE, args, this).forceLoad();
            }
        }

        else {
            onError(mContext.getResources().getString(R.string.error_5_chars_or_more));
        }
    }

    private String getUrlEncodedString(String content) {
        final String UTF_8 = "UTF-8";
        try {
            return URLEncoder.encode(content, UTF_8);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public void hideButton() {
        mQuickpostButton.setVisibility(View.INVISIBLE);
    }

    public void setVisible() {
        mQuickpostButton.setVisibility(View.VISIBLE);
    }

    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {
        onPreload();

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
        final String INTERNAL_ERROR = "-1";

        // WebRequest returns HTTP response code as string
        String responseCode = (String) data;

        if (responseCode.equals(HTTP_STATUS_OK)) {
            onSuccess(mContext.getResources().getString(R.string.post_message_ok));
        }

        else if (responseCode.equals(INTERNAL_ERROR)){
            // HTTPSUrlConnection returns -1 if there was some kind of error with the
            // response headers. Assume the post was successful, but don't display
            // "Message posted" message
            onSuccess(null);
        }

        else {
            onError((mContext.getResources().getString(R.string.error_message_failed)));
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Object> loader) {
        loader.reset();
    }
}
