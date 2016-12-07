package com.ryin.playfulview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.ryin.playfulview.ruler.OnRulerChangeListener;
import com.ryin.playfulview.ruler.RulerView;

public class MainActivity extends AppCompatActivity implements OnRulerChangeListener {
    TextView textView;
    RulerView rulerView;
    private int currLocation = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rulerView = (RulerView) findViewById(R.id.rulerView);
        rulerView.setOnRulerChangeListener(this);
        rulerView.setCurrLocation(currLocation);
        textView = (TextView) findViewById(R.id.tvValue);
    }

    @Override
    public void onChanged(int newValue) {
        textView.setText(newValue+"");
    }

    package com.benba.lyqb.controls.ruler;

    import android.animation.Animator;
    import android.animation.ValueAnimator;
    import android.content.Context;
    import android.graphics.Canvas;
    import android.graphics.Color;
    import android.graphics.Paint;
    import android.graphics.Path;
    import android.util.AttributeSet;
    import android.util.Log;
    import android.view.MotionEvent;
    import android.view.VelocityTracker;
    import android.view.View;
    import android.view.ViewConfiguration;
    import android.view.animation.DecelerateInterpolator;
    import android.widget.Scroller;

    import com.benba.lyqb.R;


    /**
     * TODO
     * 1. 配置属性添加到style.xml里面
     * 2. 设置start和end value可以为小数
     * Created by ScorpioNeal on 15/8/24.
     */
    public class RulerView extends View {

        private static final String TAG = RulerView.class.getSimpleName();

        private Context mContext;


        /**
         * 短线的高度
         */
        private float mShortLineHeight;
        /**
         * 长线的高度
         */
        private float mHighLineHeight;
        /**
         * 短线的宽度
         */
        private float mShortLineWidth;
        /**
         * 长线的宽度
         */
        private float mHighLineWidth;
        /**
         * 两个长线间间隔数量
         */
        private int mSmallPartitionCount;
        /**
         * 指示器的宽度的一半
         */
        private float mIndicatorHalfWidth;
        /**
         * 指示器数字距离上边的距离
         */
        private float mIndicatorTextTopMargin;
        /**
         * 短线长线的上边距
         */
        private float mLineTopMargin;
        /**
         * 起止数值, 暂定为int
         */
        private int mStartValue;
        private int mEndValue;

        /**
         * 当前最大值
         */
        private int mMaxValue;

        /**
         * 当前最大值paint
         */
        private Paint mMaxPaint;

        /**
         * 两个长线之间相差多少值 暂定为int
         */
        private int mPartitionValue;
        /**
         * 长线间隔宽度
         */
        private float mPartitionWidth;
        /**
         * 设置的初始值
         */
        private int mOriginValue;
        private int mOriginValueSmall;
        /**
         * 当前值
         */
        private int mCurrentValue;
        /**
         * 刻度的大小
         */
        private int mScaleTextsize;
        /**
         * 最小速度
         */
        protected int mMinVelocity;

        private Paint mBgPaint;
        private Paint mShortLinePaint;
        private Paint mHighLinePaint;
        private Paint mGrayBottomLinePaint;
        private Paint mIndicatorTxtPaint;
        private Paint mIndicatorViewPaint;
        private Paint mIndicatorGrayPaint;


        //往右边去能偏移的最大值
        private float mRightOffset;
        //往左边去能偏移的最大值
        private float mLeftOffset;
        //移动的距离
        private float mMoveX = 0f;

        private float mWidth, mHeight;

        private Scroller mScroller;
        protected VelocityTracker mVelocityTracker;

        private OnValueChangeListener listener;

        public interface OnValueChangeListener {
            void onValueChange(int intVal, int fltval);
        }

        public void setValueChangeListener(OnValueChangeListener listener) {
            this.listener = listener;
        }

        public RulerView(Context context) {
            this(context, null);
        }

        public RulerView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public RulerView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            this.mContext = context;

            mScroller = new Scroller(context);

            mMinVelocity = ViewConfiguration.get(getContext())
                    .getScaledMinimumFlingVelocity();

            initValue();

            initPaint();

        }

        private void initPaint() {
            mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBgPaint.setColor(Color.argb(255, 224, 95, 23));

            mShortLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mShortLinePaint.setColor(getResources().getColor(R.color.less_grey_line));
            mShortLinePaint.setStrokeWidth(mShortLineWidth);

            mHighLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mHighLinePaint.setColor(getResources().getColor(R.color.less_grey_line));
            mHighLinePaint.setStrokeWidth(mHighLineWidth);

            mIndicatorTxtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mIndicatorTxtPaint.setColor(getResources().getColor(R.color.less_grey_line));
            mIndicatorTxtPaint.setTextSize(mScaleTextsize);

            mIndicatorViewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mIndicatorViewPaint.setColor(getResources().getColor(R.color.less_grey_line));

            mMaxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mMaxPaint.setColor(getResources().getColor(R.color.less_grey_line));
            mMaxPaint.setTextSize(mScaleTextsize);

            mIndicatorGrayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mIndicatorGrayPaint.setColor(getResources().getColor(R.color.less_grey_line));
            mIndicatorGrayPaint.setStrokeWidth(mShortLineWidth);

            mGrayBottomLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mGrayBottomLinePaint.setColor(getResources().getColor(R.color.less_grey_line));
            mGrayBottomLinePaint.setStrokeWidth(mShortLineWidth);
        }

        private void initValue() {
            mIndicatorHalfWidth = RulerUtils.convertDpToPixel(mContext, 9);
            mPartitionWidth = RulerUtils.convertDpToPixel(mContext, 140.3f);
            mHighLineWidth = RulerUtils.convertDpToPixel(mContext, 1);
            mShortLineWidth = RulerUtils.convertDpToPixel(mContext, 1);
            mLineTopMargin = RulerUtils.convertDpToPixel(mContext, 20f);
            mHighLineHeight = RulerUtils.convertDpToPixel(mContext, 50f);
            mShortLineHeight = RulerUtils.convertDpToPixel(mContext, 20f);
            mIndicatorTextTopMargin = RulerUtils.convertDpToPixel(mContext, 15f);

            mSmallPartitionCount = 3;
            mOriginValue = 100;
            mOriginValueSmall = 0;
            mPartitionValue = 10;
            mStartValue = 50;
            mEndValue = 250;
            mScaleTextsize = 44;

            mMaxValue = mOriginValue;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            drawBackground(canvas);

            drawIndicator(canvas);

            drawLinePartition(canvas);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            mWidth = getMeasuredWidth();
            mHeight = getMeasuredHeight();
        }

        /**
         * 画背景
         *
         * @param canvas
         */
        private void drawBackground(Canvas canvas) {
//        canvas.drawRect(0, 0, mWidth, mHeight, mBgPaint);


            canvas.drawLine(0, mHeight, mWidth, mHeight, mHighLinePaint);
            //绘制选中的背景
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setStyle(Paint.Style.FILL);
//        paint.setColor(Color.RED);
//        canvas.drawRect(mOriginValue, mHeight - mShortLineHeight * 2, mEndValue, mHeight, paint);
        }

        /**
         * 画指示器
         *
         * @param canvas
         */
        private void drawIndicator(Canvas canvas) {
//        Path path = new Path();
//        path.moveTo(mWidth / 2 - mIndicatorHalfWidth, 0);
//        path.lineTo(mWidth / 2, mIndicatorHalfWidth);
//        path.lineTo(mWidth / 2 + mIndicatorHalfWidth, 0);
//        canvas.drawPath(path, mIndicatorViewPaint);

            Path path = new Path();
            path.moveTo(mWidth / 2 - mIndicatorHalfWidth, 0);
            path.lineTo(mWidth / 2, mIndicatorHalfWidth);
            path.lineTo(mWidth / 2 + mIndicatorHalfWidth, 0);
            canvas.drawPath(path, mIndicatorViewPaint);

            canvas.drawLine(mWidth / 2, 0, mWidth / 2, mHeight, mHighLinePaint);
        }

        private float mOffset = 0f;

        private void drawLinePartition(Canvas canvas) {
            //计算半个屏幕能有多少个partition
            int halfCount = (int) (mWidth / 2 / mPartitionWidth);
            //根据偏移量计算当前应该指向什么值
            int move = Math.round((mMoveX / mPartitionWidth));
            mCurrentValue = mOriginValue - move * mPartitionValue;
            //相对偏移量是多少, 相对偏移量就是假设不加入数字来指示位置， 范围是0 ~ mPartitionWidth的偏移量

            mOffset = mMoveX - move * mPartitionWidth;

            Log.e("mOffset", "b>>>>>>>>>>>" + move);
            Log.e("mOffset", "mCurrentValue>>>>>>>>>>>>>" + mCurrentValue);
            Log.e("mOffset", "mPartitionValue>>>>>>>>>>>>>" + mPartitionValue);
            Log.e("mOffset", "mOriginValue>>>>>>>>>>>>>" + mOriginValue);
            Log.e("mOffset", "mMoveX>>>>>>>>>>>>>" + mMoveX);
            Log.e("mOffset", "mPartitionWidth>>>>>>>>>>>>>" + mPartitionWidth);
            Log.e("mOffset", "mOffset>>>>>>>>>>>>>" + mOffset);

            if (null != listener) {
                listener.onValueChange(mCurrentValue, -(int) (mOffset / (mPartitionWidth / mSmallPartitionCount)));
            }

            // draw high line and  short line
            for (int i = -halfCount - 1; i <= halfCount + 1; i++) {
                int val = mCurrentValue + i * mPartitionValue;
                //只绘出范围内的图形
                if (val >= mStartValue && val <= mEndValue) {
                    //画长的刻度
                    float startx = mWidth / 2 + mOffset + i * mPartitionWidth;
                    if (startx > 0 && startx < mWidth) {

                        //画刻度值
                        canvas.drawText(val + "", mWidth / 2 + mOffset + i * mPartitionWidth - mIndicatorTxtPaint.measureText(val + "") / 2,
                                0 + mLineTopMargin + RulerUtils.calcTextHeight(mIndicatorTxtPaint, val + ""), val <= mMaxValue ? mIndicatorTxtPaint : mMaxPaint);

                        canvas.drawLine(mWidth / 2 + mOffset + i * mPartitionWidth, 0 + mLineTopMargin + mIndicatorTextTopMargin + RulerUtils.calcTextHeight(mIndicatorTxtPaint, val + ""),
                                mWidth / 2 + mOffset + i * mPartitionWidth, mHeight, val <= mMaxValue ? mHighLinePaint : mIndicatorGrayPaint);

                        if (val > mMaxValue) {
                            canvas.drawLine(mWidth / 2 + mOffset + (i - 1) * mPartitionWidth, mHeight,
                                    mWidth, mHeight, mGrayBottomLinePaint);
                            canvas.drawRect(mWidth / 2 + mOffset + (i - 1) * mPartitionWidth, mHeight - 10,
                                    mWidth, mHeight, mGrayBottomLinePaint);
                        }

                    }

                    //画短的刻度
                    if (val != mEndValue) {
                        for (int j = 1; j < mSmallPartitionCount; j++) {
                            float start_x = mWidth / 2 + mOffset + i * mPartitionWidth + j * mPartitionWidth / mSmallPartitionCount;
                            if (start_x > 0 && start_x < mWidth) {
                                canvas.drawLine(mWidth / 2 + mOffset + i * mPartitionWidth + j * mPartitionWidth / mSmallPartitionCount, 0 + mLineTopMargin + 1.5f*mIndicatorTextTopMargin + RulerUtils.calcTextHeight(mIndicatorTxtPaint, val + ""),
                                        mWidth / 2 + mOffset + i * mPartitionWidth + j * mPartitionWidth / mSmallPartitionCount, mHeight, val < mMaxValue ? mShortLinePaint : mIndicatorGrayPaint);
                            }
                        }
                    }

                }

            }
        }

        private boolean isActionUp = false;
        private float mLastX;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            float xPosition = event.getX();

            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(event);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isActionUp = false;
                    mScroller.forceFinished(true);
                    if (null != animator) {
                        animator.cancel();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    isActionUp = false;
                    float off = xPosition - mLastX;

                    if ((mMoveX <= mRightOffset) && off < 0 || (mMoveX >= mLeftOffset) && off > 0) {

                    } else {
                        mMoveX += off;
                        postInvalidate();
                    }

                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isActionUp = true;
                    f = true;
                    countVelocityTracker(event);
                    return false;
                default:
                    break;
            }

            mLastX = xPosition;
            return true;
        }

        private ValueAnimator animator;

        private boolean isCancel = false;

        private void startAnim() {
            isCancel = false;
            float smallWidth = mPartitionWidth;
//        float smallWidth = mPartitionWidth / mSmallPartitionCount;
            float neededMoveX;
            if (mMoveX < 0) {
                neededMoveX = (int) (mMoveX / smallWidth - 0.5f) * smallWidth;
            } else {
                neededMoveX = (int) (mMoveX / smallWidth + 0.5f) * smallWidth;
            }
            animator = new ValueAnimator().ofFloat(mMoveX, neededMoveX);
            animator.setDuration(1000);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (!isCancel) {
                        mMoveX = (float) animation.getAnimatedValue();
                        postInvalidate();
                    }
                }
            });
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {

                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    isCancel = true;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        }

        private boolean f = true;

        @Override
        public void computeScroll() {
            super.computeScroll();
            if (mScroller.computeScrollOffset()) {
                float off = mScroller.getFinalX() - mScroller.getCurrX();
                off = off * functionSpeed();
                if ((mMoveX <= mRightOffset) && off < 0) {
                    mMoveX = mRightOffset;
                } else if ((mMoveX >= mLeftOffset) && off > 0) {
                    mMoveX = mLeftOffset;
                } else {
                    mMoveX += off;
                    if (mScroller.isFinished()) {
                        startAnim();
                    } else {
                        postInvalidate();
                        mLastX = mScroller.getFinalX();
                    }
                }

            } else {
                if (isActionUp && f) {
                    startAnim();
                    f = false;
                }
            }
        }

        /**
         * 控制滑动速度
         *
         * @return
         */
        private float functionSpeed() {
            return 0.2f;
        }

        private void countVelocityTracker(MotionEvent event) {
            mVelocityTracker.computeCurrentVelocity(1000, 3000);
            float xVelocity = mVelocityTracker.getXVelocity();
            if (Math.abs(xVelocity) > mMinVelocity) {
                mScroller.fling(0, 0, (int) xVelocity, 0, Integer.MIN_VALUE,
                        Integer.MAX_VALUE, 0, 0);
            } else {
                startAnim();
                f = false;
            }
        }

        public void setStartValue(int mStartValue) {
            this.mStartValue = mStartValue;
            recaculate();
            invalidate();
        }

        public void setEndValue(int mEndValue) {
            this.mEndValue = mEndValue;
            recaculate();
            invalidate();
        }

        public void setPartitionValue(int mPartitionValue) {
            this.mPartitionValue = mPartitionValue;
            recaculate();
            invalidate();
        }

        public void setPartitionWidthInDP(float mPartitionWidth) {
            this.mPartitionWidth = RulerUtils.convertDpToPixel(mContext, mPartitionWidth);
            recaculate();
            invalidate();
        }

        public void setmValue(int mValue) {
            this.mCurrentValue = mValue;
            invalidate();
        }

        public void setSmallPartitionCount(int mSmallPartitionCount) {
            this.mSmallPartitionCount = mSmallPartitionCount;
            recaculate();
            invalidate();
        }

        public void setOriginValue(int mOriginValue) {
            this.mOriginValue = mOriginValue;
            recaculate();
            invalidate();
        }

        public int setOriginValue2(int mOriginValue) {
            this.mOriginValue = mOriginValue;
            invalidate();
            return mOriginValue;
        }

        public void setOriginValueSmall(int small) {
            this.mOriginValueSmall = small;
            recaculate();
            invalidate();
        }

        public void setMaxValue(int maxValue) {
            this.mMaxValue = maxValue;
        }

        private void recaculate() {
            mMoveX = -mOriginValueSmall * (mPartitionWidth / mSmallPartitionCount);
            mRightOffset = -1 * (mEndValue - mOriginValue) * mPartitionWidth / mPartitionValue;
            mLeftOffset = -1 * (mStartValue - mOriginValue) * mPartitionWidth / mPartitionValue;
        }
    }


}
