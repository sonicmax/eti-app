package com.sonicmax.etiapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.fragments.InboxFragment;
import com.sonicmax.etiapp.fragments.InboxThreadFragment;
import com.sonicmax.etiapp.fragments.MessageListFragment;
import com.sonicmax.etiapp.fragments.TopicListFragment;
import com.sonicmax.etiapp.loaders.AccountManager;

/**
 * Contains methods common to all Activities.
 */

public class BaseActivity extends AppCompatActivity implements AccountManager.EventInterface {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
    }

    /**
     * Make some stylistic changes to action bar, display title using custom view,
     * and make the view scrollable after detecting touch event.
     */

    private void initActionBar() {
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setElevation(4);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);

            final String title = getIntent().getStringExtra("title");

            if (title != null) {
                View titleView = LayoutInflater.from(this)
                        .inflate(R.layout.title_view, null);

                TextView textView = (TextView) titleView.findViewById(R.id.title);
                textView.setText(title);

                titleView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // Replace with scrollable title view
                        View scrollableView = LayoutInflater.from(BaseActivity.this)
                                .inflate(R.layout.scrollable_title_view, null);
                        TextView textView = (TextView) scrollableView.findViewById(R.id.title);
                        textView.setText(title);

                        actionBar.setCustomView(scrollableView);

                        view.setOnTouchListener(null);
                    }
                });

                actionBar.setCustomView(titleView);
            }
        }
    }

    @Override
    public void onRequiresLogin() {
        // TODO: implement this.
    }

    @Override
    public void onBackPressed() {
        // Going "backwards" in app slides screen from left to right.
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }

    @Override
    public void onLoadComplete(Intent intent) {
        // Going "forwards" in app slides screen from right to left.
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        TopicListFragment topicListFragment = (TopicListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.topic_list_container);

        InboxFragment inboxFragment = (InboxFragment) getSupportFragmentManager()
                .findFragmentById(R.id.inbox_container);

        InboxThreadFragment inboxThreadFragment = (InboxThreadFragment) getSupportFragmentManager()
                .findFragmentById(R.id.inbox_thread_container);

        MessageListFragment messageListFragment = (MessageListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.message_list_container);


        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                break;

            case R.id.action_logout:
                new AccountManager(this, this).requestLogout();
                break;

            case R.id.action_refresh_topic_list:
                topicListFragment.refreshTopicList();
                break;

            case R.id.action_refresh_message_list:
                messageListFragment.refreshMessageList();
                break;

            case R.id.action_refresh_inbox:
                inboxFragment.refreshTopicList();
                break;

            case R.id.action_refresh_inbox_thread:
                inboxThreadFragment.refreshMessageList();
                break;

            case R.id.action_clear_mem_cache:
                if (messageListFragment != null) {
                    messageListFragment.clearMemCache();
                }
                else if (inboxThreadFragment != null) {
                    inboxThreadFragment.clearMemCache();
                }
                break;

            case R.id.action_toggle_star:
                // Return false so MessageListFragment can handle it
                return false;
        }

        return super.onOptionsItemSelected(item);
    }
}
