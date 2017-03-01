package com.sonicmax.etiapp.network;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.sonicmax.etiapp.utilities.DiskLruCache;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Caches responses from GET requests so we can quickly restore content.
 * Key is generated from URL path and query parameters (eg showmessages.php?topic=1&page=2)
 */

public class ResponseCache {
    private final String LOG_TAG = ResponseCache.class.getSimpleName();
    private final String ETI_URL = "https://endoftheinter.net";
    private final String BOARDS = "https://boards.endoftheinter.net/";

    // The average request seems to average at around 10kb, so 1MB cache storage is plenty
    private final int DISK_CACHE_SIZE = 1024 * 1024;

    private DiskLruCache mDiskLruCache;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;

    public ResponseCache(Context context) {
        // Get cache directory and initialize disk cache in background thread
        File cacheDir = getDiskCacheDir(context);
        new InitDiskCacheTask().execute(cacheDir);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public getters/setters
    ///////////////////////////////////////////////////////////////////////////

    public void cacheResponseData(String url, String html, int pageNumber, int adapterPosition) {
        ResponseCacheEntry request = new ResponseCacheEntry(url, html, pageNumber, adapterPosition);
        String key = getKeyFromUrl(url.replace(BOARDS, "").replace(ETI_URL, ""));
        addObjectToDiskCache(key, request);
    }

    public ResponseCacheEntry getResponseFromCache(String url) {
        String key = getKeyFromUrl(url.replace(BOARDS, "").replace(ETI_URL, ""));
        return (ResponseCacheEntry) getObjectFromDiskCache(key);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    private String getKeyFromUrl(String url) {
        try {
            byte[] data = url.getBytes("UTF-8");
            return Base64.encodeToString(data, Base64.NO_WRAP);

        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Error while encoding URL", e);
            return url;
        }
    }

    private File getDiskCacheDir(Context context) {
        final String DISK_CACHE_SUBDIR = "web";

        // Attempt to use external storage for cache, otherwise fallback on internal cache directory
        boolean hasExternalStorage =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !Environment.isExternalStorageRemovable();

        String cachePath;

        if (hasExternalStorage && context.getExternalCacheDir() != null) {
            cachePath = context.getExternalCacheDir().getPath();
        }
        else {
            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + DISK_CACHE_SUBDIR);
    }

    private void addObjectToDiskCache(String key, Object value) {
        final int DISK_CACHE_INDEX = 0;

        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                DiskLruCache.Editor editor;
                OutputStream outputStream = null;
                ObjectOutputStream objectOutputStream = null;

                try {
                    editor = mDiskLruCache.edit(key);
                    outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
                    objectOutputStream = new ObjectOutputStream(outputStream);
                    objectOutputStream.writeObject(value);
                    editor.commit();

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error adding request to disk cache", e);

                } finally {
                    closeStream(outputStream);
                    closeStream(objectOutputStream);
                }
            }
        }
    }

    private Object getObjectFromDiskCache(String key) {
        final int DISK_CACHE_INDEX = 0;

        synchronized (mDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mDiskCacheStarting) {

                try {
                    mDiskCacheLock.wait();

                } catch (InterruptedException e) {
                    // Continue with method
                }
            }

            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                DiskLruCache.Snapshot snapshot = null;
                InputStream inputStream = null;
                ObjectInputStream objectInputStream = null;

                try {
                    snapshot = mDiskLruCache.get(key);

                    if (snapshot != null) {
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        objectInputStream = new ObjectInputStream(inputStream);
                        return objectInputStream.readObject();
                    }

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error getting response from cache", e);

                } catch (ClassNotFoundException e) {
                    Log.e(LOG_TAG, "Error reading object from input stream", e);

                } finally {
                    closeStream(inputStream);
                    closeStream(objectInputStream);

                    if (snapshot != null) {
                        snapshot.close();
                    }
                }
            }
        }

        return null;
    }

    private void closeStream(Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error closing stream" , e);
        }
    }

    private class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        private final int APP_VERSION = 1;
        private final int VALUE_COUNT = 1;

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
}
