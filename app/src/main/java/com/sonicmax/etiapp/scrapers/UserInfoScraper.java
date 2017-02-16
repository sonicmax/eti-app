package com.sonicmax.etiapp.scrapers;

import android.content.Context;

import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which scrapes page for user info.
 * Works on any page, provided it contains the bookmarks and userbar elements
 * (ie. topics, tags, or message history)
 */
public class UserInfoScraper {
    private Context mContext;
    private Document mDocument;

    public UserInfoScraper(Context context) {
        mContext = context;
    }

    public void scrapeUserInfo() {
        scrapeUserId();
        scrapePmCount();
    }

    public void setInput(String html) {
        mDocument = Jsoup.parse(html);
    }

    /**
     * Scrapes list of bookmarks and user info.
     * Bookmarks are returned as list, while user info is added to SharedPreferences
     */
    public List<Bookmark> scrapeBookmarks() {
        if (mDocument == null) {
            return null;
        }

        ArrayList<Bookmark> boards = new ArrayList<>();
        Element bookmarks = mDocument.getElementById("bookmarks");
        Elements spans = bookmarks.select("[style=position:relative]");
        int spanLength = spans.size();

        List<String> boardNames = new ArrayList<>(spanLength);
        List<String> boardUrls = new ArrayList<>(spanLength);

        for (int i = 0; i < spanLength; i++) {
            Element anchor = spans.get(i).child(0);
            String name = anchor.text();
            String url = "https:" + anchor.attr("href");
            // Add values to appropriate list to be stored in SharedPreferences
            boardNames.add(name);
            boardUrls.add(url);
            // Add Bookmark to list to display via BookmarkAdapter
            boards.add(new Bookmark(name, url));
        }

        SharedPreferenceManager.putStringList(mContext, "bookmark_names", boardNames);
        SharedPreferenceManager.putStringList(mContext, "bookmark_urls", boardUrls);

        return boards;
    }

    private void scrapeUserId() {
        Elements userbarClass = mDocument.getElementsByClass("userbar");
        if (userbarClass.size() > 0) {
            Element userbar = userbarClass.get(0);
        Element profileAnchor = userbar.getElementsByTag("a").get(0);
        String userId = profileAnchor.attr("href").replaceAll("\\D+", "");
        SharedPreferenceManager.putString(mContext, "user_id", userId);
    }
    }

    private void scrapePmCount() {
        Elements userbarClass = mDocument.getElementsByClass("userbar");
        if (userbarClass.size() > 0) {
            Element userbar = userbarClass.get(0);
        Element pmSpan = userbar.getElementById("userbar_pms");

        int count;
        if (pmSpan.attr("style").equals("display:none")) {
            count = 0;
            } else {
            count = Integer.parseInt(pmSpan.text().replaceAll("\\D+", ""));
        }

        SharedPreferenceManager.putInt(mContext, "inbox_count", count);
    }
    }

}
