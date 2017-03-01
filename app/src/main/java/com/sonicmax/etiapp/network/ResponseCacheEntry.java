package com.sonicmax.etiapp.network;

import java.io.IOException;
import java.io.Serializable;

/**
 * Stores data from topic list/message list GET requests.
 * Implements Serializable so we can write as Object to DiskLruCache (via ResponseCache)
 */

public class ResponseCacheEntry implements Serializable {
    private String mUrl;
    private String mHtml;
    private int mPageNumber;
    private int mAdapterPosition;

    public ResponseCacheEntry(String url, String html, int pageNumber, int adapterPosition) {
        this.mUrl = url;
        this.mHtml = html;
        this.mPageNumber = pageNumber;
        this.mAdapterPosition = adapterPosition;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getHtml() {
        return mHtml;
    }

    public int getPageNumber() {
        return mPageNumber;
    }

    public int getAdapterPosition() {
        return mAdapterPosition;
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {

        this.mUrl = (String) stream.readObject();
        this.mHtml = (String) stream.readObject();
        this.mPageNumber = stream.readInt();
        this.mAdapterPosition = stream.readInt();
    }

    private void writeObject(java.io.ObjectOutputStream stream)
            throws IOException {

        stream.writeObject(mUrl);
        stream.writeObject(mHtml);
        stream.writeInt(mPageNumber);
        stream.writeInt(mAdapterPosition);
    }
}
