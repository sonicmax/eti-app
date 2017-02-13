package com.sonicmax.etiapp.scrapers;

import android.content.Context;
import android.net.Uri;

import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.objects.MessageList;
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
    private String mPrevPageUrl;
    private String mNextPageUrl;

    public MessageListScraper(Context context, String url) {
        this.mContext = context;
        this.mUrl = url;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public MessageList scrapeMessages(String html, boolean isFiltered) {
        final String DIVIDER = " | ";
        final String TRIMMED_DIVIDER = ")";
        final String ETI_MONEY_BAG = "$ $";
        final String MONEY_BAG_EMOJI = "\uD83D\uDCB0";
        final String SPACE = " ";

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
        int currentPage = getCurrentPage(mUrl);
        int lastPage = 1;

        // Check anchors of infobar to get prev/next page URLs (not found in moremessages.php)
        Elements infobarClass = document.getElementsByClass("infobar");
        if (infobarClass.size() > 0) {
            Element infobar = infobarClass.get(0);
            getPageUrls(infobar);

            Element pageInfobar = infobarClass.get(1);
            lastPage = getLastPage(pageInfobar);
        }

        // Scrape posts
        ArrayList<Message> messages = new ArrayList<>();
        Elements containers = document.getElementsByClass("message-container");
        for (int i = 0; i < containers.size(); i++) {
            Element container = containers.get(i);
            Element messageTop = container.getElementsByClass("message-top").get(0);
            Element userElement = messageTop.child(1);
            int timestampIndex = 5;
            boolean needsMoneyBags = false;

            // We need to check different node for username/timestamp if user bought money bags from ETI Shop
            if (userElement.text().equals(ETI_MONEY_BAG)) {
                needsMoneyBags = true;
                userElement = messageTop.child(2);
                timestampIndex += 4;
            }

            // Get username and timestamp. These will be in different places depending on whether
            // we are in an anonymous topic or not
            String username, timeStamp;
            List<Node> children = messageTop.childNodes();

            if (userElement.tagName().equals("a")) {
                username = userElement.text();
                timeStamp = ((TextNode) children.get(timestampIndex)).text().replace(DIVIDER, "");

                if (needsMoneyBags) {
                    username = MONEY_BAG_EMOJI + SPACE + username + SPACE + MONEY_BAG_EMOJI;
                }

                // If user has changed their name recently, the TextNode at timestampIndex refers to
                // the divider following the "Formerly known as" section.
                // See: https://github.com/sonicmax/eti-app/issues/18
                if (timeStamp.equals(TRIMMED_DIVIDER)) {
                    timeStamp = ((TextNode) children.get(timestampIndex + 2)).text().replace(DIVIDER, "");
                }
            }

            else {
                // Anonymous topic - get human number from 2nd child of message-top element
                username = ((TextNode) children.get(1)).text().replace(DIVIDER, "");
                timeStamp = ((TextNode) children.get(3)).text().replace(DIVIDER, "");
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

        return new MessageList(messages, currentPage, lastPage, mPrevPageUrl, mNextPageUrl);
    }

    private int getCurrentPage(String url) {
        try {
            Uri uri = Uri.parse(url);
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
            mPrevPageUrl = HTTPS + secondAnchor.attr("href");
            mNextPageUrl = HTTPS + infobar.getElementsByTag("a").get(2).attr("href");

        }
        else if (secondAnchor.text().equals("Next Page")) {
            // Page 2
            mPrevPageUrl = HTTPS + infobar.getElementsByTag("a").get(0).attr("href");
            mNextPageUrl = HTTPS + secondAnchor.attr("href");
        }
        else {
            // Page 1
            mPrevPageUrl = null;
            mNextPageUrl = HTTPS + infobar.getElementsByTag("a").get(0).attr("href");
        }
    }

    private int getLastPage(Element infobar) {
        Elements anchors = infobar.getElementsByTag("a");
        int size = anchors.size();
        if (size > 0) {
            Element lastPageAnchor = anchors.get(size - 1);
            return Integer.parseInt(lastPageAnchor.html());
        }
        else {
            // Topic is on first page
            return 1;
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
