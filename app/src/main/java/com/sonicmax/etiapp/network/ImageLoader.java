package com.sonicmax.etiapp.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;

import com.sonicmax.etiapp.utilities.AsyncLoader;
import com.sonicmax.etiapp.utilities.ImageLoaderQueue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class ImageLoader {
    private final String LOG_TAG = ImageLoader.class.getSimpleName();
    private Context mContext;
    private LoaderManager mLoaderManager;
    private int mCurrentLoaderId;
    private Queue<Bundle> mImageQueue;
    private Iterator<Bundle> mQueueIterator;
    private ImageSpan[] mImageSpans;
    private ImageLoaderListener mCallbacks;


    public ImageLoader(Context context, ImageLoaderListener loaderQueue) {
        mContext = context;
        mLoaderManager = ((FragmentActivity) context).getSupportLoaderManager();
        mImageQueue = new LinkedList<>();
        mCallbacks = loaderQueue;
    }

    /**
     * Populates queue with ImageSpans to load from message
     * @param message Message to be loaded
     * @param position Position of message in current page
     * @return "this" - for method chaining
     */
    public ImageLoader populateQueue(SpannableStringBuilder message, int position) {
        mImageSpans = message.getSpans(0, message.length(), ImageSpan.class);

        for (int i = 0; i < mImageSpans.length; i++) {
            ImageSpan img = mImageSpans[i];

            if (onPreLoad(img)) { // returns true if image doesn't exist in LRU cache

                // Concatenate position and index to create (probably unique) id for loader.
                mCurrentLoaderId = Integer.parseInt(Integer.toString(position) + "00" + Integer.toString(i));

                // Create bundle containing args to be passed to loader
                Bundle loaderArgs = new Bundle(3);
                loaderArgs.putInt("id", mCurrentLoaderId);
                loaderArgs.putInt("index", i);
                loaderArgs.putString("src", img.getSource());

                // Add bundle to queue for later use
                mImageQueue.add(loaderArgs);
            }
        }

        return this;
    }

    /**
     * Begins iterating over queue.
     */
    public void load() {
        mQueueIterator = mImageQueue.iterator();
        getNextFromQueue();
    }

    /**
     * Aborts current task.
     */
    public void abort() {
        if (mLoaderManager.getLoader(mCurrentLoaderId) != null) {
            mLoaderManager.destroyLoader(mCurrentLoaderId);
        }
    }

    private void getNextFromQueue() {
        if (mQueueIterator.hasNext()) {
            Bundle args = mQueueIterator.next();
            int id = args.getInt("id");

            if (mLoaderManager.getLoader(id) == null) {
                mLoaderManager.initLoader(id, args, callbacks).forceLoad();
            } else {
                mLoaderManager.restartLoader(id, args, callbacks).forceLoad();
            }
        }

        else {
            mCallbacks.onQueueComplete();
        }
    }

    /**
     * Called before loader is initialised/restarted
     * @param img Placeholder image
     * @return false to interrupt process() method, true to continue execution
     */
    public boolean onPreLoad(ImageSpan img) {
        return true;
    }

    /**
     * Called after each image has finished loading
     * @param bitmap Loaded image
     * @param imageSpan Original placeholder ImageSpan
     */
    public void onFinishLoad(Bitmap bitmap, ImageSpan imageSpan) {}

    /**
     * Interface used to communicate between classes after ImageLoader has finished loading.
     */
    public interface ImageLoaderListener {
        void onQueueComplete();
    }

    /**
     * Callbacks for AsyncLoader
     */
    private LoaderManager.LoaderCallbacks<Object> callbacks = new LoaderManager.LoaderCallbacks<Object>() {

        @Override
        public Loader<Object> onCreateLoader(int id, final Bundle args) {

            return new AsyncLoader(mContext, args) {

                @Override
                public Bitmap loadInBackground() {
                    Bitmap bitmap;
                    HttpURLConnection connection = null;
                    InputStream input = null;

                    try {
                        URL url = new URL(args.getString("src"));
                        connection = (HttpURLConnection) url.openConnection();
                        connection.connect();
                        input = connection.getInputStream();

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;
                        bitmap = BitmapFactory.decodeStream(input, null, options);

                        return bitmap;

                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error while loading image", e);
                        return null;

                    } finally {
                        // Do some cleanup
                        if (connection != null) {
                            connection.disconnect();
                        }
                        if (input != null) {
                            try {
                                input.close();
                            } catch (IOException e) {
                                Log.e(LOG_TAG, "Error closing input stream", e);
                            }
                        }
                    }
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Object> loader, Object data) {
            int index = ((AsyncLoader) loader).getArgs().getInt("index");
            onFinishLoad((Bitmap) data, mImageSpans[index]);
            mLoaderManager.destroyLoader(loader.getId());
            getNextFromQueue();
        }

        @Override
        public void onLoaderReset(Loader<Object> loader) {
            loader.reset();
        }
    };
}
