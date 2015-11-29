package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.sonicmax.etiapp.Topic;

import java.util.Collections;
import java.util.List;

public class TopicListAdapter extends BaseAdapter {

    private ListView mListView;
    public static List<Topic> mTopics = Collections.emptyList();
    private final Context mContext;

    public TopicListAdapter(Context context) {
        this.mContext = context;
    }

    public void updateTopics(List<Topic> topics) {
        mTopics.clear();
        mTopics = topics;
        notifyDataSetChanged();
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

        public ViewHolder(TextView username, TextView title, TextView total, TextView tags) {
            this.userView = username;
            this.titleView = title;
            this.totalView = total;
            this.tagView = tags;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        mListView = (ListView) parent;

        TextView userView, titleView, countView, tagView;

        if (convertView == null) {

            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.list_item_topic, parent, false);
            titleView = (TextView) convertView.findViewById(R.id.list_item_topic_title);
            userView = (TextView) convertView.findViewById(R.id.list_item_username);
            countView = (TextView) convertView.findViewById(R.id.list_item_total);
            tagView = (TextView) convertView.findViewById(R.id.list_item_tags);
            convertView.setTag(new ViewHolder(userView, titleView, countView, tagView));

        } else {

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            userView = viewHolder.userView;
            titleView = viewHolder.titleView;
            countView = viewHolder.totalView;
            tagView = viewHolder.tagView;
        }

        Topic topic = getItem(position);
        userView.setText(topic.getUser());
        titleView.setText(topic.getTitle());
        countView.setText(topic.getTotalWithNewPosts());
        tagView.setText(topic.getTags());

        // Make sure that clicks on tagView are dispatched to TagSpan listener
        tagView.setMovementMethod(LinkMovementMethod.getInstance());
        countView.setOnClickListener(lastPageHandler);

        return convertView;
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
