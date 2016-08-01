package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.activities.TopicListActivity;
import com.sonicmax.etiapp.adapters.BoardListAdapter;
import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Board;
import com.sonicmax.etiapp.scrapers.BoardListScraper;
import com.sonicmax.etiapp.utilities.AsyncLoader;

import java.util.ArrayList;
import java.util.List;

public class BookmarkManagerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {

    private final int LOAD_BOARDS = 0;

    private BoardListAdapter mBoardListAdapter;
    private ProgressDialog mDialog;
    private List<Board> mBookmarks;

    public BookmarkManagerFragment() {}


    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onAttach(Context context) {
        mBoardListAdapter = new BoardListAdapter(context);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        if (savedInstanceState == null
                || savedInstanceState.getParcelableArrayList("bookmarks") == null) {

            // Prepare args for loader and scrape bookmarks from main.php
            Bundle args = new Bundle();
            args.putString("method", "GET");
            args.putString("type", "home");

            LoaderManager loaderManager = getLoaderManager();

            if (loaderManager.getLoader(LOAD_BOARDS) == null) {
                getLoaderManager().initLoader(LOAD_BOARDS, args, this).forceLoad();
            } else {
                getLoaderManager().restartLoader(LOAD_BOARDS, args, this).forceLoad();
            }
        }

        else {
            // Use bookmarks from savedInstanceState to populate adapter.
            mBookmarks = savedInstanceState.getParcelableArrayList("bookmarks");
            mBoardListAdapter.updateBoards(mBookmarks);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_bookmarks, container, false);

        ListView boardList = (ListView) rootView.findViewById(R.id.listview_boards);
        boardList.setAdapter(mBoardListAdapter);
        boardList.setOnItemClickListener(boardClickHandler);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save list of boards so we can quickly restore fragment
        if (mBookmarks != null) {
            ArrayList<Board> bookmarkArray = new ArrayList<>(mBookmarks);
            outState.putParcelableArrayList("bookmarks", bookmarkArray);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        // Make sure that we don't leak progress dialog when exiting activity
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }

        super.onDetach();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Click listener for UI
    ///////////////////////////////////////////////////////////////////////////

    AdapterView.OnItemClickListener boardClickHandler = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Board target = mBoardListAdapter.getItem(position);
            Context context = getContext();
            Intent intent = new Intent(context, TopicListActivity.class);
            intent.putExtra("url", target.getUrl());
            intent.putExtra("boardname", target.getName());
            context.startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_in_from_right,
                    R.anim.slide_out_to_left);
        }
    };


    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////

    public Loader<Object> onCreateLoader(int id, final Bundle args) {
        final Context context = getContext();
        final String HTTP_INTERNAL_SERVER_ERROR = "500";

        mDialog = new ProgressDialog(context);
        mDialog.setMessage("Getting bookmarks...");
        mDialog.show();

        return new AsyncLoader(context, args) {

            @Override
            public List<Board> loadInBackground() {
                String html = new WebRequest(context, args).sendRequest();

                // Probably means the tag server was down - but we can still access message history
                if (html.equals(HTTP_INTERNAL_SERVER_ERROR)) {
                    handleInternalServerError();
                    return null;

                } else {
                    return new BoardListScraper(context).scrapeBoards(html);
                }
            }
        };
    }

    public void onLoadFinished(Loader<Object> loader, Object data) {

        if (data != null) {
            // We can be sure that data will safely cast to List<Board>.
            mBookmarks = (List<Board>) data;
            mBoardListAdapter.updateBoards(mBookmarks);
        }

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    private void handleInternalServerError() {
        Context context = getContext();
        Intent intent = new Intent(context, TopicListActivity.class);
        intent.putExtra("url", "http://boards.endoftheinter.net/history.php?b");
        intent.putExtra("boardname", "Message History");
        context.startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_in_from_right,
                R.anim.slide_out_to_left);
    }

}