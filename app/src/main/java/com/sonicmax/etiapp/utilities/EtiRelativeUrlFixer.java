package com.sonicmax.etiapp.utilities;

/**
 * Adds protocol and domain to relative URLs from ETI.
 */

public class EtiRelativeUrlFixer {

    public static String getAbsoluteUrl(String relativeUrl) {
        final String ETI_URL = "https://endoftheinter.net";
        final String BOARD_URL = "https://boards.endoftheinter.net";
        final String WIKI_URL = "https://wiki.endoftheinter.net";

        if (relativeUrl.startsWith("/message.php") || relativeUrl.startsWith("/showmessages.php")
                || relativeUrl.startsWith("/topics/")) {

            return BOARD_URL + relativeUrl;
        }

        else if (relativeUrl.startsWith("/index.php")) {
            return WIKI_URL + relativeUrl;
        }

        else {
            return ETI_URL + relativeUrl;
        }
    }
}
