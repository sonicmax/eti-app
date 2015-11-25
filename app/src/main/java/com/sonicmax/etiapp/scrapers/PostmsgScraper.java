package com.sonicmax.etiapp.scrapers;

import android.content.Context;

import com.sonicmax.etiapp.SharedPreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *      Scrapes hidden token value and signature from postmsg.php
 */
public class PostmsgScraper {

    private Context mContext;

    public PostmsgScraper(Context context) {
        this.mContext = context;
    }

    public void parseResponse(String response) {
        Document document = Jsoup.parse(response);
        scrapeToken(document);
        scrapeSignature(document);
    }

    private void scrapeToken(Document document) {
        Element tokenField = document.select("[name=h]").get(0);
        SharedPreferenceManager.putString(mContext, "h", tokenField.attr("value"));
    }

    private void scrapeSignature(Document document) {
        Element messageInput = document.getElementById("message");
        SharedPreferenceManager.putString(mContext, "signature", messageInput.text().trim());
    }
}
