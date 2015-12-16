package com.sonicmax.etiapp.utilities;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class LineBreakConverter {
    /**
     * Fixes Jsoup behaviour regarding line breaks
     * @param html HTML to be converted
     * @return HTML where <br> elements have been replaced with \n
     */
    public static String convert(String html) {
        Document document = Jsoup.parse(html);
        document.select("br").append("\\n");
        return document.text().replace("\\n", "\n");
    }
}
