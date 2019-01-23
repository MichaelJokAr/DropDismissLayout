package com.github.jokar.library.drop_dismiss;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Create by JokAr. on 2019/1/21.
 */
public class DropDismissLayout extends FrameLayout {
    private DropDismissGestureListener mGestureListener;

    public DropDismissLayout(@NonNull Context context) {
        super(context);
    }

    public DropDismissLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DropDismissLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = getGestureListener().onTouch(this, ev);
        return intercept || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getGestureListener().onTouch(this, event);

        return true;
    }

    private DropDismissGestureListener getGestureListener() {
        if (mGestureListener == null) {
            throw new RuntimeException();
        }
        return mGestureListener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mGestureListener != null){
            mGestureListener.onDetachedFromWindow();
        }
    }

    public void setGestureListener(DropDismissGestureListener gestureListener) {
        mGestureListener = gestureListener;
    }
}
