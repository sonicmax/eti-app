package com.sonicmax.etiapp;


import android.support.v7.app.ActionBar;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;

public class Utilities {

    public static URL convertToUrl(String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            Log.v("convertToUrl", "Error converting URL from String", e);
            return null;
        }
    }

    public static String convertLineBreaks(String html) {
        Document document = Jsoup.parse(html);
        document.select("br").append("\\n");
        return document.text().replace("\\n", "\n");
    }

    public static void hideActionBar(ActionBar actionBar) {
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
        }
    }
}
