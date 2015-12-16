package com.sonicmax.etiapp.objects;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;

public class Topic implements Parcelable {

    private final String LOG_TAG = Topic.class.getSimpleName();
    private String mTitle;
    private String mUsername;
    private String mMsgs;
    private String mUrl;
    private SpannableStringBuilder mTagSpan;
    private String mTimestamp;
    private int mSize = -1;

    public Topic(String title, String username, String msgs, String url,
                 SpannableStringBuilder tags, String timestamp) {

        this.mTitle = title;
        this.mUsername = username;
        this.mMsgs = msgs;
        this.mUrl = url;
        this.mTagSpan = tags;
        this.mTimestamp = timestamp;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getUser() {
        return mUsername;
    }

    /**
     * @return Size of topic as string, including new post anchor
     *              eg. 1317 (+29)
     */
    public String etiFormatSize() {
        return mMsgs;
    }

    public String sizeAsString() {
        String count = mMsgs.replaceAll("\\(.*?\\) ?", "");
        return count.replaceAll("[^\\d.]", "");
    }
    
    public int size() {
        return Integer.parseInt(sizeAsString());
    }

    public void addToSize(int newMessages) {
        if (mSize == -1) {
            mSize = Integer.parseInt(sizeAsString() + newMessages);
        }
        else {
            mSize += newMessages;
        }
    }

    public SpannableStringBuilder getTags() {
        return mTagSpan;
    }

    public String getTimestamp() {
        return mTimestamp;
    }

    /**
     * @param newPosts Integer containing number of posts since last page load
     * @return Number of last page in topic
     */
    public int getLastPage(int newPosts) {

        int count;

        try {
            // Add 1 to post count, in case new post takes us to a new page
            count = Integer.parseInt(sizeAsString());
            if (newPosts > 0)
                count += newPosts;

        } catch (NumberFormatException e) {
            // Make sure that we still give a valid page number
            return 1;
        }
        if (count % 50 == 0)
            // Last page has exactly 50 posts, so we should let it round down
            return count / 50;
        else
            return count / 50 + 1;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getLastPageUrl() {
        return mUrl + "&page=" + getLastPage(0);
    }

    public String getId() {

        Uri uri = Uri.parse(mUrl);
        return uri.getQueryParameter("topic");

    }

    protected Topic(Parcel in) {
        mTitle = in.readString();
        mUsername = in.readString();
        mMsgs = in.readString();
        mUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mUsername);
        dest.writeString(mMsgs);
        dest.writeString(mUrl);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Topic> CREATOR = new Parcelable.Creator<Topic>() {
        @Override
        public Topic createFromParcel(Parcel in) {
            return new Topic(in);
        }

        @Override
        public Topic[] newArray(int size) {
            return new Topic[size];
        }
    };
}
