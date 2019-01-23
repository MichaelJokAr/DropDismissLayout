package com.github.jokar.dropdimisslayout;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.jokar.library.drop_dismiss.ContentSizeProvider;
import com.github.jokar.library.drop_dismiss.DropDismissCallbacks;
import com.github.jokar.library.drop_dismiss.DropDismissGestureListener;
import com.github.jokar.library.drop_dismiss.DropDismissLayout;
import com.github.jokar.library.drop_dismiss.InterceptResult;

import java.util.List;

/**
 * Create by JokAr. on 2019/1/23.
 */
public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder> {

    private LayoutInflater mInflater;
    private Context mContext;
    private List<String> mImageUrls;
    private ImageOnListener mOnListener;

    public ImageListAdapter(Context context, List<String> imageUrls) {
        mContext = context;
        mImageUrls = imageUrls;
        mInflater = LayoutInflater.from(mContext);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(viewGroup);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        Glide.with(mContext)
                .load(mImageUrls.get(position))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))

                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        viewHolder.mProgressBar.setVisibility(View.GONE);
                        viewHolder.mPhotoView.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        viewHolder.mProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onStart() {
                        super.onStart();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return mImageUrls != null ? mImageUrls.size() : 0;
    }

    public void setOnListener(ImageOnListener onListener) {
        mOnListener = onListener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public DropDismissLayout mDropDismissLayout;
        public PhotoView mPhotoView;
        public FrameLayout mRootLayout;
        private Drawable activityBackgroundDrawable;
        private ProgressBar mProgressBar;

        public ViewHolder(@NonNull ViewGroup parent) {
            super(mInflater.inflate(R.layout.item_image, parent,
                    false));
            mDropDismissLayout = itemView.findViewById(R.id.drop_dismiss_layout);
            mPhotoView = itemView.findViewById(R.id.photo_view);
            mRootLayout = itemView.findViewById(R.id.root_layout);
            mProgressBar = itemView.findViewById(R.id.progress_bar);
            initLayout();
        }

        private void initLayout() {
            mPhotoView.setOnPhotoTapListener(new OnPhotoTapListener() {
                @Override
                public void onPhotoTap(ImageView view, float x, float y) {
//                    finishInMillis(0);
                }
            });
            activityBackgroundDrawable = mRootLayout.getBackground().mutate();
            mDropDismissLayout.setGestureListener(gestureListener());
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

            DropDismissGestureListener gestureListener = new DropDismissGestureListener(mContext,
                    contentSizeProvider, callbacks) {

                @Override
                public InterceptResult gestureInterceptor(float scrollY) {
                    boolean isScrollingUpwards = scrollY < 0;
                    int directionInt = isScrollingUpwards ? -1 : 1;
                    boolean canPanFurther = mPhotoView.canScrollVertically(directionInt);
                    //只有在原尺寸大小下才能拖拽
                    if (mPhotoView.getScale() != 1.0f) {
                        return InterceptResult.INTERCEPTED;
                    } else {
                        return InterceptResult.IGNORED;
                    }
                }
            };

            return gestureListener;
        }

        private void finishInMillis(long animationDuration) {
            mRootLayout.setAlpha(0f);
            if (mOnListener != null) {
                mOnListener.finishInMillis(animationDuration);
            }
        }

        private void updateBackgroundDimmingAlpha(@FloatRange(from = 0.0, to = 1.0) float transparencyFactor) {
            float dimming = 1f - Math.min(1f, transparencyFactor * 2);
            activityBackgroundDrawable.setAlpha((int) (dimming * 255));
        }
    }

    public interface ImageOnListener {
        void finishInMillis(long animationDuration);
    }
}
