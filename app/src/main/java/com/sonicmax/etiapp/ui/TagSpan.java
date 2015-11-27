package com.sonicmax.etiapp.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

/**
 * Span which allows user to click URLs and view them using their preferred browser
 */

public class TagSpan extends ClickableSpan {

    private Context mContext;
    private String mHref;

    public TagSpan(Context context, String href) {
        mContext = context;
        mHref = href;
    }

    @Override
    public void onClick(View view) {
        // Start new TopicListActivity for selected tag
    }

}
