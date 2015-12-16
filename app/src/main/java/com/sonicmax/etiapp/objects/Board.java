package com.sonicmax.etiapp.objects;

import android.os.Parcel;
import android.os.Parcelable;

public class Board implements Parcelable {

    private final String LOG_TAG = Board.class.getSimpleName();
    private String name;
    private String url;

    /**
     * @param name Name of board to be displayed - typically uses text() of anchor tag
     * @param url URL of board.
     */
    public Board(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    protected Board(Parcel in) {
        name = in.readString();
        url = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(url);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Board> CREATOR = new Parcelable.Creator<Board>() {
        @Override
        public Board createFromParcel(Parcel in) {
            return new Board(in);
        }

        @Override
        public Board[] newArray(int size) {
            return new Board[size];
        }
    };
}