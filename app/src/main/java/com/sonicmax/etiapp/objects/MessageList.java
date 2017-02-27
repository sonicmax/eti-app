package com.sonicmax.etiapp.objects;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for list of Message objects and some other data required for message list functionality
 */

public class MessageList implements Parcelable {
    private final String mHtml;
    private final String mTitle;
    private final int mPageNumber;
    private final int mLastPage;
    private final String mPrevPageUrl;

    private List<Message> mMessages;
    private String mNextPageUrl;
    private boolean mIsStarred;

    public MessageList (String html, List<Message> messages, String title, int pageNumber,
                        int lastPage, String prevPageUrl, String nextPageUrl, boolean isStarred) {
        mHtml = html;
        mMessages = messages;
        mTitle = title;
        mPageNumber = pageNumber;
        mLastPage = lastPage;
        mPrevPageUrl = prevPageUrl;
        mNextPageUrl = nextPageUrl;
        mIsStarred = isStarred;
    }

    public String getHtml() {
        return mHtml;
    }

    public List<Message> getMessages() {
        return mMessages;
    }

    public String getTitle() {
        if (mTitle != null) {
            return mTitle;
        }
        else {
            return "";
        }
    }

    public int getPageNumber() {
        return mPageNumber;
    }

    public int getLastPage() {
        return mLastPage;
    }

    public void setNextPageUrl(String url) {
        mNextPageUrl = url;
    }

    public void addNewMessages(List<Message> messages) {
        mMessages.addAll(messages);
    }

    public String getPrevPageUrl() {
        return mPrevPageUrl;
    }

    public String getNextPageUrl() {
        return mNextPageUrl;
    }

    public boolean isStarred() {
        return mIsStarred;
    }

    public void setStarredFlag(boolean value) {
        mIsStarred = value;
    }

    private MessageList(Parcel in) {
        if (in.readByte() == 0x01) {
            mMessages = new ArrayList<>();
            in.readList(mMessages, Message.class.getClassLoader());
        } else {
            mMessages = null;
        }

        mHtml = in.readString();
        mTitle = in.readString();
        mPageNumber = in.readInt();
        mLastPage = in.readInt();
        mPrevPageUrl = in.readString();
        mNextPageUrl = in.readString();
        mIsStarred = in.readInt() != 0;
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
        dest.writeString(mHtml);
        dest.writeString(mTitle);
        dest.writeInt(mPageNumber);
        dest.writeInt(mLastPage);
        dest.writeString(mPrevPageUrl);
        dest.writeString(mNextPageUrl);
        dest.writeInt(mIsStarred ? 1 : 0);
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
