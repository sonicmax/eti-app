package com.sonicmax.etiapp.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.activities.BaseActivity;
import com.sonicmax.etiapp.activities.InboxActivity;
import com.sonicmax.etiapp.activities.MessageListActivity;
import com.sonicmax.etiapp.activities.TopicListActivity;
import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.objects.Topic;

import org.jsoup.nodes.Element;

/**
 * Span which allows user to click URLs and view them using their preferred browser
 */

public class LinkSpan extends ClickableSpan {
    private final String BOARDS = "https://boards.endoftheinter.net";

    private Context mContext;
    private Intent mIntent;
    private String mName;

    private boolean mRedirectWithinApp = false;

    public LinkSpan(Context context, Element anchor) {
        mContext = context;

        String href = anchor.attr("href");

        // Check for relative URLs
        if (href.startsWith("/")) {

            // We can redirect some URLs within the app.
            if (href.startsWith("/showmessages.php")) {
                handleMessageListLink(href);
            }

            else if (href.startsWith("/topics/")) {
                handleTopicListLink(href);
            }

            else {
                mName = href;
                mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(href));
            }
        }

        else {
            mName = href;
            mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(href));
        }
    }

    private void handleMessageListLink(String href) {
        href = BOARDS + href;
        Topic target = new Topic(null, null, null, href, null, null);

        mName = "[Topic " + target.getId() + "]";

        mIntent = new Intent(mContext, MessageListActivity.class);
        mIntent.putExtra("topic", target);
        mIntent.putExtra("page", getPageFromHref(href));
        mIntent.putExtra("last_page", false);
        mRedirectWithinApp = true;
    }

    private void handleTopicListLink(String href) {
        mName = href.replace("/topics/", "");
        if (mName.contains("?")) {
            mName = mName.substring(0, mName.indexOf("?"));
        }

        mName = "[" + mName + "]";

        href = BOARDS + href;

        Bookmark target = new Bookmark(mName, href);
        mIntent = new Intent(mContext, TopicListActivity.class);
        mIntent.putExtra("url", target.getUrl());
        mIntent.putExtra("title", target.getName());
        mRedirectWithinApp = true;
    }

    private int getPageFromHref(String href) {
        Uri uri = Uri.parse(href);
        String page = uri.getQueryParameter("page");
        if (page != null) {
            return Integer.parseInt(page);
        }
        else {
            return 0;
        }
    }

    public String getName() {
        return mName;
    }

    @Override
    public void onClick(View view) {
        mContext.startActivity(mIntent);
        if (mRedirectWithinApp) {
            ((BaseActivity) mContext).overridePendingTransition(R.anim.slide_in_from_right,
                    R.anim.slide_out_to_left);
        }
    }
    @Override
    public void updateDrawState(final TextPaint textPaint) {
        textPaint.setColor(ContextCompat.getColor(mContext, R.color.accent));
        textPaint.setUnderlineText(true);
    }
}
