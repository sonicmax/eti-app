package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
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

import java.util.List;

/**
 * Fragment for message list - handles UI actions
 */

public class MessageListFragment extends Fragment implements
        AdapterView.OnItemLongClickListener, View.OnClickListener,
        LoaderManager.LoaderCallbacks<Object> {

    private final String LOG_TAG = MessageListFragment.class.getSimpleName();
    private final int LOAD_MESSAGE = 0;
    private final int REFRESH = 1;
    private final int POST_MESSAGE = 2;

    private View mRootView;
    private ListView mMessageList;
    private MessageListScraper mScraper;
    private MessageListAdapter mMessageListAdapter;
    private ActionMode mActionMode;
    private ProgressDialog mDialog;
    private Bundle mArgs;
    private Topic mTopic;

    private int mSelection = -1;
    private int mCurrentId;
    private int mOldAdapterCount;
    public static int mPageNumber;

    private String mTitle;
    public static String prevPageUrl;
    public static String nextPageUrl;

    private static boolean mFirstRun = true;

    public MessageListFragment() {}


    /**
     *      Lifecycle stuff
     */
    @Override
    public void onAttach(Context context) {

        // Do some initialising/etc before we inflate layout.
        Intent intent = getActivity().getIntent();
        mTopic = intent.getParcelableExtra("topic");

        if (mFirstRun) {
            mPageNumber = intent.getIntExtra("page", 1);
        }

        String url = (intent.getBooleanExtra("last_page", false))
                ? mTopic.getLastPageUrl() : mTopic.getUrl();

        if (mScraper == null) {
            mScraper = new MessageListScraper(url);
        }
        else{
            // Make sure that MessageListScraper is using correct url
            mScraper.changeUrl(url);
        }

        if (mMessageListAdapter == null) {
            mMessageListAdapter = new MessageListAdapter(context);
        }
        else {
            mMessageListAdapter.clearMessages();
        }

        // Init/Restart loader and get posts for adapter
        loadMessageList(buildArgsForLoader(url, false), LOAD_MESSAGE);

        super.onAttach(context);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_message_list, container, false);
        mMessageList = (ListView) mRootView.findViewById(R.id.listview_messages);
        Button newMessageButton = (Button) mRootView.findViewById(R.id.new_message);
        TextView topicTitle = (TextView) mRootView.findViewById(R.id.topic_title);
        Intent intent = getActivity().getIntent();

        // Display topic title
        mTitle = intent.getStringExtra("title");
        topicTitle.setText(mTitle);

        // Set click listeners for views
        newMessageButton.setOnClickListener(this);
        mMessageList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mMessageList.setOnItemLongClickListener(this);
        mMessageList.setOnTouchListener(pageSwipeHandler);

        // Set up MessageListAdapter so we can display posts
        mMessageList.setAdapter(mMessageListAdapter);

        // Scroll to bottom of page if necessary
        if (intent.getBooleanExtra("last_page", false)) {
            scrollToBottom();
        }

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

    /**
     *      Helper methods for loader
     */
    private Bundle buildArgsForLoader(String url, boolean filter) {

        mArgs = new Bundle();
        mArgs.putString("method", "GET");
        mArgs.putString("type", "messagelist");
        mArgs.putString("url", url);
        mArgs.putBoolean("filter", filter);

        return mArgs;
    }

    private void loadMessageList(Bundle args, int loaderId) {

        LoaderManager loaderManager = getLoaderManager();

        if (mFirstRun) {
            loaderManager.initLoader(loaderId, args, this).forceLoad();
            mFirstRun = false;
        }
        else {
            loaderManager.restartLoader(loaderId, args, this).forceLoad();
        }

    }

    public void refreshMessageList() {
        // TODO: Need to account for cases where new post pushes topic onto next page
        mOldAdapterCount = mMessageListAdapter.getCount();
        loadMessageList(mArgs, REFRESH);
    }

    /**
     *      Contextual action mode methods
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id) {

        if (mActionMode != null && mSelection == -1) {
            // We can't do anything
            return false;
        }

        // Cast getActivity() value to AppCompatActivity so we can use startSupportActionMode
        mActionMode = ((AppCompatActivity) getActivity())
                .startSupportActionMode(mActionModeCallback);

        if (mSelection > -1) {
            // Set old position to false
            mMessageList.setItemChecked(mSelection, false);
        }

        mMessageList.setItemChecked(position, true);
        mSelection = position;

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

            Message target = mMessageListAdapter.getItem(mSelection);

            switch (item.getItemId()) {
                case R.id.quote:
                    String quote = new QuoteHandler().parse(target.getHtml());
                    // Start PostMessageActivity with quoted message
                    Context context = getContext();
                    Intent intent = new Intent(context, PostMessageActivity.class);
                    intent.putExtra("quote", quote);
                    intent.putExtra("title", mTitle);
                    intent.putExtra("id", mTopic.getId());
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
            mMessageList.setItemChecked(mSelection, false);
            mActionMode = null;
        }
    };


    /**
     *      Starts PostMessageActivity when user clicks new post button
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_message:
                Bundle args = new Bundle();
                args.putString("method", "GET");
                args.putString("type", "newtopic");
                getLoaderManager().initLoader(POST_MESSAGE, args, this).forceLoad();
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
                loadMessageList(buildArgsForLoader(nextPageUrl, false), LOAD_MESSAGE);
                mPageNumber++;
                Toaster.makeToast(getContext(), "Page " + mPageNumber);
            }
        }

        @Override
        public void onSwipeRight() {
            if (prevPageUrl != null) {
                loadMessageList(buildArgsForLoader(prevPageUrl, false), LOAD_MESSAGE);
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
     *      Loader callbacks.
     */
    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {

        final Context context = getContext();
        mCurrentId = id;

        switch (id) {

            case LOAD_MESSAGE:
            case REFRESH:

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

            case POST_MESSAGE:

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

        switch (mCurrentId) {

            // Let LOAD_MESSAGE fall through to REFRESH
            case LOAD_MESSAGE:
            case REFRESH:

                if (data != null) {
                    // We can be sure that data will safely cast to List<Message>.
                    @SuppressWarnings("unchecked")
                    List<Message> messages = (List<Message>) data;
                    mMessageListAdapter.updateMessages(messages);
                }

                if (mCurrentId == REFRESH) {
                    int adapterCount = mMessageListAdapter.getCount();
                    if (adapterCount > mOldAdapterCount) {
                        // Scroll to first unread post
                        scrollToPosition(adapterCount + 1);
                    } else {
                        // No new posts - just scroll to end of message list
                        scrollToPosition(adapterCount);
                    }
                }

                break;

            case POST_MESSAGE:

                Context context = getContext();

                String response = (String) data;
                new PostmsgScraper(context).parseResponse(response);

                Intent intent = new Intent(context, PostMessageActivity.class);
                intent.putExtra("topic", mTopic);
                intent.putExtra("title", mTitle);
                intent.putExtra("id", mTopic.getId());
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                context.startActivity(intent);

                break;

            default:
                break;
        }

        mDialog.dismiss();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }
}
