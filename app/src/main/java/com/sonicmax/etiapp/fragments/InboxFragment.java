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
import com.sonicmax.etiapp.adapters.TopicListAdapter;
import com.sonicmax.etiapp.listeners.OnSwipeListener;
import com.sonicmax.etiapp.loaders.TopicListLoader;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.objects.TopicList;
import com.sonicmax.etiapp.utilities.Toaster;

import java.util.List;

/**
 * Same functionality as TopicListFragment, but with some minor differences
 * (eg. different click handlers)
 */

public class InboxFragment extends Fragment
        implements TopicListLoader.EventInterface, TopicListAdapter.EventInterface {
    public InboxFragment() {}

    public TopicListAdapter mTopicListAdapter;
    private String mUrl;
    private ProgressDialog mDialog;
    private TopicList mTopicList;
    private List<Topic> mTopics;
    private TopicListLoader mTopicListLoader;
    public TextView mBoardName;

    private int mPageNumber;
    private boolean mFirstRun = true;

    private String mPrevPageUrl;
    private String mNextPageUrl;

    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onAttach(Context context) {
        // Create adapter to display list of inbox threads
        mTopicListAdapter = new TopicListAdapter(context, this);
        mTopicListLoader = new TopicListLoader(context, this);

        if (mFirstRun) {
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

                if (mNextPageUrl != null) {
                    mTopicListAdapter.setHasNextPage(true);
                }
                else {
                    mTopicListAdapter.setHasNextPage(false);
                }

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

        View rootView = inflater.inflate(R.layout.fragment_inbox, container, false);

        mBoardName = (TextView) rootView.findViewById(R.id.board_name_text);
        String name = getActivity().getIntent().getStringExtra("boardname");
        mBoardName.setText(name);

        FloatingActionButton newTopicButton = (FloatingActionButton) rootView.findViewById(R.id.new_topic);
        newTopicButton.setOnClickListener(inboxThreadCreator);

        ListView topicList = (ListView) rootView.findViewById(R.id.listview_topics);
        topicList.setAdapter(mTopicListAdapter);
        mTopicListAdapter.setListView(topicList);
        topicList.setOnItemClickListener(inboxClickHandler);
        topicList.setOnTouchListener(inboxSwipeHandler);

        return rootView;
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
        dismissDialog();

        super.onDetach();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Event interface methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onRequestNextPage() {
        if (mNextPageUrl != null) {
            loadTopicList(null, mNextPageUrl);
            mPageNumber++;
            Toaster.makeToast(getContext(), "Page " + mPageNumber);
        }
    }

    @Override
    public void onLoadTopicList(TopicList topicList) {
        dismissDialog();
        mTopics = topicList.getTopics();
        mPrevPageUrl = topicList.getPrevPageUrl();
        mNextPageUrl = topicList.getNextPageUrl();
        mPageNumber = topicList.getPageNumber();

        if (mNextPageUrl != null) {
            mTopicListAdapter.setHasNextPage(true);
        }
        else {
            mTopicListAdapter.setHasNextPage(false);
        }

        mTopicListAdapter.updateTopics(mTopics);
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
        mFirstRun = false;

        mDialog = new ProgressDialog(getContext());
        mDialog.setMessage("Loading topics...");
        mDialog.show();

        if (name != null) {
            mBoardName.setText(name);
        }

        mTopicListLoader.load(url);
    }

    public void refreshTopicList() {
        loadTopicList(null, mUrl);
    }

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // User input listeners
    ///////////////////////////////////////////////////////////////////////////

    OnSwipeListener inboxSwipeHandler = new OnSwipeListener(getContext()) {

        @Override
        public void onSwipeLeft() {
            if (mNextPageUrl != null) {
                loadTopicList(null, mNextPageUrl);
                mPageNumber++;
                Toaster.makeToast(getContext(), "Page " + mPageNumber);
            }
        }

        @Override
        public void onSwipeRight() {
            if (mPrevPageUrl != null) {
                loadTopicList(null, mPrevPageUrl);
                mPageNumber = 1;
                Toaster.makeToast(getContext(), "Page " + mPageNumber);
            }
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
