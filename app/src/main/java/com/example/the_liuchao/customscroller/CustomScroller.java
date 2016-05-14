package com.example.the_liuchao.customscroller;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Created by the_liuchao on 2016/5/14.
 */
public class CustomScroller extends ViewGroup {
    private Scroller mScroller;
    private int mTouchSlop;//能够进行手势滑动的距离
    private int mMinimumVelocity, mMaximumVelocity;//允许执行一个fling手势动作的最小/大速度值
    private int maxScrollEdge;
    private float mLastMotionY;//最后触摸y位置
    private VelocityTracker mVelocityTracker;//速度跟踪器
    private boolean mIsInEdge;//是否滑动到最大值
    private int childTotalHeight,height,statusHeight;//所有孩子控件总高度，屏幕高度

    public CustomScroller(Context context) {
        this(context, null);
    }

    public CustomScroller(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public CustomScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /***
     * 初始化
     * @param context
     */
    private void init(Context context) {
        mScroller = new Scroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);//先分发给Child View进行处理，如果所有的Child View都没有处理，则自己再处理
        setWillNotDraw(false);//要调用draw方法进行绘画，所以应将此语句参数置为false.
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        height = metrics.heightPixels;
//        int resId = context.getResources().getIdentifier("status_bar_height","dimen",context.getPackageName());
//       statusHeight =  context.getResources().getDimensionPixelSize(resId);
    }

    /**
     * 处理滑动操作的方法fling(int velocityY),
     *
     * @param velocityY
     */
    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            //maxScrollEdge Y方向的最大值，scroller不会滚过此点
            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0, maxScrollEdge);
//            final boolean movingDown = velocityY > 0;
            awakenScrollBars(mScroller.getDuration());//在这里给出动画开始的延时
            invalidate();
        }
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int childCount = getChildCount();
        measureChildren(widthMeasureSpec,heightMeasureSpec);
        int childHeights = 0;
        for (int i=0;i<childCount;i++){
            childHeights += getChildAt(i).getMeasuredHeight();
        }
        this.childTotalHeight = childHeights;
        maxScrollEdge = this.childTotalHeight - height;
    }

    /**
     * 速度跟踪器初始化
     * @param event
     */
    private void obtainVelocityTracter(MotionEvent event){
     if(mVelocityTracker==null)
          mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);
    }

    /**
     * 释放速度跟踪器
     */
    private void releaseVelocityTracker(){
        if(mVelocityTracker!=null){
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      // getEdgeFlags():当事件类型是ActionDown时可以通过此方法获得,手指触控开始的边界. 如果是的话,有如下几种值:EDGE_LEFT,EDGE_TOP,EDGE_RIGHT,EDGE_BOTTOM
        if(event.getAction()==MotionEvent.ACTION_DOWN&&event.getEdgeFlags()!=0){
            return false;
        }
        obtainVelocityTracter(event);
        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                if(!mScroller.isFinished())//如果scroll还在滑动，就终止上次滑动
                    mScroller.abortAnimation();
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                final int deltaY = (int)(mLastMotionY-y);
                mLastMotionY = y;
                if(deltaY<0){
                    if(getScrollY()>0) {
                        scrollBy(0, deltaY);
                    }
                }else if(deltaY>0){
                    //判断是否滑动到边界
                    mIsInEdge = getScrollY() <=childTotalHeight - height + statusHeight;
                    if(mIsInEdge){
                        scrollBy(0,deltaY);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000,mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity();
                if(Math.abs(initialVelocity)>mMinimumVelocity&&getChildCount()>0){
                    fling(-initialVelocity);
                }
                releaseVelocityTracker();
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if(mScroller.computeScrollOffset()){
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            int oldX = scrollX;
            int oldY = scrollY;
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            scrollX = x;
            scrollY = y;
            scrollY = scrollY + 10;
            scrollTo(scrollX,scrollY);
            postInvalidate();
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
       if(changed){
           int childTop = 0;
           final int childCount = getChildCount();
           for (int i=0;i<childCount;i++){
              final View childView = getChildAt(i);
               if(childView.getVisibility()!=View.GONE){
                   final int mesureChildHeight = childView.getMeasuredHeight();
                   childView.layout(0,childTop, childView.getMeasuredWidth(),childTop+mesureChildHeight);
                   childTop += mesureChildHeight;
               }
           }
       }
    }
}
