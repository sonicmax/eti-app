package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.activities.MessageListActivity;
import com.sonicmax.etiapp.activities.PostTopicActivity;
import com.sonicmax.etiapp.activities.TopicListActivity;
import com.sonicmax.etiapp.adapters.TopicListAdapter;
import com.sonicmax.etiapp.listeners.OnSwipeListener;
import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.scrapers.PostmsgScraper;
import com.sonicmax.etiapp.scrapers.TopicListScraper;
import com.sonicmax.etiapp.utilities.AsyncLoader;
import com.sonicmax.etiapp.utilities.Toaster;

import java.util.ArrayList;
import java.util.List;

public class TopicListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {

    private final int LOAD_TOPIC_LIST = 0;
    private final int POST_TOPIC = 1;

    private boolean mInternalServerError = false;
    private TopicListAdapter mTopicListAdapter;
    private String mUrl;
    private ProgressDialog mDialog;
    private List<Topic> mTopics;
    private TextView mBoardName;

    private int mPageNumber;
    private boolean mFirstRun = true;

    public static String nextPageUrl;
    public static String prevPageUrl;

    public TopicListFragment() {}


    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onAttach(Context context) {
        // Create adapter to display list of topics
        mTopicListAdapter = new TopicListAdapter(context);

        // Topic list will always start at page 1. mFirstRun is set to false after starting loader,
        // in case user changes pages (handled with swipe event)
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
            // Get url in case we need to refresh topic list
            mUrl = savedInstanceState.getString("url");

            // Populate adapter with list of topics from savedInstanceState
            mTopics = savedInstanceState.getParcelableArrayList("topics");
            mTopicListAdapter.updateTopics(mTopics);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_topic_list, container, false);

        mBoardName = (TextView) rootView.findViewById(R.id.board_name_text);
        String name = getActivity().getIntent().getStringExtra("boardname");
        mBoardName.setText(name);

        FloatingActionButton newTopicButton = (FloatingActionButton) rootView.findViewById(R.id.new_topic);
        newTopicButton.setOnClickListener(newTopicHandler);

        ListView topicList = (ListView) rootView.findViewById(R.id.listview_topics);
        topicList.setAdapter(mTopicListAdapter);
        topicList.setOnItemClickListener(topicClickHandler);
        topicList.setOnTouchListener(topicSwipeHandler);

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
        if (mTopics != null) {
            ArrayList<Topic> topicArray = new ArrayList<>(mTopics);
            outState.putParcelableArrayList("topics", topicArray);
        }
        // Save URL in case we need to refresh topic list
        if (mUrl != null) {
            outState.putString("url", mUrl);
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
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    public void loadTopicList(String name, String url) {
        if (name != null) {
            mBoardName.setText(name);
        }

        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "topiclist");
        args.putString("url", url);

        LoaderManager loaderManager = getLoaderManager();

        if (loaderManager.getLoader(LOAD_TOPIC_LIST) == null) {
            loaderManager.initLoader(LOAD_TOPIC_LIST, args, this).forceLoad();
            mFirstRun = false;
        }
        else {
            loaderManager.restartLoader(LOAD_TOPIC_LIST, args, this).forceLoad();
        }
    }

    public void refreshTopicList() {
        loadTopicList(null, mUrl);
    }

    OnSwipeListener topicSwipeHandler = new OnSwipeListener(getContext()) {

        @Override
        public void onSwipeLeft() {
            if (nextPageUrl != null) {
                loadTopicList(null, nextPageUrl);
                mPageNumber++;
                Toaster.makeToast(getContext(), "Page " + mPageNumber);
            }
        }

        @Override
        public void onSwipeRight() {
            if (prevPageUrl != null) {
                loadTopicList(null, prevPageUrl);
                mPageNumber = 1;
                Toaster.makeToast(getContext(), "Page " + mPageNumber);
            }
        }
    };

    AdapterView.OnItemClickListener topicClickHandler = new AdapterView.OnItemClickListener() {

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
                    openPostTopicActivity();
                    break;
            }

        }
    };

    private void openPostTopicActivity() {
        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "newtopic");

        LoaderManager loaderManager = getLoaderManager();

        if (loaderManager.getLoader(POST_TOPIC) == null) {
            getLoaderManager().initLoader(POST_TOPIC, args, this).forceLoad();

        } else {
            getLoaderManager().restartLoader(POST_TOPIC, args, this).forceLoad();
        }
    }

    private void handleInternalServerError() {
        final String name = "Message History";
        final String url = "https://boards.endoftheinter.net/history.php?b";
        loadTopicList(name, url);
    }

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {
        final String HTTP_INTERNAL_SERVER_ERROR = "500";
        final Context context = getContext();

        switch (id) {
            case LOAD_TOPIC_LIST:
                mDialog = new ProgressDialog(context);

                if (mInternalServerError) {
                    // NOTE: mDialog will already be showing at this point
                    mDialog.setMessage("Internal server error. Loading message history...");

                } else {
                    mDialog.setMessage("Loading topics...");
                }

                mDialog.show();

                return new AsyncLoader(context, args) {

                    @Override
                    public List<Topic> loadInBackground() {
                        String html = new WebRequest(context, args).sendRequest();

                        if (html.equals(HTTP_INTERNAL_SERVER_ERROR)) {
                            mInternalServerError = true;
                            return null;

                        } else {
                            mInternalServerError = false;
                            return new TopicListScraper(getContext()).scrapeTopics(html);
                        }
                    }
                };

            case POST_TOPIC:
                mDialog = new ProgressDialog(getContext());
                mDialog.setMessage("Loading...");
                mDialog.show();

                return new AsyncLoader(context, args) {

                    @Override
                    public String loadInBackground() {
                        return new WebRequest(context, args).sendRequest();
                    }
                };

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data != null) {
            switch (loader.getId()) {
                case LOAD_TOPIC_LIST:
                    // We can be sure that data will safely cast to List<Topic>.
                    mTopics = (List<Topic>) data;
                    mTopicListAdapter.getCurrentTime();
                    mTopicListAdapter.updateTopics(mTopics);
                    break;

                case POST_TOPIC:
                    Context context = getContext();

                    String response = (String) data;
                    new PostmsgScraper(context).parseResponse(response);

                    Intent intent = new Intent(context, PostTopicActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    context.startActivity(intent);
                    break;
            }

            dismissDialog();

        } else {
            if (mInternalServerError) {
                // We can try to load message history
                dismissDialog();
                handleInternalServerError();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }
}
