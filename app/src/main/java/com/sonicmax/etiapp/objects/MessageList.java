package com.sonicmax.etiapp.objects;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for list of Message objects and some other data required for message list functionality
 */

public class MessageList implements Parcelable {
    private final List<Message> mMessages;
    private final int mPageNumber;
    private final int mLastPage;
    private final String mPrevPageUrl;
    private final String mNextPageUrl;

    public MessageList (List<Message> messages, int pageNumber, int lastPage, String prevPageUrl, String nextPageUrl) {
        mMessages = messages;
        mPageNumber = pageNumber;
        mLastPage = lastPage;
        mPrevPageUrl = prevPageUrl;
        mNextPageUrl = nextPageUrl;
    }

    public List<Message> getMessages() {
        return mMessages;
    }

    public int getPageNumber() {
        return mPageNumber;
    }

    public int getLastPage() {
        return mLastPage;
    }

    public String getPrevPageUrl() {
        return mPrevPageUrl;
    }

    public String getNextPageUrl() {
        return mNextPageUrl;
    }

    private MessageList(Parcel in) {
        if (in.readByte() == 0x01) {
            mMessages = new ArrayList<>();
            in.readList(mMessages, Message.class.getClassLoader());
        } else {
            mMessages = null;
        }
        mPageNumber = in.readInt();
        mLastPage = in.readInt();
        mPrevPageUrl = in.readString();
        mNextPageUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mMessages == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mMessages);
        }
        dest.writeInt(mPageNumber);
        dest.writeInt(mLastPage);
        dest.writeString(mPrevPageUrl);
        dest.writeString(mNextPageUrl);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<MessageList> CREATOR = new Parcelable.Creator<MessageList>() {
        @Override
        public MessageList createFromParcel(Parcel in) {
            return new MessageList(in);
        }

        @Override
        public MessageList[] newArray(int size) {
            return new MessageList[size];
        }
    };
}
