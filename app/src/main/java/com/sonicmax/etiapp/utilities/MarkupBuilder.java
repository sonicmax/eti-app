package com.sonicmax.etiapp.utilities;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.List;

/**
 * Class which takes HTML of selected message (as String) and creates ETI-formatted markup
 * for quoting.
 */
public class MarkupBuilder {

    private final String NEWLINE = "\n";
    private final String CLOSE_QUOTE = "</quote>";
    private final String LOG_TAG = MarkupBuilder.class.getSimpleName();

    public String parse(String html) {

        // Log.v(LOG_TAG, html);
        Element container = Jsoup.parse(html).getElementsByClass("message-container").get(0);
        Element message = container.getElementsByClass("message").get(0);
        String openQuote = "<quote msgid=\"" + message.attr("msgid") + "\">";
        List<Node> children = message.childNodes();

        return openQuote + getMarkupFrom(children) + CLOSE_QUOTE + NEWLINE;
    }

    private String getMarkupFrom(List<Node> children) {

        final String OPEN_PRE = "<pre>";
        final String CLOSE_PRE = "</pre>";
        final String SIG_BELT = "---";

        String output = "";

        int childSize = children.size();
        for (int i = 0; i < childSize; i++) {
            Node child = children.get(i);

            // Handle text nodes
            if (child instanceof TextNode) {

                String textNode = ((TextNode) child).text();

                if (!textNode.replaceAll("^\\s+|\\s+$", "").equals(SIG_BELT)) {
                    output += textNode;
                }

                else {
                    // Stop loop once we reach sig belt
                    break;
                }
            }

            else {
                // Cast Node to Element so we can check tag and class names
                Element element = (Element) child;

                // Handle newlines/anchors/bold/underline/italics
                if (element.tagName() != null) {
                    switch (element.tagName()) {
                        case "br":
                            output += NEWLINE;
                            break;
                        case "a":
                            output += element.attr("href");
                            break;
                        case "b":
                        case "i":
                        case "u":
                            output += "<" + element.tagName() + ">" + element.text()
                                    + "</" + element.tagName() + ">";
                            break;
                        default:
                            break;
                    }
                }

                // Handle preformatted text, images, spoiler tags and quoted messages
                if (element.className() != null) {
                    switch (element.className()) {
                        case "pr":
                            output += OPEN_PRE + LineBreakConverter.convert(element.html()) + CLOSE_PRE;
                            break;
                        case "imgs":
                            output += getImagesFrom(element);
                            break;
                        case "spoiler_closed":
                            output += getSpoilersFrom(element);
                            break;
                        case "quoted-message":
                            output += getQuotesFrom(element);
                            break;
                        default:
                            // Do nothing
                            break;
                    }
                }
            }
        }

        return output;
    }

    private String getImagesFrom(Element imgs) {

        final String OPEN_IMG = "<img imgsrc=\"";
        final String CLOSE_IMG = "\" />";

        // Iterate over image anchor tags to get src attribute
        Elements anchors = imgs.getElementsByTag("a");
        String imgOutput = "";
        int anchorLength = anchors.size();

        for (int j = 0; j < anchorLength; j++) {
            Element imgAnchor = anchors.get(j);
            imgOutput += OPEN_IMG  + imgAnchor.attr("imgsrc") + CLOSE_IMG + NEWLINE;
        }

        return imgOutput;
    }

    private String getSpoilersFrom(Element spoiler) {

        final String OPEN_SPOILER = "<spoiler>";
        final String CLOSE_SPOILER = "</spoiler>";

        String caption = spoiler.getElementsByClass("caption").get(0)
                .text().replaceAll("<|/>", "");

        if (caption != null) {
            return "<spoiler caption=\"" + caption + "\">"
                    + getSpoilerContents(spoiler) + CLOSE_SPOILER;
        }
        else {
            return OPEN_SPOILER + getSpoilerContents(spoiler) + CLOSE_SPOILER;
        }
    }

    private String getSpoilerContents(Element spoiler) {
        // Get child nodes from spoiler
        List<Node> spoilerNodes = spoiler.getElementsByClass("spoiler_on_open")
                .get(0).childNodes();

        // Remove first and last nodes - these are anchors used by base.js to close the
        // spoiler tag after it has been opened, and should not be included in the markup.
        spoilerNodes.get(0).remove();
        spoilerNodes.get(spoilerNodes.size() - 1).remove();

        // Iterate over spoiler nodes & pass result back
        return getMarkupFrom(spoilerNodes);
    }

    private String getQuotesFrom(Element quote) {

        String openQuote = "<quote>";
        // All quotes have this attribute, but it will be empty in some cases
        if (!quote.attr("msgid").equals("")) {
            openQuote = "<quote msgid=\"" + quote.attr("msgid") + "\">";
        }

        // Check for nested quotes
        Elements nestedQuotes = quote.getElementsByClass("quoted-message");

        if (nestedQuotes.size() > 1) {
            String nestedOutput = "";

            // Iterate backwards - oldest message should be in centre of node tree
            for (int j = nestedQuotes.size() - 1; j >= 0; j--) {

                Element nestedQuote = nestedQuotes.get(j);

                String nestedOpenQuote = "<quote>";
                if (!nestedQuote.attr("msgid").equals("")) {
                    nestedOpenQuote = "<quote msgid=\"" + nestedQuote.attr("msgid") + "\">";
                }

                // Preserve nested structure by building new quotes around existing ones
                // (eg. <quote 1><quote 2> this is quote 2 </quote 2> this is quote 1 </quote 1>
                nestedOutput = nestedOpenQuote
                        + nestedOutput + getQuoteContents(nestedQuote) + CLOSE_QUOTE;
            }
            return nestedOutput;
        }
        else {
            return openQuote + getQuoteContents(quote) + CLOSE_QUOTE;
        }
    }

    private String getQuoteContents(Element quote) {

        List<Node> quoteNodes = quote.childNodes();
        return getMarkupFrom(quoteNodes);
    }

}
