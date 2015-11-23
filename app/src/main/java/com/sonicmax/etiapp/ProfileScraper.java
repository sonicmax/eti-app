package com.sonicmax.etiapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *  Scrapes profile.php for data and saves to SharedPreferences for later use.
 */
public class ProfileScraper {

    private Context mContext;

    public ProfileScraper(Context context) {
        mContext = context;
    }

    public void getProfile() {

        Bundle args = new Bundle();
        args.putString("method", "GET");
        args.putString("type", "url");
        args.putString("url", SharedPreferenceManager.getString(mContext, "profile_url"));

        new WebRequestAsyncTask(mContext, args) {

            @Override
            protected void onPostExecute(String response) {
                scrapeProfile(response);
            }

        }.execute();

    }

    private void scrapeProfile(String response) {

        Document document = Jsoup.parse(response);
        Element table = document.getElementsByClass("grid").get(0);
        Elements tableRows = table.getElementsByTag("tr");
        int rowLength = tableRows.size();
        for (int i = 0; i < rowLength; i++) {
            Elements cells = tableRows.get(i).getElementsByTag("td");
            if (cells.size() > 0) {
                Element cell = cells.get(0);
                if (cell.text().equals("Signature")) {
                    // Next cell will contain signature data - save it to SharedPreferences
                    String signature = cells.get(1).text();
                    SharedPreferenceManager.putString(mContext, "signature", signature);
                    // We don't need to do anything else.
                    break;
                }
            }
        }
    }

}
