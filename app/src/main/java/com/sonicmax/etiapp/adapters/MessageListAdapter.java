package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.loaders.ImageLoader;
import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.ui.Builder;
import com.sonicmax.etiapp.ui.ImagePlaceholderSpan;
import com.sonicmax.etiapp.ui.MessageBuilder;
import com.sonicmax.etiapp.ui.SupportMessageBuilder;
import com.sonicmax.etiapp.utilities.FuzzyTimestampBuilder;
import com.sonicmax.etiapp.utilities.ImageCache;
import com.sonicmax.etiapp.utilities.ImageLoaderQueue;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;

public class MessageListAdapter extends SelectableAdapter {
    private final String LOG_TAG = MessageListAdapter.class.getSimpleName();
    private final int MESSAGE_LIMIT = 50;

    private final int BG_GREY;
    private final int FG_GREY;
    private final Context mContext;
    private final EventInterface mEventInterface;
    private final FuzzyTimestampBuilder mTimestampBuilder;
    private final Builder mMessageBuilder;
    private final ImageCache mImageCache;
    private final ImageLoaderQueue mImageLoaderQueue;
    private final DisplayMetrics mDisplayMetrics;

    private int mMaxWidth;
    private int mCurrentPage;
    private List<Message> mMessages;

    // For inbox threads:
    private boolean mIsInboxThread = false;
    private String mSelf;

    private boolean mHasNextPage = false;

    /**
     * Adapter to display Message objects in a RecyclerView.
     * @param context
     * @param eventInterface Interface to dispatch click/long click events back to fragment
     */
    public MessageListAdapter(Context context, EventInterface eventInterface) {
        final String ETI_DATE_FORMAT = "MM/dd/yyyy hh:mm:ss aa";

        mContext = context;
        mEventInterface = eventInterface;
        mTimestampBuilder = new FuzzyTimestampBuilder(ETI_DATE_FORMAT);

        // Resolve colours from resources for use in onBindViewHolder() method
        BG_GREY = ContextCompat.getColor(context, R.color.bg_grey);
        FG_GREY = ContextCompat.getColor(context, R.color.fg_grey);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mMessageBuilder = new MessageBuilder(mContext);
        } else {
            mMessageBuilder = new SupportMessageBuilder(mContext);
        }

        mImageCache = new ImageCache(context);
        mImageLoaderQueue = new ImageLoaderQueue();
        mMessages = Collections.emptyList();
        mDisplayMetrics = new DisplayMetrics();

        ((FragmentActivity) context).getWindowManager().getDefaultDisplay()
                .getMetrics(mDisplayMetrics);

        // For now, just use screen width. This will be modified later
        mMaxWidth = mDisplayMetrics.widthPixels;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Getters/setters/etc
    ///////////////////////////////////////////////////////////////////////////

    public void replaceAllMessages(List<Message> messages) {
        mMessages.clear();
        mMessages = messages;
        notifyDataSetChanged();
    }

    public void addMessages(List<Message> messages) {
        mMessages.addAll(messages);
        // Account for 0-indexing in item range
        int start = mMessages.size() - 1;
        int end = (mMessages.size() - 1) + (messages.size() - 1);
        notifyItemRangeChanged(start, end);
    }

    public void clearMessages() {
        mMessages.clear();
        notifyDataSetChanged();
    }

    public Message getItem(int position) {
        return mMessages.get(position);
    }

    public int getMessageCount() {
        return mMessages.size();
    }

    @Override
    public int getItemCount() {
        int size = mMessages.size();

        if (mHasNextPage && size == MESSAGE_LIMIT) {
            // Include next_page_button with mMessages
            return size + 1;
        }
        else {
            return size;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setCurrentTime() {
        mTimestampBuilder.setCurrentTime();
    }

    public void clearMemoryCache() {
        mImageCache.clearLruCache();
    }

    public void closeDiskCache() {
        mImageCache.closeDiskCache();;
    }

    public void setMaxImageWidth(int width) {
        mMaxWidth = width;
    }

    public void setCurrentPage(int page) {
        mCurrentPage = page;
    }

    public void setNextPageFlag(boolean value) {
        mHasNextPage = value;
    }

    public void setInboxThreadFlag(boolean value) {
        mIsInboxThread = value;
    }

    public void setSelf(String name) {
        mSelf = name;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Methods which modify UI elements
    ///////////////////////////////////////////////////////////////////////////
    public class MessageViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        private CardView cardView;
        private TextView userView;
        private TextView timeView;
        private TextView postNumberView;
        private TextView messageView;
        private Button nextPageButton;
        private ImageView avatarView;

        MessageViewHolder(View view) {
            super(view);

            if (view.getId() == R.id.next_page_button) {
                nextPageButton = (Button) view;
            }

            else {
                cardView = (CardView) view;
                userView = (TextView) view.findViewById(R.id.list_item_username);
                timeView = (TextView) view.findViewById(R.id.list_item_time);
                postNumberView = (TextView) view.findViewById(R.id.list_item_count);
                messageView = (TextView) view.findViewById(R.id.list_item_message_body);

                if (mIsInboxThread) {
                    avatarView = (ImageView) view.findViewById(R.id.list_item_avatar);
                }

                // Set MaxCardElevation to 6 so we can increase it dynamically when selecting cards
                cardView.setMaxCardElevation(6);

                // Dispatch long click events to ClickListener so we can handle them in fragment
                cardView.setOnLongClickListener(this);

                // Handle ClickableSpans
                messageView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        @Override
        public void onClick(View view) {
            mEventInterface.onItemClick(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            mEventInterface.onItemLongClick(getAdapterPosition());
            return false;
        }
    }

    public interface EventInterface {
        void onRequestNextPage();
        void onItemClick(int position);
        boolean onItemLongClick(int position);
    }

    /**
     * Returns layout ID for given item position.
     * PM inbox threads use a different layout to message lists.
     *
     * @param position Position of message in adapter
     * @return Layout ID
     */

    @Override
    public int getItemViewType(int position) {
        if (position == 50) {
            return R.layout.next_page_button;
        }

        else {
            if (mIsInboxThread) {
                if (mMessages.get(position).getUser().equals(mSelf)) {
                    return R.layout.list_pm_thread_self;
                }
                else {
                    return R.layout.list_pm_thread_partner;
                }
            }

            else {
                return R.layout.list_item_message;
            }
        }
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new MessageViewHolder(LayoutInflater.
                from(viewGroup.getContext()).
                inflate(viewType, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder viewHolder, final int position) {
        if (mHasNextPage && position == MESSAGE_LIMIT) {
            Resources resources = mContext.getResources();
            String nextPageText = resources.getString(R.string.continued_next_page) + " " + (mCurrentPage + 1);

            viewHolder.nextPageButton.setText(nextPageText);

            viewHolder.nextPageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mEventInterface.onRequestNextPage();
                }
            });
        }

        else {
            Message message = getItem(position);

            if (viewHolder.messageView == null) {
                // Something went wrong
                Log.v("MessageListAdapter", "ViewHolder not populated at position " + position);
                return;
            }

            final SpannableStringBuilder messageSpan = mMessageBuilder.buildMessage(message.getHtml(), mIsInboxThread);
            viewHolder.messageView.setText(messageSpan);

            // Check whether we need to load images
            if (messageSpan.getSpans(0, messageSpan.length(), ImageSpan.class).length > 0) {

                // Push new ImageLoader to queue
                mImageLoaderQueue.add(viewHolder.getAdapterPosition(),
                        new ImageLoader(mContext, mImageLoaderQueue, mMaxWidth) {

                            @Override
                            public boolean onPreLoad(ImagePlaceholderSpan placeholder) {
                                // Check LRU cache before attempting to load image
                                Bitmap cachedBitmap = mImageCache.getBitmapFromCache(placeholder.getSource());

                                if (cachedBitmap != null) {
                                    onFinishLoad(cachedBitmap, placeholder);
                                    return false;
                                } else {
                                    return true; // ImageLoader.loadImages() will continue to execute
                                }
                            }

                            @Override
                            public void onFinishLoad(Bitmap bitmap, ImagePlaceholderSpan placeholder) {
                                BitmapDrawable bitmapDrawable = getDrawableFromBitmap(bitmap);
                                ImageSpan img = new ImageSpan(bitmapDrawable, placeholder.getSource());
                                mImageCache.addBitmapToCache(placeholder.getSource(), bitmap);

                                // Find position of placeholder, remove and replace with loaded image
                                int start = messageSpan.getSpanStart(placeholder);
                                int end = messageSpan.getSpanEnd(placeholder);
                                messageSpan.removeSpan(placeholder);
                                messageSpan.setSpan(img, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                // Update view
                                viewHolder.messageView.setText(messageSpan);
                            }

                        }.populateQueue(messageSpan, position));
            }

            viewHolder.userView.setText(message.getUser());
            viewHolder.timeView.setText(mTimestampBuilder.getFuzzyTimestamp(message.getTimestamp()));
            viewHolder.postNumberView.setText(message.getPosition());

            if (mIsInboxThread) {
                displayAvatar(message, viewHolder.avatarView);
            }

            if (isSelected(position)) {
                // Increase card elevation in accordance with design guidelines
                viewHolder.cardView.setCardBackgroundColor(FG_GREY);
                viewHolder.cardView.setCardElevation(6);
            } else {
                // We need to manually set default background color/card elevation
                viewHolder.cardView.setCardBackgroundColor(BG_GREY);
                viewHolder.cardView.setCardElevation(2);
            }

            // Animate new posts as they are added to adapter
            if (message.needsAnimation()) {
                if (mIsInboxThread) {
                    if (message.getUser().equals(mSelf)) {
                        slideInFromRight(viewHolder.cardView);
                    }
                    else {
                        slideInFromLeft(viewHolder.cardView);
                    }
                }
                else {
                    slideInFromRight(viewHolder.cardView);
                }

                message.setAnimationFlag(false);
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(MessageViewHolder holder) {
        // If view is no longer in window, we should remove ImageLoader from queue.
        mImageLoaderQueue.removeFromQueue(holder.getAdapterPosition());
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public boolean onFailedToRecycleView(MessageViewHolder holder) {
        // Force adapter to recycle the ViewHolder.
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    private BitmapDrawable getDrawableFromBitmap(Bitmap bitmap) {
        BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
        bitmapDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());

        return bitmapDrawable;
    }

    private void displayAvatar(Message message, ImageView avatarView) {
        if (message.getAvatarUrl() != null) {
            Picasso.with(mContext)
                    .load(message.getAvatarUrl())
                    .resize(100, 100)
                    .centerInside()
                    .into(avatarView);
        }

        else {
            // User has no avatar
            Picasso.with(mContext)
                    .load(R.drawable.unknown_avatar)
                    .resize(100, 100)
                    .centerInside()
                    .into(avatarView);
        }
    }

    private void slideInFromRight(View view) {
        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_from_right);
        view.startAnimation(animation);
    }

    private void slideInFromLeft(View view) {
        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_from_left);
        view.startAnimation(animation);
    }
}
