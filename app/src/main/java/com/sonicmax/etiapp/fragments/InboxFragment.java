package com.sonicmax.etiapp.fragments;

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
import com.sonicmax.etiapp.network.ResponseCache;
import com.sonicmax.etiapp.network.ResponseCacheEntry;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.objects.TopicList;
import com.sonicmax.etiapp.utilities.DialogHandler;
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
    private TopicList mTopicList;
    private List<Topic> mTopics;
    private TopicListLoader mTopicListLoader;
    private View mRootView;
    private ListView mListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ResponseCache mResponseCache;

    private int mPageNumber;
    private int mStartPoint;
    private String mPrevPageUrl;
    private String mNextPageUrl;

    private boolean mIsLoading = false;

    public InboxFragment() {}

    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        init(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mUrl = getUrlFromIntent();
            loadTopicList(mUrl, null);
        }

        else {
            restoreFragmentFromBundle(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_inbox, container, false);

        FloatingActionButton newTopicButton = (FloatingActionButton) mRootView.findViewById(R.id.new_topic);
        newTopicButton.setOnClickListener(inboxThreadCreator);

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
        mListView.setOnItemClickListener(inboxClickHandler);
        mListView.setOnTouchListener(inboxSwipeHandler);

        return mRootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        cachePageData();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mIsLoading && !restoreFromCache()) {
            Snacker.showSnackBar(mRootView, "Cache load failed");
            refreshTopicList();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("topiclist", mTopicList);
        outState.putParcelableArrayList("topics", new ArrayList<>(mTopics));
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
        ((AppCompatActivity) getContext()).overridePendingTransition(R.anim.slide_in_from_right,
                R.anim.slide_out_to_left);
    }

    @Override
    public void onLoadTopicList(TopicList topicList) {
        // Clean up UI
        DialogHandler.dismissDialog();
        mSwipeRefreshLayout.setRefreshing(false);

        // Get inbox threads and update adapter
        mTopicList = topicList;
        mTopics = topicList.getTopics();
        mPrevPageUrl = topicList.getPrevPageUrl();
        mNextPageUrl = topicList.getNextPageUrl();
        mPageNumber = topicList.getPageNumber();

        mTopicListAdapter.setCurrentPage(mPageNumber);
        mTopicListAdapter.getCurrentTime();
        mTopicListAdapter.setHasNextPage((mNextPageUrl != null));
        mTopicListAdapter.updateTopics(mTopics);

        scrollToPosition(mStartPoint);

        if (mStartPoint > 0) {
            mStartPoint = 0;
        }

        if (mPageNumber > 1) {
            Snacker.showSnackBar(mRootView, "Page " + mPageNumber);
        }

        mIsLoading = false;
    }

    @Override
    public void onCreateTopic(Intent intent) {
        DialogHandler.dismissDialog();
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

    private void init(Context context) {
        mTopicListAdapter = new TopicListAdapter(context, this);
        mTopicListLoader = new TopicListLoader(context, this);
        mResponseCache = new ResponseCache(context);

        if (mPageNumber == 0) {
            mPageNumber = 1;
        }
    }

    private String getUrlFromIntent() {
        Intent intent = getActivity().getIntent();
        return intent.getStringExtra("url");
    }

    private void setIntentUrl(String url) {
        Intent intent = getActivity().getIntent();
        intent.putExtra("url", url);
    }

    private void restoreFragmentFromBundle(Bundle savedInstanceState) {
        mTopicList = savedInstanceState.getParcelable("topiclist");
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

    /**
     * Caches current URL, loaded HTML, page number and adapter position of current fragment
     * so we can restore them later.
     */
    private void cachePageData() {
        int position = 0;

        if (mListView != null) {
            position = mListView.getFirstVisiblePosition();
        }

        mResponseCache.cacheResponseData(mUrl, mTopicList.getHtml(), mPageNumber, position);
    }

    /**
     * Attempts to restore adapter content from cache.
     * @return boolean value indicating whether operation was successful
     */
    private boolean restoreFromCache() {
        if (mResponseCache == null) {
            mResponseCache = new ResponseCache(getContext());
        }

        ResponseCacheEntry cachedResponse = mResponseCache.getResponseFromCache(getUrlFromIntent());

        if (cachedResponse != null) {
            String html = cachedResponse.getHtml();
            mUrl = cachedResponse.getUrl();
            mPageNumber = cachedResponse.getPageNumber();
            mStartPoint = cachedResponse.getAdapterPosition();

            mTopicListLoader = new TopicListLoader(getContext(), this);
            mTopicListLoader.loadFromCache(mUrl, html);

            Snacker.showSnackBar(mRootView, "Cache load success");
            return true;
        }

        else {
            return false;
        }
    }

    public void loadTopicList(String url, String name) {
        mIsLoading = true;

        if (mSwipeRefreshLayout == null) {
            DialogHandler.showDialog(getContext(), "Loading...");
        }
        else if (!mSwipeRefreshLayout.isRefreshing()) {
            DialogHandler.showDialog(getContext(), "Loading...");
        }

        updateActionBarTitle(name);
        setIntentUrl(url);
        mTopicListLoader.load(url);
    }

    public void refreshTopicList() {
        if (mTopicListLoader == null) {
            mTopicListLoader = new TopicListLoader(getContext(), this);
        }

        loadTopicList(mUrl, null);
    }

    private void loadNextPage() {
        if (mNextPageUrl != null) {
            loadTopicList(mNextPageUrl, null);
        }
    }

    private void loadPrevPage() {
        if (mPrevPageUrl != null) {
            loadTopicList(mPrevPageUrl, null);
        }
    }

    private void scrollToPosition(int position) {
        mListView.setSelection(position);
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
