package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
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
import com.sonicmax.etiapp.activities.TopicListActivity;
import com.sonicmax.etiapp.adapters.MessageListAdapter;
import com.sonicmax.etiapp.listeners.OnSwipeListener;
import com.sonicmax.etiapp.loaders.LivelinksSubscriber;
import com.sonicmax.etiapp.loaders.MessageListLoader;
import com.sonicmax.etiapp.loaders.QuickpostHandler;
import com.sonicmax.etiapp.network.ResponseCache;
import com.sonicmax.etiapp.network.ResponseCacheEntry;
import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.objects.MessageList;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.ui.QuickpostWindow;
import com.sonicmax.etiapp.utilities.DialogHandler;
import com.sonicmax.etiapp.utilities.MarkupBuilder;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;
import com.sonicmax.etiapp.utilities.Snacker;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays content from PM inbox threads.
 */

public class InboxThreadFragment extends Fragment implements
        MessageListLoader.EventInterface, MessageListAdapter.EventInterface,
        LivelinksSubscriber.EventInterface, View.OnClickListener {

    private final String LOG_TAG = MessageListFragment.class.getSimpleName();
    private final int LOAD_THREAD = 0;
    private final int REFRESH = 1;
    private final int LOAD_FROM_CACHE = 2;

    private View mRootView;
    private FloatingActionButton mQuickpostButton;
    private MessageListAdapter mMessageListAdapter;
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
    private Bundle mLastRequest;
    private MessageList mMessageList;
    private int mCurrentPage;
    private ResponseCache mResponseCache;

    private String mUrl;
    private String mPrevPageUrl;
    private String mNextPageUrl;
    private int mStartPoint;

    private boolean mIsLoading = false;

    public InboxThreadFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        init(context);
        updateActionBarTitle(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mUrl = getUrlFromIntent();
            mStartPoint = getStartPoint();
            mMessageListLoader = new MessageListLoader(getContext(), this);
            loadMessageList(buildArgsForLoader(mUrl, false), LOAD_THREAD);
        }

        else {
            restoreFragmentFromBundle(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_inbox_thread, container, false);
        mQuickpostButton = (FloatingActionButton) mRootView.findViewById(R.id.new_message);

        // Keep reference to container incase we need to inflate quickpost view
        mContainer = container;

        // Prepare RecyclerView so we can display posts after loading has finished
        final RecyclerView mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.listview_messages);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mMessageListAdapter);

        // Set listeners
        mQuickpostButton.setOnClickListener(this);
        mRecyclerView.setOnTouchListener(pageSwipeHandler);

        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                // Find width of messageList and set maximum image width.
                // For inbox threads, we want to reduce image size to make sure they
                // fit inside the chat UI.
                mMessageListAdapter.setMaxImageWidth(mRecyclerView.getWidth() / 2);
            }
        });

        return mRootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        DialogHandler.dismissDialog();
        cachePageData();

        if (mLivelinksSubscriber != null) {
            mLivelinksSubscriber.unsubscribe();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mIsLoading && !restoreFromCache()) {
            // Snacker.showSnackBar(mRootView, "Cache load failed");
            refreshMessageList();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mMessageListAdapter.clearMessages();
        mMessageListAdapter.clearMemoryCache();

        if (mLivelinksSubscriber != null) {
            mLivelinksSubscriber.unsubscribe();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("messagelist", mMessageList);
        outState.putParcelableArrayList("messages", new ArrayList<>(mMessages));
        outState.putParcelable("topic", mTopic);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        DialogHandler.dismissDialog();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Initialises classes required for fragment to function correctly
     * (adapter, loader, layout manager, etc)
     * @param context
     */
    private void init(Context context) {
        mMessageListAdapter = new MessageListAdapter(context, this);
        mMessageListAdapter.setInboxThreadFlag(true);
        mMessageListAdapter.setSelf(SharedPreferenceManager.getString(context, "username"));

        mLayoutManager = new LinearLayoutManager(getContext());
        mResponseCache = new ResponseCache(context);
        mMessageListLoader = new MessageListLoader(context, this);
    }

    private void updateActionBarTitle(Context context) {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        String title = getActivity().getIntent().getStringExtra("title");

        if (actionBar != null && title != null) {
            LayoutInflater inflator = LayoutInflater.from(context);
            View v = inflator.inflate(R.layout.title_view, null);
            ((TextView) v.findViewById(R.id.title)).setText(title);
            actionBar.setCustomView(v);
        }
    }

    private String getUrlFromIntent() {
        Intent intent = getActivity().getIntent();
        mTopic = intent.getParcelableExtra("topic");
        String url = (intent.getBooleanExtra("last_page", false))
                ? mTopic.getLastPageUrl() : mTopic.getUrl();
        int page = intent.getIntExtra("page", 0);

        if (page > 0) {
            url += "&page=" + page;
            intent.putExtra("page", 0);
        }

        return url;
    }

    private int getStartPoint() {
        Intent intent = getActivity().getIntent();
        int startPoint = intent.getIntExtra("post", 0);
        intent.putExtra("post", 0);
        return startPoint;
    }

    private void restoreFragmentFromBundle(Bundle savedInstanceState) {
        mMessageList = savedInstanceState.getParcelable("messagelist");
        mTopic = savedInstanceState.getParcelable("topic");

        mMessages = savedInstanceState.getParcelableArrayList("messages");
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

    private void cachePageData() {
        int position = 0;

        if (mLayoutManager != null) {
            position = mLayoutManager.findFirstVisibleItemPosition();
        }

        mResponseCache.cacheResponseData(mUrl, mMessageList.getHtml(), mCurrentPage, position);
    }

    /**
     * Attempts to restore adapter content from cache.
     * @return boolean value indicating whether operation was successful
     */
    private boolean restoreFromCache() {
        if (mResponseCache == null) {
            mResponseCache = new ResponseCache(getContext());
        }

        ResponseCacheEntry cachedResponse = mResponseCache.getResponseFromCache(getUrlFromIntent());

        if (cachedResponse != null) {
            mUrl = cachedResponse.getUrl();
            mCurrentPage = cachedResponse.getPageNumber();
            mStartPoint = cachedResponse.getAdapterPosition();
            mMessageListLoader = new MessageListLoader(getContext(), this);
            loadMessageList(buildArgsForLoader(cachedResponse.getHtml(), mUrl, false), LOAD_FROM_CACHE);
            Snacker.showSnackBar(mRootView, "Cache load success");
            return true;
        }

        else {
            return false;
        }
    }

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

    public Bundle buildArgsForLoader(String html, String url, boolean filter) {
        Bundle args = new Bundle();
        args.putString("html", html);
        args.putString("url", url);
        args.putBoolean("filter", filter);

        return args;
    }

    private void loadMessageList(Bundle args, int id) {
        mIsLoading = true;
        DialogHandler.showDialog(getContext(), "Loading...");
        mCurrentPage = getActivity().getIntent().getIntExtra("page", 1);
        mMessageListLoader.load(args, id);
    }

    private void loadPrevPage() {
        if (mPrevPageUrl != null) {
            mMessageListAdapter.clearMessages();
            loadMessageList(buildArgsForLoader(mPrevPageUrl, false), LOAD_THREAD);
        }
    }

    private void loadNextPage() {
        if (mNextPageUrl != null) {
            mMessageListAdapter.clearMessages();
            loadMessageList(buildArgsForLoader(mNextPageUrl, false), LOAD_THREAD);
        }
    }

    public void refreshMessageList() {
        if (mMessageListLoader == null) {
            mMessageListLoader = new MessageListLoader(getContext(), this);
        }

        if (mArgs == null) {
            loadMessageList(buildArgsForLoader(getUrlFromIntent(), false), REFRESH);
        }
        else {
            loadMessageList(mArgs, REFRESH);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // MessageListLoader.EventInterface methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onLoadMessageList(Bundle args, MessageList messageList) {
        mLastRequest = args;
        mMessageList = messageList;
        mMessages = messageList.getMessages();
        mCurrentPage = messageList.getPageNumber();
        mPrevPageUrl = messageList.getPrevPageUrl();
        mNextPageUrl = messageList.getNextPageUrl();

        mMessageListAdapter.setNextPageFlag((mNextPageUrl != null));
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

        DialogHandler.dismissDialog();

        if (mCurrentPage > 1) {
            Snacker.showSnackBar(mRootView, "Page " + mCurrentPage);
        }

        scrollToPosition(mStartPoint);
        mStartPoint = 0;
        mIsLoading = false;
    }

    @Override
    public void onLoadError() {
        // Clean up UI, display notification and retry last working request
        DialogHandler.dismissDialog();
        Snacker.showSnackBar(mRootView, "Error while loading page");

        if (mArgs != null && mLastRequest != null) {
            loadMessageList(mLastRequest, LOAD_THREAD);
            // Make sure that we don't repeat request if something goes wrong.
            mArgs = null;
        }
        else {
            // Go back to inbox
            Bookmark inbox = new Bookmark("Inbox", "https://endoftheinter.net/inbox.php");
            Intent intent = new Intent(getActivity(), InboxActivity.class);
            intent.putExtra("url", inbox.getUrl());
            intent.putExtra("title", inbox.getName());
            getActivity().startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_out_to_left,
                    R.anim.slide_in_from_right);
        }
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
        loadNextPage();
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
            scrollToPosition(mMessageListAdapter.getItemCount() - 1);
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

        pmSnackbar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.accent));

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
                    intent.putExtra("title", intent.getStringExtra("title"));
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
                DialogHandler.showDialog(getContext(), "Posting message...");
            }

            @Override
            public void onSuccess(String message) {
                DialogHandler.dismissDialog();

                if (message != null) {
                    Snacker.showSnackBar(mRootView, message);
                }
            }

            @Override
            public void onError(String errorMessage) {
                DialogHandler.dismissDialog();
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
            loadNextPage();
        }

        @Override
        public void onSwipeRight() {
            loadPrevPage();
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
