package com.sonicmax.etiapp.ui;

import android.text.SpannableStringBuilder;

/**
 * Class for MessageBuilder and SupportMessageBuilder to extend. Makes MessageListAdapter simpler
 * as we can check API level in constructor and assign either type of builder to same variable,
 * instead of having to check whether we have used MessageBuilder/SupportMessageBuilder each time
 * we want to update the UI.
 */
public class Builder {

    public SpannableStringBuilder buildMessage(String html) {
        // Intentionally blank - override when extending class
        return null;
    }
}
