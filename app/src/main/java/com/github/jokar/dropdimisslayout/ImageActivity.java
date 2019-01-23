package com.github.jokar.dropdimisslayout;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.jokar.library.drop_dismiss.ContentSizeProvider;
import com.github.jokar.library.drop_dismiss.DropDismissCallbacks;
import com.github.jokar.library.drop_dismiss.DropDismissGestureListener;
import com.github.jokar.library.drop_dismiss.DropDismissLayout;
import com.github.jokar.library.drop_dismiss.InterceptResult;

/**
 * Create by JokAr. on 2019/1/22.
 */
public class ImageActivity extends AppCompatActivity {

    private DropDismissLayout mDropDismissLayout;
    private PhotoView mPhotoView;
    private FrameLayout mRootLayout;
    private Drawable activityBackgroundDrawable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        mPhotoView = findViewById(R.id.photo_view);
        mDropDismissLayout = findViewById(R.id.drop_dismiss_layout);
        mRootLayout = findViewById(R.id.root_layout);
        animateDimmingOnEntry();
        mDropDismissLayout.setGestureListener(gestureListener());
        //
        loadImage();
    }

    private void loadImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPhotoView.setTransitionName("test");
        }
        Glide.with(this)
                .load("https://img01.sogoucdn.com/app/a/100520146/0e86cec2133ac65ea74dc6adbf2a7fc2")
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        mPhotoView.setImageDrawable(resource);
                    }
                });
    }

    private void animateDimmingOnEntry() {
        activityBackgroundDrawable = mRootLayout.getBackground().mutate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mRootLayout.setBackground(activityBackgroundDrawable);
        }
    }

    private void updateBackgroundDimmingAlpha(@FloatRange(from = 0.0, to = 1.0) float transparencyFactor) {
        float dimming = 1f - Math.min(1f, transparencyFactor * 2);
        activityBackgroundDrawable.setAlpha((int) (dimming * 255));
    }

    private DropDismissGestureListener gestureListener() {

        ContentSizeProvider contentSizeProvider = new ContentSizeProvider() {
            @Override
            public int heightForDismissAnimation() {
                return mPhotoView.getHeight();
            }

            @Override
            public int heightForCalculatingDismissThreshold() {
                return mPhotoView.getHeight() / 2;
            }
        };

        DropDismissCallbacks callbacks = new DropDismissCallbacks() {
            @Override
            public void onDismiss(long animationDuration) {
                finishInMillis(animationDuration);
            }

            @Override
            public void onMove(float moveRation) {
                updateBackgroundDimmingAlpha(Math.abs(moveRation));
            }
        };

        DropDismissGestureListener gestureListener = new DropDismissGestureListener(this,
                contentSizeProvider, callbacks) {

            @Override
            public InterceptResult gestureInterceptor(float scrollY) {
                boolean isScrollingUpwards = scrollY < 0;
                int directionInt = isScrollingUpwards ? -1 : 1;
                boolean canPanFurther = mPhotoView.canScrollVertically(directionInt);
                if (canPanFurther) {
                    return InterceptResult.INTERCEPTED;
                } else {
                    return InterceptResult.IGNORED;
                }
            }
        };

        return gestureListener;
    }

    private void finishInMillis(long animationDuration) {
        mRootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, animationDuration);
    }
}
