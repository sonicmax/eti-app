package com.sonicmax.etiapp;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Topic implements Parcelable {

    private final String LOG_TAG = Topic.class.getSimpleName();
    private String title;
    private String username;
    private String total;
    private String url;
    private SpannableStringBuilder tags;

    public Topic(String title, String username, String total, String url, SpannableStringBuilder tags) {
        this.title = title;
        this.username = username;
        this.total = total;
        this.url = url;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public String getUser() {
        return username;
    }

    public String getTotalWithNewPosts() {
        return total;
    }

    public String getTotal() {
        String count = total.replaceAll("\\(.*?\\) ?", "");
        return count.replaceAll("[^\\d.]", "");
    }

    public SpannableStringBuilder getTags() {
        return tags;
    }

    /**
     * @param newPosts Integer containing number of posts since last page load
     * @return Number of last page in topic
     */
    public int getLastPage(int newPosts) {

        int count;

        try {
            // Add 1 to post count, in case new post takes us to a new page
            count = Integer.parseInt(getTotal());
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
        return url;
    }

    public String getLastPageUrl() {
        return url + "&page=" + getLastPage(0);
    }

    public String getId() {

        Uri uri = Uri.parse(url);
        return uri.getQueryParameter("topic");

    }

    protected Topic(Parcel in) {
        title = in.readString();
        username = in.readString();
        total = in.readString();
        url = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(username);
        dest.writeString(total);
        dest.writeString(url);
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
