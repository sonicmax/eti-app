package com.sonicmax.etiapp.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.text.Layout;
import android.text.style.QuoteSpan;

/**
 * Same behaviour as QuoteSpan but with larger GAP_WIDTH constant to improve readability
 */

public class CustomQuoteSpan extends QuoteSpan {

    private static final int STRIPE_WIDTH = 2;
    private static final int GAP_WIDTH = 30;

    private int mColor;

    public CustomQuoteSpan(@ColorInt int color) {
        super();
        this.mColor = color;
    }

    @ColorInt
    public int getColor() {
        return mColor;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return STRIPE_WIDTH + GAP_WIDTH;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
                                  int top, int baseline, int bottom,
                                  CharSequence text, int start, int end,
                                  boolean first, Layout layout) {
        Paint.Style style = p.getStyle();
        int color = p.getColor();
        p.setStyle(Paint.Style.FILL);
        p.setColor(mColor);
        c.drawRect(x, top, x + dir * STRIPE_WIDTH, bottom, p);
        p.setStyle(style);
        p.setColor(color);
    }

}