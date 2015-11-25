package com.sonicmax.etiapp.scrapers;

import android.net.Uri;
import android.util.Log;

import com.sonicmax.etiapp.Message;
import com.sonicmax.etiapp.MessageListFragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class MessageListScraper {

    private String mUrl;

    public MessageListScraper(String url) {
        this.mUrl = url;
    }

    public List<Message> scrapeMessages(String html, boolean isFiltered) {

        Document document = Jsoup.parse(html);
        ArrayList<Message> messages = new ArrayList<>();
        Elements containers = document.getElementsByClass("message-container");

        // Get current page number
        int currentPage = getCurrentPage(Uri.parse(mUrl));

        // Check anchors of infobar to get prev/next page URLs.
        getPageUrls(document);

        // Iterate over message-container elements and populate List<Message>
        for (int i = 0; i < containers.size(); i++) {
            Element container = containers.get(i);
            Element messageTop = container.getElementsByClass("message-top").get(0);
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

            // Pass null into Message object if topic has already been filtered
            String filterUrl = null;

            if (!isFiltered) {
                // Build URL to filter posts by current user
                if (userElement.tagName().equals("a")) {
                    // Get userID from href of anchor
                    String id = userElement.attr("href").replaceAll("\\D+", "");
                    filterUrl = mUrl + "&u=" + id;

                } else {
                    // Get human number from username
                    filterUrl = mUrl + "&u=-" + username.replaceAll("[^\\d.]", "");
                }
            }

            // Get position of current message in topic
            int position = getPosition(currentPage, i);

            Message message = new Message(container.outerHtml(), username, filterUrl, position);

            messages.add(message);
        }

        MessageListFragment.mPageNumber = currentPage;

        return messages;
    }

    private int getCurrentPage(Uri uri) {

        try {
            return Integer.parseInt(uri.getQueryParameter("page"));

        } catch (NumberFormatException e) {
            // (Message list displays first page if parameter is missing)
            return 1;
        }
    }

    private void getPageUrls(Document document) {

        final String HTTPS = "https:";

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
    }

    private int getPosition(int currentPage, int index) {
        return ((currentPage - 1) * 50) + (index + 1);
    }

    public void changeUrl(String url) {
        mUrl = url;
    }
}
