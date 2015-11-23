package com.sonicmax.etiapp;

import android.net.Uri;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class MessageListScraper {

    private final String LOG_TAG = MessageListScraper.class.getSimpleName();
    private final String mUrl;

    public MessageListScraper(String url) {
        this.mUrl = url;
    }

    public List<Message> scrapeMessages(String html, boolean isFiltered) {

        final String HTTPS = "https:";
        int currentPage;

        Document document = Jsoup.parse(html);
        ArrayList<Message> messages = new ArrayList<>();
        Elements containers = document.getElementsByClass("message-container");

        // Get current page number
        Uri uri = Uri.parse(mUrl);
        try {
            currentPage = Integer.parseInt(uri.getQueryParameter("page"));
            Log.v("messagelistprovider", "current page: " + currentPage);
        } catch (NumberFormatException e) {
            // Message list displays page 1 if parameter is missing
            currentPage = 1;
        }

        // Check anchors of infobar to get prev/next page URLs.
        Element infobar = document.getElementsByClass("infobar").get(0);
        Element secondAnchor = infobar.getElementsByTag("a").get(1);

        if (secondAnchor.text().equals("Previous Page")) {
            // Could be anywhere from pages 3-101
            MessageListFragment.prevPageUrl = HTTPS + secondAnchor.attr("href");
            MessageListFragment.nextPageUrl = HTTPS + infobar.getElementsByTag("a").get(2).attr("href");

        }
        else if (secondAnchor.text().equals("Next Page")) {
            // Page 2
            MessageListFragment.prevPageUrl = HTTPS + infobar.getElementsByTag("a").get(0).attr("href");
            MessageListFragment.nextPageUrl = HTTPS + secondAnchor.attr("href");
        }
        else {
            // Page 1
            MessageListFragment.prevPageUrl = null;
            MessageListFragment.nextPageUrl = HTTPS + infobar.getElementsByTag("a").get(0).attr("href");
        }

        for (int i = 0; i < containers.size(); i++) {
            Element container = containers.get(i);
            Element messageTop = container.getElementsByClass("message-top").get(0);
            Element bodyElement = container.getElementsByClass("message").get(0);
            Element userElement = messageTop.child(1);
            String username;
            if (userElement.tagName().equals("a")) {
                username = userElement.text();
            }
            else {
                // Anonymous topic - check text nodes for human number
                List<Node> children = messageTop.childNodes();
                Node child = children.get(1);
                username = ((TextNode) child).text().replace(" | ", "");
            }
            // Pass null into Message object if user has already been filtered
            String filterUrl = null;

            if (!isFiltered) {

                if (userElement.tagName().equals("a")) {
                    // Get userID from href of anchor
                    String id = userElement.attr("href").replaceAll("\\D+", "");
                    filterUrl = mUrl + "&u=" + id;

                } else {
                    // Get human number from username
                    filterUrl = mUrl + "&u=-" + username.replaceAll("[^\\d.]", "");
                }
            }

            // Add 1 to index - we don't want post numbers to be 0-indexed
            Message message = new Message(container.outerHtml(), username, filterUrl, i + 1);
            messages.add(message);
        }

        MessageListFragment.mPageNumber = currentPage;

        return messages;
    }
}
