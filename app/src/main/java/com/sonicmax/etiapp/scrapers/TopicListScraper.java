package com.sonicmax.etiapp.scrapers;

import android.content.Context;
import android.os.AsyncTask;

import com.sonicmax.etiapp.SharedPreferenceManager;
import com.sonicmax.etiapp.Topic;
import com.sonicmax.etiapp.TopicListFragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class TopicListScraper {

    private final Context mContext;

    public TopicListScraper(Context context) {
        this.mContext = context;
    }

    public ArrayList<Topic> scrapeTopics(String html) {

        final String URL_PREFIX = "https://boards.endoftheinter.net";

        Document document = Jsoup.parse(html);
        ArrayList<Topic> topics = new ArrayList<>();

        // Get user's profile page from userbar element
        Element userbar = document.getElementsByClass("userbar").get(0);
        Element userAnchor = userbar.getElementsByTag("a").get(0);
        SharedPreferenceManager.putString(mContext,
                "profile_url", "https:" + userAnchor.attr("href"));

        // Get next page URL
        Elements infobars = document.getElementsByClass("infobar");
        Element infobar;

        if (infobars.size() == 2) {
            // User is on first page of topic list - no previous page
            infobar = infobars.get(0);
            TopicListFragment.prevPageUrl = null;
        }
        else {
            infobar = infobars.get(1);
            Element prevPageInfobar = infobars.get(0);
            Element prevPageAnchor = prevPageInfobar.getElementsByTag("a").get(0);
            TopicListFragment.prevPageUrl = URL_PREFIX + prevPageAnchor.attr("href");
        }

        Element nextPageAnchor = infobar.getElementsByTag("a").get(0);

        if (nextPageAnchor != null) {
            TopicListFragment.nextPageUrl = URL_PREFIX + nextPageAnchor.attr("href");
        } else {
            TopicListFragment.nextPageUrl = null;
        }

        Elements tableRows = document.getElementsByTag("tr");
        int tableRowSize = tableRows.size();
        // Skip first table row - doesn't contain any useful data
        for (int i = 1; i < tableRowSize; i++) {
            Element row = tableRows.get(i);
            Elements tableCells = row.getElementsByTag("td");

            Element titleCell = tableCells.get(0);
            Element titleDiv = titleCell.getElementsByClass("fl").get(0);
            Element titleAnchor = titleDiv.getElementsByTag("a").get(0);
            // Topic hrefs don't contain protocol - add it manually
            String url = "https:" + titleAnchor.attr("href");
            String title = titleAnchor.text();

            Element usernameCell = tableCells.get(1);
            String username;

            try {
                Element usernameAnchor = usernameCell.getElementsByTag("a").get(0);
                username = usernameAnchor.text();
            } catch (IndexOutOfBoundsException e) {
                // Topic is anonymous
                username = "Human";
            }

            Element postCountCell = tableCells.get(2);
            String postCount = postCountCell.text();

            Topic topic = new Topic(title, username, postCount, url);
            topics.add(topic);

        }

        return topics;
    }

}
