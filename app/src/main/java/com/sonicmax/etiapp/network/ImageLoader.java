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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoader {
    private final String LOG_TAG = ImageLoader.class.getSimpleName();
    private Context mContext;
    private LoaderManager mLoaderManager;
    private ImageSpan[] mImageSpans;


    public ImageLoader(Context context) {
        mContext = context;
        mLoaderManager = ((FragmentActivity) context).getSupportLoaderManager();
    }

    public void loadImages(SpannableStringBuilder message, int position) {
        mImageSpans = message.getSpans(0, message.length(), ImageSpan.class);

        for (int i = 0; i < mImageSpans.length; i++ ) {
            ImageSpan img = mImageSpans[i];
            if (onPreLoad(img)) {
                Bundle args = new Bundle(2);
                args.putString("src", img.getSource());
                args.putInt("index", i);

                // Concatenate position and index to create (probably unique) id for loader.
                int id = Integer.parseInt(Integer.toString(position) + "00" + Integer.toString(i));

                if (mLoaderManager.getLoader(id) == null) {
                    mLoaderManager.initLoader(id, args, callbacks).forceLoad();
                } else {
                    mLoaderManager.restartLoader(id, args, callbacks).forceLoad();
                }
            }
        }
    }

    /**
     * Called before loader is initialised/restarted (eg. to check for cached version of image)
     * @param img Placeholder image
     * @return false to interrupt process() method, true to continue execution
     */
    public boolean onPreLoad(ImageSpan img) {
        return true;
    }

    /**
     * Called after each image in message has finished loading
     * @param bitmap Loaded image
     * @param imageSpan Original placeholder ImageSpan
     */
    public void onFinishLoad(Bitmap bitmap, ImageSpan imageSpan) {
        // Override me
    }

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
            // We won't need to use this loader again
            mLoaderManager.destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<Object> loader) {
            loader.reset();
        }
    };
}
