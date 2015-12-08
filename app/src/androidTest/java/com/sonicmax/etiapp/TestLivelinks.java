package com.sonicmax.etiapp;

import android.test.AndroidTestCase;
import android.util.Log;

import java.math.BigInteger;

public class TestLivelinks extends AndroidTestCase {
    // Use your own topic/user ID for following variables. Get payload values by checking livelinks
    // request payload for given topic/user ID in your browser.
    final static int TOPIC_ID = 9229536;
    final static int USER_ID = 5599;
    final static long PAYLOAD_TOPIC_KEY = 144115188085085408L;
    final static long PAYLOAD_INBOX_KEY = 72057594037933535L;

    public void testPayloadGeneration() {
        final String LOG_TAG = "testPayloadGeneration";
        final int TOPIC_CHANNEL = 0x0200;
        final int INBOX_CHANNEL = 0x0100;
        final int SHIFT_CONSTANT = 48;

        // Payload consists of channel type and identifier packed into 64-bit int.
        BigInteger topicForPayload = BigInteger.valueOf(TOPIC_CHANNEL)
                // 100000000
                .shiftLeft(SHIFT_CONSTANT) // SHIFT_CONSTANT is 48
                // 1000000000000000000000000000000000000000000000000000000000
                .or(BigInteger.valueOf(TOPIC_ID));
                // 1000000000000000000000000000000000100011011110010111000111 == 144115188085085408

        if (BuildConfig.DEBUG && !(topicForPayload.equals(BigInteger.valueOf(PAYLOAD_TOPIC_KEY)))) {

            Log.d(LOG_TAG, "Expected: " + BigInteger.valueOf(PAYLOAD_TOPIC_KEY).toString(2)
                    + "\n Generated: " + topicForPayload.toString(2));
            throw new AssertionError();

        }

        BigInteger inboxForPayload = BigInteger.valueOf(INBOX_CHANNEL)
                .shiftLeft(SHIFT_CONSTANT)
                .or(BigInteger.valueOf(USER_ID));

        if (BuildConfig.DEBUG && !(inboxForPayload.equals(BigInteger.valueOf(PAYLOAD_INBOX_KEY)))) {

            Log.d(LOG_TAG, "Expected: " + BigInteger.valueOf(PAYLOAD_INBOX_KEY).toString(2)
                    + "\n Generated: " + inboxForPayload.toString(2));

            throw new AssertionError();

        }
    }
}
