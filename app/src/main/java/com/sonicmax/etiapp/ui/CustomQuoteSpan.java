package com.sonicmax.etiapp.ui;

import android.text.style.QuoteSpan;

/**
 * Same behaviour as QuoteSpan but with larger GAP_WIDTH constant to improve readability
 */

public class CustomQuoteSpan extends QuoteSpan {

    private static final int STRIPE_WIDTH = 2;
    private static final int GAP_WIDTH = 30;

    @Override
    public int getLeadingMargin(boolean first) {
        return STRIPE_WIDTH + GAP_WIDTH;
    }
}
