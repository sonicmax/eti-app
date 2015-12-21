package com.sonicmax.etiapp.utilities;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class FuzzyTimestampBuilder {
    private final SimpleDateFormat mDateFormat;

    private int CURRENT_YEAR;
    private int CURRENT_MONTH;
    private int CURRENT_DAY_OF_MONTH;
    private int CURRENT_HOUR_OF_DAY;
    private int CURRENT_MINUTE;
    private int CURRENT_SECOND;

    public FuzzyTimestampBuilder(String dateFormat) {
        mDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
        setCurrentTime();
        startTimer();
    }

    public void setCurrentTime() {
        // Set current date/time so we can create fuzzy timestamps (eg "1 minute ago")
        GregorianCalendar calendar = new GregorianCalendar();
        CURRENT_YEAR = calendar.get(GregorianCalendar.YEAR);
        CURRENT_MONTH = calendar.get(GregorianCalendar.MONTH);
        CURRENT_DAY_OF_MONTH = calendar.get(GregorianCalendar.DAY_OF_MONTH);
        CURRENT_HOUR_OF_DAY = calendar.get(GregorianCalendar.HOUR_OF_DAY);
        CURRENT_MINUTE = calendar.get(GregorianCalendar.MINUTE);
        CURRENT_SECOND = calendar.get(GregorianCalendar.SECOND);
    }

    private void startTimer() {
        final int THIRTY_SECONDS = 30000;
        final int ONE_MINUTE = 60000;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {setCurrentTime();
            }

        }, 0, ONE_MINUTE);
    }

    public String getFuzzyTimestamp(String timestamp) {
        final String LOG_TAG = "getFuzzyTimestamp";
        Date date;

        try {
            date = mDateFormat.parse(timestamp);
        } catch (ParseException e) {
            Log.e(LOG_TAG, "Error parsing date for getView method", e);
            return null;
        }

        GregorianCalendar postCalendar = new GregorianCalendar();
        postCalendar.setTime(date);

        return getDifference(postCalendar);
    }

    private String getDifference(GregorianCalendar postCalendar) {

        int postYear = postCalendar.get(GregorianCalendar.YEAR);

        if (CURRENT_YEAR > postYear) {
            if (CURRENT_YEAR - postYear > 1) {
                return (CURRENT_YEAR - postYear) + " years ago";
            } else {
                return "1 year ago";
            }
        }

        int postMonth = postCalendar.get(GregorianCalendar.MONTH);

        if (CURRENT_MONTH > postMonth) {
            if (CURRENT_MONTH - postMonth > 1) {
                return (CURRENT_MONTH - postMonth) + " months ago";
            } else {
                return "1 month ago";
            }
        }

        int postDay = postCalendar.get(GregorianCalendar.DAY_OF_MONTH);

        if (CURRENT_DAY_OF_MONTH > postDay) {
            if (CURRENT_DAY_OF_MONTH - postDay > 1) {
                return (CURRENT_DAY_OF_MONTH - postDay) + " days ago";
            } else {
                return "1 day ago";
            }
        }

        int postHour = postCalendar.get(GregorianCalendar.HOUR_OF_DAY);

        if (CURRENT_HOUR_OF_DAY > postHour) {
            if (CURRENT_HOUR_OF_DAY - postHour > 1) {
                return (CURRENT_HOUR_OF_DAY - postHour) + " hours ago";
            } else {
                return "1 hour ago";
            }
        }

        int postMinute = postCalendar.get(GregorianCalendar.MINUTE);

        if (CURRENT_MINUTE > postMinute) {
            if (CURRENT_MINUTE - postMinute > 1) {
                return (CURRENT_MINUTE - postMinute) + " minutes ago";
            } else {
                return "1 minute ago";
            }
        }

        int postSecond = postCalendar.get(GregorianCalendar.SECOND);

        if (CURRENT_SECOND > postSecond) {
            if (CURRENT_SECOND - postSecond > 1) {
                return (CURRENT_SECOND - postSecond) + " seconds ago";
            } else {
                return "1 second ago";
            }
        }

        return "Just now";
    }
}
