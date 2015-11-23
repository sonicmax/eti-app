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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class TopicListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {

    private final String LOG_TAG = TopicListFragment.class.getSimpleName();
    private int mPageNumber = 1;
    private String mBoardName;
    private ProgressDialog mDialog;
    private static boolean mFirstRun = true;

    public static TopicListAdapter topicListAdapter;
    public static String nextPageUrl;
    public static String prevPageUrl;

    public TopicListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_topic_list, container, false);
        if (topicListAdapter == null) {
            topicListAdapter = new TopicListAdapter(getActivity());
        }
        else {
            topicListAdapter.clearTopics();
        }

        TextView boardName = (TextView) rootView.findViewById(R.id.board_name);
        // Might need to use this string later
        mBoardName = getActivity().getIntent().getStringExtra("boardname");
        boardName.setText(mBoardName);

        Button newTopicButton = (Button) rootView.findViewById(R.id.new_topic);
        newTopicButton.setOnClickListener(newTopicHandler);

        ListView topicList = (ListView) rootView.findViewById(R.id.listview_topics);
        topicList.setAdapter(topicListAdapter);
        topicList.setOnItemClickListener(topicClickHandler);
        topicList.setOnTouchListener(topicSwipeHandler);

        loadTopicList(getActivity().getIntent().getStringExtra("url"));

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

    private void loadTopicList(String url) {

        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "topiclist");
        args.putString("url", url);

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
    }

    OnSwipeListener topicSwipeHandler = new OnSwipeListener(getContext()) {

        @Override
        public void onSwipeLeft() {

            if (nextPageUrl != null) {
                loadTopicList(nextPageUrl);
                mPageNumber++;
                Toaster.makeToast(getContext(), "Page " + mPageNumber);
            }
        }

        @Override
        public void onSwipeRight() {
            // TODO: Store visited pages instead of using "Back to the present" href
            if (prevPageUrl != null) {
                loadTopicList(prevPageUrl);
                mPageNumber = 1;
                Toaster.makeToast(getContext(), "Page " + mPageNumber);
            }
        }
    };

    AdapterView.OnItemClickListener topicClickHandler = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Topic topic = topicListAdapter.getItem(position);
            Context context = getContext();
            Intent intent = new Intent(context, MessageListActivity.class);
            intent.putExtra("topic", topic);
            intent.putExtra("title", topic.getTitle());
            context.startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
        }

    };

    private View.OnClickListener newTopicHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.new_topic:
                    // Create new intent for BoardListActivity and attach response
                    Context context = getContext();
                    Intent intent = new Intent(context, PostTopicActivity.class);
                    intent.putExtra("title", mBoardName);
                    context.startActivity(intent);
                    break;
            }

        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Loader callbacks.
     */
    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {

        final Context context = getContext();

        mDialog = new ProgressDialog(getContext());
        mDialog.setMessage("Getting topics...");
        mDialog.show();

        return new AsyncLoadHandler(context, args) {

            @Override
            public List<Topic> loadInBackground() {
                String html = new WebRequest(context, args).sendRequest();
                return new TopicListScraper(getContext()).scrapeTopics(html);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data != null) {
            // We can be sure that data will safely cast to List<Topic>.
            @SuppressWarnings("unchecked")
            List<Topic> topics = (List<Topic>) data;
            topicListAdapter.updateTopics(topics);
        }
        mDialog.dismiss();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }

    public void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        final int REFRESH = 0;

        if (requestCode == REFRESH) {
            Log.v(LOG_TAG, "hit back button");
        }
    }

}
