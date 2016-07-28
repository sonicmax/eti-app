package com.sonicmax.etiapp.ui;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import com.sonicmax.etiapp.utilities.Toaster;

/**
 * Reveals spoiler-tagged content on click.
 */

public class SpoilerSpan extends ClickableSpan {

    private Context mContext;
    private SpannableStringBuilder mSpoilerContent;

    public SpoilerSpan(Context context, SpannableStringBuilder spoilerContent) {
        mContext = context;
        mSpoilerContent = spoilerContent;
    }

    @Override
    public void onClick(View view) {
        // For debugging
        Toaster.makeToast(mContext, mSpoilerContent.toString());
    }
}
