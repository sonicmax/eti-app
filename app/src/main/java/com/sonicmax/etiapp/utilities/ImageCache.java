package com.sonicmax.etiapp.utilities;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.util.LruCache;

public class ImageCache {
    private final LruCache<String, BitmapDrawable> mMemoryCache;

    public ImageCache(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        int memoryClass = am.getMemoryClass(); // Tells us how many MBs of RAM our app can use.

        // We probably can't use *all* of that RAM for the cache, so divide by a reasonable number
        final int memCacheSize = (memoryClass * 1000000) / 2;

        mMemoryCache = new LruCache<String, BitmapDrawable>(memCacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable bitmap) {
                return bitmap.getBitmap().getByteCount() / 1024;
            }
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    // Getters/setters
    ///////////////////////////////////////////////////////////////////////////

    public void clearLruCache() {
        mMemoryCache.evictAll();
    }

    public void addBitmapToCache(String key, BitmapDrawable bitmap) {
        if (getBitmapFromCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public BitmapDrawable getBitmapFromCache(String key) {
        return mMemoryCache.get(key);
    }

}
