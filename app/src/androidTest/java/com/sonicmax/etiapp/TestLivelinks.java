package com.sonicmax.etiapp;

import android.test.AndroidTestCase;
import android.util.Log;

public class TestLivelinks extends AndroidTestCase {

    final String LOG_TAG = TestLivelinks.class.getSimpleName();
    // Use your own topic/user ID for following variables. Get payload values by checking livelinks
    // request payload for given topic/user ID in your browser.
    final int topicId = 9299399;
    final int userId = 5599;
    final long payloadTopic = 144115188085155271L;
    final long payloadUser = 72057594037933535L;

    public void testIdGeneration() {
        // Zero out top 8 bits to get topic/user id from long
        long topicFromPayload = payloadTopic & 0x00FFFFFFFFFFFFFFL;
        long userFromPayload = payloadUser & 0x00FFFFFFFFFFFFFFL;

        if (BuildConfig.DEBUG && !(topicFromPayload == topicId)) {
            Log.v(LOG_TAG, "Expected " + topicId + ", got " + topicFromPayload);
            throw new AssertionError();
        } else {
            Log.v(LOG_TAG, "Topic ID: " + topicFromPayload);
        }

        if (BuildConfig.DEBUG && !(userFromPayload == userId)) {
            Log.v(LOG_TAG, "Expected " + userId + ", got " + userFromPayload);
            throw new AssertionError();
        } else {
            Log.v(LOG_TAG, "User ID: " + userFromPayload);
        }
    }

}
