package ba.ahavic.circularprogressbar;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class CircularProgressBar extends View {
    private static final String TAG = "CircularProgressBar";

    private static final int DEFAULT_STROKE_WIDTH = 20;
    private static final int DEFAULT_TITLE_SIZE = 60;
    private static final int DEFAULT_SUBTITLE_SIZE = 20;
    private static final int MAX_PROGRESS = 100;

    private final RectF mProgressCircleBounds = new RectF();

    private int mMaxProgress;
    private int mProgress;

    private int mProgressPadding;

    private int mStartAngle;
    private int mSweepAngle;

    private double mStartThumbX;
    private double mStartThumbY;

    private double mEndThumbX;
    private double mEndThumbY;

    private final Paint mProgressColorPaint = new Paint();
    private final Paint mSecondaryProgressColorPaint = new Paint();
    private final Paint mBackgroundColorPaint = new Paint();

    private final Paint mThumbPaint = new Paint();
    private final Paint mTitlePaint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint mSubtitlePaint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);

    private String mGradientStartColor = null;
    private String mGradientEndColor = null;

    private int mProgressWidth;

    private String mTitle = "";
    private String mSubTitle = "";

    private int mTitleSize;
    private int mSubtitleSize;

    private boolean mRoundCorners;
    private boolean mThumbEnabled;
    private boolean mEnabled;
    private boolean mDrawBackground;

    public interface ProgressAnimationListener {
        void onAnimationStart();

        void onAnimationFinish();

        void onAnimationProgress(int progress);
    }

    public CircularProgressBar(Context context) {
        super(context);
        init(null, 0);
    }

    public CircularProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CircularProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public void init(AttributeSet attrs, int style) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        Resources res = getResources();
        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.CircularProgressBar, style, 0);

        try {
            mProgressColorPaint.setColor(a.getColor(R.styleable.CircularProgressBar_progressColor,
                    res.getColor(R.color.cpb_default_progress)));

            mSecondaryProgressColorPaint.setColor(a.getColor(R.styleable.CircularProgressBar_secondaryProgressColor,
                    res.getColor(R.color.cpb_default_background)));

            mBackgroundColorPaint.setColor(a.getColor(R.styleable.CircularProgressBar_backgroundColor,
                    res.getColor(R.color.cpb_default_background_color)));

            mTitlePaint.setColor(a.getColor(R.styleable.CircularProgressBar_titleColor,
                    res.getColor(R.color.cpb_default_title)));

            mSubtitlePaint.setColor(a.getColor(R.styleable.CircularProgressBar_subtitleColor,
                    res.getColor(R.color.cpb_default_subtitle)));

            mThumbPaint.setColor(a.getColor(R.styleable.CircularProgressBar_thumbColor,
                    res.getColor(R.color.cpb_default_progress)));

            mGradientStartColor = a.getString(R.styleable.CircularProgressBar_gradientColorStart);

            mGradientEndColor = a.getString(R.styleable.CircularProgressBar_gradientColorEnd);

            mProgress = a.getInt(R.styleable.CircularProgressBar_progress, 0);

            mProgressWidth = (int) a.getDimension(R.styleable.CircularProgressBar_progressWidth, DEFAULT_STROKE_WIDTH);

            mMaxProgress = a.getInt(R.styleable.CircularProgressBar_maxProgress, MAX_PROGRESS);

            mStartAngle = a.getInt(R.styleable.CircularProgressBar_startAngle, 0);

            mSweepAngle = a.getInt(R.styleable.CircularProgressBar_sweepAngle, 360);

            mTitle = a.getString(R.styleable.CircularProgressBar_title);

            mTitleSize = a.getInt(R.styleable.CircularProgressBar_titleSize, DEFAULT_TITLE_SIZE);

            mSubTitle = a.getString(R.styleable.CircularProgressBar_subtitle);

            mSubtitleSize = a.getInt(R.styleable.CircularProgressBar_subtitleSize, DEFAULT_SUBTITLE_SIZE);

            mRoundCorners = a.getBoolean(R.styleable.CircularProgressBar_roundCorners, false);

            mThumbEnabled = a.getBoolean(R.styleable.CircularProgressBar_thumbEnabled, false);

            mEnabled = a.getBoolean(R.styleable.CircularProgressBar_enabled, true);

            mDrawBackground = a.getBoolean(R.styleable.CircularProgressBar_drawBackground, false);

        } finally {
            a.recycle();
        }

        mProgressColorPaint.setAntiAlias(true);
        mProgressColorPaint.setStyle(Paint.Style.STROKE);
        mProgressColorPaint.setStrokeWidth(mProgressWidth);

        mSecondaryProgressColorPaint.setAntiAlias(true);
        mSecondaryProgressColorPaint.setStyle(Paint.Style.STROKE);
        mSecondaryProgressColorPaint.setStrokeWidth(mProgressWidth);

        mBackgroundColorPaint.setAntiAlias(true);
        mBackgroundColorPaint.setStyle(Style.FILL);

        mThumbPaint.setAntiAlias(true);
        mThumbPaint.setStyle(Style.FILL);

        mTitlePaint.setTextSize(spToPx(mTitleSize, getContext()));
        mTitlePaint.setStyle(Style.FILL);
        mTitlePaint.setTypeface(Typeface.create("Roboto-Thin", Typeface.NORMAL));

        mSubtitlePaint.setTextSize(spToPx(mSubtitleSize, getContext()));
        mSubtitlePaint.setStyle(Style.FILL);
        mSubtitlePaint.setTypeface(Typeface.create("Roboto-Thin", Typeface.NORMAL));
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);

        mProgressPadding = (int) (Math.min(width, height) * 0.12);

        int top = mProgressPadding;
        int left = mProgressPadding;

        mProgressCircleBounds.set(left, top, width - left, height - top);

        mStartThumbX = Math.cos(Math.toRadians(mStartAngle)) * (mProgressCircleBounds.centerX() - left) + mProgressCircleBounds.centerX();
        mStartThumbY = Math.sin(Math.toRadians(mStartAngle)) * (mProgressCircleBounds.centerY() - top) + mProgressCircleBounds.centerY();

        setGradient();

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (mRoundCorners) {
            mSecondaryProgressColorPaint.setStrokeCap(Paint.Cap.ROUND);
            mProgressColorPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        //Draw background black transparent circle
        if (mDrawBackground) {
            canvas.drawCircle(getWidth() / 2.f, getHeight() / 2.f, getWidth() / 2.f, mBackgroundColorPaint);
        }

        //Draw secondary progress arc
        canvas.drawArc(mProgressCircleBounds, mStartAngle, mSweepAngle, false, mSecondaryProgressColorPaint);

        //Draw progress arc
        float scale = mMaxProgress > 0 ? (float) getProgress() / mMaxProgress * mSweepAngle : 0;
        if (mEnabled) {
            canvas.drawArc(mProgressCircleBounds, mStartAngle, scale, false, mProgressColorPaint);
        } else {
            //If progress is disabled set the color of thumb to secondary progress bar color
            mThumbPaint.setColor(mSecondaryProgressColorPaint.getColor());
            scale = mSweepAngle;
        }

        //Draw progress thumb at starting and ending point of the Arc
        if (mThumbEnabled) {
            drawThumb(canvas, scale);
        }

        drawText(canvas);
        super.onDraw(canvas);
    }

    private void drawThumb(Canvas canvas, float scale) {
        //Draw start circle thumb
        canvas.drawCircle((float) mStartThumbX, (float) mStartThumbY, mProgressWidth, mThumbPaint);
        mEndThumbX = Math.cos(Math.toRadians(mStartAngle + scale)) * (mProgressCircleBounds.centerX() - mProgressPadding) + mProgressCircleBounds.centerX();
        mEndThumbY = Math.sin(Math.toRadians(mStartAngle + scale)) * (mProgressCircleBounds.centerY() - mProgressPadding) + mProgressCircleBounds.centerY();
        //Draw end circle thumb
        canvas.drawCircle((float) mEndThumbX, (float) mEndThumbY, mProgressWidth, mThumbPaint);
    }

    private void drawText(Canvas canvas) {
        if (!TextUtils.isEmpty(mTitle)) {
            int xPos = (int) (getWidth() / 2 - mTitlePaint.measureText(mTitle) / 2);
            int yPos = (int) (getHeight() / 2);

            float titleHeight = Math.abs(mTitlePaint.descent() + mTitlePaint.ascent());

            if (TextUtils.isEmpty(mSubTitle)) {
                yPos += titleHeight / 2;
            }

            canvas.drawText(mTitle, xPos, yPos, mTitlePaint);

            yPos += titleHeight * 1.3;
            xPos = (int) (getWidth() / 2 - mSubtitlePaint.measureText(mSubTitle) / 2);

            canvas.drawText(mSubTitle, xPos, yPos, mSubtitlePaint);
        }
    }

    private void setGradient() {
        if (mGradientStartColor != null && mGradientEndColor != null) {
            setGradientProgressColor();
            setGradientSecondaryProgressColor();
        }
    }

    private void setGradientProgressColor() {
        LinearGradient gradientProgress = new LinearGradient(mProgressCircleBounds.left * 4,
                mProgressCircleBounds.top * 3.5f,
                mProgressCircleBounds.right,
                mProgressCircleBounds.bottom / 1.5f,
                Color.parseColor(mGradientStartColor),
                Color.parseColor(mGradientEndColor),
                Shader.TileMode.CLAMP);

        mProgressColorPaint.setShader(gradientProgress);
    }

    private void setGradientSecondaryProgressColor() {
        LinearGradient gradientProgress = new LinearGradient(mProgressCircleBounds.left * 4,
                mProgressCircleBounds.top * 3.7f,
                mProgressCircleBounds.right,
                mProgressCircleBounds.bottom / 1.7f,
                getResources().getColor(R.color.white),
                Color.parseColor(mGradientEndColor),
                Shader.TileMode.CLAMP);

        mSecondaryProgressColorPaint.setShader(gradientProgress);
    }

    /**
     * Sets progress for progress bar. If value is less than 0, progress is set to 0.
     * If progress is greater than <code>mMaxProgress</code> then progress is set to
     * <code>mMaxProgress</code>.
     * @param progress
     */
    public synchronized void setProgress(int progress) {
        if (progress > mMaxProgress) mProgress = mMaxProgress;
        else if (progress < 0) mProgress = 0;
        else mProgress = progress;
        invalidate();
    }

    /**
     * Convenience method for object animator. Calls <code>setProgress(int progress)</code> if
     * received value is different from <code>getProgress()</code> value.
     */
    @SuppressWarnings("unused")
    private synchronized void setProgress(float progress) {
        int progressInt = (int) progress;
        if (progressInt != getProgress()) {
            setProgress(progressInt);
        }
    }

    /**
     * Animate progress bar between <code>start</code> and <code>end</code> value
     * @param start value between 0 and <code>mMaxProgress</code>
     * @param end value between 0 and <code>mMaxProgress</code>
     */
    public void animateProgressTo(final int start, final int end) {
        if (start != 0 && start < mMaxProgress)
            setProgress(start);

        @SuppressLint("ObjectAnimatorBinding")
        final ObjectAnimator progressBarAnimator = ObjectAnimator.ofFloat(this, "progress", start, end);
        progressBarAnimator.setDuration(1000);
        progressBarAnimator.setInterpolator(new LinearInterpolator());

        progressBarAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                CircularProgressBar.this.setProgress(end);
            }
        });

        progressBarAnimator.start();
    }

    public synchronized void setTitle(String title) {
        this.mTitle = title;
        invalidate();
    }

    public synchronized void setSubtitle(String subtitle) {
        this.mSubTitle = subtitle;
        invalidate();
    }

    public synchronized void setSubtitleColor(int color) {
        mSubtitlePaint.setColor(color);
        invalidate();
    }

    public synchronized void setTitleColor(int color) {
        mTitlePaint.setColor(color);
        invalidate();
    }

    public String getTitle() {
        return mTitle;
    }

    public int getProgress() {
        return mProgress;
    }

    private static int spToPx(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }
}
