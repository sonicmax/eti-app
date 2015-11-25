package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.sonicmax.etiapp.adapters.BoardListAdapter;
import com.sonicmax.etiapp.scrapers.BoardListScraper;

import java.util.List;

public class BoardListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {

    private final String LOG_TAG = BoardListFragment.class.getSimpleName();
    private BoardListAdapter mBoardListAdapter;
    private ProgressDialog mDialog;
    private boolean mFirstRun = true;

    public BoardListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_board_list, container, false);

        if (mBoardListAdapter == null) {
            mBoardListAdapter = new BoardListAdapter(getActivity());
        }
        else {
            mBoardListAdapter.clearBoards();
        }

        ListView boardList = (ListView) rootView.findViewById(R.id.listview_boards);
        boardList.setAdapter(mBoardListAdapter);
        boardList.setOnItemClickListener(boardClickHandler);

        // Prepare args for loader
        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "home");

        LoaderManager loaderManager = getLoaderManager();
        if (mFirstRun) {
            Log.v(LOG_TAG, "first run");
            loaderManager.initLoader(0, args, this).forceLoad();
            mFirstRun = false;
        }
        else {
            Log.v(LOG_TAG, "second run");
            // Restart loader so it can use cached result (eg. if back button is pressed, or screen
            // orientation changes).
            loaderManager.restartLoader(0, args, this).forceLoad();
        }

        return rootView;
    }

    @Override
    public void onDetach() {

        // Make sure that we don't leak progress dialog when exiting activity
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }

        super.onDetach();
    }

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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Loader callbacks.
     */
    public Loader<Object> onCreateLoader(int id, final Bundle args) {

        final Context context = getContext();

        mDialog = new ProgressDialog(getContext());
        mDialog.setMessage("Getting bookmarks...");
        mDialog.show();

        return new AsyncLoadHandler(context, args) {

            @Override
            public List<Board> loadInBackground() {
                String html = new WebRequest(context, args).sendRequest();
                return new BoardListScraper().scrapeBoards(html);
            }
        };
    }

    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data != null) {
            // We can be sure that data will safely cast to List<Board>.
            @SuppressWarnings("unchecked")
            List<Board> boards = (List<Board>) data;
            mBoardListAdapter.updateBoards(boards);
        }
        mDialog.dismiss();
    }

    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }

}
