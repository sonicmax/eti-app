package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Fragment for message list - handles UI actions
 */

public class MessageListFragment extends Fragment implements
        AdapterView.OnItemLongClickListener, View.OnClickListener,
        LoaderManager.LoaderCallbacks<Object> {

    private final String LOG_TAG = MessageListFragment.class.getSimpleName();
    private String mTopicId;
    private String mTitle;
    private View mRootView;
    private ListView mMessageList;
    private ActionMode mActionMode;
    private ProgressDialog mDialog;
    private int mPosition = -1;
    private MessageListScraper mScraper;
    private MessageListAdapter mMessageListAdapter;
    private static boolean mFirstRun = true;

    public static String prevPageUrl;
    public static String nextPageUrl;
    public static int mPageNumber;

    public MessageListFragment() {}


    /**
     *      Lifecycle stuff
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_message_list, container, false);
        mMessageList = (ListView) mRootView.findViewById(R.id.listview_messages);
        Button newMessageButton = (Button) mRootView.findViewById(R.id.new_message);
        TextView topicTitle = (TextView) mRootView.findViewById(R.id.topic_title);

        Intent intent = getActivity().getIntent();
        Topic topic = intent.getParcelableExtra("topic");
        mTopicId = topic.getId();
        mTitle = intent.getStringExtra("title");
        mPageNumber = intent.getIntExtra("page", 1);

        String url = (intent.getBooleanExtra("lastpage", false))
                ? topic.getLastPageUrl() : topic.getUrl();

        if (mScraper == null) {
            mScraper = new MessageListScraper(url);
        }

        topicTitle.setText(mTitle);
        newMessageButton.setOnClickListener(this);

        if (mMessageListAdapter == null) {
            mMessageListAdapter = new MessageListAdapter(getActivity());
        }
        else {
            mMessageListAdapter.clearMessages();
        }

        mMessageList.setAdapter(mMessageListAdapter);
        mMessageList.setOnItemLongClickListener(this);
        mMessageList.setOnTouchListener(pageSwipeHandler);
        mMessageList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if (intent.getBooleanExtra("needs_scroll", false)) {
            scrollToBottom();
        }

        loadMessageList(url, false);

        return mRootView;
    }

    @Override
    public void onDetach() {

        // Make sure that we don't leak progress dialog when exiting activity
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }

        super.onDetach();
    }

    private void loadMessageList(String url, boolean filter) {

        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "messagelist");
        args.putString("url", url);
        args.putBoolean("filter", filter);

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

    /**
     *      Contextual action mode methods
     */

    @Override
    public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id) {

        if (mActionMode != null && mPosition == -1) {
            // We can't do anything
            return false;
        }

        // Cast getActivity() value to AppCompatActivity so we can use startSupportActionMode
        mActionMode = ((AppCompatActivity) getActivity())
                .startSupportActionMode(mActionModeCallback);

        if (mPosition > -1) {
            // Set old position to false
            mMessageList.setItemChecked(mPosition, false);
        }

        mMessageList.setItemChecked(position, true);
        mPosition = position;
        return true;
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_message_action, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            Message target = mMessageListAdapter.getItem(mPosition);

            switch (item.getItemId()) {
                case R.id.quote:
                    String quote = new QuoteHandler().parse(target.getHtml());
                    // Start PostMessageActivity with quoted message
                    Context context = getContext();
                    Intent intent = new Intent(context, PostMessageActivity.class);
                    intent.putExtra("quote", quote);
                    intent.putExtra("title", mTitle);
                    intent.putExtra("id", mTopicId);
                    intent.putExtra("lastpage",
                            getActivity().getIntent().getIntExtra("lastpage", 1));
                    context.startActivity(intent);
                    return true;
                case R.id.highlight:
                    return true;
                case R.id.ignore:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mMessageList.setItemChecked(mPosition, false);
            mActionMode = null;
        }
    };


    /**
     *      Allows user to post new messages
     */

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_message:
                Context context = getContext();
                Intent intent = new Intent(context, PostMessageActivity.class);
                intent.putExtra("title", mTitle);
                intent.putExtra("id", mTopicId);
                context.startActivity(intent);
                break;
        }

    }

    /**
     *      Allows user to swipe left/right on screen to change pages in topic
     */

    OnSwipeListener pageSwipeHandler = new OnSwipeListener(getContext()) {

        @Override
        public void onSwipeLeft() {
            if (nextPageUrl != null) {
                loadMessageList(nextPageUrl, false);
                mPageNumber++;
                Toaster.makeToast(getContext(), "Page " + mPageNumber);
            }
        }

        @Override
        public void onSwipeRight() {
            if (prevPageUrl != null) {
                loadMessageList(prevPageUrl, false);
                mPageNumber--;
                Toaster.makeToast(getContext(), "Page " + mPageNumber);
            }
        }
    };

    private void scrollToBottom() {
        final ListView messageList = (ListView) mRootView.findViewById(R.id.listview_messages);

        messageList.post(new Runnable() {
            @Override
            public void run() {
                messageList.setSelection(mMessageListAdapter.getCount() - 1);
            }
        });
    }

    private void scrollToPosition(final int position) {

        final ListView messageList = (ListView) mRootView.findViewById(R.id.listview_messages);
        messageList.post(new Runnable() {
            @Override
            public void run() {
                messageList.setSelection(position);
            }
        });
    }

    /**
     * Loader callbacks.
     */
    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {

        final Context context = getContext();

        mDialog = new ProgressDialog(getContext());
        mDialog.setMessage("Getting messages...");
        mDialog.show();

        return new AsyncLoadHandler(context, args) {

            @Override
            public List<Message> loadInBackground() {
                String html = new WebRequest(context, args).sendRequest();
                return mScraper.scrapeMessages(html, args.getBoolean("filter"));
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data != null) {
            // We can be sure that data will safely cast to List<Message>.
            @SuppressWarnings("unchecked")
            List<Message> messages = (List<Message>) data;
            mMessageListAdapter.updateMessages(messages);
        }
        mDialog.dismiss();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }
}
