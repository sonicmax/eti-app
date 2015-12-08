package com.sonicmax.etiapp.network;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;

public class LivelinksHandler {

    private final Context mContext;
    private final BigInteger mTopicPayload;
    private final BigInteger mInboxPayload;

    public LivelinksHandler(Context context, int topicId, int userId) {
        final int TOPIC_CHANNEL = 0x0200;
        final int INBOX_CHANNEL = 0x0100;
        final int SHIFT_CONSTANT = 48;

        this.mContext = context;

        // Payload consists of channel type and identifier packed into 64-bit int.
        mTopicPayload = BigInteger.valueOf(TOPIC_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(topicId));

        mInboxPayload = BigInteger.valueOf(INBOX_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(userId));
    }

    public BigInteger getTopicPayload() {
        return mTopicPayload;
    }

    public BigInteger getInboxPayload() {
        return mInboxPayload;
    }

    public JSONObject parseResponse(String response) {
        // TODO: This will probably break if user has unread PMs
        String trimmedResponse = response.replace("}{", "{");

        JSONObject parsedResponse;

        try {
            parsedResponse = new JSONObject(trimmedResponse);
        } catch (JSONException e) {
            throw new AssertionError(e);
        }

        return parsedResponse;
    }
}
