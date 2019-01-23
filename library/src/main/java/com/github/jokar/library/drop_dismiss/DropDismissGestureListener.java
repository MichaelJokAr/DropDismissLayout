package com.github.jokar.library.drop_dismiss;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Create by JokAr. on 2019/1/21.
 */
public class DropDismissGestureListener implements View.OnTouchListener {
    private final Context mContext;
    private final ContentSizeProvider mContentSizeProvider;
    private final DropDismissCallbacks mDropDismissCallbacks;
    //
    private final FastOutSlowInInterpolator ANIM_INTERPOLATOR = new FastOutSlowInInterpolator();
    private final float DEFAULT_FLICK_THRESHOLD = 0.3f;
    /**
     * 触发移动事件的最小距离
     */
    private final int mTouchSlop;
    /**
     * 表示飞速滑动的最大初始速率值
     */
    private final int mMaximumFlingVelocity;
    //
    private ViewConfiguration mViewConfiguration;
    private float mThresholdSlop = DEFAULT_FLICK_THRESHOLD;

    //
    private float mDownX = 0f;
    private float mDownY = 0f;
    private float mLastTouchX = 0f;
    private float mLastTouchY = 0f;
    private int mLastAction = -1;
    private VelocityTracker mVelocityTracker;
    private boolean mVerticalScrollRegistered = false;
    private boolean mGestureCanceledUntilNextTouchDown = false;
    private boolean mGestureInterceptedUntilNextTouchDown = false;
    private ObjectAnimator mBackToPositionAnimation;
    private ObjectAnimator mAutoDismissAnimation;

    public DropDismissGestureListener(Context context,
                                      ContentSizeProvider contentSizeProvider,
                                      DropDismissCallbacks dropDismissCallbacks) {
        mContext = context;
        mContentSizeProvider = contentSizeProvider;
        mDropDismissCallbacks = dropDismissCallbacks;
        //
        mViewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = mViewConfiguration.getScaledTouchSlop();
        mMaximumFlingVelocity = mViewConfiguration.getScaledMaximumFlingVelocity();
    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float touchX = event.getRawX();
        float touchY = event.getRawY();

        float distanceX = touchX - mDownX;
        float distanceY = touchY - mDownY;
        float distanceXAbs = Math.abs(distanceX);
        float distanceYAbs = Math.abs(distanceY);
        float deltaX = touchX - mLastTouchX;
        float deltaY = touchY - mLastTouchY;

        if (touchX == mLastTouchX && touchY == mLastTouchY && mLastAction == event.getAction()) {
            return false;
        }

        mLastTouchX = touchX;
        mLastTouchY = touchY;
        mLastAction = event.getAction();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mDownX = touchX;
                mDownY = touchY;
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(event);
                return false;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mGestureCanceledUntilNextTouchDown || mGestureInterceptedUntilNextTouchDown) {
                    return false;
                }

                //
                if (!mVerticalScrollRegistered && gestureInterceptor(distanceY) == InterceptResult.INTERCEPTED) {
                    mGestureInterceptedUntilNextTouchDown = true;
                    return false;
                }
                boolean isScrollingVertically = distanceYAbs > mTouchSlop && distanceYAbs > distanceXAbs;
                boolean isScrollingHorizontally = distanceXAbs > mTouchSlop && distanceXAbs > distanceYAbs;

                if (!mVerticalScrollRegistered && isScrollingHorizontally) {
                    mGestureCanceledUntilNextTouchDown = true;
                    return false;
                }

                if (mVerticalScrollRegistered || isScrollingVertically) {
                    mVerticalScrollRegistered = true;
                    v.setTranslationX(v.getTranslationX() + deltaX);
                    v.setTranslationY(v.getTranslationY() + deltaY);

                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    v.setPivotY(0f);

                    dispatchOnLayoutMoveCallback(v);

                    mVelocityTracker.addMovement(event);

                    return true;
                } else {
                    return false;
                }
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mVerticalScrollRegistered) {
                    boolean registered = hasFingerMovedEnoughToLayout(distanceYAbs);
                    //是否往下滑
                    boolean wasSwipeDownwards = distanceY > 0;

                    if (registered) {
                        animateDismissal(v, wasSwipeDownwards);
                    } else {
                        mVelocityTracker.computeCurrentVelocity(1000);
                        float yVelocityAbs = Math.abs(mVelocityTracker.getYVelocity());
                        float requiredYVelocity = v.getHeight() * 6 / 10;
                        float minSwipeDistanceForFling = v.getHeight() / 10;

                        if (yVelocityAbs > requiredYVelocity
                                && distanceYAbs >= minSwipeDistanceForFling
                                && yVelocityAbs < mMaximumFlingVelocity) {
                            animateDismissal(v, wasSwipeDownwards, 100);
                        } else {
                            animateViewBackToPosition(v);
                        }
                    }
                }
                mVelocityTracker.recycle();
                mVerticalScrollRegistered = false;
                mGestureCanceledUntilNextTouchDown = false;
                mGestureInterceptedUntilNextTouchDown = false;
                return false;
            }
        }

        return false;
    }

    private void animateDismissal(View v, boolean wasSwipeDownwards) {
        animateDismissal(v, wasSwipeDownwards, 200);
    }

    private void animateDismissal(final View view, boolean downwards, final long flickAnimDuration) {
        if (view.getPivotY() != 0f) {
            throw new RuntimeException();
        }

        float rotationAngle = view.getRotation();
        int distanceRotated = (int) Math.ceil(Math.abs(Math.sin(Math.toRadians(rotationAngle))
                * view.getWidth() / 2));
        int throwDistance = distanceRotated + Math.max(mContentSizeProvider.heightForDismissAnimation(),
                view.getRootView().getHeight());

        if (mAutoDismissAnimation != null && mAutoDismissAnimation.isRunning()) {
            mAutoDismissAnimation.cancel();
        }
        mAutoDismissAnimation = ObjectAnimator.ofFloat(view,
                View.TRANSLATION_Y, downwards ? throwDistance : -throwDistance)
                .setDuration(flickAnimDuration);
        mAutoDismissAnimation.setInterpolator(ANIM_INTERPOLATOR);
        mAutoDismissAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                dispatchOnLayoutMoveCallback(view);
            }
        });
        mAutoDismissAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mDropDismissCallbacks.onDismiss(flickAnimDuration);
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mAutoDismissAnimation.start();
    }

    /**
     * 返回原来位置动画
     *
     * @param view
     */
    private void animateViewBackToPosition(final View view) {
        if (mBackToPositionAnimation != null && mBackToPositionAnimation.isRunning()) {
            mBackToPositionAnimation.cancel();
        }
        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                dispatchOnLayoutMoveCallback(view);
            }
        };
        mBackToPositionAnimation = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f),
                PropertyValuesHolder.ofFloat(View.ROTATION, 0f));
        mBackToPositionAnimation.addUpdateListener(updateListener);
        mBackToPositionAnimation.setInterpolator(ANIM_INTERPOLATOR);
        mBackToPositionAnimation.setDuration(200);
        mBackToPositionAnimation.start();
    }

    private boolean hasFingerMovedEnoughToLayout(float distanceYAbs) {
        return distanceYAbs > (mContentSizeProvider.heightForCalculatingDismissThreshold() *
                mThresholdSlop);
    }

    private void dispatchOnLayoutMoveCallback(View view) {
        float moveRation = view.getTranslationY() / view.getHeight();
        mDropDismissCallbacks.onMove(moveRation);
    }

    public InterceptResult gestureInterceptor(float scrollY) {
        return InterceptResult.IGNORED;
    }

    public void onDetachedFromWindow() {
        if (mBackToPositionAnimation != null) {
            mBackToPositionAnimation.removeAllListeners();
            if (mBackToPositionAnimation.isRunning()) {
                mBackToPositionAnimation.cancel();
            }
        }

        mBackToPositionAnimation = null;
        if (mAutoDismissAnimation != null) {
            if (mAutoDismissAnimation.isRunning()) {
                mAutoDismissAnimation.cancel();
            }
            mAutoDismissAnimation.removeAllListeners();
        }

        mAutoDismissAnimation = null;
    }
}
