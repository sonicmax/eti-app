package com.sonicmax.etiapp.objects;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for list of Topic objects and some other data required for topic list functionality
 */

public class TopicList implements Parcelable {
    private List<Topic> mTopics;
    private int mPageNumber;
    private String mPrevPageUrl;
    private String mNextPageUrl;
    private String mCurrentUrl;

    public TopicList (List<Topic> topics, int pageNumber, String currentUrl, String prevPageUrl, String nextPageUrl) {
        mTopics = topics;
        mPageNumber = pageNumber;
        mCurrentUrl = currentUrl;
        mPrevPageUrl = prevPageUrl;
        mNextPageUrl = nextPageUrl;
    }

    public List<Topic> getTopics() {
        if (mTopics != null) {
            return mTopics;
        }
        else {
            return new ArrayList<>();
        }
    }

    public int getPageNumber() {
        return mPageNumber;
    }

    public String getUrl() {
        return mCurrentUrl;
    }

    public String getPrevPageUrl() {
        return mPrevPageUrl;
    }

    public String getNextPageUrl() {
        return mNextPageUrl;
    }

    private TopicList(Parcel in) {
        if (in.readByte() == 0x01) {
            mTopics = new ArrayList<>();
            in.readList(mTopics, Message.class.getClassLoader());
        } else {
            mTopics = null;
        }
        mPageNumber = in.readInt();
        mCurrentUrl = in.readString();
        mPrevPageUrl = in.readString();
        mNextPageUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mTopics == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mTopics);
        }
        dest.writeInt(mPageNumber);
        dest.writeSerializable(mCurrentUrl);
        dest.writeString(mPrevPageUrl);
        dest.writeString(mNextPageUrl);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<TopicList> CREATOR = new Parcelable.Creator<TopicList>() {
        @Override
        public TopicList createFromParcel(Parcel in) {
            return new TopicList(in);
        }

        @Override
        public TopicList[] newArray(int size) {
            return new TopicList[size];
        }
    };
}
