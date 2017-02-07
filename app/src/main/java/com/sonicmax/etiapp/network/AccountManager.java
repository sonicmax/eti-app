package com.sonicmax.etiapp.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.sonicmax.etiapp.activities.BookmarkManagerActivity;
import com.sonicmax.etiapp.activities.TopicListActivity;
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

    private Context mContext;
    private ProgressDialog mDialog;
    private EventInterface mEventInterface;
    private LoaderManager mLoaderManager;

    private boolean mReloadBookmarks = false;

    public AccountManager(Context context, ProgressDialog dialog, EventInterface eventInterface) {
        mContext = context;
        mDialog = dialog;
        mEventInterface = eventInterface;
        mLoaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods for logging in, logging out, etc
    ///////////////////////////////////////////////////////////////////////////

    public void login(Bundle args) {
        mLoaderManager.initLoader(LOGIN, args, this).forceLoad();
    }

    public void requestLogout() {
        mDialog = new ProgressDialog(mContext);
        mDialog.setMessage("Logging out...");
        mDialog.show();

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

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    private String getDialogMessage(int id) {
        switch (id) {
            case LOGIN:
                return "Logging in";
            case LOGOUT:
                return "Logging out...";
            case STATUS_CHECK:
                return "Checking login status...";

            default:
                return "Undefined message for AccountManager id " + id + " lolz.";
        }
    }

    public interface EventInterface {
        void onLoadComplete(Intent intent);
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
                mDialog = new ProgressDialog(mContext);
                mDialog.setMessage(getDialogMessage(id));
                mDialog.show();

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

            if (mDialog != null) {
                mDialog.dismiss();
            }

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

                    Toaster.makeToast(mContext, "IP: " + response);

                    mLoaderManager.initLoader(STATUS_CHECK, args, this).forceLoad();
                    break;

                case STATUS_CHECK:
                    // FOR DEBUG ONLY
                    Toaster.makeToast(mContext, "Status check response = " + response);

                    // Possible responses:
                    // "0"              Not logged in
                    // "1:username"     Logged in

                    if (response == null || response.trim().equals("0")) {
                        // Can't do anything else - wait for user to login manually
                        mDialog.dismiss();

                    } else {
                        // Use stored cookies to get board list and start activity
                        mDialog.dismiss();
                        intent = new Intent(mContext, BookmarkManagerActivity.class);
                        intent.putExtra("title", "ETI");
                    }

                    break;

                case LOGIN:
                    SharedPreferenceManager.putBoolean(mContext, "is_logged_in", true);
                    mDialog.dismiss();

                    // Check whether we have already saved list of bookmarks.
                    // Bookmark lists are serialized as "bookmark_thing0", "bookmark_thing1", (etc)
                    String firstBookmarkName = SharedPreferenceManager.getString(mContext, "bookmark_names0");
                    String firstBookmarkUrl = SharedPreferenceManager.getString(mContext, "bookmark_urls0");

                    if (firstBookmarkName != null && firstBookmarkUrl != null && !mReloadBookmarks) {
                        // Open topic list to first scraped bookmark
                        intent = new Intent(mContext, TopicListActivity.class);
                        intent.putExtra("boardname", firstBookmarkName);
                        intent.putExtra("url", firstBookmarkUrl);

                    } else {
                        // Get list of bookmarks from main.php & start BookmarkManagerActivity
                        intent = new Intent(mContext, BookmarkManagerActivity.class);
                        mContext.startActivity(intent);
                    }

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
