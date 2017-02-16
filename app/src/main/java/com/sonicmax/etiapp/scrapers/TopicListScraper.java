package com.sonicmax.etiapp.scrapers;

import android.content.Context;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import com.sonicmax.etiapp.objects.TopicList;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.ui.TagSpan;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class TopicListScraper {
    private final Context mContext;
    private final String mUrl;
    private String mPrevPageUrl;
    private String mNextPageUrl;

    public TopicListScraper(Context context, String url) {
        this.mContext = context;
        this.mUrl = url;
    }

    public TopicList scrapeTopics(String html) {
        Document document = Jsoup.parse(html);
        Elements tableRows = document.getElementsByTag("tr");
        Elements infobars = document.getElementsByClass("infobar");

        if (mUrl.contains("inbox.php")) {
            getPageUrlsFromInbox(infobars);
        }
        else {
            getPageUrls(infobars);
        }

        return new TopicList(getTopics(tableRows), getPageNumber(), mUrl, mPrevPageUrl, mNextPageUrl);
        }

    private ArrayList<Topic> getTopics(Elements tableRows) {
        ArrayList<Topic> topics = new ArrayList<>();
        int tableRowSize = tableRows.size();
        // Skip first table row - doesn't contain any useful data
        for (int i = 1; i < tableRowSize; i++) {
            Element row = tableRows.get(i);
            Elements tableCells = row.getElementsByTag("td");

            Element titleCell = tableCells.get(0);

            Elements titleDivs = titleCell.getElementsByClass("fl");
            Element titleAnchor;
            String url;

            if (titleDivs.size() < 1) {
                // Inbox thread
                titleAnchor = titleCell.getElementsByTag("a").get(0);
                url = "https://endoftheinter.net" + titleAnchor.attr("href");
            }
            else {
                titleAnchor = titleDivs.get(0).getElementsByTag("a").get(0);
                url = "https:" + titleAnchor.attr("href");
            }

            String title = titleAnchor.text();

            Elements tags = titleCell.getElementsByClass("fr");
            SpannableStringBuilder tagSpan;
            if (tags.size() > 0) {
                Element tagDiv = tags.get(0);
                tagSpan = getTags(tagDiv);
            }
            else {
                // Inbox thread - no tags at all
                tagSpan = new SpannableStringBuilder();
            }

            Element usernameCell = tableCells.get(1);
            String username;

            try {
                Element usernameAnchor = usernameCell.getElementsByTag("a").get(0);
                username = usernameAnchor.text().trim();
            } catch (IndexOutOfBoundsException e) {
                // Username anchor doesn't exist if topic is anonymous
                username = "Human";
            }

            // Topic row will always contain these elements
            String postCount = tableCells.get(2).text().replace(" x", "");
            String timestamp = tableCells.get(3).text();

            topics.add(new Topic(title, username, postCount, url, tagSpan, timestamp));
        }

        return topics;
    }

    /**
     * Method which scrapes div for tags and returns SpannableStringBuilder with clickable TagSpans
     */
    private SpannableStringBuilder getTags(Element tagDiv) {
        final String SPACE = " ";
        final String URL_PREFIX = "https://boards.endoftheinter.net";

        Elements tagAnchors = tagDiv.getElementsByTag("a");
        SpannableStringBuilder tagBuilder = new SpannableStringBuilder();
        int tagSize = tagAnchors.size();

        for (int j = 0; j < tagSize; j++) {
            Element tagElement = tagAnchors.get(j);
            String tag = tagElement.text();
            tagBuilder.append(tag);
            tagBuilder.setSpan(new TagSpan(mContext, tag, URL_PREFIX + tagElement.attr("href")),
                    tagBuilder.length() - tag.length(),
                    tagBuilder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tagBuilder.append(SPACE);
        }

        return tagBuilder;
    }

    /**
     * Scrapes previous/next page URLs from Private Messages page.
     * @param infobars Elements with class of "infobar"
     */
    private void getPageUrlsFromInbox(Elements infobars) {
        final String NEXT_ANCHOR_TEXT = "Next Page";
        final String URL_PREFIX = "https://endoftheinter.net";

        // All anchors for page navigation can be found in first infobar element.
        Element infobar = infobars.get(0);
        Elements anchors = infobar.getElementsByTag("a");
        if (anchors.get(0).text().equals(NEXT_ANCHOR_TEXT)) {
            // First page - no previous page.
            mPrevPageUrl = null;
            mNextPageUrl = URL_PREFIX + anchors.get(0).attr("href");
        }
        else if (anchors.get(1).text().equals(NEXT_ANCHOR_TEXT)) {
            // Second page.
            mPrevPageUrl = URL_PREFIX + anchors.get(0).attr("href");
            mNextPageUrl = URL_PREFIX + anchors.get(1).attr("href");
        }
        else if (anchors.get(2).text().equals(NEXT_ANCHOR_TEXT)) {
            // Third page up to penultimate page.
            mPrevPageUrl = URL_PREFIX + anchors.get(1).attr("href");
            mNextPageUrl = URL_PREFIX + anchors.get(2).attr("href");
        }
        else {
            // Last page
            mPrevPageUrl = URL_PREFIX + anchors.get(1).attr("href");
            mNextPageUrl = null;
        }
    }


    /**
     * Scrapes previous/next page URLs from topic list page.
     * @param infobars Elements with class of "infobar"
     */
    private void getPageUrls(Elements infobars) {
        final String URL_PREFIX = "https://boards.endoftheinter.net";

        boolean firstPage = true;

        if (infobars.size() == 2) {
            // User is on first page of topic list - no previous page
            mPrevPageUrl = null;
        }

        else {
            firstPage = false;
            Element prevPageInfobar = infobars.get(0);
            Element prevPageAnchor = prevPageInfobar.getElementsByTag("a").get(0);
            mPrevPageUrl = URL_PREFIX + prevPageAnchor.attr("href");
        }

        Element nextPageInfobar;

        if (firstPage) {
            nextPageInfobar = infobars.get(0);
        } else {
            nextPageInfobar = infobars.get(1);
        }

        Elements pageAnchors = nextPageInfobar.getElementsByTag("a");

        if (pageAnchors.size() > 0) {
            Element nextPageAnchor = pageAnchors.get(0);
            mNextPageUrl = URL_PREFIX + nextPageAnchor.attr("href");
        }

        else {
            mNextPageUrl = null;
        }
    }

    private int getPageNumber() {
        try {
            Uri uri = Uri.parse(mUrl);
            return Integer.parseInt(uri.getQueryParameter("page"));

        } catch (NumberFormatException e) {
            // (Displays first page if parameter is missing)
            return 1;
        }
    }
}
