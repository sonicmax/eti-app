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

import com.sonicmax.etiapp.adapters.TopicListAdapter;
import com.sonicmax.etiapp.scrapers.PostmsgScraper;
import com.sonicmax.etiapp.scrapers.TopicListScraper;

import java.util.List;

public class TopicListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {

    private final String LOG_TAG = TopicListFragment.class.getSimpleName();
    private final int LOAD_TOPIC = 0;
    private final int POST_TOPIC = 1;

    private int mPageNumber = 1;
    private ProgressDialog mDialog;
    private static boolean mFirstRun = true;
    private int currentLoader;

    public static TopicListAdapter topicListAdapter;
    public static String nextPageUrl;
    public static String prevPageUrl;

    public TopicListFragment() {}

    @Override
    public void onAttach(Context context) {

        if (topicListAdapter == null) {
            topicListAdapter = new TopicListAdapter(getActivity());
        }
        else {
            topicListAdapter.clearTopics();
        }

        loadTopicList(getActivity().getIntent().getStringExtra("url"));

        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_topic_list, container, false);

        TextView boardName = (TextView) rootView.findViewById(R.id.board_name);
        String name = getActivity().getIntent().getStringExtra("boardname");
        boardName.setText(name);

        Button newTopicButton = (Button) rootView.findViewById(R.id.new_topic);
        newTopicButton.setOnClickListener(newTopicHandler);

        ListView topicList = (ListView) rootView.findViewById(R.id.listview_topics);
        topicList.setAdapter(topicListAdapter);
        topicList.setOnItemClickListener(topicClickHandler);
        topicList.setOnTouchListener(topicSwipeHandler);

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
        getLoaderManager().initLoader(1, args, this).forceLoad();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {

        final Context context = getContext();
        currentLoader = id;

        switch (id) {

            case LOAD_TOPIC:

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

            case POST_TOPIC:

                mDialog = new ProgressDialog(getContext());
                mDialog.setMessage("Loading...");
                mDialog.show();

                return new AsyncLoadHandler(context, args) {

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

            if (currentLoader == LOAD_TOPIC) {

                // We can be sure that data will safely cast to List<Topic>.
                @SuppressWarnings("unchecked")
                List<Topic> topics = (List<Topic>) data;
                topicListAdapter.updateTopics(topics);
            }

            else if (currentLoader == POST_TOPIC) {

                Context context = getContext();

                String response = (String) data;
                new PostmsgScraper(context).parseResponse(response);

                Intent intent = new Intent(context, PostTopicActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                context.startActivity(intent);
            }
        }

        mDialog.dismiss();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }
}
