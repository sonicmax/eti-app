package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.sonicmax.etiapp.activities.InboxThreadActivity;
import com.sonicmax.etiapp.activities.MessageListActivity;
import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.utilities.FuzzyTimestampBuilder;

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
    private final EventInterface mEventInterface;
    private final FuzzyTimestampBuilder mTimestampBuilder;

    private ListView mListView;
    private List<Topic> mTopics = Collections.emptyList();
    private boolean mHasNextPage = false;

    public TopicListAdapter(Context context, EventInterface eventInterface) {
        final String ETI_DATE_FORMAT = "MM/dd/yyyy HH:mm";

        this.mContext = context;
        this.mEventInterface = eventInterface;
        mTimestampBuilder = new FuzzyTimestampBuilder(ETI_DATE_FORMAT);

        // Resolve colours from resources for use in getView() method
        BG_GREY = ContextCompat.getColor(context, R.color.bg_grey);
        HEADER_GREY = ContextCompat.getColor(context, R.color.header_grey);
    }

    public interface EventInterface {
        void onRequestNextPage();
    }

    public void setListView(ListView view) {
        mListView = view;
    }

    public void setHasNextPage(boolean hasNextPage) {
        mHasNextPage = hasNextPage;
    }

    public void updateTopics(List<Topic> topics) {
        mTopics.clear();
        mTopics = topics;
        notifyDataSetChanged();

        if (mHasNextPage) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            Button nextPageButton = (Button) inflater.inflate(R.layout.next_page_button, null);
            nextPageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mEventInterface.onRequestNextPage();
                }
            });

            mListView.addFooterView(nextPageButton);
        }
    }

    public void getCurrentTime() {
        // Get current date/time so we can create fuzzy timestamps (eg "1 minute ago")
        mTimestampBuilder.setCurrentTime();
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
        timestampView.setText(mTimestampBuilder.getFuzzyTimestamp(topic.getTimestamp()));

        // Make sure that clicks on tagView are dispatched to TagSpan listener
        tagView.setMovementMethod(LinkMovementMethod.getInstance());
        countView.setOnClickListener(lastPageHandler);

        // Highlight topic if pinned
        if (topic.getTags().toString().matches(".*\\bPinned\\b.*")) {
            ((CardView) convertView).setCardBackgroundColor(HEADER_GREY);
        }
        else {
            // We have to manually set the default colour because the adapter will reuse views
            ((CardView) convertView).setCardBackgroundColor(BG_GREY);
        }

        return convertView;
    }

    private View.OnClickListener lastPageHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            // Get Topic object from adapter
            final int position = mListView.getPositionForView((View) view.getParent());
            Topic target = mTopics.get(position);

            Intent intent;
            if (target.getUrl().contains("inboxthread.php")) {
                intent = new Intent(mContext, InboxThreadActivity.class);
            }
            else {
                intent = new Intent(mContext, MessageListActivity.class);
            }

            // Create new intent for MessageListActivity using Topic data
            intent.putExtra("topic", target);
            intent.putExtra("title", target.getTitle());
            intent.putExtra("last_page", true);

            mContext.startActivity(intent);
        }
    };

}
