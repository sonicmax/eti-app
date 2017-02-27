package com.sonicmax.etiapp.loaders;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.sonicmax.etiapp.activities.BookmarkManagerActivity;
import com.sonicmax.etiapp.activities.TopicListActivity;
import com.sonicmax.etiapp.network.LoginScriptBuilder;
import com.sonicmax.etiapp.network.NetworkStatusChecker;
import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.utilities.AsyncLoader;
import com.sonicmax.etiapp.activities.LoginActivity;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;
import com.sonicmax.etiapp.utilities.Toaster;

/**
 * Class which allows us to perform account-related actions like logging in,
 * logging out, checking login status, etc.
 */

public class AccountManager implements LoaderManager.LoaderCallbacks<Object> {
    private final String LOG_TAG = AccountManager.class.getSimpleName();

    // Ids for Loader
    private final int LOGIN = 0;
    private final int LOGOUT = 1;
    private final int SCRIPT_BUILD = 2;
    private final int STATUS_CHECK = 3;
    private final int STAR_TOPIC = 4;
    private final int UNSTAR_TOPIC = 5;

    private Context mContext;
    private EventInterface mEventInterface;
    private LoaderManager mLoaderManager;

    private boolean mReloadBookmarks = false;

    public AccountManager(Context context, EventInterface eventInterface) {
        mContext = context;
        mEventInterface = eventInterface;
        mLoaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
    }

    public interface EventInterface {
        void onRequiresLogin();
        void onLoadComplete(Intent intent);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods for logging in, logging out, etc
    ///////////////////////////////////////////////////////////////////////////

    public void login(String username, String password) {
        ContentValues values = new ContentValues(2);
        values.put("username", username);
        values.put("password", password);

        Bundle args = new Bundle(3);
        args.putString("method", "POST");
        args.putString("type", "login");
        args.putParcelable("values", values);

        mLoaderManager.initLoader(LOGIN, args, this).forceLoad();
    }

    public void requestLogout() {
        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "logout");

        mLoaderManager.initLoader(LOGOUT, args, this).forceLoad();
    }

    public void checkLoginStatus() {
        // Make sure that user is still logged in
        // (ie. hasn't logged in using a different IP address)
       mLoaderManager.initLoader(SCRIPT_BUILD, null, this).forceLoad();
    }

    public void checkLoginCredentials() {
        // Insert username and password from SharedPreferences (if they exist)
        String username = SharedPreferenceManager.getString(mContext, "username");
        String password = SharedPreferenceManager.getString(mContext, "password");

        if (username != null & password != null) {
            login(username, password);
        }

        else {
            mEventInterface.onRequiresLogin();
        }
    }

    public void starTopic(String topicId) {
        ContentValues values = new ContentValues(2);
        values.put("id", topicId);

        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "star");
        args.putParcelable("values", values);

        mLoaderManager.initLoader(STAR_TOPIC, args, this).forceLoad();
    }

    public void unstarTopic(String topicId) {
        ContentValues values = new ContentValues(2);
        values.put("id", topicId);

        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "unstar");
        args.putParcelable("values", values);

        mLoaderManager.initLoader(UNSTAR_TOPIC, args, this).forceLoad();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {
        switch (id) {
            case SCRIPT_BUILD:
                return new LoginScriptBuilder(mContext);

            // Allow cases to fallthrough
            case STATUS_CHECK:
            case LOGIN:
            case LOGOUT:
            case STAR_TOPIC:
            case UNSTAR_TOPIC:

                return new AsyncLoader(mContext, args) {
                    @Override
                    public String loadInBackground() {
                        return new WebRequest(mContext, args).sendRequest();
                    }
                };

            default:
                Log.e(LOG_TAG, "Couldn't find loader with id " + id);
                return null;
        }
    }

        @Override
        public void onLoadFinished(Loader<Object> loader, Object data) {
            Intent intent = null;
            String response = (String) data;

            switch (loader.getId()) {
                case LOGOUT:
                    SharedPreferenceManager.putBoolean(mContext, "is_logged_in", false);
                    intent = new Intent(mContext, LoginActivity.class);
                    break;

                case SCRIPT_BUILD:
                    Bundle args = new Bundle();
                    args.putString("method", "GET");
                    args.putString("type", "url");
                    args.putString("url", response);

                    mLoaderManager.initLoader(STATUS_CHECK, args, this).forceLoad();
                    break;

                case STATUS_CHECK:
                    // Possible responses:
                    // "0"              Not logged in
                    // "1:username"     Logged in

                    if (response == null || response.trim().equals("0")) {
                        mEventInterface.onRequiresLogin();
                    }
                    else {
                        // Use stored cookies to get board list and start activity
                        String firstBookmarkName = SharedPreferenceManager.getString(mContext, "bookmark_names0");
                        String firstBookmarkUrl = SharedPreferenceManager.getString(mContext, "bookmark_urls0");
                        intent = new Intent(mContext, TopicListActivity.class);
                        intent.putExtra("title", firstBookmarkName);
                        intent.putExtra("url", firstBookmarkUrl);
                    }

                    break;

                case LOGIN:
                    SharedPreferenceManager.putBoolean(mContext, "is_logged_in", true);

                    // Check whether we have already saved list of bookmarks.
                    // Bookmark lists are serialized as "bookmark_thing0", "bookmark_thing1", (etc)
                    String firstBookmarkName = SharedPreferenceManager.getString(mContext, "bookmark_names0");
                    String firstBookmarkUrl = SharedPreferenceManager.getString(mContext, "bookmark_urls0");

                    if (firstBookmarkName != null && firstBookmarkUrl != null && !mReloadBookmarks) {
                        // Open topic list to first scraped bookmark
                        intent = new Intent(mContext, TopicListActivity.class);
                        intent.putExtra("title", firstBookmarkName);
                        intent.putExtra("url", firstBookmarkUrl);

                    } else {
                        // Get list of bookmarks from main.php & start BookmarkManagerActivity
                        intent = new Intent(mContext, BookmarkManagerActivity.class);
                        mContext.startActivity(intent);
                    }

                    break;

                case STAR_TOPIC:
                case UNSTAR_TOPIC:
                    // We don't need to do anything with the response for these requests
                    break;

                default:
                    break;
            }

            if (intent != null) {
                mEventInterface.onLoadComplete(intent);
            }
        }

        @Override
        public void onLoaderReset(Loader<Object> loader) {
            loader.reset();
        }
}
