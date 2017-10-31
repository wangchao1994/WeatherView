package com.android.topview.view;

import com.android.topview.R;
import com.android.topview.R.anim;
import com.android.topview.R.layout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

/**
 * 自定义可扩展拉伸View
 * 
 * */
public class ExpandView extends FrameLayout{

    
    private Animation mExpandAnimation;
    private Animation mCollapseAnimation;
    private boolean mIsExpand;
    
    public ExpandView(Context context) {
        this(context,null);
        // TODO Auto-generated constructor stub
    }
    public ExpandView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
        // TODO Auto-generated constructor stub
    }
    public ExpandView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
        initExpandView();
    }
    
    private void initExpandView() {
        LayoutInflater.from(getContext()).inflate(R.layout.leftscreen_weather_future_layout, this, true);
        
        mExpandAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.leftscreen_open);
        mExpandAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // TODO Auto-generated method stub
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.VISIBLE);
            }
        });
        
        mCollapseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.leftscreen_close);
        mCollapseAnimation.setAnimationListener(new Animation.AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
                // TODO Auto-generated method stub
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.GONE);
            }
        });
        
    }
    public void collapse() {
        if (mIsExpand) {
            mIsExpand = false;
            clearAnimation();
            startAnimation(mCollapseAnimation);
        }
    }
    
    public void expand() {
        if (!mIsExpand) {
            mIsExpand = true;
            clearAnimation();
            startAnimation(mExpandAnimation);
        }
    }

    public boolean isExpand() {
        return mIsExpand;
    }
    
    @SuppressLint("InflateParams")
	public void setContentView(){
        View view = null;
        view = LayoutInflater.from(getContext()).inflate(R.layout.leftscreen_weather_future_layout, null);
        removeAllViews();
        addView(view);
    }

}