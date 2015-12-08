package com.sonicmax.etiapp;

import android.test.AndroidTestCase;
import android.util.Log;

import com.sonicmax.etiapp.network.LivelinksHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;

public class TestLivelinks extends AndroidTestCase {
    // Use your own topic/user ID for following variables. Get payload values by checking livelinks
    // request payload for given topic/user ID in your browser.
    final static int TOPIC_ID = 9229536;
    final static int USER_ID = 5599;
    final static long PAYLOAD_TOPIC_KEY = 144115188085085408L;
    final static long PAYLOAD_INBOX_KEY = 72057594037933535L;

    private final LivelinksHandler mLivelinks;

    public TestLivelinks() {
        this.mLivelinks = new LivelinksHandler(getContext(), TOPIC_ID, USER_ID);
    }

    /**
     *      Tests that payload values generated by app match values generated by ETI
     */
    public void testPayloadGeneration() {
        final String LOG_TAG = "testPayloadGeneration";

        /* BigInteger payload = BigInteger.valueOf(TOPIC_CHANNEL) // 100000000 in binary

                .shiftLeft(SHIFT_CONSTANT) // SHIFT_CONSTANT is 48
                     1000000000000000000000000000000000000000000000000000000000

                .or(BigInteger.valueOf(ID));
                     1000000000000000000000000000000000100011011110010111000111
                     (equals 144115188085085408)
        */

        BigInteger topicPayload = mLivelinks.getTopicPayload();
        BigInteger inboxPayload = mLivelinks.getInboxPayload();

        if (BuildConfig.DEBUG && !(topicPayload.equals(BigInteger.valueOf(PAYLOAD_TOPIC_KEY)))) {

            Log.d(LOG_TAG, "Expected: " + BigInteger.valueOf(PAYLOAD_TOPIC_KEY).toString(2)
                    + "\n Generated: " + topicPayload.toString(2));

            throw new AssertionError();

        }

        if (BuildConfig.DEBUG && !(inboxPayload.equals(BigInteger.valueOf(PAYLOAD_INBOX_KEY)))) {

            Log.d(LOG_TAG, "Expected: " + BigInteger.valueOf(PAYLOAD_INBOX_KEY).toString(2)
                    + "\n Generated: " + inboxPayload.toString(2));

            throw new AssertionError();

        }
    }

    /**
     *      Test whether we can parse response from livelinks server
     */
    public void testResponseParsing() {
        final String testResponse = "}{\"144115188085085408\":42}";

        JSONObject parsedResponse;
        int postCount;

        try {
            parsedResponse = mLivelinks.parseResponse(testResponse);
            postCount = parsedResponse.getInt(Long.toString(PAYLOAD_TOPIC_KEY));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }

        if (BuildConfig.DEBUG && !(postCount == 42)) {
            throw new AssertionError();
        }
    }
}
