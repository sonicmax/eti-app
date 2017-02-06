package com.sonicmax.etiapp.adapters;

import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.network.ImageLoader;
import com.sonicmax.etiapp.objects.Message;
import com.sonicmax.etiapp.ui.Builder;
import com.sonicmax.etiapp.ui.ImagePlaceholderSpan;
import com.sonicmax.etiapp.ui.MessageBuilder;
import com.sonicmax.etiapp.ui.SupportMessageBuilder;
import com.sonicmax.etiapp.utilities.FuzzyTimestampBuilder;
import com.sonicmax.etiapp.utilities.ImageCache;
import com.sonicmax.etiapp.utilities.ImageLoaderQueue;

import java.util.Collections;
import java.util.List;

public class MessageListAdapter extends SelectableAdapter {
    private final String LOG_TAG = MessageListAdapter.class.getSimpleName();
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
    private int mLastPosition;
    private List<Message> mMessages;

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
        mLastPosition = messages.size() - 1;
    }

    public void addMessages(List<Message> messages) {
        mMessages.addAll(messages);
        // Account for 0-indexing in item range
        notifyItemRangeChanged(mMessages.size() - 1, mMessages.size() + messages.size() - 2);
    }

    public void clearMessages() {
        mMessages.clear();
        notifyDataSetChanged();
    }

    public Message getItem(int position) {
        return mMessages.get(position);
    }

    @Override
    public int getItemCount() {
        int size = mMessages.size();
        if (size < 50) {
            return size;
        }
        else {
            // Account for next page button
            return size + 1;
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

        public MessageViewHolder(View view) {
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
     * If adapter contains 50 posts, we want to display next_page_button as the 51st item
     */
    @Override
    public int getItemViewType(int position) {
        return (position == 50) ? R.layout.next_page_button : R.layout.list_item_message;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View itemView;

        if (viewType == R.layout.list_item_message) {
            itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.list_item_message, viewGroup, false);
        }

        else {
            itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.next_page_button, viewGroup, false);
        }

        return new MessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder viewHolder, final int position) {
        if (position == mMessages.size()) {
            viewHolder.nextPageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mEventInterface.onRequestNextPage();
                }
            });
        }

        else {
            Message message = getItem(position);

            final SpannableStringBuilder messageSpan = mMessageBuilder.buildMessage(message.getHtml());
            viewHolder.messageView.setText(messageSpan);

            // Check whether we need to load images
            if (messageSpan.getSpans(0, messageSpan.length(), ImageSpan.class).length > 0) {

                // Push new ImageLoader to queue
                mImageLoaderQueue.add(viewHolder.getAdapterPosition(),
                        new ImageLoader(mContext, mImageLoaderQueue) {

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
                                BitmapDrawable bitmapDrawable = getDrawableFromBitmap(bitmap, placeholder.isNested());
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

            if (isSelected(position)) {
                // Increase card elevation in accordance with design guidelines
                viewHolder.cardView.setCardBackgroundColor(FG_GREY);
                viewHolder.cardView.setCardElevation(6);
            } else {
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

    private BitmapDrawable getDrawableFromBitmap(Bitmap bitmap, boolean shouldShrink) {
        Bitmap resizedBitmap;

        if (shouldShrink) {
            resizedBitmap = shrinkBitmap(bitmap);
        }
        else {
            resizedBitmap = resizeBitmapToFitScreen(bitmap);
        }

        BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), resizedBitmap);
        bitmapDrawable.setBounds(0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        return bitmapDrawable;
    }

    /**
     * Basic method for resizing bitmaps to fit message list view. Returns original bitmap
     * if no adjustments are required.
     *
     * In an ideal world, we would also decode the bounds first & downsample large images,
     * but decoding Bitmap multiple times can cause "SkImageDecoder:: Factory returned null" errors
     *
     * @param bitmap Bitmap to be resized
     * @return Resized Bitmap
     */
    private Bitmap resizeBitmapToFitScreen(Bitmap bitmap) {
        final boolean SHOULD_FILTER = true;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > mMaxWidth) {
            // Scale Bitmap to fit screen.
            float ratio = (float) width / (float) height;
            int newWidth = mMaxWidth;
            int newHeight = (int) ((float) mMaxWidth / ratio);

            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, SHOULD_FILTER);
        }

        // TODO: Maybe we should upscale images that are too small?

        return bitmap;
    }

    /**
     * Shrinks bitmap to an arbitrary size (1/4 of maximum allowed width)
     * Returns original bitmap if resizing is not necessary.
     * Used to shrink images contained in nested quotes
     *
     * @param bitmap Bitmap to be shrunk
     * @return Shrunk bitmap
     */
    private Bitmap shrinkBitmap(Bitmap bitmap) {
        final boolean SHOULD_FILTER = true;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Scale Bitmap to fit 1/4 of screen.
        float ratio = (float) width / (float) height;
        int newWidth = mMaxWidth / 4;
        int newHeight = (int) ((float) newWidth / ratio);

        if (newWidth < width) {
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, SHOULD_FILTER);
        }
        else {
            return bitmap;
        }
    }

    /**
     * Slides given view in from right of screen.
     * Used to animate new livelink posts as they are added to adapter
     *
     * @param view View to be animated
     */
    private void slideInFromRight(View view) {
        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_from_right);
        view.startAnimation(animation);
    }


}
