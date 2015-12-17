package com.sonicmax.etiapp.ui;

import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;

/**
 * Created by Max on 12/17/2015.
 */
public class QuickpostHandler {

    private final FloatingActionButton mQuickpostButton;

    public final PopupWindow.OnDismissListener dismissListener;

    public QuickpostHandler(FloatingActionButton quickpostButton) {
        this.mQuickpostButton = quickpostButton;
        this.dismissListener = new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setVisible();
            }
        };
    }

    public void setInvisible() {
        mQuickpostButton.setVisibility(View.INVISIBLE);
    }

    public void setVisible() {
        mQuickpostButton.setVisibility(View.VISIBLE);
    }
}
