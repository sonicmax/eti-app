package com.sonicmax.etiapp.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.fragments.TopicListFragment;
import com.sonicmax.etiapp.loaders.AccountManager;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import java.util.List;

public class TopicListActivity extends AppCompatActivity implements AccountManager.EventInterface {

    private final String LOG_TAG = TopicListActivity.class.getSimpleName();
    @SuppressWarnings("unused")
    private ProgressDialog mDialog;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        mDrawerList = (ListView)findViewById(R.id.drawer_list);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        populateDrawerAdapter();
        initDrawer();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(4);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void populateDrawerAdapter() {
        final List<String> nameArray = SharedPreferenceManager.getStringList(this, "bookmark_names");
        final List<String> urlArray = SharedPreferenceManager.getStringList(this, "bookmark_urls");
        mDrawerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nameArray);
        mDrawerList.setAdapter(mDrawerAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TopicListFragment fragment = (TopicListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.topic_list_container);
                fragment.loadTopicList(nameArray.get(position), urlArray.get(position));
                mDrawerLayout.closeDrawers();
            }
        });
    }

    private void initDrawer() {
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
                    return true;

                case R.id.action_logout:
                    new AccountManager(this, mDialog, this).requestLogout();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }

    @Override
    public void onLoadComplete(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
    }
}
