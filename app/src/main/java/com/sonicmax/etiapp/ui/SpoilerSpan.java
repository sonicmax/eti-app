package com.sonicmax.etiapp.ui;

import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

/**
 * Reveals spoiler-tagged content on click.
 */

public class SpoilerSpan extends ClickableSpan {

    private SpannableStringBuilder mSpoilerContent;

    public SpoilerSpan(SpannableStringBuilder spoilerContent) {
        mSpoilerContent = spoilerContent;
        Log.v("poop", spoilerContent.toString());
    }

    @Override
    public void onClick(View view) {
        Log.v("spoiler", mSpoilerContent.toString());
    }

}
