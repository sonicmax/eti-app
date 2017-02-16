package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.activities.TopicListActivity;
import com.sonicmax.etiapp.adapters.BookmarkAdapter;
import com.sonicmax.etiapp.loaders.UserInfoLoader;
import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class BookmarkManagerFragment extends Fragment
        implements UserInfoLoader.EventInterface {

    private BookmarkAdapter mBookmarkAdapter;
    private ProgressDialog mDialog;
    private UserInfoLoader mBookmarkLoader;
    private List<Bookmark> mBookmarks;

    public BookmarkManagerFragment() {}

    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onAttach(Context context) {
        mBookmarkAdapter = new BookmarkAdapter(context);
        mBookmarkLoader = new UserInfoLoader(context, this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null
                || savedInstanceState.getParcelableArrayList("bookmarks") == null) {

            mDialog = new ProgressDialog(getContext());
            mDialog.setMessage("Getting bookmarks...");
            mDialog.show();

            mBookmarkLoader.loadUserInfo();
        }

        else {
            // Use bookmarks from savedInstanceState to populate adapter.
            mBookmarks = savedInstanceState.getParcelableArrayList("bookmarks");
            mBookmarkAdapter.updateBoards(mBookmarks);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_bookmarks, container, false);

        ListView boardList = (ListView) rootView.findViewById(R.id.listview_boards);
        boardList.setAdapter(mBookmarkAdapter);
        boardList.setOnItemClickListener(boardClickHandler);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save list of boards so we can quickly restore fragment
        if (mBookmarks != null) {
            ArrayList<Bookmark> bookmarkArray = new ArrayList<>(mBookmarks);
            outState.putParcelableArrayList("bookmarks", bookmarkArray);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        // Make sure that we don't leak progress dialog when exiting activity
        dismissDialog();
        super.onDetach();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    public void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private Bookmark getInboxBookmark() {
        int count = SharedPreferenceManager.getInt(getContext(), "inbox_count");
        String inboxWithCount = "Inbox " + "(" + count + ")";
        return new Bookmark(inboxWithCount, "https://endoftheinter.net/inbox.php");
    }

    ///////////////////////////////////////////////////////////////////////////
    // EventInterface callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onLoadBookmarks(List<Bookmark> bookmarks) {
        dismissDialog();
        mBookmarks = bookmarks;
        mBookmarks.add(0, getInboxBookmark());
        mBookmarkAdapter.updateBoards(bookmarks);
    }

    @Override
    public void onLoadFail() {
        // Lol error handling
    }

    ///////////////////////////////////////////////////////////////////////////
    // Click listener for UI
    ///////////////////////////////////////////////////////////////////////////

    AdapterView.OnItemClickListener boardClickHandler = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Bookmark target = mBookmarkAdapter.getItem(position);
            Context context = getContext();
            Intent intent = new Intent(context, TopicListActivity.class);
            intent.putExtra("url", target.getUrl());
            intent.putExtra("boardname", target.getName());
            context.startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_in_from_right,
                    R.anim.slide_out_to_left);
        }
    };
}
