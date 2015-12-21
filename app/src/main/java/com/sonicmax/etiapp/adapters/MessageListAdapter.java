package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.ui.Builder;
import com.sonicmax.etiapp.ui.MessageBuilder;
import com.sonicmax.etiapp.ui.SupportMessageBuilder;
import com.sonicmax.etiapp.utilities.FuzzyTimestampBuilder;

import java.util.Collections;
import java.util.List;

public class MessageListAdapter extends SelectableAdapter {
    private final int BG_GREY;
    private final int FG_GREY;
    private final Context mContext;
    private final ClickListener mClickListener;

    private int mLastPosition;
    private Builder mMessageBuilder;
    private FuzzyTimestampBuilder mTimestampBuilder;
    private List<Message> mMessages = Collections.emptyList();

    public MessageListAdapter(Context context, ClickListener clickListener) {
        final String etiDateFormat = "MM/dd/yyyy hh:mm:ss aa";

        mContext = context;
        mClickListener = clickListener;
        mTimestampBuilder = new FuzzyTimestampBuilder(etiDateFormat);

        // Resolve colours from resources for use in onBindViewHolder() method
        BG_GREY = ContextCompat.getColor(context, R.color.bg_grey);
        FG_GREY = ContextCompat.getColor(context, R.color.fg_grey);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mMessageBuilder = new MessageBuilder(mContext);
        } else {
            mMessageBuilder = new SupportMessageBuilder(mContext);
        }
    }

    public void updateMessages(List<Message> messages) {
        mMessages.clear();
        mMessages = messages;
        notifyDataSetChanged();
        mLastPosition = messages.size() - 1;
    }

    public void appendMessages(List<Message> messages) {
        mMessages.addAll(messages);
        notifyItemRangeChanged(mMessages.size() - 1,
                mMessages.size() + messages.size() - 2);
    }

    public void clearMessages() {
        mMessages.clear();
        notifyDataSetChanged();
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
            cardView.setOnLongClickListener(this);

            // messageView.setLongClickable(true);
            // messageView.setMovementMethod(LinkMovementMethod.getInstance());
            // userView.setOnClickListener(this);
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

        viewHolder.messageView.setText(mMessageBuilder.buildMessage(message.getHtml()));
        viewHolder.userView.setText(message.getUser());
        viewHolder.timeView.setText(mTimestampBuilder.getFuzzyTimestamp(message.getTimestamp()));
        viewHolder.postNumberView.setText(message.getPosition());

        if (isSelected(position)) {
            // Increase card elevation in accordance with design guidelines
            viewHolder.cardView.setCardBackgroundColor(FG_GREY);
            viewHolder.cardView.setCardElevation(6);
        }
        else {
            // We need to manually set default background color/card elevation
            viewHolder.cardView.setCardBackgroundColor(BG_GREY);
            viewHolder.cardView.setCardElevation(2);
        }

        // Animate new posts as they are added to adapter (but not for initial page load)
        if (position > mLastPosition) {
            slideInFromRight(viewHolder.cardView);
            mLastPosition = position;
        }
    }

    private void slideInFromRight(View view) {
        // Animate new livelinks posts as they are added to adapter
        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_from_right);
        view.startAnimation(animation);
    }

}
