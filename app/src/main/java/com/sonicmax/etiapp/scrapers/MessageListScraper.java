package com.sonicmax.etiapp.scrapers;

import android.content.Context;
import android.net.Uri;

import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.fragments.MessageListFragment;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class MessageListScraper {

    private Context mContext;
    private String mUrl;

    public MessageListScraper(Context context, String url) {
        this.mContext = context;
        this.mUrl = url;
    }

    public List<Message> scrapeMessages(String html, boolean isFiltered) {

        Document document = Jsoup.parse(html);

        // Get hidden token and signature from quickpost elements, found in all non-archived topics
        Elements formClass = document.getElementsByClass("quickpost");
        if (formClass.size() > 0) {
            Element form = formClass.get(0);
            Element body = form.getElementsByClass("quickpost-body").get(0);
            getToken(form);
            getSignature(body);
        }

        // Get current page number
        int currentPage = getCurrentPage(Uri.parse(mUrl));

        // Check anchors of infobar to get prev/next page URLs (not found in moremessages.php)
        Elements infobarClass = document.getElementsByClass("infobar");
        if (infobarClass.size() > 0) {
            Element infobar = infobarClass.get(0);
            getPageUrls(infobar);
        }

        // Scrape posts
        ArrayList<Message> messages = new ArrayList<>();
        Elements containers = document.getElementsByClass("message-container");
        for (int i = 0; i < containers.size(); i++) {
            Element container = containers.get(i);
            Element messageTop = container.getElementsByClass("message-top").get(0);
            Element userElement = messageTop.child(1);

            // Get username and timestamp. These will be in different places depending on whether
            // we are in an anonymous topic or not
            String username, timeStamp;
            List<Node> children = messageTop.childNodes();
            if (userElement.tagName().equals("a")) {
                username = userElement.text();
                timeStamp = ((TextNode) children.get(5)).text().replace(" | ", "");
            }
            else {
                // Anonymous topic - get human number from 2nd child of message-top element
                username = ((TextNode) children.get(1)).text().replace(" | ", "");
                timeStamp = ((TextNode) children.get(3)).text().replace(" | ", "");
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

            Message message = new Message(container.outerHtml(), username, timeStamp, filterUrl, position);

            messages.add(message);
        }

        MessageListFragment.currentPage = currentPage;

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

    private void getPageUrls(Element infobar) {

        final String HTTPS = "https:";

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

    private void getToken(Element form) {
        Element tokenField = form.getElementsByAttributeValue("name", "h").get(0);
        SharedPreferenceManager.putString(mContext, "h", tokenField.attr("value"));
    }

    private void getSignature(Element body) {
        Element message = body.getElementsByAttributeValue("name", "message").get(0);
        SharedPreferenceManager.putString(mContext, "signature", message.text().trim());
    }
}
