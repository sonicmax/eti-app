package com.sonicmax.etiapp;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class MessageListAdapter extends BaseAdapter {

    public static List<Message> messages = Collections.emptyList();
    private ListView mListView;
    private final Context mContext;

    public MessageListAdapter(Context context) {
        mContext = context;
    }

    public void updateMessages(List<Message> messageList) {
        messages.clear();
        messages = messageList;
        notifyDataSetChanged();
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Message getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        public final TextView userView;
        public final TextView postNumberView;
        public final TextView messageView;

        public ViewHolder(TextView username, TextView count, TextView message) {
            this.userView = username;
            this.postNumberView = count;
            this.messageView = message;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Fix to ensure that ClickableSpans work correctly in ListView
        parent.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        mListView = (ListView) parent;
        TextView userView, postNumberView, messageView;

        if (convertView == null) {

            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.list_item_message, parent, false);

            userView = (TextView) convertView.findViewById(R.id.list_item_msg_username);
            postNumberView = (TextView) convertView.findViewById(R.id.list_item_count);
            messageView = (TextView) convertView.findViewById(R.id.list_item_msg_body);

            convertView.setTag(new ViewHolder(userView, postNumberView, messageView));

        } else {
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            userView = viewHolder.userView;
            postNumberView = viewHolder.postNumberView;
            messageView = viewHolder.messageView;
        }

        Message message = getItem(position);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            MessageBuilder builder = new MessageBuilder(mContext);
            messageView.setText(builder.buildMessage(message.getHtml()));
        }
        else {
            SupportMessageBuilder builder = new SupportMessageBuilder(mContext);
            messageView.setText(builder.buildMessage(message.getHtml()));
        }

        userView.setText(message.getUser());
        postNumberView.setText(message.getPosition());

        messageView.setMovementMethod(LinkMovementMethod.getInstance());
        messageView.setLongClickable(true);
        userView.setOnClickListener(filterHandler);

        return convertView;
    }

    private View.OnClickListener filterHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            final boolean FILTERED = true;
            final int position = mListView.getPositionForView((View) view.getParent());
            Message target = messages.get(position);
            // TODO: Implement LoaderCallbacks here or use fragment
        }
    };

}
