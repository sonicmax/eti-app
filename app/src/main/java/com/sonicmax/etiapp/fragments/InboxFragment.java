package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import com.sonicmax.etiapp.utilities.Snacker;

import java.util.ArrayList;
import java.util.List;

/**
 * Same functionality as TopicListFragment, but with some minor differences
 * (eg. different click handlers)
 */

public class InboxFragment extends Fragment
        implements TopicListLoader.EventInterface, TopicListAdapter.EventInterface {

    private TopicListAdapter mTopicListAdapter;
    private String mUrl;
    private ProgressDialog mDialog;
    private TopicList mTopicList;
    private List<Topic> mTopics;
    private TopicListLoader mTopicListLoader;
    private View mRootView;
    private ListView mListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private int mPageNumber;
    private String mPrevPageUrl;
    private String mNextPageUrl;

    public InboxFragment() {}

    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onAttach(Context context) {
        // Create adapter to display list of inbox threads
        mTopicListAdapter = new TopicListAdapter(context, this);
        mTopicListLoader = new TopicListLoader(context, this);

        if (mPageNumber == 0) {
            mPageNumber = 1;
        }

        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // Get url of chosen topic list form intent and pass it to loader
            mUrl = getActivity().getIntent().getStringExtra("url");
            loadTopicList(mUrl);
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
                loadTopicList(mUrl);
            }
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_inbox, container, false);

        FloatingActionButton newTopicButton = (FloatingActionButton) mRootView.findViewById(R.id.new_topic);
        newTopicButton.setOnClickListener(inboxThreadCreator);

        mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.listview_topics_container);

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
        mListView.setOnItemClickListener(inboxClickHandler);
        mListView.setOnTouchListener(inboxSwipeHandler);

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
        // Make sure that we don't leak progress dialog when exiting activity
        dismissDialog();

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
        dismissDialog();
        mSwipeRefreshLayout.setRefreshing(false);

        // Get inbox threads and update adapter
        mTopics = topicList.getTopics();
        mPrevPageUrl = topicList.getPrevPageUrl();
        mNextPageUrl = topicList.getNextPageUrl();
        mPageNumber = topicList.getPageNumber();

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
        // TODO: Can we even access the pm inbox?
        final String name = "Message History";
        final String url = "https://boards.endoftheinter.net/history.php?b";
        loadTopicList(url, name);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    public void loadTopicList(String url, String name) {
        if (mSwipeRefreshLayout == null) {
            showDialog("Loading...");
        }
        else if (!mSwipeRefreshLayout.isRefreshing()) {
            showDialog("Loading...");
        }

        updateActionBarTitle(name);

        mTopicListLoader.load(url);
    }

    public void loadTopicList(String url) {
        if (mSwipeRefreshLayout == null) {
            showDialog("Loading...");
        }
        else if (!mSwipeRefreshLayout.isRefreshing()) {
            showDialog("Loading...");
        }

        mTopicListLoader.load(url);
    }

    public void refreshTopicList() {
        loadTopicList(mUrl);
    }

    private void loadNextPage() {
        if (mNextPageUrl != null) {
            loadTopicList(mNextPageUrl);
        }
    }

    private void loadPrevPage() {
        if (mPrevPageUrl != null) {
            loadTopicList(mPrevPageUrl);
        }
    }

    private void showDialog(String message) {
        mDialog = new ProgressDialog(getContext());
        mDialog.setMessage(message);
        mDialog.show();
    }

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
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

    OnSwipeListener inboxSwipeHandler = new OnSwipeListener(getContext()) {

        @Override
        public void onSwipeLeft() {
            loadNextPage();
        }

        @Override
        public void onSwipeRight() {
            loadPrevPage();
        }
    };

    private AdapterView.OnItemClickListener inboxClickHandler = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Topic topic = mTopicListAdapter.getItem(position);
            Context context = getContext();
            Intent intent = new Intent(context, InboxThreadActivity.class);
            intent.putExtra("topic", topic);
            intent.putExtra("title", topic.getTitle());
            context.startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_in_from_right,
                    R.anim.slide_out_to_left);
        }

    };

    private View.OnClickListener inboxThreadCreator = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // TODO: Figure out how to create new PM threads in non-sucky way
        }
    };
}
