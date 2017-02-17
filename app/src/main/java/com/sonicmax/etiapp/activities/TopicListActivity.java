package com.sonicmax.etiapp.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.fragments.TopicListFragment;
import com.sonicmax.etiapp.loaders.AccountManager;
import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class TopicListActivity extends BaseActivity {
    public ListView mDrawerList;
    public DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private Button mInboxButton;
    private List<Bookmark> mBookmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        mDrawerList = (ListView)findViewById(R.id.drawer_list);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mInboxButton = (Button)findViewById(R.id.inbox_button);

        populateInboxButton();
        populateDrawerAdapter();
        initDrawer();

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setElevation(4);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    private void populateInboxButton() {
        mInboxButton.setText(getInboxString());
        mInboxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bookmark inbox = new Bookmark("Inbox", "https://endoftheinter.net/inbox.php");
                Intent intent = new Intent(TopicListActivity.this, InboxActivity.class);
                intent.putExtra("url", inbox.getUrl());
                intent.putExtra("title", inbox.getName());
                TopicListActivity.this.startActivity(intent);
                TopicListActivity.this.overridePendingTransition(R.anim.slide_in_from_right,
                        R.anim.slide_out_to_left);
            }
        });
    }

    private String getInboxString() {
        int count = SharedPreferenceManager.getInt(this, "inbox_count");
        return "Inbox " + "(" + count + ")";
    }

    public void populateDrawerAdapter() {
        List<String> nameArray = SharedPreferenceManager.getStringList(this, "bookmark_names");
        List<String> urlArray = SharedPreferenceManager.getStringList(this, "bookmark_urls");
        mBookmarks = new ArrayList<>();

        for (int i = 0; i < nameArray.size(); i++) {
            mBookmarks.add(new Bookmark(nameArray.get(i), urlArray.get(i)));
        }

        mDrawerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nameArray);
        mDrawerList.setAdapter(mDrawerAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TopicListFragment fragment = (TopicListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.topic_list_container);
                Bookmark bookmark = mBookmarks.get(position);
                fragment.loadTopicList(bookmark.getName(), bookmark.getUrl());
                mDrawerLayout.closeDrawers();
            }
        });
    }

    public void initDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
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

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        else {
            switch (id) {
                case R.id.action_settings:
                    Intent intent = new Intent(this, SettingsActivity.class);
                    this.startActivity(intent);
                    break;

                case R.id.action_logout:
                    new AccountManager(this, this).requestLogout();
                    break;

                case R.id.action_refresh:
                    TopicListFragment fragment = (TopicListFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.topic_list_container);
                    fragment.refreshTopicList();
                    break;
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
