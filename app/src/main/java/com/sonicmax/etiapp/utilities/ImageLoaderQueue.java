package com.sonicmax.etiapp.utilities;

import com.sonicmax.etiapp.network.ImageLoader;

import java.util.LinkedList;
import java.util.Queue;


public class ImageLoaderQueue implements ImageLoader.ImageLoaderListener {
    private final Queue<ImageLoader> mQueue;

    public ImageLoaderQueue() {
        mQueue = new LinkedList<>();
    }

    public void push(ImageLoader loader) {
        if (!mQueue.contains(loader)) {
            mQueue.add(loader);

            if (mQueue.size() == 1) {
                getNextFromQueue();
            }
        }

        else {
            // Remove previous instance of loader and push to end of queue
            for (ImageLoader loaderFromQueue : mQueue) {
                if (loaderFromQueue == loader) {
                    mQueue.remove(loaderFromQueue);
                }
            }

            mQueue.add(loader);
        }
    }

    private void getNextFromQueue() {
        if (!mQueue.isEmpty()) {
            ImageLoader imgLoader = mQueue.remove();
            imgLoader.load();
        }
    }

    @Override
    public void onQueueComplete() {
        getNextFromQueue();
    }

}
