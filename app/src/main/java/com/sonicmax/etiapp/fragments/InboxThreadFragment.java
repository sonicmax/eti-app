package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import android.widget.TextView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.activities.InboxActivity;
import com.sonicmax.etiapp.activities.PostMessageActivity;
import com.sonicmax.etiapp.adapters.MessageListAdapter;
import com.sonicmax.etiapp.listeners.OnSwipeListener;
import com.sonicmax.etiapp.loaders.LivelinksSubscriber;
import com.sonicmax.etiapp.loaders.MessageListLoader;
import com.sonicmax.etiapp.loaders.QuickpostHandler;
import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.objects.MessageList;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.ui.QuickpostWindow;
import com.sonicmax.etiapp.utilities.MarkupBuilder;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;
import com.sonicmax.etiapp.utilities.Snacker;

import java.util.List;

/**
 * Displays content from PM inbox threads.
 */

public class InboxThreadFragment extends Fragment implements
        MessageListLoader.EventInterface, MessageListAdapter.EventInterface,
        LivelinksSubscriber.EventInterface, View.OnClickListener {

    private final String LOG_TAG = MessageListFragment.class.getSimpleName();
    private final int LOAD_MESSAGE = 0;
    private final int REFRESH = 1;

    private View mRootView;
    private FloatingActionButton mQuickpostButton;
    public MessageListAdapter mMessageListAdapter;
    private MessageListLoader mMessageListLoader;
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

    private MessageList mMessageList;
    private String mTitle;
    private int mCurrentPage;
    private String mPrevPageUrl;
    private String mNextPageUrl;

    public InboxThreadFragment() {}

    @Override
    public void onAttach(Context context) {
        mMessageListAdapter = new MessageListAdapter(context, this);

        // Set some values required for MessageListAdapter to work correctly with inbox threads.
        // TODO: Allow users to choose between regular message list UI and chat UI
        mMessageListAdapter.setInboxThreadFlag(true);
        String self = SharedPreferenceManager.getString(context, "username");
        mMessageListAdapter.setSelf(self);

        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            Intent intent = getActivity().getIntent();
            mTopic = intent.getParcelableExtra("topic");
            String url = (intent.getBooleanExtra("last_page", false))
                    ? mTopic.getLastPageUrl() : mTopic.getUrl();

            mMessageListLoader = new MessageListLoader(getContext(), this, url);

            loadMessageList(buildArgsForLoader(url, false), LOAD_MESSAGE);
        }

        else {
            mMessageList = savedInstanceState.getParcelable("messagelist");
            mTopic = savedInstanceState.getParcelable("topic");

            mMessages = mMessageList.getMessages();
            mPrevPageUrl = mMessageList.getPrevPageUrl();
            mNextPageUrl = mMessageList.getNextPageUrl();
            mCurrentPage = mMessageList.getPageNumber();

            mMessageListAdapter.replaceAllMessages(mMessages);
            mMessageListAdapter.setCurrentPage(mCurrentPage);

            if (mNextPageUrl != null) {
                mMessageListAdapter.setNextPageFlag(true);
            }
            else {
                mMessageListAdapter.setNextPageFlag(false);
            }
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_inbox_thread, container, false);
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
                // Find width of messageList and set maximum image width.
                // For inbox threads, we want to reduce image size to make sure they
                // fit inside the chat UI.
                // TODO: Expand images on click
                mMessageListAdapter.setMaxImageWidth(messageList.getWidth() / 2);
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
        outState.putParcelable("messagelist", mMessageList);
        outState.putParcelable("topic", mTopic);

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

    private void scrollToPosition(final int position) {
        mLayoutManager.scrollToPosition(position);
    }

    private int getTotalPosts() {
        if (mCurrentPage > 1) {
            // Account for posts on previous pages & add current adapter count
            return ((mTopic.getLastPage(0) - 1) * 50) + mMessageListAdapter.getMessageCount();
        }
        else {

            return mMessageListAdapter.getMessageCount();
        }
    }

    /**
     * For debugging
     */
    public void clearMemCache() {
        mMessageListAdapter.clearMemoryCache();
    }

    public Bundle buildArgsForLoader(String url, boolean filter) {
        mArgs = new Bundle();
        mArgs.putString("method", "GET");
        mArgs.putString("type", "messagelist");
        mArgs.putString("url", url);
        mArgs.putBoolean("filter", filter);

        return mArgs;
    }

    private void loadMessageList(Bundle args, int id) {
        displayDialog("Loading...");
        mCurrentPage = getActivity().getIntent().getIntExtra("page", 1);
        mMessageListLoader.load(args, id);
    }

    public void displayDialog(String message) {
        mDialog = new ProgressDialog(getContext());
        mDialog.setMessage(message);
        mDialog.show();
    }

    public void refreshMessageList() {
        loadMessageList(mArgs, REFRESH);
    }

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // MessageListLoader.EventInterface methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onLoadMessageList(MessageList messageList) {
        mMessageList = messageList;
        mMessages = messageList.getMessages();
        mCurrentPage = messageList.getPageNumber();
        mPrevPageUrl = messageList.getPrevPageUrl();
        mNextPageUrl = messageList.getNextPageUrl();

        if (mNextPageUrl != null) {
            mMessageListAdapter.setNextPageFlag(true);
        }

        mMessageListAdapter.replaceAllMessages(mMessages);
        mMessageListAdapter.setCurrentPage(mCurrentPage);

        boolean isLastPage = mCurrentPage == mTopic.getLastPage(0);

        if (isLastPage && mLivelinksSubscriber == null) {
            final int totalPosts = getTotalPosts();
            String userId = SharedPreferenceManager.getString(getContext(), "user_id");
            int inboxCount = SharedPreferenceManager.getInt(getContext(), "inbox_count");

            mLivelinksSubscriber = new LivelinksSubscriber(getContext(), this, mTopic.getId(), userId, totalPosts, inboxCount);
            mLivelinksSubscriber.setPmThreadFlag(true);
            mLivelinksSubscriber.subscribe();

            // Update unread post count for this thread
            mLivelinksSubscriber.updateBookmarkCount();
        }

        else if (!isLastPage && mLivelinksSubscriber != null) {
            // User navigated from last page to a different page.
            // We should unsubscribe from livelinks updates
            mLivelinksSubscriber.unsubscribe();
        }

        dismissDialog();
    }

    ///////////////////////////////////////////////////////////////////////////
    // MessageListAdapter.EventInterface methods
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

    @Override
    public void onRequestNextPage() {
        final int FIRST_POST = 0;

        if (mNextPageUrl != null) {
            mMessageListAdapter.clearMessages();
            loadMessageList(buildArgsForLoader(mNextPageUrl, false), LOAD_MESSAGE);
            Snacker.showSnackBar(mRootView, "Page " + mCurrentPage);
            scrollToPosition(FIRST_POST);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // LivelinksSubscriber.EventInterface methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onReceiveNewPost(MessageList messageList, int position) {
        final int totalPosts = getTotalPosts();

        // Update mNextPageUrl, in case new page was created
        mNextPageUrl = messageList.getNextPageUrl();

        if (mNextPageUrl != null) {
            mMessageList.setNextPageUrl(mNextPageUrl);
            mMessageListAdapter.setNextPageFlag(true);
        }

        List<Message> newMessages = messageList.getMessages();

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
    public void onReceivePrivateMessage(int oldCount, int newCount) {
        // Update PM count in shared preferences and notify user.
        SharedPreferenceManager.putInt(getContext(), "inbox_count", newCount);
        int difference = newCount - oldCount;
        String message = getPmSnackBarMessage(difference);
        showPmSnackBar(message);
    }

    @Override
    public void onUpdateBookmarkCount() {

    }

    private void showPmSnackBar(String message) {
        final Snackbar pmSnackbar = Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG);

        pmSnackbar.setAction(R.string.read, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pmSnackbar.dismiss();
                Bookmark inbox = new Bookmark("Inbox", "https://endoftheinter.net/inbox.php");
                Context context = getContext();
                Intent intent = new Intent(context, InboxActivity.class);
                intent.putExtra("url", inbox.getUrl());
                intent.putExtra("boardname", inbox.getName());
                context.startActivity(intent);
                getActivity().overridePendingTransition(R.anim.slide_in_from_right,
                        R.anim.slide_out_to_left);
            }
        });

        pmSnackbar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.snackbar_action));

        pmSnackbar.show();
    }

    private String getPmSnackBarMessage(int difference) {
        if (difference > 1) {
            return difference + " " + getResources().getString(R.string.new_messages);
        }
        else {
            return difference + " " + getResources().getString(R.string.new_message);
        }
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
    // Quickpost window
    ///////////////////////////////////////////////////////////////////////////

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
        popup.showAtLocation(getActivity().findViewById(R.id.inbox_thread_container),
                Gravity.TOP, 0, 25);

        // Set up quickpostHandler to handle UI changes and manage loading
        mQuickpostHandler = new QuickpostHandler(getContext(), mQuickpostButton, mTopic) {

            @Override
            public void onPreload() {
                displayDialog("Posting message...");
            }

            @Override
            public void onSuccess(String message) {
                dismissDialog();

                if (message != null) {
                    Snacker.showSnackBar(mRootView, message);
                }
            }

            @Override
            public void onError(String errorMessage) {
                dismissDialog();
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
    // User input listeners
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Captures floating action button clicks
     */

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_message:
                showQuickpostWindow();
                break;
        }

    }

    OnSwipeListener pageSwipeHandler = new OnSwipeListener(getContext()) {

        @Override
        public void onSwipeLeft() {
            if (mNextPageUrl != null) {
                loadMessageList(buildArgsForLoader(mNextPageUrl, false), LOAD_MESSAGE);
                Snacker.showSnackBar(mRootView, "Page " + mCurrentPage);
            }
        }

        @Override
        public void onSwipeRight() {
            if (mPrevPageUrl != null) {
                loadMessageList(buildArgsForLoader(mPrevPageUrl, false), LOAD_MESSAGE);
                Snacker.showSnackBar(mRootView, "Page " + mCurrentPage);
            }
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // Animations
    ///////////////////////////////////////////////////////////////////////////

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
}