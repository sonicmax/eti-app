package com.sonicmax.etiapp.loaders;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;

import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Board;
import com.sonicmax.etiapp.scrapers.BoardListScraper;
import com.sonicmax.etiapp.utilities.AsyncLoader;

import java.util.List;

/**
 * Scrapes list of user-created bookmarks from ETI.
 */
public class BookmarkLoader implements LoaderManager.LoaderCallbacks<Object> {
    private final int LOAD_BOOKMARKS = 0;

    private Context mContext;
    private EventInterface mEventInterface;
    private LoaderManager mLoaderManager;


    public BookmarkLoader (Context context, BookmarkLoader.EventInterface eventInterface) {
        mContext = context;
        mEventInterface = eventInterface;
        mLoaderManager = ((FragmentActivity) mContext).getSupportLoaderManager();
    }

    public interface EventInterface {
        void onLoadBookmarks(List<Board> bookmarks);
        void onLoadFail();
    }

    public void loadBookmarks() {
        // Prepare args for loader and scrape bookmarks from history.php
        // (using history.php because it will work even if tag server is broken)
        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "bookmarks");

        if (mLoaderManager.getLoader(LOAD_BOOKMARKS) == null) {
            mLoaderManager.initLoader(LOAD_BOOKMARKS, args, this).forceLoad();
        } else {
            mLoaderManager.restartLoader(LOAD_BOOKMARKS, args, this).forceLoad();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////

    public android.support.v4.content.Loader<Object> onCreateLoader(int id, final Bundle args) {

        return new AsyncLoader(mContext, args) {

            @Override
            public List<Board> loadInBackground() {
                String html = new WebRequest(mContext, args).sendRequest();
                return new BoardListScraper(mContext).scrapeBoards(html);
            }
        };
    }

    public void onLoadFinished(android.support.v4.content.Loader<Object> loader, Object data) {

        if (data != null) {
            // We can be sure that data will safely cast to List<Board>.
            List<Board> bookmarks = (List<Board>) data;
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
