package com.github.jokar.dropdimisslayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by JokAr. on 2019/1/23.
 */
public class ImageListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ImageListAdapter mImageListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,
                false));
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(mRecyclerView);

        setData();
    }

    private void setData() {
        List<String> imageUrls = new ArrayList<>();
        imageUrls.add("https://img04.sogoucdn.com/app/a/100520146/654a788feb0da1415df1c1a708ad537c");
        imageUrls.add("https://img01.sogoucdn.com/app/a/100520021/687957033be1a2bb28cf7b459593ab8d");
        imageUrls.add("https://img01.sogoucdn.com/app/a/100520021/410e833885859062d305415733816b98");
        imageUrls.add("https://img01.sogoucdn.com/app/a/100520021/78ab14291893897b0ea6d4d34acbcea1");
        imageUrls.add("https://img04.sogoucdn.com/app/a/100520021/89e48bb190d666c1ef5a8b4a687cc616");

        mImageListAdapter = new ImageListAdapter(this, imageUrls);
        mImageListAdapter.setOnListener(new ImageListAdapter.ImageOnListener() {
            @Override
            public void finishInMillis(long animationDuration) {
                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                },animationDuration);
            }
        });
        mRecyclerView.setAdapter(mImageListAdapter);
    }
}
