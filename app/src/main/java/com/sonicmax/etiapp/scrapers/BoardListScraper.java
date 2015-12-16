package com.sonicmax.etiapp.scrapers;

import com.sonicmax.etiapp.objects.Board;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;


public class BoardListScraper {

    public BoardListScraper() {}

    public List<Board> scrapeBoards(String html) {

        if (html == null) {
            return null;
        }

        Document document = Jsoup.parse(html);
        ArrayList<Board> boards = new ArrayList<>();
        Element bookmarks = document.getElementById("bookmarks");
        Elements spans = bookmarks.select("[style=position:relative]");
        int spanLength = spans.size();
        for (int i = 0; i < spanLength; i++) {
            Element anchor = spans.get(i).child(0);
            String name = anchor.text();
            String url = "https:" + anchor.attr("href");
            Board board = new Board(name, url);
            boards.add(board);
        }

        return boards;
    }

}
