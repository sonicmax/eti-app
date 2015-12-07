package com.sonicmax.etiapp;

import android.test.AndroidTestCase;
import android.util.Log;

public class TestLivelinks extends AndroidTestCase {

    final String LOG_TAG = TestLivelinks.class.getSimpleName();
    // Use your own topic/user ID for following variables. Get payload values by checking livelinks
    // request payload for given topic/user ID in your browser.
    final static int topicId = 9299399;
    final static int userId = 5599;
    final static long payloadTopic = 144115188085155271L;
    final static long payloadInbox = 72057594037933535L;
    final int topicChannel = 0x100;
    final int inboxChannel = 0x200;

    public void testIdGeneration() {
        long testPayload;

        // Zero out top 8 bits to get topic id from long
        testPayload = payloadTopic & 0x00ffffffL;

        if (BuildConfig.DEBUG && !(testPayload == topicId)) {
            Log.v(LOG_TAG, "Expected " + topicId + ", got " + testPayload);
            throw new AssertionError();
        } else {
            Log.v(LOG_TAG, "Topic ID: " + testPayload);
        }

        // Zero out top 8 bits to get user id from long
        testPayload = payloadInbox & 0x00ffffffL;

        if (BuildConfig.DEBUG && !(testPayload == userId)) {
            Log.v(LOG_TAG, "Expected " + userId + ", got " + testPayload);
            throw new AssertionError();
        } else {
            Log.v(LOG_TAG, "User ID: " + testPayload);
        }
    }
}
