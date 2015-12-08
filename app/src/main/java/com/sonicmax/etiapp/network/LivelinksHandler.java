package com.sonicmax.etiapp.network;

import android.content.Context;

import java.math.BigInteger;

public class LivelinksHandler {

    final Context mContext;
    final BigInteger topicPayload;
    final BigInteger inboxPayload;

    public LivelinksHandler(Context context, int topicId, int userId) {
        final int TOPIC_CHANNEL = 0x0200;
        final int INBOX_CHANNEL = 0x0100;
        final int SHIFT_CONSTANT = 48;

        this.mContext = context;

        // Payload consists of channel type and identifier packed into 64-bit int.
        topicPayload = BigInteger.valueOf(TOPIC_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(topicId));

        inboxPayload = BigInteger.valueOf(INBOX_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(userId));
    }

    public BigInteger getTopicPayload() {
        return topicPayload;
    }

    public BigInteger getInboxPayload() {
        return inboxPayload;
    }
}
