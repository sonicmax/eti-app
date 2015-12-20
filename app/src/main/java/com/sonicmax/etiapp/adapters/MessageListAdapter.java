package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.TextView;

import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.ui.Builder;
import com.sonicmax.etiapp.ui.MessageBuilder;
import com.sonicmax.etiapp.ui.SupportMessageBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MessageListAdapter extends SelectableAdapter {

    private final String LOG_TAG = MessageListAdapter.class.getSimpleName();
    private final int BG_GREY;
    private final int FG_GREY;

    private final Context mContext;
    private final ClickListener mClickListener;

    private Builder mBuilder;
    private SimpleDateFormat mDateFormat;
    private List<Message> mMessages = Collections.emptyList();

    private int CURRENT_YEAR;
    private int CURRENT_MONTH;
    private int CURRENT_DAY_OF_MONTH;
    private int CURRENT_HOUR_OF_DAY;
    private int CURRENT_MINUTE;
    private int CURRENT_SECOND;

    public MessageListAdapter(Context context, ClickListener clickListener) {
        final String etiTimestamp = "MM/dd/yyyy hh:mm:ss aa";

        mContext = context;
        mDateFormat = new SimpleDateFormat(etiTimestamp, Locale.US);
        mClickListener = clickListener;

        // Resolve colours from resources for use in onBindViewHolder() method
        BG_GREY = ContextCompat.getColor(context, R.color.bg_grey);
        FG_GREY = ContextCompat.getColor(context, R.color.fg_grey);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mBuilder = new MessageBuilder(mContext);
        } else {
            mBuilder = new SupportMessageBuilder(mContext);
        }

        startTimer();
    }

    public void updateMessages(List<Message> messages) {
        setCurrentTime();
        mMessages.clear();
        mMessages = messages;
        notifyDataSetChanged();
    }

    public void appendMessages(List<Message> messages) {
        mMessages.addAll(messages);
        notifyItemRangeChanged(mMessages.size() - 1,
                mMessages.size() + messages.size() - 1);
    }

    public void clearMessages() {
        mMessages.clear();
        notifyDataSetChanged();
    }

    private void startTimer() {
        final int THIRTY_SECONDS = 30000;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                setCurrentTime();
            }

        }, 0, THIRTY_SECONDS);
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

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public Message getItem(int position) {
        return mMessages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener{

        private final String LOG_TAG = MessageViewHolder.class.getSimpleName();

        private final CardView cardView;
        private final TextView userView;
        private final TextView timeView;
        private final TextView postNumberView;
        private final TextView messageView;

        public MessageViewHolder(View view) {
            super(view);
            cardView = (CardView) view;
            userView = (TextView) view.findViewById(R.id.list_item_username);
            timeView = (TextView) view.findViewById(R.id.list_item_time);
            postNumberView = (TextView) view.findViewById(R.id.list_item_count);
            messageView = (TextView) view.findViewById(R.id.list_item_message_body);

            // Set MaxCardElevation to 6 so we can increase it dynamically when selecting cards
            cardView.setMaxCardElevation(6);

            // Dispatch click events to ClickListener
            userView.setOnClickListener(this);
            view.setOnLongClickListener(this);

            // messageView.setLongClickable(true);
            // messageView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                mClickListener.onItemClick(getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (mClickListener != null) {
                return mClickListener.onItemLongClick(getAdapterPosition());
            }

            return false;
        }
    }

    public interface ClickListener {
        void onItemClick(int position);
        boolean onItemLongClick(int position);
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        // Fix to ensure that ClickableSpans work correctly in ListView
        viewGroup.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        return new MessageViewHolder(LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.list_item_message, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(MessageViewHolder viewHolder, int position) {
        Message message = getItem(position);

        viewHolder.messageView.setText(mBuilder.buildMessage(message.getHtml()));
        viewHolder.userView.setText(message.getUser());
        viewHolder.timeView.setText(getFuzzyTimestamp(message.getTimestamp()));
        viewHolder.postNumberView.setText(message.getPosition());

        if (isSelected(position)) {
            viewHolder.cardView.setCardBackgroundColor(FG_GREY);
            viewHolder.cardView.setCardElevation(6);
        }
        else {
            viewHolder.cardView.setCardBackgroundColor(BG_GREY);
            viewHolder.cardView.setCardElevation(2);
        }
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
