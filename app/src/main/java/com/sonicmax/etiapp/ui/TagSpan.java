package com.sonicmax.etiapp.ui;

import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;

import com.sonicmax.etiapp.activities.TopicListActivity;

/**
 * Span which displays tag name and allows user to navigate to its topic list
 */

public class TagSpan extends ClickableSpan {

    private Context mContext;
    private String mName;
    private String mUrl;

    public TagSpan(Context context, String name, String url) {
        mContext = context;
        mName = name;
        mUrl = url;
    }

    @Override
    public void onClick(View view) {
        // Start new TopicListActivity for selected tag
        Intent intent = new Intent(mContext, TopicListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("url", mUrl);
        intent.putExtra("page", 1);
        intent.putExtra("boardname", mName);
        mContext.startActivity(intent);
    }

}
