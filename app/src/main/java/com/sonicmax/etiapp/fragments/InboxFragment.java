package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
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
    private TextView mBoardName;
    private View mRootView;
    private ListView mListView;

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

        mRootView = inflater.inflate(R.layout.fragment_inbox, container, false);

        mBoardName = (TextView) mRootView.findViewById(R.id.board_name_text);
        String name = getActivity().getIntent().getStringExtra("boardname");
        mBoardName.setText(name);

        FloatingActionButton newTopicButton = (FloatingActionButton) mRootView.findViewById(R.id.new_topic);
        newTopicButton.setOnClickListener(inboxThreadCreator);

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
        if (mTopicList != null) {
            outState.putParcelable("topiclist", mTopicList);
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
        dismissDialog();
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
        }
    }

    private void loadPrevPage() {
        if (mPrevPageUrl != null) {
            loadTopicList(null, mPrevPageUrl);
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

        }
    };
}
