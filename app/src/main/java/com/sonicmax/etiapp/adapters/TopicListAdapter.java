package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.utilities.FuzzyTimestampBuilder;

import java.util.Collections;
import java.util.List;

public class TopicListAdapter extends BaseAdapter {
    private final String LOG_TAG = TopicListAdapter.class.getSimpleName();
    private final int TOPICS_PER_PAGE = 50;

    private final int BG_GREY;
    private final int HEADER_GREY;
    private final Context mContext;
    private final EventInterface mEventInterface;
    private final FuzzyTimestampBuilder mTimestampBuilder;

    private ListView mListView;
    private List<Topic> mTopics = Collections.emptyList();
    private int mCurrentPage;
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
        void onRequestLastUnreadPost(int position);
    }

    public void setListView(ListView view) {
        mListView = view;
    }

    public void setCurrentPage(int page) {
        mCurrentPage = page;
    }

    public void setHasNextPage(boolean hasNextPage) {
        mHasNextPage = hasNextPage;
    }

    public void updateTopics(List<Topic> topics) {
        mTopics.clear();
        mTopics = topics;
        notifyDataSetChanged();
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

    private class ViewHolder {
        final TextView userView;
        final TextView titleView;
        final TextView totalView;
        final TextView tagView;
        final TextView timestampView;

        ViewHolder(TextView username, TextView title, TextView total, TextView tags, TextView timestamp) {
            this.userView = username;
            this.titleView = title;
            this.totalView = total;
            this.tagView = tags;
            this.timestampView = timestamp;
        }
    }

    private View getNextPageButton(ViewGroup parent) {
        Button nextPageButton = (Button) LayoutInflater.from(mContext).inflate(R.layout.next_page_button, parent, false);
        Resources resources = mContext.getResources();
        String nextPageText = resources.getString(R.string.continued_next_page) + " " + (mCurrentPage + 1);
        nextPageButton.setText(nextPageText);

        nextPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEventInterface.onRequestNextPage();
            }
        });

        return nextPageButton;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item_topic, parent, false);
            setViewHolder(view);
        }

        populateTopicView(view, (ViewHolder) view.getTag(), position);

        return view;
    }

    private void setViewHolder(View view) {
        TextView titleView = (TextView) view.findViewById(R.id.list_item_topic_title);
        TextView userView = (TextView) view.findViewById(R.id.list_item_username);
        TextView totalView = (TextView) view.findViewById(R.id.list_item_total);
        TextView tagView = (TextView) view.findViewById(R.id.list_item_tags);
        TextView timestampView = (TextView) view.findViewById(R.id.list_item_time);
        view.setTag(new ViewHolder(userView, titleView, totalView, tagView, timestampView));
    }

    private void populateTopicView(View convertView, ViewHolder viewHolder, int position) {
        Topic topic = getItem(position);

        if (viewHolder != null) {
            viewHolder.userView.setText(topic.getUser());
            viewHolder.titleView.setText(topic.getTitle());
            viewHolder.totalView.setText(topic.etiFormatSize());
            viewHolder.tagView.setText(topic.getTags());
            viewHolder.timestampView.setText(mTimestampBuilder.getFuzzyTimestamp(topic.getTimestamp()));

            // Make sure that clicks on tagView are dispatched to TagSpan listener
            viewHolder.tagView.setMovementMethod(LinkMovementMethod.getInstance());
            viewHolder.totalView.setOnClickListener(lastUnreadPostHandler);

            // Highlight topic if pinned
            if (topic.getTags().toString().matches(".*\\bPinned\\b.*")) {
                ((CardView) convertView).setCardBackgroundColor(HEADER_GREY);
            } else {
                // We have to manually set the default colour because the adapter will reuse views
                ((CardView) convertView).setCardBackgroundColor(BG_GREY);
            }
        }
    }

    private View.OnClickListener lastUnreadPostHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
        int position = mListView.getPositionForView((View) view.getParent());
        mEventInterface.onRequestLastUnreadPost(position);
        }
    };

}
