package com.sonicmax.etiapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.sonicmax.etiapp.R;
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }

    @Override
    public void onRequiresLogin() {

    }

    @Override
    public void onLoadComplete(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
    }
}
