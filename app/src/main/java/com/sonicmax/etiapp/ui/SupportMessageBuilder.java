package com.sonicmax.etiapp.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;

import com.sonicmax.etiapp.Utilities;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Parses message-container HTML and creates SpannableStringBuilder with equivalent formatting.
 *  (uses setSpan() instead of append() to create spans - required when API level < 21)
 */

public class SupportMessageBuilder {

    private final String NEWLINE = "\n";
    private Context mContext;
    // Quote depth indicates the current level of nesting while iterating over quoted-message elements
    private int quoteDepth = 0;

    public SupportMessageBuilder(Context context) {
        mContext = context;
    }

    public SpannableStringBuilder buildMessage(String html) {

        Element container = Jsoup.parse(html).getElementsByClass("message-container").get(0);
        Element message = container.getElementsByClass("message").get(0);
        List<Node> children = message.childNodes();
        return getSpannableStringFrom(children);
    }

    /**
     * Creates SpannableStringBuilder containing formatted message from HTML.
     * @param children List of child nodes to iterate over
     * @return SpannableStringBuilder (to be inserted into TextView)
     */
    private SpannableStringBuilder getSpannableStringFrom(List<Node> children) {

        final String SIG_BELT = "---";
        final int DARK_GREY = Color.rgb(66, 66, 66);

        SpannableStringBuilder builder = new SpannableStringBuilder();

        int childSize = children.size();
        for (int i = 0; i < childSize; i++) {
            Node child = children.get(i);

            // Handle text nodes
            if (child instanceof TextNode) {

                String text = ((TextNode) child).text();

                if (!text.replaceAll("^\\s+|\\s+$", "").equals(SIG_BELT)) {
                    builder.append(text);
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
                    String tagName = element.tagName();
                    String text = element.text();
                    switch (tagName) {
                        case "br":
                            builder.append(NEWLINE);
                            break;
                        case "a":
                            String href = element.attr("href");
                            builder.append(href);
                            builder.setSpan(
                                    new StyleSpan(android.graphics.Typeface.BOLD),
                                    builder.length() - href.length(),
                                    builder.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "b":
                            builder.append(text);
                            builder.setSpan(
                                    new StyleSpan(Typeface.BOLD),
                                    builder.length() - text.length(),
                                    builder.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "i":
                            builder.append(text);
                            builder.setSpan(
                                    new StyleSpan(Typeface.ITALIC),
                                    builder.length() - text.length(),
                                    builder.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "u":
                            builder.append(text);
                            builder.setSpan(
                                    new UnderlineSpan(),
                                    builder.length() - text.length(),
                                    builder.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        default:
                            // Ignore div/img/etc
                            break;
                    }
                }

                // Handle preformatted text, images, spoiler tags and quoted messages
                if (element.className() != null) {
                    switch (element.className()) {
                        case "pr":
                            String text = Utilities.convertLineBreaks(element.html());
                            builder.append(text);
                            builder.setSpan(
                                    new TypefaceSpan("monospace"),
                                    builder.length() - text.length(),
                                    builder.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "imgs":
                            // TODO: Implement image thumbnails
                            builder.append("[Images]");
                            builder.append(NEWLINE);
                            break;
                        case "spoiler_closed":
                            String spoilerCaption = getSpoilerCaption(element);
                            builder.append(spoilerCaption);
                            builder.setSpan(
                                    new SpoilerSpan(getSpoilerContents(element)),
                                    builder.length() - spoilerCaption.length(),
                                    builder.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "quoted-message":
                            SpannableStringBuilder quotes = getQuotesFrom(element);
                            builder.append(quotes);
                            builder.setSpan(
                                    new CustomQuoteSpan(DARK_GREY),
                                    builder.length() - quotes.length(),
                                    builder.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            builder.append(NEWLINE);
                            break;
                        default:
                            // Do nothing
                            break;
                    }
                }
            }
        }

        return builder;
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

    private String getSpoilerCaption(Element spoiler) {

        return "[" + spoiler.getElementsByClass("caption").get(0)
                .text().replaceAll("<| />", "") + "]";
    }

    private SpannableStringBuilder getSpoilerContents(Element spoiler) {
        // Get child nodes from spoiler
        List<Node> spoilerNodes = spoiler.getElementsByClass("spoiler_on_open")
                .get(0).childNodes();

        // Remove first and last nodes - these are anchors used by base.js to close the
        // spoiler tag after it has been opened, and should not be included.
        spoilerNodes.get(0).remove();
        spoilerNodes.get(spoilerNodes.size() - 1).remove();

        // Iterate over spoiler nodes & pass result back
        return getSpannableStringFrom(spoilerNodes);
    }

    private SpannableStringBuilder getQuotesFrom(Element quote) {

        final int LIGHT_GREY = Color.rgb(66, 66, 66);
        final String QUOTE_ARROW = "⇗";

        // Increase quote depth to account for gap/stripe width in CustomQuoteSpan.
        quoteDepth++;

        SpannableStringBuilder output = new SpannableStringBuilder();

        // Kludgy fix to make sure that QuoteBackgroundSpan is aligned with top of CustomQuoteSpan
        output.append(NEWLINE);

        if (!quote.attr("msgid").equals("")) {
            // Check message-top element for username
            Element messageTop = quote.getElementsByClass("message-top").get(0);
            Element userAnchor = messageTop.getElementsByTag("a").get(0);
            String username;
            if (!userAnchor.text().equals(QUOTE_ARROW)) {
                // Get username from anchor tag
                username = userAnchor.text();
            }
            else {
                // Get human number from text of message-top
                Pattern p = Pattern.compile("(Human #)\\d+");
                Matcher m = p.matcher(messageTop.text());

                if (m.find()) {
                    username = m.group(0);
                }
                else {
                    username = "Human";
                }
            }

            output.append(username);
            output.setSpan(
                    new QuoteBackgroundSpan(LIGHT_GREY, quoteDepth),
                    output.length() - username.length(),
                    output.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            output.append(NEWLINE);
        }

        // Use quote depth to determine which alpha value to use for background colour.
        int alpha = quoteDepth * 10;

        if (quoteDepth < 3) {
            SpannableStringBuilder quotes = getQuoteContents(quote);
            output.append(quotes);
            output.setSpan(
                    new QuoteBackgroundSpan(Color.argb(alpha, 0, 0, 0), quoteDepth),
                    output.length() - quotes.length(),
                    output.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        else {
            // TODO: Create method for displaying omitted quote (to match ChromeLL behaviour)
            output.append("[quoted text omitted]");
            output.setSpan(
                    new QuoteBackgroundSpan(Color.argb(alpha, 0, 0, 0), quoteDepth),
                    output.length() - 21,
                    output.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Reduce quote depth after appending contents.
        quoteDepth--;

        return output;
    }

    private SpannableStringBuilder getQuoteContents(Element quote) {

        List<Node> quoteNodes = quote.childNodes();
        return getSpannableStringFrom(quoteNodes);
    }
}

