package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sonicmax.etiapp.MessageListActivity;
import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.objects.Topic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class TopicListAdapter extends BaseAdapter {

    private final String LOG_TAG = TopicListAdapter.class.getSimpleName();
    private final int BG_GREY;
    private final int HEADER_GREY;

    private final Context mContext;
    private final SimpleDateFormat mDateFormat;

    private ListView mListView;
    private List<Topic> mTopics = Collections.emptyList();

    private int CURRENT_YEAR;
    private int CURRENT_MONTH;
    private int CURRENT_DAY_OF_MONTH;
    private int CURRENT_HOUR_OF_DAY;
    private int CURRENT_MINUTE;

    public TopicListAdapter(Context context) {
        this.mContext = context;

        // Prepare SimpleDateFormat to parse ETI timestamp (will always be in this format)
        mDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);

        // Resolve colours from resources for use in getView() method
        BG_GREY = ContextCompat.getColor(context, R.color.bg_grey);
        HEADER_GREY = ContextCompat.getColor(context, R.color.header_grey);
    }

    public void updateTopics(List<Topic> topics) {
        mTopics.clear();
        mTopics = topics;
        notifyDataSetChanged();
    }

    public void getCurrentTime() {
        // Get current date/time so we can create fuzzy timestamps (eg "1 minute ago")
        GregorianCalendar calendar = new GregorianCalendar();
        CURRENT_YEAR = calendar.get(GregorianCalendar.YEAR);
        CURRENT_MONTH = calendar.get(GregorianCalendar.MONTH);
        CURRENT_DAY_OF_MONTH = calendar.get(GregorianCalendar.DAY_OF_MONTH);
        CURRENT_HOUR_OF_DAY = calendar.get(GregorianCalendar.HOUR_OF_DAY);
        CURRENT_MINUTE = calendar.get(GregorianCalendar.MINUTE);
    }

    @Override
    public int getCount() {
        return mTopics.size();
    }

    @Override
    public Topic getItem(int position) {
        return mTopics.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        public final TextView userView;
        public final TextView titleView;
        public final TextView totalView;
        public final TextView tagView;
        public final TextView timestampView;

        public ViewHolder(TextView username, TextView title, TextView total, TextView tags,
                          TextView timestamp) {
            this.userView = username;
            this.titleView = title;
            this.totalView = total;
            this.tagView = tags;
            this.timestampView = timestamp;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        mListView = (ListView) parent;

        TextView userView, titleView, countView, tagView, timestampView;

        if (convertView == null) {

            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.list_item_topic, parent, false);
            titleView = (TextView) convertView.findViewById(R.id.list_item_topic_title);
            userView = (TextView) convertView.findViewById(R.id.list_item_username);
            countView = (TextView) convertView.findViewById(R.id.list_item_total);
            tagView = (TextView) convertView.findViewById(R.id.list_item_tags);
            timestampView = (TextView) convertView.findViewById(R.id.list_item_time);
            convertView.setTag(new ViewHolder(userView, titleView, countView, tagView, timestampView));

        } else {

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            userView = viewHolder.userView;
            titleView = viewHolder.titleView;
            countView = viewHolder.totalView;
            tagView = viewHolder.tagView;
            timestampView = viewHolder.timestampView;
        }

        Topic topic = getItem(position);

        userView.setText(topic.getUser());
        titleView.setText(topic.getTitle());
        countView.setText(topic.etiFormatSize());
        tagView.setText(topic.getTags());
        timestampView.setText(getFuzzyTimestamp(topic.getTimestamp()));

        // Make sure that clicks on tagView are dispatched to TagSpan listener
        tagView.setMovementMethod(LinkMovementMethod.getInstance());
        countView.setOnClickListener(lastPageHandler);

        // Highlight topic if pinned
        if (topic.getTags().toString().matches(".*\\bPinned\\b.*")) {
            convertView.setBackgroundColor(HEADER_GREY);
        }
        else {
            // We have to manually set the default colour because the adapter will reuse views
            convertView.setBackgroundColor(BG_GREY);
        }

        return convertView;
    }

    private String getFuzzyTimestamp(String timestamp) {
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

        return "Just now";
    }

    private View.OnClickListener lastPageHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            // Get Topic object from adapter
            final int position = mListView.getPositionForView((View) view.getParent());
            Topic target = mTopics.get(position);
            // Create new intent for MessageListActivity using Topic data
            Intent intent = new Intent(mContext, MessageListActivity.class);
            intent.putExtra("topic", target);
            intent.putExtra("title", target.getTitle());
            intent.putExtra("last_page", true);

            mContext.startActivity(intent);
        }
    };

}
