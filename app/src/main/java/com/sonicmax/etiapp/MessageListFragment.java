package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sonicmax.etiapp.adapters.MessageListAdapter;
import com.sonicmax.etiapp.listeners.OnSwipeListener;
import com.sonicmax.etiapp.network.LivelinksSubscriber;
import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.scrapers.MessageListScraper;
import com.sonicmax.etiapp.network.QuickpostHandler;
import com.sonicmax.etiapp.ui.QuickpostWindow;
import com.sonicmax.etiapp.utilities.AsyncLoadHandler;
import com.sonicmax.etiapp.utilities.MarkupBuilder;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;
import com.sonicmax.etiapp.utilities.Toaster;

import java.util.ArrayList;
import java.util.List;

public class MessageListFragment extends Fragment implements
        AdapterView.OnItemLongClickListener, View.OnClickListener,
        LoaderManager.LoaderCallbacks<Object> {

    private final String LOG_TAG = MessageListFragment.class.getSimpleName();
    private final int LOAD_MESSAGE = 0;
    private final int REFRESH = 1;
    private final int POST_MESSAGE = 2;

    private View mRootView;
    private ListView mMessageList;
    private FloatingActionButton mQuickpostButton;
    private MessageListScraper mScraper;
    private MessageListAdapter mMessageListAdapter;
    private ActionMode mActionMode;
    private ProgressDialog mDialog;
    private Bundle mArgs;
    private Topic mTopic;
    private Message mSelectedMessage;
    private ViewGroup mContainer;
    private List<Message> mMessages;
    private QuickpostHandler mQuickpostHandler;
    private LivelinksSubscriber mLivelinksSubscriber;

    private int mSelection = -1;
    private int mOldAdapterCount;
    public static int mPageNumber;

    private String mTitle;
    public static String prevPageUrl;
    public static String nextPageUrl;

    private static boolean mFirstRun = true;

    public MessageListFragment() {}

    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onAttach(Context context) {
        mMessageListAdapter = new MessageListAdapter(context);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {

            Intent intent = getActivity().getIntent();
            mTopic = intent.getParcelableExtra("topic");

            if (mFirstRun) {
                mPageNumber = intent.getIntExtra("page", 1);
            }

            String url = (intent.getBooleanExtra("last_page", false))
                    ? mTopic.getLastPageUrl() : mTopic.getUrl();

            mScraper = new MessageListScraper(getContext(), url);

            // Init/Restart loader and get posts for adapter
            loadMessageList(buildArgsForLoader(url, false), LOAD_MESSAGE);
        }

        else {
            mTopic = savedInstanceState.getParcelable("topic");
            mPageNumber = savedInstanceState.getInt("page");

            mMessages = savedInstanceState.getParcelableArrayList("messages");
            mMessageListAdapter.updateMessages(mMessages);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_message_list, container, false);
        mContainer = container;

        mMessageList = (ListView) mRootView.findViewById(R.id.listview_messages);
        mQuickpostButton = (FloatingActionButton) mRootView.findViewById(R.id.new_message);
        TextView topicTitle = (TextView) mRootView.findViewById(R.id.topic_title);
        Intent intent = getActivity().getIntent();

        // Display topic title
        mTitle = intent.getStringExtra("title");
        topicTitle.setText(mTitle);

        // Set up quickpost handler and click listener
        mQuickpostButton.setOnClickListener(this);

        // Set up other listeners
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
    public void onStop() {
        // Clear adapter before stopping activity
        mMessageListAdapter.clearMessages();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save list of boards so we can quickly restore fragment
        if (mMessages != null) {
            ArrayList<Message> messageArray = new ArrayList<>(mMessages);
            outState.putParcelableArrayList("messages", messageArray);
        }
        outState.putParcelable("topic", mTopic);
        outState.putInt("page", mPageNumber);

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
    // Helper methods for loader
    ///////////////////////////////////////////////////////////////////////////
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

    ///////////////////////////////////////////////////////////////////////////
    // Contextual action mode methods
    ///////////////////////////////////////////////////////////////////////////
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
        mSelectedMessage = mMessageListAdapter.getItem(position);
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

            switch (item.getItemId()) {
                case R.id.quote:
                    String quote = new MarkupBuilder().parse(mSelectedMessage.getHtml());
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
            mSelectedMessage = null;
            mActionMode = null;
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // Methods for quickpost feature
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_message:
                showQuickpostWindow();
                break;
        }

    }

    private void showQuickpostWindow() {
        // Get device width (required for PopupWindow width)
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int deviceWidth = metrics.widthPixels;

        // Prepare quickpost_window layout to be used in PopupWindow constructor
        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View quickpostView = inflater.inflate(R.layout.quickpost_window, mContainer, false);
        Button button = (Button) quickpostView.findViewById(R.id.quickpost_button);

        quickpostView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        if (mSelectedMessage != null) {
            // Make it clear that we are replying to selected message
            button.setText(R.string.reply);
        }

        final QuickpostWindow popup = new QuickpostWindow(
                quickpostView,
                deviceWidth,
                quickpostView.getMeasuredHeight(),
                true);

        // Show quickpost window 25px from top of screen (aligned with bottom of status bar)
        popup.showAtLocation(getActivity().findViewById(R.id.message_list_container),
                Gravity.TOP, 0, 25);

        // Set up quickpostHandler to handle UI changes and manage loading
        mQuickpostHandler = new QuickpostHandler(getContext(), mQuickpostButton, mTopic) {

            @Override
            public void onPreLoad() {
                mDialog = new ProgressDialog(getContext());
                mDialog.setMessage("Loading messages...");
                mDialog.show();
            }

            @Override
            public void onError(String errorMessage) {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.hide();
                }

                Snackbar.make(mRootView, errorMessage, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess() {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.hide();
                }

                Snackbar.make(mRootView, R.string.post_message_ok, Snackbar.LENGTH_SHORT).show();
            }
        };

        mQuickpostHandler.setInvisible();

        // Add listener which allows user to post/reply to message
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final String NEWLINE = "\n";

                String quote = "";

                if (mSelectedMessage != null) {
                    // Quote selected message and append message to it
                    quote = new MarkupBuilder().parse(mSelectedMessage.getHtml()) + NEWLINE;
                }

                String signature = SharedPreferenceManager.getString(getContext(), "signature");
                EditText messageView = (EditText) quickpostView.findViewById(R.id.quickpost_edit);
                String message = quote + messageView.getText().toString() + NEWLINE + signature;
                mQuickpostHandler.postMessage(message);

                // Clean up UI
                mMessageList.setItemChecked(mSelection, false);
                mSelectedMessage = null;
                popup.dismiss();
            }

        });

        // Make sure that quickpost button is made visible after popup window is dismissed
        popup.setOnDismissListener(mQuickpostHandler.dismissListener);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods for page changing
    ///////////////////////////////////////////////////////////////////////////
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

    View.OnClickListener snackbarAction = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            loadMessageList(buildArgsForLoader(mTopic.getLastPageUrl(), false), LOAD_MESSAGE);
        }
    };

    private void scrollToBottom() {
        final ListView messageList = (ListView) mRootView.findViewById(R.id.listview_messages);

        messageList.post(new Runnable() {
            @Override
            public void run() {
                // Minus 1 from count as position is 0-indexed
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

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public Loader<Object> onCreateLoader(final int id, final Bundle args) {
        final Context context = getContext();

        mDialog = new ProgressDialog(context);
        mDialog.setMessage("Loading messages...");
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
            mMessages = (List<Message>) data;
            mMessageListAdapter.updateMessages(mMessages);

            if (mPageNumber == mTopic.getLastPage(0) && mLivelinksSubscriber == null) {
                initLivelinksSubscriber();
            }

            if (loader.getId() == REFRESH) {
                int adapterCount = mMessageListAdapter.getCount();
                if (adapterCount > mOldAdapterCount) {
                    // Scroll to first unread post
                    scrollToPosition(adapterCount);
                } else {
                    // No new posts - just scroll to end of message list
                    scrollToPosition(adapterCount - 1);
                }
            }
        }

        else {
            // Handle error
        }

        mDialog.dismiss();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }

    public void initLivelinksSubscriber() {
        // TODO: Replace these with actual values
        final String DEBUG_USER_ID = "5599";
        final int DEBUG_INBOX_COUNT = 0;

        int totalPosts;
        if (mPageNumber == 1) {
            totalPosts = mMessageListAdapter.getCount();
        }
        else {
            // Account for posts on previous pages & add current adapter count
            totalPosts = ((mTopic.getLastPage(0) - 1) * 50) + mMessageListAdapter.getCount();
        }

        mLivelinksSubscriber = new LivelinksSubscriber(getContext(), mTopic.getId(),
                DEBUG_USER_ID, totalPosts, DEBUG_INBOX_COUNT) {

            @Override
            public void onReceiveUpdate(String response, int position) {
                // Can't parse HTML unless we remove these characters
                String escapedResponse = response.replace("\\/", "/")
                        .replace("\\\"", "\"")
                        .replace("\\n", "");

                List<Message> newMessages = mScraper.scrapeMessages(escapedResponse, false);

                int sizeOfNewMessages = newMessages.size();
                int currentTopicSize = mTopic.size();

                // We have to set position manually because count from scraper will be incorrect
                for (int i = 0; i < sizeOfNewMessages; i++) {
                    Message message = newMessages.get(i);
                    message.setPosition(currentTopicSize + (i + 1));
                }

                Log.v(LOG_TAG, "position = " + position);
                Log.v(LOG_TAG, "size = " + currentTopicSize);

                if (position > currentTopicSize) {

                    mTopic.addToSize(sizeOfNewMessages);

                    if (mPageNumber == mTopic.getLastPage(0)) {
                        // We can append new post to current page
                        mMessageListAdapter.appendMessages(newMessages);
                        animateTimestampChange();
                    } else {
                        // Notify user of new post and allow them to navigate to last page
                        int lastPage = mTopic.getLastPage(0);
                        String message = "New post on page " + lastPage;

                        Snackbar.make(mRootView, message, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.snackbar_view, snackbarAction)
                                .show();
                    }
                } else {
                    // Position of new message should never be less than size of topic.
                    // Do nothing and hope for the best
                    Log.e(LOG_TAG, "Cannot add new post to topic. \n" +
                            "Position = " + position + ", topic size = " + currentTopicSize);
                }
            }
        };
    }

    public void animateTimestampChange() {
        final Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(500);

        final Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500);

        int firstVisiblePosition = mMessageList.getFirstVisiblePosition();
        int lastVisiblePosition = mMessageList.getLastVisiblePosition();
        int visibleSize = lastVisiblePosition - firstVisiblePosition;
        Log.v(LOG_TAG, "first position: " + firstVisiblePosition);
        Log.v(LOG_TAG, "last position: " + lastVisiblePosition);

        for (int i = 0; i < visibleSize; i++) {
            View view = mMessageList.getChildAt(i);
            TextView timestamp = (TextView) view.findViewById(R.id.list_item_time);
            timestamp.startAnimation(fadeOut);
        }

        mMessageListAdapter.setCurrentTime();
        mMessageListAdapter.notifyDataSetChanged();

        for (int i = 0; i < visibleSize; i++) {
            View view = mMessageList.getChildAt(i);
            TextView timestamp = (TextView) view.findViewById(R.id.list_item_time);
            timestamp.startAnimation(fadeIn);
        }
    }
}
