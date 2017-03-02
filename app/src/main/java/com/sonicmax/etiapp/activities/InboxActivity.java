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
import android.widget.ListView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.adapters.DrawerAdapter;
import com.sonicmax.etiapp.fragments.TopicListFragment;
import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class InboxActivity extends BaseActivity {

    private final String LOG_TAG = TopicListActivity.class.getSimpleName();
    public ListView mDrawerList;
    public DrawerLayout mDrawerLayout;
    private DrawerAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private List<Bookmark> mBookmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        mDrawerList = (ListView)findViewById(R.id.drawer_list);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        populateDrawerAdapter();
        initDrawer();

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setElevation(4);
            // Required for ActionBarDrawerToggle to function correctly
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
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

                    Intent intent = new Intent(InboxActivity.this, TopicListActivity.class);
                    intent.putExtra("url", bookmark.getUrl());
                    intent.putExtra("boardname", bookmark.getName());
                    InboxActivity.this.startActivity(intent);
                    InboxActivity.this.overridePendingTransition(R.anim.slide_in_from_right,
                            R.anim.slide_out_to_left);

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
        getMenuInflater().inflate(R.menu.menu_inbox, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return (mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item));
    }
}
