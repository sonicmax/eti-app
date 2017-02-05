package com.sonicmax.etiapp.ui;

import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

public class ImagePlaceholderSpan extends ImageSpan {
    private boolean mIsNested;

    /**
     * @param d Drawable to display as placeholder
     * @param source Source for actual image
     * @param nested Whether image is nested within a quote
     */
    public ImagePlaceholderSpan(Drawable d, String source, boolean nested) {
        super(d, source);
        mIsNested = nested;
    }

    /**
     * Returns true if placeholder is nested in a quoted message.
     * We have to handle nested images slightly differently to non-nested images
     */
    public boolean isNested() {
        return mIsNested;
    }
}
