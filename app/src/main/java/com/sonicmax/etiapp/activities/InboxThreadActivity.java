package com.sonicmax.etiapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.fragments.InboxThreadFragment;
import com.sonicmax.etiapp.loaders.AccountManager;

public class InboxThreadActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox_thread);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        InboxThreadFragment fragment = (InboxThreadFragment) getSupportFragmentManager()
                .findFragmentById(R.id.inbox_thread_container);

        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                break;

            case R.id.action_logout:
                new AccountManager(this, this).requestLogout();
                break;

            case R.id.action_refresh:
                fragment.refreshMessageList();
                break;

            case R.id.action_clear_mem_cache:
                fragment.clearMemCache();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
