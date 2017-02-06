package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.activities.PostMessageActivity;
import com.sonicmax.etiapp.adapters.MessageListAdapter;
import com.sonicmax.etiapp.listeners.OnSwipeListener;
import com.sonicmax.etiapp.network.LivelinksSubscriber;
import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.scrapers.MessageListScraper;
import com.sonicmax.etiapp.network.QuickpostHandler;
import com.sonicmax.etiapp.ui.QuickpostWindow;
import com.sonicmax.etiapp.utilities.AsyncLoader;
import com.sonicmax.etiapp.utilities.MarkupBuilder;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;
import com.sonicmax.etiapp.utilities.Toaster;

import java.util.ArrayList;
import java.util.List;

public class MessageListFragment extends Fragment implements
        MessageListAdapter.ClickListener, View.OnClickListener,
        LoaderManager.LoaderCallbacks<Object> {

    private final String LOG_TAG = MessageListFragment.class.getSimpleName();
    private final int LOAD_MESSAGE = 0;
    private final int REFRESH = 1;

    private View mRootView;
    private FloatingActionButton mQuickpostButton;
    private MessageListScraper mScraper;
    private MessageListAdapter mMessageListAdapter;
    private ActionMode mActionMode;
    private ProgressDialog mDialog;
    private Bundle mArgs;
    private Topic mTopic;
    private Message mSelectedMessage;
    private ViewGroup mContainer;
    private LinearLayoutManager mLayoutManager;
    private List<Message> mMessages;
    private QuickpostHandler mQuickpostHandler;
    private LivelinksSubscriber mLivelinksSubscriber;

    private String mTitle;

    public static String prevPageUrl;
    public static String nextPageUrl;
    public static int currentPage;

    public MessageListFragment() {}

    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onAttach(Context context) {
        // this = MessageListAdapter.ClickListener
        mMessageListAdapter = new MessageListAdapter(context, this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {

            Intent intent = getActivity().getIntent();
            mTopic = intent.getParcelableExtra("topic");
            String url = (intent.getBooleanExtra("last_page", false))
                    ? mTopic.getLastPageUrl() : mTopic.getUrl();

            mScraper = new MessageListScraper(getContext(), url);
            loadMessageList(buildArgsForLoader(url, false), LOAD_MESSAGE);
        }

        else {
            mTopic = savedInstanceState.getParcelable("topic");
            currentPage = savedInstanceState.getInt("page");
            mMessages = savedInstanceState.getParcelableArrayList("messages");

            mMessageListAdapter.replaceAllMessages(mMessages);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_message_list, container, false);
        mQuickpostButton = (FloatingActionButton) mRootView.findViewById(R.id.new_message);

        // Keep reference to container incase we need to inflate quickpost view
        mContainer = container;

        // Prepare RecyclerView so we can display posts after loading has finished
        final RecyclerView messageList = (RecyclerView) mRootView.findViewById(R.id.listview_messages);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        messageList.setLayoutManager(mLayoutManager);
        messageList.setAdapter(mMessageListAdapter);

        // Display current topic title
        TextView topicTitle = (TextView) mRootView.findViewById(R.id.topic_title_text);
        Intent intent = getActivity().getIntent();
        mTitle = intent.getStringExtra("title");
        topicTitle.setText(mTitle);

        // Set listeners
        mQuickpostButton.setOnClickListener(this);
        mRootView.setOnTouchListener(pageSwipeHandler);

        messageList.post(new Runnable() {
            @Override
            public void run() {
                // Find width of message list and set maximum image width
                mMessageListAdapter.setMaxImageWidth(messageList.getWidth());
            }
        });

        return mRootView;
    }

    @Override
    public void onResume() {
        if (mLivelinksSubscriber != null) {
            mLivelinksSubscriber = null;
        }

        super.onResume();
    }

    @Override
    public void onStop() {
        // Clear adapter and memory cache before stopping activity
        mMessageListAdapter.clearMessages();
        mMessageListAdapter.clearMemoryCache();
        // Close disk cache
        mMessageListAdapter.closeDiskCache();

        // Make sure that livelinks loader is destroyed
        if (mLivelinksSubscriber != null) {
            mLivelinksSubscriber.unsubscribe();
        }

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
        outState.putInt("page", currentPage);

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

    private void loadMessageList(Bundle args, int id) {
        LoaderManager loaderManager = getLoaderManager();

        if (loaderManager.getLoader(id) == null) {
            currentPage = getActivity().getIntent().getIntExtra("page", 1);
            loaderManager.initLoader(id, args, this).forceLoad();
        }
        else {
            loaderManager.restartLoader(id, args, this).forceLoad();
        }
    }

    public void refreshMessageList() {
        loadMessageList(mArgs, REFRESH);
    }

    ///////////////////////////////////////////////////////////////////////////
    // MessageListAdapter.ClickListener methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onItemClick(int position) {
        if (mActionMode != null) {
            // This would let us select multiple items easily
            // mMessageListAdapter.toggleSelection(position);
            mMessageListAdapter.clearSelection();
        }
    }

    @Override
    public boolean onItemLongClick(int position) {
        mActionMode = ((AppCompatActivity) getActivity())
                .startSupportActionMode(mActionModeCallback);

        if (!mMessageListAdapter.isSelected(position)) {
            mMessageListAdapter.clearSelection();
            mMessageListAdapter.setSelection(position);
            mSelectedMessage = mMessageListAdapter.getItem(position);
        }

        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ActionMode methods
    ///////////////////////////////////////////////////////////////////////////

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate context menu layout
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
            mMessageListAdapter.clearSelection();
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
            public void onPreload() {
                mDialog = new ProgressDialog(getContext());
                mDialog.setMessage("Posting message...");
                mDialog.show();
            }

            @Override
            public void onSuccess() {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.hide();
                }

                Snackbar.make(mRootView, R.string.post_message_ok, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.hide();
                }

                Snackbar.make(mRootView, errorMessage, Snackbar.LENGTH_SHORT).show();
            }
        };

        mQuickpostHandler.hideButton();

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
                mMessageListAdapter.clearSelection();
                mSelectedMessage = null;
                popup.dismiss();
            }

        });

        // Makes sure that quickpost button is made visible after popup window is dismissed
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
                currentPage++;
                Toaster.makeToast(getContext(), "Page " + currentPage);
            }
        }

        @Override
        public void onSwipeRight() {
            if (prevPageUrl != null) {

                loadMessageList(buildArgsForLoader(prevPageUrl, false), LOAD_MESSAGE);
                currentPage--;
                Toaster.makeToast(getContext(), "Page " + currentPage);
            }
        }
    };

    private void scrollToPosition(final int position) {
        mLayoutManager.scrollToPosition(position);
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

        return new AsyncLoader(context, args) {
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
            mMessageListAdapter.replaceAllMessages(mMessages);

            boolean isLastPage = currentPage == mTopic.getLastPage(0);

            if (isLastPage && mLivelinksSubscriber == null) {
                initLivelinksSubscriber();
            }
            else if (!isLastPage && mLivelinksSubscriber != null) {
                // User navigated from last page to a different page - can't use livelinks anymore
                // as we can't get an accurate post count
                mLivelinksSubscriber.unsubscribe();
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
        final String DEBUG_USER_ID = "5599";
        final int DEBUG_INBOX_COUNT = 0;

        // Calculate total number of posts in topic to avoid bug where posts would be duplicated
        // due to inaccurate total from topic list
        final int totalPosts = getTotalPosts();

        mLivelinksSubscriber = new LivelinksSubscriber(getContext(), mTopic.getId(),
                DEBUG_USER_ID, totalPosts, DEBUG_INBOX_COUNT) {

            @Override
            public void onReceiveNewPost(String response, int position) {
                // Can't parse HTML unless we remove these characters
                String escapedResponse = response.replace("\\/", "/")
                        .replace("\\\"", "\"")
                        .replace("\\n", "");

                List<Message> newMessages = mScraper.scrapeMessages(escapedResponse, false);

                int sizeOfNewMessages = newMessages.size();
                // We have to set position manually because count from moremessages.php will be incorrect
                for (int i = 0; i < sizeOfNewMessages; i++) {
                    Message message = newMessages.get(i);
                    message.setPosition(totalPosts + (i + 1));
                }

                if (position > totalPosts) {
                    mTopic.addToSize(sizeOfNewMessages);
                    mMessageListAdapter.addMessages(newMessages);
                    animateTimestampChange();
                }
                else {
                    // Position of new message should never be less than size of topic.
                    Log.e(LOG_TAG, "Cannot add new post to topic. \n" +
                            "Position = " + position + ", topic size = " + totalPosts);
                }
            }

            @Override
            public void onReceivePrivateMessage(int unreadMessages) {
                Snackbar.make(mRootView, unreadMessages + R.string.unread_messages, Snackbar.LENGTH_INDEFINITE)
                        .show();
            }
        };
    }

    private int getTotalPosts() {
        if (currentPage > 1) {
            // Account for posts on previous pages & add current adapter count
            return ((mTopic.getLastPage(0) - 1) * 50) + mMessageListAdapter.getItemCount();
        }
        else {
            return mMessageListAdapter.getItemCount();
        }
    }

    public void animateTimestampChange() {
        final Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(250);

        final Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(250);

        int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
        int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();
        int visibleSize = lastVisiblePosition - firstVisiblePosition;

        for (int i = 0; i < visibleSize; i++) {
            View view = mLayoutManager.getChildAt(i);
            TextView timestamp = (TextView) view.findViewById(R.id.list_item_time);
            timestamp.startAnimation(fadeOut);
        }

        mMessageListAdapter.setCurrentTime();
        mMessageListAdapter.notifyDataSetChanged();

        for (int i = 0; i < visibleSize; i++) {
            View view = mLayoutManager.getChildAt(i);
            TextView timestamp = (TextView) view.findViewById(R.id.list_item_time);
            timestamp.startAnimation(fadeIn);
        }
    }

    /**
     * For debugging
     */
    public void clearMemCache() {
        mMessageListAdapter.clearMemoryCache();
    }
}
