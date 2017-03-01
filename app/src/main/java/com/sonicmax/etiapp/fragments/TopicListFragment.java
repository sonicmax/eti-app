package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.activities.InboxThreadActivity;
import com.sonicmax.etiapp.activities.MessageListActivity;
import com.sonicmax.etiapp.adapters.TopicListAdapter;
import com.sonicmax.etiapp.listeners.OnSwipeListener;
import com.sonicmax.etiapp.loaders.TopicListLoader;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.objects.TopicList;
import com.sonicmax.etiapp.utilities.DialogHandler;
import com.sonicmax.etiapp.utilities.Snacker;
import com.sonicmax.etiapp.utilities.Toaster;

import java.util.ArrayList;
import java.util.List;

public class TopicListFragment extends Fragment
        implements TopicListLoader.EventInterface, TopicListAdapter.EventInterface {

    private TopicListAdapter mTopicListAdapter;
    private String mUrl;
    private TopicList mTopicList;
    private List<Topic> mTopics;
    private TopicListLoader mTopicListLoader;
    private ListView mListView;
    private View mRootView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

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
                mTopics = savedInstanceState.getParcelableArrayList("topics");
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

        FloatingActionButton newTopicButton = (FloatingActionButton) mRootView.findViewById(R.id.new_topic);
        newTopicButton.setOnClickListener(newTopicHandler);

        mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.listview_topics_container);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.accent);

        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshTopicList();
                    }
                }
        );

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
        super.onSaveInstanceState(outState);
        outState.putParcelable("topiclist", mTopicList);
        ArrayList<Topic> parcelableTopics = new ArrayList<>(mTopics);
        outState.putParcelableArrayList("topics", parcelableTopics);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        DialogHandler.dismissDialog();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Event interface methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onRequestNextPage() {
        loadNextPage();
    }

    @Override
    public void onRequestLastUnreadPost(int position) {
        // Get Topic object from adapter
        Topic target = mTopics.get(position);

        Intent intent;
        if (target.getUrl().contains("inboxthread.php")) {
            intent = new Intent(getContext(), InboxThreadActivity.class);
        }
        else {
            intent = new Intent(getContext(), MessageListActivity.class);
        }

        String total = target.etiFormatSize();
        if (total.contains("(")) {
            // Redirect user to page containing last unread post, and add scroll position to intent
            String trimmedTotal = total.substring(total.indexOf("(")).replace("(+", "");
            int unreadPosts = Integer.parseInt(trimmedTotal.substring(0, trimmedTotal.indexOf(")")));
            int lastUnreadPost = target.size() - unreadPosts;
            int startPage = (lastUnreadPost / 50) + 1;
            int remainder = lastUnreadPost % 50;

            intent.putExtra("page", startPage);
            intent.putExtra("post", remainder);
        }

        else {
            intent.putExtra("last_page", true);
        }

        intent.putExtra("topic", target);
        intent.putExtra("title", target.getTitle());

        getContext().startActivity(intent);
    }

    @Override
    public void onLoadTopicList(TopicList topicList) {
        // Clean up UI
        DialogHandler.dismissDialog();
        mSwipeRefreshLayout.setRefreshing(false);

        // Get data from TopicList and update adapter
        mTopics = topicList.getTopics();
        mPrevPageUrl = topicList.getPrevPageUrl();
        mNextPageUrl = topicList.getNextPageUrl();

        mTopicListAdapter.setCurrentPage(mPageNumber);
        mTopicListAdapter.getCurrentTime();
        mTopicListAdapter.setHasNextPage((mNextPageUrl != null));
        mTopicListAdapter.updateTopics(mTopics);

        if (mPageNumber > 1) {
            // TODO: Should maintain screen position in some cases
            scrollToFirstTopic();
            Snacker.showSnackBar(mRootView, "Page " + mPageNumber);
        }
    }

    @Override
    public void onCreateTopic(Intent intent) {
        DialogHandler.dismissDialog();
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
        if (mSwipeRefreshLayout == null) {
            DialogHandler.showDialog(getContext(), "Loading...");
        }
        else if (!mSwipeRefreshLayout.isRefreshing()) {
            DialogHandler.showDialog(getContext(), "Loading...");
        }

        updateActionBarTitle(name);
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
    }

    private void updateActionBarTitle(String newTitle) {
        if (newTitle != null) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            ActionBar actionBar = activity.getSupportActionBar();

            if (actionBar != null) {
                LayoutInflater inflator = LayoutInflater.from(getContext());
                View v = inflator.inflate(R.layout.title_view, null);

                String title = activity.getIntent().getStringExtra("title");
                ((TextView) v.findViewById(R.id.title)).setText(title);

                actionBar.setCustomView(v);
            }
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
