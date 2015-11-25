package com.sonicmax.etiapp.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.LineBackgroundSpan;

/**
 * Implementation of LineBackgroundSpan that highlights background of entire line, even if
 * text doesn't fill it. Used to fill background of quote header/body
 */
public class QuoteBackgroundSpan implements LineBackgroundSpan {

    private final int color;
    private final int margin;
    private Rect mRect;

    /**
     * @param color Color to use for background
     * @param quoteDepth Current depth of quote tree
     */
    public QuoteBackgroundSpan(int color, int quoteDepth) {
        this.color = color;
        // We need to account for STRIPE_WIDTH and GAP_WIDTH constants in CustomQuoteSpan.
        // Subtract 1 from quoteDepth to get desired level of indentation
        this.margin = (quoteDepth - 1) * 32;
        mRect = new Rect();
    }

    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline,
                               int bottom, CharSequence text, int start, int end, int lnum) {

        final int paintColor = p.getColor();
        p.setColor(color);
        mRect.set(left + margin, top, right, bottom);
        c.drawRect(mRect, p);
        p.setColor(paintColor);
    }
}
