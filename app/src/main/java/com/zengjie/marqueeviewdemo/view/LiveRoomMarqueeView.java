package com.zengjie.marqueeviewdemo.view;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.zengjie.marqueeviewdemo.R;
import com.zengjie.marqueeviewdemo.utils.common.CommonUtils;


/**
 * 直播间标题跑马灯动画，可配置文字大小、gravity、style、文字颜色等属性
 * 可以配置动画的延迟、速度、两行间距，可拓展动画的开始、停止等
 *
 * Created by zengjie on 2020/07/10 10:00
 */
public class LiveRoomMarqueeView extends View implements IMarqueeView{

    private float density = 2.0f;
    private float scaleDensity = 2.0f;
    private final int mGravityStart = 1;
    private final int mGravityCenterHorizontal = 2;
    private int mGravity = mGravityStart;
    //font size
    private float mTextSize = 33.0f;
    //font color
    private int mTextColor = Color.parseColor("#000000");
    //style
    private Typeface mTypeFace = Typeface.DEFAULT;

    //文本
    private String mText = "";
    //compute text width if txtWidth>width  user marquee
    private int mTxtWidth = 0;
    //shadow,if background is not color , that is not useful
    private float mShadowWidth = 0f;
    //the system marquee textview is 12L
    private long mSpeed = 12L;
    //animation delay
    private long mAnimDelay = 1000L;
    //between two texts margin
    private float mMargin = 0f;
    //0 text 1 marquee
    private int mShowMode = 0;
    private static final int MARQUEE_MODE = 1;
    private static final int NORMAL_MODE = 0;
    private ValueAnimator mAnim;
    private int mAnimValue = 0;

    private LinearGradient leftShadow;
    private LinearGradient rightShadow;

    private Rect mPaddingRect = new Rect();
    private Paint shadowPaint = new Paint();
    private TextPaint mTextPaint;

    public LiveRoomMarqueeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveRoomMarqueeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
        density = dm.density;
        scaleDensity = dm.scaledDensity;

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.LiveRoomMarqueeView, defStyleAttr, defStyleAttr
        );

        mShadowWidth = a.getDimension(R.styleable.LiveRoomMarqueeView_shadow_width, CommonUtils.dip2px(getContext(), 14f));
        mMargin = a.getDimension(R.styleable.LiveRoomMarqueeView_margin_txt, CommonUtils.dip2px(getContext(), 133f));
        mSpeed = a.getInt(R.styleable.LiveRoomMarqueeView_speed, 12);
        mAnimDelay = a.getInt(R.styleable.LiveRoomMarqueeView_delay, 1000);
        mGravity = a.getInt(R.styleable.LiveRoomMarqueeView_gravity, 1);
        mTextColor = a.getColor(R.styleable.LiveRoomMarqueeView_textColor, Color.parseColor("#000000"));
        mTextSize = a.getDimension(R.styleable.LiveRoomMarqueeView_textSize, CommonUtils.sp2px(getContext(), 12f));
        int textStyle = a.getInt(R.styleable.LiveRoomMarqueeView_textStyle, 1);
        String text = a.getString(R.styleable.LiveRoomMarqueeView_text);
        a.recycle();
        initTextPaint(text,textStyle);
    }

    private void initTextPaint(String text, int textStyle){
        //初始化TextPaint
        if (textStyle == 1) {
            mTypeFace = Typeface.DEFAULT;
        } else if (textStyle == 2) {
            mTypeFace = Typeface.DEFAULT_BOLD;
        } else if (textStyle == 3) {
            mTypeFace = Typeface.defaultFromStyle(Typeface.ITALIC);
        } else if (textStyle == 4) {
            mTypeFace = Typeface.defaultFromStyle(Typeface.BOLD_ITALIC);
        }
        mTextPaint = new TextPaint();
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTypeface(mTypeFace);
        mTextPaint.setAntiAlias(true);
        setText(text);
    }

    @Override
    public void setText(String text) {
        if (TextUtils.equals(mText,text)) {
            return;
        }
        this.mText = text;
        stopAnim();
        post(new Runnable() {
            @Override
            public void run() {
                if (getVisibility() == VISIBLE) {
                    //initShadow();
                    measureTxt();
                    switchShowMode();
                    showIfNeed();
                    //确保setText后触发绘制
                    invalidate();
                }
            }
        });
    }

    private String getText() {
        return mText;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (this.getVisibility() != visibility) {
            if (visibility == View.VISIBLE) {
                setText(mText);
            } else {
                stopAnim();
            }
        }
    }

    private void handleShow() {
        mAnimValue = 0;
        if (mShowMode == NORMAL_MODE) {
            invalidate();
        } else {
            invalidate();
            startAnim();
        }
    }

    private void startAnim() {
        stopAnim();
        mAnim = ValueAnimator.ofInt(0, (mTxtWidth + (int) mMargin));
        if (mAnim != null) {
            mAnim.setDuration((long) (mTxtWidth + mMargin) * mSpeed);
            mAnim.setInterpolator(new LinearInterpolator());
            mAnim.setRepeatCount(0);
            mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (mAnim != null){
                        if (mShowMode == 0) {
                            mAnim.cancel();
                            mAnimValue = 0;
                        } else {
                            mAnimValue = (int) mAnim.getAnimatedValue();
                        }
                        invalidate();
                    }
                }
            });

            mAnim.addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    showIfNeed();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            mAnim.setStartDelay(mAnimDelay);
            mAnim.start();
        }
    }

    /**
     *停止动画
     */
    private void stopAnim() {
        if (mAnim != null && mAnim.isRunning()) {
            mAnim.removeAllListeners();
            mAnim.removeAllUpdateListeners();
            mAnim.cancel();
            mAnim = null;
            mAnimValue = 0;
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float x;
        if (mShowMode == MARQUEE_MODE || mGravity == mGravityStart) {
            x = -(float) mAnimValue + getPaddingStart();
        } else {
            x = getPaddingStart() + (getWidth() - getPaddingStart() - getPaddingEnd() - mTxtWidth) / 2f;
        }
        mPaddingRect.left = getPaddingStart();
        mPaddingRect.top = getPaddingTop();
        mPaddingRect.right = getWidth() - getPaddingEnd();
        mPaddingRect.bottom = getHeight() - getPaddingBottom();
        // 计算Baseline绘制的Y坐标 ，计算方式：画布高度的一半 - 文字总高度的一半
        int baseY = (int) (((canvas.getHeight() - (mTextPaint.descent() + mTextPaint.ascent())) / 2));
        //先绘制一行，与后面绘制的形成联动，达到跑马灯的效果
        if (canvas != null && mText != null) {
            canvas.clipRect(mPaddingRect);
            canvas.drawText(mText, x, baseY, mTextPaint);
        }

        //处理跑马灯动画
        if (mShowMode == MARQUEE_MODE) {
            float y = x + mMargin + mTxtWidth;
            if (canvas != null && mText != null) {
                canvas.drawText(mText, y, baseY, mTextPaint);
            }
        }
    }


    private void switchShowMode() {
        if (mTxtWidth + getPaddingStart() + getPaddingEnd() + mTextSize > getWidth()) {
            //跑马灯模式
            mShowMode = MARQUEE_MODE;
        } else {
            //正常显示
            mShowMode = NORMAL_MODE;
        }
    }

    //compute txt width
    private void measureTxt() {
        if (!TextUtils.isEmpty(getText())){
            mTxtWidth = (int) mTextPaint.measureText(getText());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
//        EventBus.getDefault().register(this);
        resume();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pause();
//        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightSpecMode != MeasureSpec.EXACTLY) {
            int tMin = CommonUtils.dip2px(getContext(), 3f);
            int pTop = getPaddingTop() < tMin ? tMin : getPaddingTop();
            int pBottom = getPaddingBottom() < tMin ? tMin : getPaddingBottom();
            setMeasuredDimension(widthSpecSize, ((int) mTextSize + pTop + pBottom));
        }
    }

    private void pause() {
        if (mShowMode == NORMAL_MODE){
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (mAnim != null && mAnim.isRunning()) {
                mAnim.pause();
            }
        } else {
            //兼容sdk 19以下
            stopAnim();
        }
    }

    private void resume() {
        if (mShowMode == NORMAL_MODE) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (mAnim != null) {
                mAnim.resume();
            } else {
                showIfNeed();
            }
        } else {
            //兼容sdk 19以下
            startAnim();
        }

    }

    private void showIfNeed(){
        if (mShowMode == NORMAL_MODE){
            return;
        }
        handleShow();
    }
}