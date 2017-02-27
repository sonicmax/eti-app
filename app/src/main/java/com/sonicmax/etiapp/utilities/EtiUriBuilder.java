package com.sonicmax.etiapp.utilities;

import android.content.ContentValues;
import android.net.Uri;

/**
 * Class which creates Uri for WebRequest using request type and provided values
 */
public class EtiUriBuilder {

    public EtiUriBuilder() {}

    public Uri build(String requestType, ContentValues values) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https");

        switch (requestType) {
            case "topiclist":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("topics")
                        .appendPath(values.get("tags").toString());
                break;

            case "newtopic":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("postmsg.php");
                break;

            case "newmessage":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("postmsg.php")
                        .appendQueryParameter("topic", values.get("id").toString());
                break;

            case "newpm":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("postmsg.php")
                        .appendQueryParameter("pm", values.get("pm").toString());
                break;

            case "livelinks":
                builder.authority("evt0.endoftheinter.net")
                        .appendPath("subscribe");
                break;

            case "moremessages":
                builder.authority("boards.endoftheinter.net")
                        .appendPath("moremessages.php");

                if (values.get("topic") != null) {
                    builder.appendQueryParameter("topic", values.get("topic").toString());
                }
                else if (values.get("pm") != null) {
                    builder.appendQueryParameter("pm", values.get("pm").toString());
                }

                builder.appendQueryParameter("old", values.get("old").toString())
                        .appendQueryParameter("new", values.get("new").toString())
                        .appendQueryParameter("filter", values.get("filter").toString());
                break;

            case "async-update-bookmark":
                builder.authority("endoftheinter.net")
                        .appendPath("async-update-bookmark.php")
                        .appendQueryParameter("pm", values.get("pm").toString())
                        .appendQueryParameter("count", values.get("count").toString());
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
