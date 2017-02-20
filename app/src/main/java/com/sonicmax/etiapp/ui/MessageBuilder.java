package com.sonicmax.etiapp.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.utilities.LineBreakConverter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Parses HTML from message.php and creates SpannableStringBuilder with equivalent formatting.
 */

public class MessageBuilder extends Builder {
    private final String NEWLINE = "\n";
    private Context mContext;
    // Quote depth indicates the current level of nesting while iterating over quoted-message elements
    private int mQuoteDepth = 0;
    private Drawable mSpinner;
    private boolean mNeedsChatUi = false;

    public MessageBuilder(Context context) {
        mContext = context;
        mSpinner = ContextCompat.getDrawable(context, R.drawable.spinner_16_inner_holo);
        mSpinner.setBounds(0, 0, mSpinner.getIntrinsicWidth(), mSpinner.getIntrinsicHeight());
    }

    @Override
    public SpannableStringBuilder buildMessage(String html, boolean needsChatUi) {
        mNeedsChatUi = needsChatUi;

        Element container = Jsoup.parse(html).getElementsByClass("message-container").get(0);
        Element message = container.getElementsByClass("message").get(0);
        List<Node> children = message.childNodes();

        return getSpannableStringFrom(children);
    }

    @TargetApi(21)
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
                    // Append text nodes to builder until we reach sig belt
                    builder.append(text);
                }
                else {
                    // Stop loop - message is ready to be displayed
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
                            builder.append(NEWLINE);
                            break;
                        case "a":
                            LinkSpan linkSpan = new LinkSpan(mContext, element);
                            builder.append(linkSpan.getName(),
                                    linkSpan,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "b":
                            builder.append(element.text(),
                                    new StyleSpan(Typeface.BOLD),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "i":
                            builder.append(element.text(),
                                    new StyleSpan(Typeface.ITALIC),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "u":
                            builder.append(element.text(),
                                    new UnderlineSpan(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                            builder.append(LineBreakConverter.convert(element.html()),
                                    new TypefaceSpan("monospace"),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "imgs":
                            builder.append(NEWLINE);
                            builder.append(getImagesFrom(element));
                            builder.append(NEWLINE);
                            break;
                        case "spoiler_closed":
                            builder.append(getSpoilerCaption(element),
                                    new SpoilerSpan(mContext, getSpoilerContents(element)),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "quoted-message":
                            builder.append(getQuotesFrom(element),
                                    new CustomQuoteSpan(DARK_GREY),
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

    @TargetApi(21)
    private SpannableStringBuilder getImagesFrom(Element imgs) {
        final String SPACE = " ";
        final boolean NESTED = true;
        final boolean NOT_NESTED = false;

        SpannableStringBuilder output = new SpannableStringBuilder();

        // Iterate over image anchor tags to get src attribute
        Elements anchors = imgs.getElementsByTag("a");
        int anchorLength = anchors.size();

        for (int j = 0; j < anchorLength; j++) {
            Element imgAnchor = anchors.get(j);

            // Find width and height of img-placeholder element
            Element imgPlaceholder = imgAnchor.child(0);
            String style = imgPlaceholder.attr("style");
            String[] entries = style.split(";");
            int width = Integer.parseInt(entries[0].replace("width:", "").replace("px", ""));
            int height = Integer.parseInt(entries[1].replace("height:", "").replace("px", ""));

            if (mQuoteDepth == 0) {
                // Apparently this is the only way to append an ImageSpan
                output.append(SPACE,
                        new ImagePlaceholderSpan(mSpinner, width, height, imgAnchor.attr("imgsrc"), NOT_NESTED),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else {
                output.append(SPACE,
                        new ImagePlaceholderSpan(mSpinner, width, height, imgAnchor.attr("imgsrc"), NESTED),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return output;
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

    @TargetApi(21)
    private SpannableStringBuilder getQuotesFrom(Element quote) {

        final int MESSAGE_HEADER_COLOUR = Color.rgb(66, 66, 66);
        final int CHAT_HEADER_COLOUR = Color.rgb(206, 206, 206);
        final String QUOTE_ARROW = "⇗";

        // Increase quote depth to account for gap/stripe width in CustomQuoteSpan.
        mQuoteDepth++;

        SpannableStringBuilder output = new SpannableStringBuilder();

        // Kludgy fix to make sure that QuoteBackgroundSpan is aligned with top of CustomQuoteSpan
        output.append(NEWLINE);

        String username;

        if (!quote.attr("msgid").equals("")) {
            // Check message-top element for username
            Elements messageTops = quote.getElementsByClass("message-top");
            if (messageTops.size() > 0) {
                Element messageTop = messageTops.get(0);
                Element userAnchor = messageTop.getElementsByTag("a").get(0);

                if (!userAnchor.text().equals(QUOTE_ARROW)) {
                    // Get username from anchor tag
                    username = userAnchor.text();
                } else {
                    // Get human number from text of message-top
                    Pattern p = Pattern.compile("(Human #)\\d+");
                    Matcher m = p.matcher(messageTop.text());

                    if (m.find()) {
                        username = m.group(0);
                    } else {
                        username = "Human";
                    }
                }

                if (!mNeedsChatUi) {
                    output.append(username,
                            new QuoteBackgroundSpan(MESSAGE_HEADER_COLOUR, mQuoteDepth),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                else {
                    output.append(username,
                            new QuoteBackgroundSpan(CHAT_HEADER_COLOUR, mQuoteDepth),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                output.append(NEWLINE);
            }

            else {
                // Inside PM thread - don't append username header to quote.
            }
        }

        // Use quote depth to determine which alpha value to use for background colour.
        int alpha = mQuoteDepth * 10;

        if (mQuoteDepth < 3) {
            output.append(getQuoteContents(quote),
                    new QuoteBackgroundSpan(Color.argb(alpha, 0, 0, 0), mQuoteDepth),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        else {
            // TODO: Create method for displaying omitted quote (to match ChromeLL behaviour)
            output.append("\n[quoted text omitted]",
                    new QuoteBackgroundSpan(Color.argb(alpha, 0, 0, 0), mQuoteDepth),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Reduce quote depth after appending contents.
        mQuoteDepth--;

        return output;
    }

    private SpannableStringBuilder getQuoteContents(Element quote) {

        List<Node> quoteNodes = quote.childNodes();
        return getSpannableStringFrom(quoteNodes);
    }
}
