package com.sonicmax.etiapp.loaders;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.util.Log;

import com.sonicmax.etiapp.ui.ImagePlaceholderSpan;
import com.sonicmax.etiapp.utilities.AsyncLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class ImageLoader {
    private final String LOG_TAG = ImageLoader.class.getSimpleName();

    private final int DEFAULT_SAMPLE_SIZE = 2;

    private final Context mContext;
    private final LoaderManager mLoaderManager;
    private final Queue<Bundle> mImageQueue;
    private final ImageLoaderListener mCallbacks;
    private final BitmapFactory.Options mDefaultOptions;

    private Iterator<Bundle> mQueueIterator;
    private ImagePlaceholderSpan[] mPlaceholders;
    private int mCurrentLoaderId;
    private int mMaxWidth;

    public ImageLoader(Context context, ImageLoaderListener loaderQueue, int maxWidth) {
        mContext = context;
        mLoaderManager = ((FragmentActivity) context).getSupportLoaderManager();
        mImageQueue = new LinkedList<>();
        mCallbacks = loaderQueue;
        mMaxWidth = (int) (maxWidth * (float) 0.9);

        DisplayMetrics metrics = context.getApplicationContext().getResources().getDisplayMetrics();
        mDefaultOptions = new BitmapFactory.Options();
        mDefaultOptions.inScreenDensity = metrics.densityDpi;
        mDefaultOptions.inTargetDensity =  metrics.densityDpi;
        mDefaultOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        mDefaultOptions.inSampleSize = DEFAULT_SAMPLE_SIZE;
    }

    /**
     * Populates queue with ImageSpans to load from message
     * @param message Message to be loaded
     * @param position Position of message in current page
     * @return "this" - for method chaining
     */
    public ImageLoader populateQueue(SpannableStringBuilder message, int position) {
        mPlaceholders = message.getSpans(0, message.length(), ImagePlaceholderSpan.class);

        for (int i = 0; i < mPlaceholders.length; i++) {
            ImagePlaceholderSpan placeholder = mPlaceholders[i];

            if (onPreLoad(placeholder)) { // returns true if image doesn't exist in LRU cache

                // Concatenate position and index to create (probably unique) id for loader.
                mCurrentLoaderId = Integer.parseInt(Integer.toString(position) + "00" + Integer.toString(i));

                // Create bundle containing args to be passed to loader
                Bundle loaderArgs = new Bundle(3);
                loaderArgs.putInt("id", mCurrentLoaderId);
                loaderArgs.putInt("index", i);
                loaderArgs.putString("src", placeholder.getSource());

                // Add bundle to queue for later use
                mImageQueue.add(loaderArgs);
            }
        }

        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Queue handling methods
    ///////////////////////////////////////////////////////////////////////////

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

    ///////////////////////////////////////////////////////////////////////////
    // Image processing methods
    ///////////////////////////////////////////////////////////////////////////

    private Bitmap getDownsampledAndResizedBitmap(Bundle args) {
        ImagePlaceholderSpan placeholder = mPlaceholders[args.getInt("index")];
        String src = args.getString("src");

        int[] finalSize;
        int actualWidth = placeholder.getActualWidth();
        int actualHeight = placeholder.getActualHeight();

        if (mPlaceholders[args.getInt("index")].isNested()) {
            // Nested images should take up a quarter of the screen
            finalSize = resizeToFitWidth(actualWidth, actualHeight, mMaxWidth / 4);
        }
        else {
            finalSize = resizeToFitWidth(actualWidth, actualHeight, mMaxWidth);
        }

        Bitmap bitmap = loadDownsampledBitmap(src, finalSize);

        return resizeBitmap(bitmap, finalSize);
    }

    private int calculateInSampleSize(int width, int height) {
        // Raw height and width of image
        int inSampleSize = 1;
        int reqWidth, reqHeight;

        if (width > mMaxWidth) {
            // Scale Bitmap to fit screen.
            float ratio = (float) width / (float) height;
            reqWidth = mMaxWidth;
            reqHeight = (int) ((float) mMaxWidth / ratio);
        }

        else {
            return DEFAULT_SAMPLE_SIZE;
        }

        final int halfHeight = height / 2;
        final int halfWidth = width / 2;

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) >= reqHeight
                && (halfWidth / inSampleSize) >= reqWidth) {

            inSampleSize *= 2;
        }

        return inSampleSize;
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int[] newSize) {
        final boolean FILTER = true;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > newSize[0] || height > newSize[1]) {
            return Bitmap.createScaledBitmap(bitmap, newSize[0], newSize[1], FILTER);
        }

        return bitmap;
    }


    private int[] resizeToFitWidth(int width, int height, int maxWidth) {
        float ratio = (float) width / (float) height;

        int[] newSize = new int[2];

        newSize[0] = maxWidth;
        newSize[1] = (int) ((float) newSize[0] / ratio);

        if (newSize[0] < width) {
            return newSize;
        }
        else {
            int[] originalSize = new int[2];
            originalSize[0] = width;
            originalSize[1] = height;
            return originalSize;
        }
    }

    private void cleanUpAfterLoad(HttpURLConnection connection, InputStream input) {
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

    /**
     * Uses decoded bounds to determine optimal inSampleSize, loads image using HttpUrlConnection,
     * and returns decoded Bitmap.
     * @param src src of image to load
     * @param finalSize [width, height]
     * @return Bitmap
     */

    private Bitmap loadDownsampledBitmap(String src, int[] finalSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScreenDensity = mDefaultOptions.inScreenDensity;
        options.inTargetDensity =  mDefaultOptions.inTargetDensity;
        options.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        options.inSampleSize = calculateInSampleSize(finalSize[0], finalSize[1]);

        HttpURLConnection connection = null;
        InputStream input = null;

        try {
            URL url = new URL(src);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            input = connection.getInputStream();

            return BitmapFactory.decodeStream(input, null, options);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while loading image", e);
            return null;

        } finally {
            cleanUpAfterLoad(connection, input);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////

    private LoaderManager.LoaderCallbacks<Object> callbacks = new LoaderManager.LoaderCallbacks<Object>() {

        @Override
        public Loader<Object> onCreateLoader(int id, final Bundle args) {

            return new AsyncLoader(mContext, args) {

                @Override
                public Bitmap loadInBackground() {
                    return getDownsampledAndResizedBitmap(args);
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Object> loader, Object data) {
            int index = ((AsyncLoader) loader).getArgs().getInt("index");
            onFinishLoad((Bitmap) data, mPlaceholders[index]);
            mLoaderManager.destroyLoader(loader.getId());
            getNextFromQueue();
        }

        @Override
        public void onLoaderReset(Loader<Object> loader) {
            loader.reset();
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // Communication with other classes
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Called before loader is initialised/restarted
     * @param placeholder Placeholder image
     * @return false to interrupt process() method, true to continue execution
     */
    public boolean onPreLoad(ImagePlaceholderSpan placeholder) {
        return true;
    }

    /**
     * Called after each Bitmap has finished loading
     * @param bitmap Loaded image
     * @param placeholder Placeholder to be replaced
     */
    public void onFinishLoad(Bitmap bitmap, ImagePlaceholderSpan placeholder) {}

    /**
     * Interface used to communicate between classes after ImageLoader has finished loading.
     */
    public interface ImageLoaderListener {
        void onQueueComplete();
    }
}
