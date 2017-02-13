package com.sonicmax.etiapp.utilities;

import android.content.ContentValues;
import android.net.Uri;

/**
 * Class which creates Uri for WebRequest using request type and provided values
 */
public class EtiUriBuilder {
    private String mRequestType;
    private ContentValues mValues;

    public EtiUriBuilder(String requestType, ContentValues values) {
        mRequestType = requestType;
        mValues = values;
    }

    public Uri build() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https");

        switch (mRequestType) {
            case "topiclist":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("topics")
                        .appendPath(mValues.get("tags").toString());
                break;

            case "newtopic":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("postmsg.php");
                break;

            case "newmessage":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("postmsg.php")
                        .appendQueryParameter("topic", mValues.get("id").toString());
                break;

            case "livelinks":
                builder.authority("evt0.endoftheinter.net")
                        .appendPath("subscribe");
                break;

            case "moremessages":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("moremessages.php")
                        .appendQueryParameter("topic", mValues.get("topic").toString())
                        .appendQueryParameter("old", mValues.get("old").toString())
                        .appendQueryParameter("new", mValues.get("new").toString())
                        .appendQueryParameter("filter", mValues.get("filter").toString());
                break;

            case "history":
            case "bookmarks":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("history.php");
                break;

            case "login":
                builder.authority("iphone.endoftheinter.net");
                break;

            case "logout":
                builder.authority("endoftheinter.net")
                        .appendPath("logout.php");
                break;

            default:
                break;
        }

        return builder.build();
    }
}
