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
import com.sonicmax.etiapp.adapters.DrawerAdapter;
import com.sonicmax.etiapp.fragments.TopicListFragment;
import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class TopicListActivity extends BaseActivity {
    private Button mInboxButton;
    public ListView mDrawerList;
    public DrawerLayout mDrawerLayout;
    private DrawerAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private List<Bookmark> mBookmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        mInboxButton = (Button) findViewById(R.id.inbox_button);
        mDrawerList = (ListView) findViewById(R.id.drawer_list);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        initInboxButton();
        initDrawer();

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setElevation(4);
            // Required for ActionBarDrawerToggle to function correctly
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    private void initInboxButton() {
        mInboxButton.setText(getUnreadPms());

        mInboxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.closeDrawers();

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

    private String getUnreadPms() {
        int count = SharedPreferenceManager.getInt(this, "inbox_count");
        return "Inbox " + "(" + count + ")";
    }

    public void initDrawer() {
        populateDrawerAdapter();

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

    public void populateDrawerAdapter() {
        mBookmarks = new ArrayList<>();
        mBookmarks.add(new Bookmark("TOTM", "https://boards.endoftheinter.net/topics/LUE?popular"));
        getBookmarksFromSharedPreferences();

        mDrawerAdapter = new DrawerAdapter(this);
        mDrawerAdapter.setBookmarks(mBookmarks);
        mDrawerList.setAdapter(mDrawerAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bookmark bookmark = mBookmarks.get(position);

                Intent intent = new Intent(TopicListActivity.this, TopicListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("url", bookmark.getUrl());
                intent.putExtra("title", bookmark.getName());
                intent.putExtra("page", 1);
                TopicListActivity.this.startActivity(intent);

                mDrawerLayout.closeDrawers();
            }
        });
    }

    private void getBookmarksFromSharedPreferences() {
        List<String> nameArray = SharedPreferenceManager.getStringList(this, "bookmark_names");
        List<String> urlArray = SharedPreferenceManager.getStringList(this, "bookmark_urls");

        for (int i = 0; i < nameArray.size(); i++) {
            mBookmarks.add(new Bookmark(nameArray.get(i), urlArray.get(i)));
        }
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
        return (mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item));
    }
}
