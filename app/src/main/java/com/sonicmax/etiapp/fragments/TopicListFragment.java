package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.activities.MessageListActivity;
import com.sonicmax.etiapp.adapters.TopicListAdapter;
import com.sonicmax.etiapp.listeners.OnSwipeListener;
import com.sonicmax.etiapp.loaders.TopicListLoader;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.objects.TopicList;
import com.sonicmax.etiapp.utilities.Snacker;
import com.sonicmax.etiapp.utilities.Toaster;

import java.util.ArrayList;
import java.util.List;

public class TopicListFragment extends Fragment
        implements TopicListLoader.EventInterface, TopicListAdapter.EventInterface {

    private TopicListAdapter mTopicListAdapter;
    private String mUrl;
    private ProgressDialog mDialog;
    private TopicList mTopicList;
    private List<Topic> mTopics;
    private TopicListLoader mTopicListLoader;
    private TextView mBoardName;
    private ListView mListView;
    private View mRootView;

    private int mPageNumber;

    private String mPrevPageUrl;
    private String mNextPageUrl;

    public TopicListFragment() {}

    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onAttach(Context context) {
        // Create adapter to display list of topics
        mTopicListAdapter = new TopicListAdapter(context, this);
        mTopicListLoader = new TopicListLoader(context, this);

        // Topic list will always start at page 1. mFirstRun is set to false after starting loader,
        // in case user changes pages (handled with swipe event)
        if (mPageNumber == 0) {
            mPageNumber = 1;
        }

        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            mUrl = getActivity().getIntent().getStringExtra("url");
            loadTopicList(null, mUrl);
        }

        else {
            mTopicList = savedInstanceState.getParcelable("topiclist");

            if (mTopicList != null) {
                mTopics = mTopicList.getTopics();
                mPageNumber = mTopicList.getPageNumber();
                mUrl = mTopicList.getUrl();
                mPrevPageUrl = mTopicList.getPrevPageUrl();
                mNextPageUrl = mTopicList.getNextPageUrl();

                mTopicListAdapter.setCurrentPage(mPageNumber);
                mTopicListAdapter.getCurrentTime();
                mTopicListAdapter.setHasNextPage((mNextPageUrl != null));
                mTopicListAdapter.updateTopics(mTopics);
            }
            else {
                mUrl = getActivity().getIntent().getStringExtra("url");
                loadTopicList(null, mUrl);
            }
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_topic_list, container, false);

        mBoardName = (TextView) mRootView.findViewById(R.id.board_name_text);
        String name = getActivity().getIntent().getStringExtra("boardname");
        mBoardName.setText(name);

        FloatingActionButton newTopicButton = (FloatingActionButton) mRootView.findViewById(R.id.new_topic);
        newTopicButton.setOnClickListener(newTopicHandler);

        mListView = (ListView) mRootView.findViewById(R.id.listview_topics);
        mListView.setAdapter(mTopicListAdapter);
        mTopicListAdapter.setListView(mListView);
        mListView.setOnItemClickListener(topicClickHandler);
        mListView.setOnTouchListener(topicSwipeHandler);

        return mRootView;
    }

    @Override
    public void onResume() {
        // loadTopicList(mUrl);
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save list of topics so we can quickly restore fragment
        if (mTopicList != null) {
            outState.putParcelable("topiclist", mTopicList);
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
    // Event interface methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onRequestNextPage() {
        loadNextPage();
    }

    @Override
    public void onLoadTopicList(TopicList topicList) {
        dismissDialog();
        mTopics = topicList.getTopics();
        mPrevPageUrl = topicList.getPrevPageUrl();
        mNextPageUrl = topicList.getNextPageUrl();

        mTopicListAdapter.setCurrentPage(mPageNumber);
        mTopicListAdapter.getCurrentTime();
        mTopicListAdapter.setHasNextPage((mNextPageUrl != null));
        mTopicListAdapter.updateTopics(mTopics);

        if (mPageNumber > 1) {
            scrollToFirstTopic();
            Snacker.showSnackBar(mRootView, "Page " + mPageNumber);
        }
    }

    @Override
    public void onCreateTopic(Intent intent) {
        dismissDialog();
        getContext().startActivity(intent);
    }

    @Override
    public void onInternalServerError() {
        final String name = "Message History";
        final String url = "https://boards.endoftheinter.net/history.php?b";
        loadTopicList(name, url);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    public void loadTopicList(String name, String url) {
        showDialog("Loading...");
        if (name != null) {
            mBoardName.setText(name);
        }
        mTopicListLoader.load(url);
    }

    public void refreshTopicList() {
        loadTopicList(null, mUrl);
    }

    private void loadNextPage() {
        if (mNextPageUrl != null) {
            loadTopicList(null, mNextPageUrl);
            mPageNumber++;
        }
    }

    private void loadFirstPage() {
        if (mPrevPageUrl != null) {
            loadTopicList(null, mPrevPageUrl);
            mPageNumber = 1;
        }
    }

    private void scrollToFirstTopic() {
        final int FIRST_TOPIC = 0;
        mListView.setSelection(FIRST_TOPIC);
        // mListView.setSelectionAfterHeaderView();
    }

    public void showDialog(String message) {
        mDialog = new ProgressDialog(getContext());
        mDialog.setMessage(message);
        mDialog.show();
    }

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // User input listeners
    ///////////////////////////////////////////////////////////////////////////

    OnSwipeListener topicSwipeHandler = new OnSwipeListener(getContext()) {

        @Override
        public void onSwipeLeft() {
            loadNextPage();
        }

        @Override
        public void onSwipeRight() {
            loadFirstPage();
        }
    };

    public AdapterView.OnItemClickListener topicClickHandler = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Topic topic = mTopicListAdapter.getItem(position);
            Context context = getContext();
            Intent intent = new Intent(context, MessageListActivity.class);
            intent.putExtra("topic", topic);
            intent.putExtra("title", topic.getTitle());
            context.startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_in_from_right,
                    R.anim.slide_out_to_left);
        }

    };

    private View.OnClickListener newTopicHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.new_topic:
                    mTopicListLoader.openPostTopicActivity();
                    break;
            }
        }
    };
}
