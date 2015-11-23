package com.sonicmax.etiapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class TopicListAdapter extends BaseAdapter {

    private ListView mListView;
    public static List<Topic> topics = Collections.emptyList();
    private final Context context;

    public TopicListAdapter(Context context) {
        // Use context to inflate views in getView method
        this.context = context;
    }

    public void updateTopics(List<Topic> updatedTopics) {
        topics = null;
        topics = updatedTopics;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return topics.size();
    }

    @Override
    public Topic getItem(int position) {
        return topics.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clearTopics() {
        topics = Collections.emptyList();
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        public final TextView userView;
        public final TextView titleView;
        public final TextView totalView;

        public ViewHolder(TextView username, TextView title, TextView total) {
            this.userView = username;
            this.titleView = title;
            this.totalView = total;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        mListView = (ListView) parent;

        TextView username, title, totalPosts;

        if (convertView == null) {

            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_topic, parent, false);
            title = (TextView) convertView.findViewById(R.id.list_item_topic_title);
            username = (TextView) convertView.findViewById(R.id.list_item_username);
            totalPosts = (TextView) convertView.findViewById(R.id.list_item_total);
            convertView.setTag(new ViewHolder(username, title, totalPosts));

        } else {

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            username = viewHolder.userView;
            title = viewHolder.titleView;
            totalPosts = viewHolder.totalView;
        }

        Topic topic = getItem(position);
        username.setText(topic.getUser());
        title.setText(topic.getTitle());
        totalPosts.setText(topic.getTotalWithNewPosts());

        totalPosts.setOnClickListener(lastPageHandler);

        return convertView;
    }

    private View.OnClickListener lastPageHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            final int position = mListView.getPositionForView((View) view.getParent());
            Topic target = topics.get(position);

            Intent intent = new Intent(context, MessageListActivity.class);
            intent.putExtra("topic", target);
            intent.putExtra("title", target.getTitle());
            intent.putExtra("lastpage", true);

            context.startActivity(intent);
        }
    };

}
