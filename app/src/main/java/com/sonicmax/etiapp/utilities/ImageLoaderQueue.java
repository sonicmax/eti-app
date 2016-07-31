package com.sonicmax.etiapp.utilities;

import com.sonicmax.etiapp.network.ImageLoader;

import java.util.LinkedList;

public class ImageLoaderQueue implements ImageLoader.ImageLoaderListener {
    private final LinkedList<Integer> mPositions;
    private final LinkedList<ImageLoader> mQueue;

    public ImageLoaderQueue() {
        mQueue = new LinkedList<>();
        mPositions = new LinkedList<>();
    }

    /**
     * Pushes MessageViewHolder position and ImageLoader to end of queue.
     * @param position Result of MessageViewHolder.getAdapterPosition()
     * @param loader ImageLoader for MessageViewHolder
     */
    public void add(int position, ImageLoader loader) {
        if (!mPositions.contains(position)) {

            mQueue.add(loader);
            mPositions.add(position);

            // If added item was first item in queue, start processing immediately
            // (otherwise, wait for previous item to finish loading)
            if (mQueue.size() == 1) {
                getNextFromQueue();
            }
        }

        else {
            // Remove previous instance of loader and push to end of queue
            removeFromQueue(position);
            mQueue.add(loader);
            mPositions.add(position);
        }
    }

    /**
     * Searches for ImageLoader corresponding to given position and removes from queue
     * @param position Result of MessageViewHolder.getAdapterPosition()
     */
    public void removeFromQueue(int position) {
        int queueLength = mPositions.size();
        for (int i = 0; i < queueLength; i++) {
            int storedPosition = mPositions.get(i);
            if (storedPosition == position) {
                mQueue.get(i).abort();
                mQueue.remove(i);
                mPositions.remove(i);
                break;
            }
        }
    }

    private void getNextFromQueue() {
        if (!mQueue.isEmpty()) {
            mPositions.remove(); // We can discard this value
            ImageLoader imgLoader = mQueue.remove();
            imgLoader.load();
        }
    }

    @Override
    public void onQueueComplete() {
        getNextFromQueue();
    }

}
