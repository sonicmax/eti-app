package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sonicmax.etiapp.adapters.MessageListAdapter;
import com.sonicmax.etiapp.network.WebRequest;
import com.sonicmax.etiapp.scrapers.MessageListScraper;
import com.sonicmax.etiapp.scrapers.PostmsgScraper;

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

    private View mRootView;
    private ListView mMessageList;
    private Button mQuickpostButton;
    private MessageListScraper mScraper;
    private MessageListAdapter mMessageListAdapter;
    private ActionMode mActionMode;
    private ProgressDialog mDialog;
    private Bundle mArgs;
    private Topic mTopic;
    private Message mSelectedMessage;
    private ViewGroup mContainer;

    private int mSelection = -1;
    private int mCurrentId;
    private int mOldAdapterCount;
    public static int mPageNumber;

    private String mTitle;
    public static String prevPageUrl;
    public static String nextPageUrl;

    private static boolean mFirstRun = true;

    public MessageListFragment() {}

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
        mContainer = container;

        mMessageList = (ListView) mRootView.findViewById(R.id.listview_messages);
        mQuickpostButton = (Button) mRootView.findViewById(R.id.new_message);
        TextView topicTitle = (TextView) mRootView.findViewById(R.id.topic_title);
        Intent intent = getActivity().getIntent();

        // Display topic title
        mTitle = intent.getStringExtra("title");
        topicTitle.setText(mTitle);

        // Set click listeners for views
        mQuickpostButton.setOnClickListener(this);
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
                    String quote = new QuoteHandler().parse(mSelectedMessage.getHtml());
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


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_message:
                showQuickpostWindow();
                break;
        }

    }

    private void showQuickpostWindow() {

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int deviceWidth = metrics.widthPixels;

        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View popupView = inflater.inflate(R.layout.quickpost_window, mContainer, false);
        // Measure view so we can get measured height for PopupWindow constructor
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        Button button = (Button) popupView.findViewById(R.id.quickpost_button);

        if (mSelectedMessage != null) {
            button.setText(R.string.reply);
        }

        final PopupWindow popup = new PopupWindow(
                popupView,
                deviceWidth,
                popupView.getMeasuredHeight(),
                true);

        // Following line allows popup to be dismissed after touching outside of it
        popup.setBackgroundDrawable(new ColorDrawable());

        popup.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);

        // Gravity.TOP means that 0,0 is at the top of the screen
        popup.showAtLocation(getActivity().findViewById(R.id.message_list_container),
                Gravity.TOP, 0, 50);

        // Hide quickpost button while popup is visible
        mQuickpostButton.setVisibility(View.INVISIBLE);

        // Add listeners to handle clicks and dismiss events
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final String NEWLINE = "\n";

                String quote = "";

                if (mSelectedMessage != null) {
                    // Quote selected message and append message to it
                    quote = new QuoteHandler().parse(mSelectedMessage.getHtml()) + NEWLINE;
                }

                String signature = SharedPreferenceManager.getString(getContext(), "signature");
                EditText messageView = (EditText) popupView.findViewById(R.id.quickpost_edit);
                String message = quote + messageView.getText().toString() + NEWLINE + signature;

                Log.v(LOG_TAG, message);

                popup.dismiss();
            }

        });

        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mQuickpostButton.setVisibility(View.VISIBLE);
            }
        });
    }

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

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {

        final Context context = getContext();
        mCurrentId = id;

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

            if (mCurrentId == REFRESH) {

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
}
