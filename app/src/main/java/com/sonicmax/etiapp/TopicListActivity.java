package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.sonicmax.etiapp.network.AccountManager;

public class TopicListActivity extends AppCompatActivity {

    private final String LOG_TAG = TopicListActivity.class.getSimpleName();
    @SuppressWarnings("unused")
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_topic_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {

            case R.id.action_settings:
                return true;

            case R.id.action_logout:
                new AccountManager(this, mDialog).requestLogout();
                break;

            case R.id.action_refresh:
                TopicListFragment fragment = (TopicListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.topic_list_container);
                fragment.refreshTopicList();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
