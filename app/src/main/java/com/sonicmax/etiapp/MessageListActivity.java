package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.sonicmax.etiapp.network.AccountManager;

public class MessageListActivity extends AppCompatActivity {

    private final String LOG_TAG = MessageListActivity.class.getSimpleName();
    @SuppressWarnings("unused")
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message_list, menu);
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
                MessageListFragment fragment = (MessageListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.message_list_container);
                fragment.refreshMessageList();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }
}
