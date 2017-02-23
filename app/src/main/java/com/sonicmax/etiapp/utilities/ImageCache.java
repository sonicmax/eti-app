package com.sonicmax.etiapp.utilities;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ImageCache {
    private final String LOG_TAG = ImageCache.class.getSimpleName();
    // Constants for DiskLruCache
    private final int DISK_CACHE_INDEX = 0;
    private final int DISK_CACHE_SIZE = 1024 * 1024 * 50;
    private final int APP_VERSION = 1;
    private final int VALUE_COUNT = 1;
    private final String DISK_CACHE_SUBDIR = "images";

    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;

    public ImageCache(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        int memoryClass = am.getMemoryClass(); // Tells us how many MBs of RAM our app can use.

        // We probably can't use *all* of that RAM for the cache, so divide by a reasonable number
        final int memCacheSize = (memoryClass * 1000000) / 2;

        mMemoryCache = new LruCache<String, Bitmap>(memCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        // Get cache directory and initialize disk cache on background thread
        File cacheDir = getDiskCacheDir(context);
        new InitDiskCacheTask().execute(cacheDir);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public getters/setters
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds bitmap to memory and disk caches. Note that memory cache uses the full URL as
     * key, while disk cache uses md5 hash (taken from URL).
     * @param key URL of image to be cached
     * @param bitmap
     */
    public void addBitmapToCache(String key, Bitmap bitmap) {
        if (getBitmapFromCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }

        // Also add to disk cache
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                try {
                    String hash = getMd5FromUrl(key);
                    if (mDiskLruCache.get(hash) == null) {
                        addBitmapToDiskCache(hash, bitmap);
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error adding bitmap to disk cache", e);
                }
            }
        }
    }

    public Bitmap getBitmapFromCache(String key) {
        Bitmap bitmap = mMemoryCache.get(key);

        if (bitmap == null) {
            bitmap = getBitmapFromDiskCache(getMd5FromUrl(key));

            if (bitmap != null) {
                mMemoryCache.put(key, bitmap);
            }
            return bitmap; // Always return bitmap, even if it's null
        }

        else {
            return bitmap;
        }
    }

    public void clearLruCache() {
        mMemoryCache.evictAll();
    }

    public void closeDiskCache() {
        if (!mDiskLruCache.isClosed()) {
            try {
                mDiskLruCache.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error closing disk cache", e);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    private void addBitmapToDiskCache(final String key, final Bitmap value) {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                DiskLruCache.Editor editor;
                DiskLruCache.Snapshot snapshot = null;
                OutputStream out = null;

                try {
                    snapshot = mDiskLruCache.get(key);

                    if (snapshot == null) {
                        editor = mDiskLruCache.edit(key);

                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX);
                            value.compress(Bitmap.CompressFormat.PNG, 100, out);
                            editor.commit();
                        }
                    }

                    else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close();
                    }

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error adding bitmap to disk cache", e);

                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Error closing output stream" , e);
                        }
                    }

                    if (snapshot != null) {
                        snapshot.close();
                    }
                }
            }
        }
    }

    private Bitmap getBitmapFromDiskCache(String key) {
        synchronized (mDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mDiskCacheStarting) {

                try {
                    mDiskCacheLock.wait();

                } catch (InterruptedException e) {
                    // Do nothing
                }
            }

            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                DiskLruCache.Snapshot snapshot = null;
                InputStream input;
                try {
                    snapshot = mDiskLruCache.get(key);

                    if (snapshot != null) {
                        input = snapshot.getInputStream(DISK_CACHE_INDEX);
                        FileDescriptor fd = ((FileInputStream) input).getFD();
                        return BitmapFactory.decodeFileDescriptor(fd);
                    }

                    else {
                        return null;
                    }

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error getting bitmap from cache", e);

                } finally {
                    if (snapshot != null) {
                        snapshot.close();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Path of image URLs is always structured like this:
     * http://i2.endoftheinter.net/i/n/14142368fa22019b592f9303547c743f/ok_hand.png
     * where the second to last path segment contains the MD5 hash of the file.
     * @param url URL of image
     * @return MD5 hash from URL
     */
    private String getMd5FromUrl(String url) {
        Uri uri = Uri.parse(url);
        List<String> path = uri.getPathSegments();
        return path.get(path.size() - 2);
    }

    private class InitDiskCacheTask extends AsyncTask<File, Void, Void> {

        @Override
        protected Void doInBackground(File... params) {
            synchronized (mDiskCacheLock) {
                File cacheDir = params[0];
                try {
                    mDiskLruCache = DiskLruCache.open(cacheDir, APP_VERSION, VALUE_COUNT, DISK_CACHE_SIZE);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error initialising disk cache", e);
                }

                mDiskCacheStarting = false; // Finished initialization
                mDiskCacheLock.notifyAll(); // Wake any waiting threads
            }

            return null;
        }
    }

    private File getDiskCacheDir(Context context) {
        // Attempt to use external storage for cache, otherwise fallback on internal cache directory
        boolean hasExternalStorage =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !Environment.isExternalStorageRemovable();

        final String cachePath;

        if (hasExternalStorage && context.getExternalCacheDir() != null) {
            cachePath = context.getExternalCacheDir().getPath();
        }
        else {
            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + DISK_CACHE_SUBDIR);
    }
}
