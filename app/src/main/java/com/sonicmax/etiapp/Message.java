package com.sonicmax.etiapp;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class Message implements Parcelable {

    private String html;
    private String username;
    private String position;
    private String filterUrl;

    public Message(String html, String user, String filter, int position) {
        this.html = html;
        this.username = user;
        this.filterUrl = filter;
        this.position = Integer.toString(position);
    }

    public String getUser() {
        return username;
    }

    public String getPosition() {
        return position;
    }

    public URL filterUser() {
        try {
            return new URL(filterUrl);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public String getHtml() {
        return html;
    }

    protected Message(Parcel in) {
        html = in.readString();
        username = in.readString();
        position = in.readString();
        filterUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(html);
        dest.writeString(username);
        dest.writeString(position);
        dest.writeString(filterUrl);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}