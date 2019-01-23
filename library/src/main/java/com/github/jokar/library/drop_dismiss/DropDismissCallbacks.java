package com.github.jokar.library.drop_dismiss;

import android.support.annotation.FloatRange;

/**
 * Create by JokAr. on 2019/1/21.
 */
public interface DropDismissCallbacks {

    /**
     * 当view消失时调用
     * @param animationDuration
     */
    void onDismiss(long animationDuration);

    /**
     * 当view移动时调用
     * @param moveRation
     */
    void onMove(@FloatRange(from = -1.0, to = 1.0) float moveRation);
}
