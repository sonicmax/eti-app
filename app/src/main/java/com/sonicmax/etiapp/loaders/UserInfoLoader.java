package com.sonicmax.etiapp.loaders;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;

import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.scrapers.UserInfoScraper;
import com.sonicmax.etiapp.utilities.AsyncLoader;

import java.util.List;

/**
 * Scrapes user info from ETI. Uses history.php as it's the most reliable source
 */
public class UserInfoLoader implements LoaderManager.LoaderCallbacks<Object> {
    private final int LOAD_USER_INFO = 0;

    private Context mContext;
    private EventInterface mEventInterface;
    private LoaderManager mLoaderManager;


    public UserInfoLoader(Context context, UserInfoLoader.EventInterface eventInterface) {
        mContext = context;
        mEventInterface = eventInterface;
        mLoaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
    }

    public interface EventInterface {
        void onLoadBookmarks(List<Bookmark> bookmarks);
        void onLoadFail();
    }

    public void loadUserInfo() {
        // Prepare args for loader and scrape bookmarks from history.php
        // (using history.php because it will work even if tag server is broken)
        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "history");

        if (mLoaderManager.getLoader(LOAD_USER_INFO) == null) {
            mLoaderManager.initLoader(LOAD_USER_INFO, args, this).forceLoad();
        } else {
            mLoaderManager.restartLoader(LOAD_USER_INFO, args, this).forceLoad();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////

    public android.support.v4.content.Loader<Object> onCreateLoader(int id, final Bundle args) {

        return new AsyncLoader(mContext, args) {

            @Override
            public List<Bookmark> loadInBackground() {
                String html = new WebRequest(mContext, args).sendRequest();
                UserInfoScraper scraper = new UserInfoScraper(mContext);
                scraper.setInput(html);
                scraper.scrapeUserInfo();
                return scraper.scrapeBookmarks();
            }
        };
    }

    public void onLoadFinished(android.support.v4.content.Loader<Object> loader, Object data) {
        if (data != null) {
            // We can be sure that data will safely cast to List<Bookmark>.
            List<Bookmark> bookmarks = (List<Bookmark>) data;
            mEventInterface.onLoadBookmarks(bookmarks);
        }

        else {
            mEventInterface.onLoadFail();
        }
    }

    public void onLoaderReset(android.support.v4.content.Loader<Object> loader) {
        loader.reset();
    }
}
