package com.sonicmax.etiapp;

import android.app.ProgressDialog;
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

import com.sonicmax.etiapp.network.AccountManager;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;
import com.sonicmax.etiapp.utilities.Toaster;

import java.util.List;

public class TopicListActivity extends AppCompatActivity {

    private final String LOG_TAG = TopicListActivity.class.getSimpleName();
    @SuppressWarnings("unused")
    private ProgressDialog mDialog;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        mDrawerList = (ListView)findViewById(R.id.drawer_list);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mTitle = getTitle().toString();

        addDrawerItems();
        setupDrawer();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void addDrawerItems() {
        final List<String> nameArray = SharedPreferenceManager.getStringList(this, "board_names");
        final List<String> urlArray = SharedPreferenceManager.getStringList(this, "board_urls");
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nameArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toaster.makeToast(getBaseContext(), urlArray.get(position));
            }
        });
    }

    private void setupDrawer() {
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
        mDrawerLayout.setDrawerListener(mDrawerToggle);
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
                    new AccountManager(this, mDialog).requestLogout();
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
}
