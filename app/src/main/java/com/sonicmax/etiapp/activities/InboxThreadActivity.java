package com.sonicmax.etiapp.activities;

import android.os.Bundle;
import android.view.Menu;

import com.sonicmax.etiapp.R;

public class InboxThreadActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox_thread);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_inbox_thread, menu);
        return true;
    }
}
