package com.sonicmax.etiapp.scrapers;

import android.content.Context;

import com.sonicmax.etiapp.objects.Board;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class BoardListScraper {

    private Context mContext;

    public BoardListScraper(Context context) {
        mContext = context;
    }

    public List<Board> scrapeBoards(String html) {

        if (html == null) {
            return null;
        }

        Document document = Jsoup.parse(html);
        ArrayList<Board> boards = new ArrayList<>();
        Element bookmarks = document.getElementById("bookmarks");
        Elements spans = bookmarks.select("[style=position:relative]");
        int spanLength = spans.size();

        List<String> boardNames = new ArrayList<>(spanLength);
        List<String> boardUrls = new ArrayList<>(spanLength);

        for (int i = 0; i < spanLength; i++) {
            Element anchor = spans.get(i).child(0);
            String name = anchor.text();
            String url = "https:" + anchor.attr("href");
            // Add values to appropriate list to be stored in SharedPreferences
            boardNames.add(name);
            boardUrls.add(url);
            // Add Board to list to display via BoardListAdapter
            boards.add(new Board(name, url));
        }

        SharedPreferenceManager.putStringList(mContext, "bookmark_names", boardNames);
        SharedPreferenceManager.putStringList(mContext, "bookmark_urls", boardUrls);

        return boards;
    }

}
