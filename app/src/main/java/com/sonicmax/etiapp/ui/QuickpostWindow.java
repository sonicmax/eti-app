package com.sonicmax.etiapp.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.WindowManager;

import com.sonicmax.etiapp.R;

/**
 * Has same behaviour as PopupWindow but with some extra code for convenience
 */
public class QuickpostWindow extends android.widget.PopupWindow {

    private final Context mContext;
    private final WindowManager mWindowManager;

    /**
     * Create a new popup window which can display the contentView.
     * The dimension of the window must be passed to this constructor.
     *
     * The popup does not provide any background. This should be handled
     * by the content view.
     *
     * @param contentView the popup's content
     * @param width the popup's width
     * @param height the popup's height
     * @param focusable true if the popup can be focused, false otherwise
     */

    public QuickpostWindow(View contentView, int width, int height, boolean focusable) {
        // Same as Android source
        mContext = contentView.getContext();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        setContentView(contentView);
        setWidth(width);
        setHeight(height);
        setFocusable(focusable);

        // Set some other properties that we need for QuickpostWindow
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        setOutsideTouchable(true);
        setAnimationStyle(R.style.AnimationPopup);

        // Touch events won't work if background drawable is null - even if it's specified in XML.
        // See http://stackoverflow.com/a/3122696/3842017
        setBackgroundDrawable(new ColorDrawable());
    }
}
