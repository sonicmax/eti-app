package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sonicmax.etiapp.Message;
import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.ui.Builder;
import com.sonicmax.etiapp.ui.MessageBuilder;
import com.sonicmax.etiapp.ui.SupportMessageBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class MessageListAdapter extends BaseAdapter {

    private final String LOG_TAG = MessageListAdapter.class.getSimpleName();
    private final Context mContext;

    private Builder mBuilder;
    private SimpleDateFormat mDateFormat;
    private List<Message> messages = Collections.emptyList();
    private ListView mListView;

    private int CURRENT_YEAR;
    private int CURRENT_MONTH;
    private int CURRENT_DAY_OF_MONTH;
    private int CURRENT_HOUR_OF_DAY;
    private int CURRENT_MINUTE;
    private int CURRENT_SECOND;

    public MessageListAdapter(Context context) {
        mContext = context;

        // Prepare SimpleDateFormat to parse ETI timestamp (will always be in this format)
        mDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mBuilder = new MessageBuilder(mContext);
        } else {
            mBuilder = new SupportMessageBuilder(mContext);
        }
    }

    public void updateMessages(List<Message> messages) {
        messages.clear();
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void appendMessages(List<Message> newMessages) {
        this.messages.addAll(newMessages);
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
        CURRENT_SECOND = calendar.get(GregorianCalendar.SECOND);
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
        public final TextView timeView;
        public final TextView postNumberView;
        public final TextView messageView;

        public ViewHolder(TextView username, TextView time, TextView count, TextView message) {
            this.userView = username;
            this.timeView = time;
            this.postNumberView = count;
            this.messageView = message;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Fix to ensure that ClickableSpans work correctly in ListView
        parent.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        mListView = (ListView) parent;
        TextView userView, timeView, postNumberView, messageView;

        if (convertView == null) {

            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.list_item_message, parent, false);

            userView = (TextView) convertView.findViewById(R.id.list_item_username);
            timeView = (TextView) convertView.findViewById(R.id.list_item_time);
            postNumberView = (TextView) convertView.findViewById(R.id.list_item_count);
            messageView = (TextView) convertView.findViewById(R.id.list_item_message_body);

            convertView.setTag(new ViewHolder(userView, timeView, postNumberView, messageView));

        } else {
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            userView = viewHolder.userView;
            timeView = viewHolder.timeView;
            postNumberView = viewHolder.postNumberView;
            messageView = viewHolder.messageView;
        }

        Message message = getItem(position);
        messageView.setText(mBuilder.buildMessage(message.getHtml()));

        userView.setText(message.getUser());
        timeView.setText(getFuzzyTimestamp(message.getTimestamp()));
        postNumberView.setText(message.getPosition());

        messageView.setMovementMethod(LinkMovementMethod.getInstance());
        messageView.setLongClickable(true);
        userView.setOnClickListener(filterHandler);

        return convertView;
    }

    private String getFuzzyTimestamp(String timestamp) {
        Date date;

        // Try to parse timestamp using provided SimpleDateFormat
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

        return null;
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
