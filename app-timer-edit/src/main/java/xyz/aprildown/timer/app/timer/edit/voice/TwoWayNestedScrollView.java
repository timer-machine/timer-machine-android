/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2020 Maxim Naumov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package xyz.aprildown.timer.app.timer.edit.voice;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.OverScroller;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ScrollingView;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityRecordCompat;
import androidx.core.widget.EdgeEffectCompat;

import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

/**
 * TwoWayNestedScrollView is just like {@link android.widget.ScrollView}, but it supports acting
 * as both a nested scrolling parent and child on both new and old versions of Android.
 * Nested scrolling is enabled by default.
 */
public class TwoWayNestedScrollView extends FrameLayout implements NestedScrollingParent3,
        NestedScrollingChild3, ScrollingView {
    static final int ANIMATED_SCROLL_GAP = 250;

    static final float MAX_SCROLL_FACTOR = 0.5f;

    private static final String TAG = "TwoWayNestedScrollView";
    private static final int DEFAULT_SMOOTH_SCROLL_DURATION = 250;

    /**
     * Interface definition for a callback to be invoked when the scroll
     * X or Y positions of a view change.
     *
     * <p>This version of the interface works on all versions of Android, back to API v4.</p>
     *
     * @see #setOnScrollChangeListener(OnScrollChangeListener)
     */
    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param v The view whose scroll position has changed.
         * @param scrollX Current horizontal scroll origin.
         * @param scrollY Current vertical scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        void onScrollChange(TwoWayNestedScrollView v, int scrollX, int scrollY,
                int oldScrollX, int oldScrollY);
    }

    private long mLastScroll;

    private final Rect mTempRect = new Rect();
    private OverScroller mScroller;
    private EdgeEffect mEdgeGlowLeft;
    private EdgeEffect mEdgeGlowTop;
    private EdgeEffect mEdgeGlowRight;
    private EdgeEffect mEdgeGlowBottom;

    /**
     * Position of the last motion event.
     */
    private int mLastMotionX;
    private int mLastMotionY;

    /**
     * True when the layout has changed but the traversal has not come through yet.
     * Ideally the view hierarchy would keep track of this for us.
     */
    private boolean mIsLayoutDirty = true;
    private boolean mIsLaidOut = false;

    /**
     * The child to give focus to in the event that a child has requested focus while the
     * layout is dirty. This prevents the scroll from being wrong if the child has not been
     * laid out before requesting focus.
     */
    private View mChildToScrollTo = null;

    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts their finger).
     */
    private boolean mIsBeingDragged = false;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    /**
     * When set to true, the scroll view measure its child to make it fill the currently
     * visible area.
     */
    private boolean mFillViewport;

    /**
     * Whether arrow scrolling is animated.
     */
    private boolean mSmoothScrollingEnabled = true;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    /**
     * Used during scrolling to retrieve the new offset within the window.
     */
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedXOffset;
    private int mNestedYOffset;

    private int mLastScrollerX;
    private int mLastScrollerY;

    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    private SavedState mSavedState;

    private static final AccessibilityDelegate ACCESSIBILITY_DELEGATE = new AccessibilityDelegate();

    private static final int[] SCROLLVIEW_STYLEABLE = new int[] {
            android.R.attr.fillViewport
    };

    private final NestedScrollingParentHelper mParentHelper;
    private final NestedScrollingChildHelper mChildHelper;

    private float mHorizontalScrollFactor;
    private float mVerticalScrollFactor;

    private OnScrollChangeListener mOnScrollChangeListener;

    public TwoWayNestedScrollView(@NonNull Context context) {
        this(context, null);
    }

    public TwoWayNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwoWayNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initScrollView();

        final TypedArray a = context.obtainStyledAttributes(
                attrs, SCROLLVIEW_STYLEABLE, defStyleAttr, 0);

        setFillViewport(a.getBoolean(0, false));

        a.recycle();

        mParentHelper = new NestedScrollingParentHelper(this);
        mChildHelper = new NestedScrollingChildHelper(this);

        // ...because why else would you be using this widget?
        setNestedScrollingEnabled(true);

        ViewCompat.setAccessibilityDelegate(this, ACCESSIBILITY_DELEGATE);
    }

    // NestedScrollingChild3

    @Override
    public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, @Nullable int[] offsetInWindow, int type, @NonNull int[] consumed) {
        mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow, type, consumed);
    }

    // NestedScrollingChild2

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow,
            int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return startNestedScroll(axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    // NestedScrollingParent3

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        onNestedScrollInternal(dxUnconsumed, dyUnconsumed, type, consumed);
    }

    private void onNestedScrollInternal(int dxUnconsumed, int dyUnconsumed, int type, @Nullable int[] consumed) {
        final int oldScrollX = getScrollX();
        final int oldScrollY = getScrollY();
        scrollBy(dxUnconsumed, dyUnconsumed);
        final int myConsumedX = getScrollX() - oldScrollX;
        final int myConsumedY = getScrollY() - oldScrollY;

        if (consumed != null) {
            consumed[0] += myConsumedX;
            consumed[1] += myConsumedY;
        }
        final int myUnconsumedX = dxUnconsumed - myConsumedX;
        final int myUnconsumedY = dyUnconsumed - myConsumedY;

        mChildHelper.dispatchNestedScroll(myConsumedX, myConsumedY, myUnconsumedX, myUnconsumedY, null, type, consumed);
    }

    // NestedScrollingParent2

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
            int type) {
        return (axes & (ViewCompat.SCROLL_AXIS_HORIZONTAL | ViewCompat.SCROLL_AXIS_VERTICAL)) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
            int type) {
        mParentHelper.onNestedScrollAccepted(child, target, axes, type);
        startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL | ViewCompat.SCROLL_AXIS_VERTICAL, type);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        mParentHelper.onStopNestedScroll(target, type);
        stopNestedScroll(type);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int type) {
        onNestedScrollInternal(dxUnconsumed, dyUnconsumed, type, null);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
            int type) {
        dispatchNestedPreScroll(dx, dy, consumed, null, type);
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(
            @NonNull View child, @NonNull View target, int axes) {
        return onStartNestedScroll(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScrollAccepted(
            @NonNull View child, @NonNull View target, int axes) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target) {
        onStopNestedScroll(target, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed) {
        onNestedScrollInternal(dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH, null);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean onNestedFling(
            @NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if (!consumed) {
            dispatchNestedFling(velocityX, velocityY, true);
            fling((int) velocityX, (int) velocityY);
            return true;
        }
        return false;
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public int getNestedScrollAxes() {
        return mParentHelper.getNestedScrollAxes();
    }

    // ScrollView import

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getHorizontalFadingEdgeLength();
        final int scrollX = getScrollX();
        if (scrollX < length) {
            return scrollX / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getVerticalFadingEdgeLength();
        final int scrollY = getScrollY();
        if (scrollY < length) {
            return scrollY / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        View child = getChildAt(0);
        final TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int length = getHorizontalFadingEdgeLength();
        final int rightEdge = getWidth() - getPaddingRight();
        final int span = child.getRight() + lp.rightMargin - getScrollX() - rightEdge;
        if (span < length) {
            return span / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        View child = getChildAt(0);
        final TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int length = getVerticalFadingEdgeLength();
        final int bottomEdge = getHeight() - getPaddingBottom();
        final int span = child.getBottom() + lp.bottomMargin - getScrollY() - bottomEdge;
        if (span < length) {
            return span / (float) length;
        }

        return 1.0f;
    }

    /**
     * @return The maximum amount this scroll view will scroll in response to
     * an arrow event.
     */
    public int getMaxScrollAmountX() {
        return (int) (MAX_SCROLL_FACTOR * getWidth());
    }

    /**
     * @return The maximum amount this scroll view will scroll in response to
     * an arrow event.
     */
    public int getMaxScrollAmountY() {
        return (int) (MAX_SCROLL_FACTOR * getHeight());
    }

    private void initScrollView() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    public void addView(View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index, params);
    }

    /**
     * Register a callback to be invoked when the scroll X or Y positions of
     * this view change.
     * <p>This version of the method works on all versions of Android, back to API v4.</p>
     *
     * @param l The listener to notify when the scroll X or Y position changes.
     * @see android.view.View#getScrollX()
     * @see android.view.View#getScrollY()
     */
    public void setOnScrollChangeListener(@Nullable OnScrollChangeListener l) {
        mOnScrollChangeListener = l;
    }

    /**
     * @return Returns true this ScrollView can be scrolled
     */
    private boolean canScrollX() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getWidth() + lp.leftMargin + lp.rightMargin;
            int parentSpace = getWidth() - getPaddingLeft() - getPaddingRight();
            return childSize > parentSpace;
        }
        return false;
    }

    /**
     * @return Returns true this ScrollView can be scrolled
     */
    private boolean canScrollY() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSpace = getHeight() - getPaddingTop() - getPaddingBottom();
            return childSize > parentSpace;
        }
        return false;
    }

    /**
     * Indicates whether this ScrollView's content is stretched to fill the viewport.
     *
     * @return True if the content fills the viewport, false otherwise.
     * @attr name android:fillViewport
     */
    public boolean isFillViewport() {
        return mFillViewport;
    }

    /**
     * Set whether this ScrollView should stretch its content height to fill the viewport or not.
     *
     * @param fillViewport True to stretch the content's height to the viewport's
     * boundaries, false otherwise.
     * @attr name android:fillViewport
     */
    public void setFillViewport(boolean fillViewport) {
        if (fillViewport != mFillViewport) {
            mFillViewport = fillViewport;
            requestLayout();
        }
    }

    /**
     * @return Whether arrow scrolling will animate its transition.
     */
    public boolean isSmoothScrollingEnabled() {
        return mSmoothScrollingEnabled;
    }

    /**
     * Set whether arrow scrolling will animate its transition.
     *
     * @param smoothScrollingEnabled whether arrow scrolling will animate its transition
     */
    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
        mSmoothScrollingEnabled = smoothScrollingEnabled;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (mOnScrollChangeListener != null) {
            mOnScrollChangeListener.onScrollChange(this, l, t, oldl, oldt);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mFillViewport) {
            return;
        }

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            return;
        }

        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int childWidth = child.getMeasuredWidth();
            int parentSpaceX = getMeasuredWidth()
                    - getPaddingLeft()
                    - getPaddingRight()
                    - lp.leftMargin
                    - lp.rightMargin;
            int childHeight = child.getMeasuredHeight();
            int parentSpaceY = getMeasuredHeight()
                    - getPaddingTop()
                    - getPaddingBottom()
                    - lp.topMargin
                    - lp.bottomMargin;

            if (childWidth < parentSpaceX || childHeight < parentSpaceY) {
                int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        Math.max(parentSpaceX, childWidth),
                        MeasureSpec.EXACTLY
                );
                int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        Math.max(parentSpaceY, childHeight),
                        MeasureSpec.EXACTLY
                );
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        mTempRect.setEmpty();

        if (!canScrollX() && !canScrollY()) {
            if (isFocused() && event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
                View currentFocused = findFocus();
                if (currentFocused == this) currentFocused = null;
                View nextFocused = FocusFinder.getInstance().findNextFocus(this,
                        currentFocused, View.FOCUS_DOWN);
                return nextFocused != null
                        && nextFocused != this
                        && nextFocused.requestFocus(View.FOCUS_DOWN);
            }
            return false;
        }

        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_LEFT);
                    } else {
                        handled = fullScroll(View.FOCUS_LEFT);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_UP);
                    } else {
                        handled = fullScroll(View.FOCUS_UP);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_RIGHT);
                    } else {
                        handled = fullScroll(View.FOCUS_RIGHT);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_DOWN);
                    } else {
                        handled = fullScroll(View.FOCUS_DOWN);
                    }
                    break;
                case KeyEvent.KEYCODE_SPACE:
                    pageScroll(event.isShiftPressed() ? View.FOCUS_UP : View.FOCUS_DOWN);
                    break;
            }
        }

        return handled;
    }

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY
                    || y >= child.getBottom() - scrollY
                    || x < child.getLeft() - scrollX
                    || x >= child.getRight() - scrollX);
        }
        return false;
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and they are moving their finger.  We want to intercept this
         * motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && mIsBeingDragged) {
            return true;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from their original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionY is set to the y value
                 * of the down event.
                 */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + activePointerId
                            + " in onInterceptTouchEvent");
                    break;
                }

                final int x = (int) ev.getX(pointerIndex);
                final int y = (int) ev.getY(pointerIndex);
                final int xDiff = Math.abs(x - mLastMotionX);
                final int yDiff = Math.abs(y - mLastMotionY);
                boolean doX = xDiff > mTouchSlop
                        && (getNestedScrollAxes() & ViewCompat.SCROLL_AXIS_HORIZONTAL) == 0;
                boolean doY = yDiff > mTouchSlop
                        && (getNestedScrollAxes() & ViewCompat.SCROLL_AXIS_VERTICAL) == 0;
                if (doX || doY) {
                    mIsBeingDragged = true;
                    mLastMotionX = x;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    mNestedXOffset = 0;
                    mNestedYOffset = 0;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                if (!inChild(x, y)) {
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = x;
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when
                 * being flinged. We need to call computeScrollOffset() first so that
                 * isFinished() is correct.
                 */
                mScroller.computeScrollOffset();
                mIsBeingDragged = !mScroller.isFinished();
                startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL | ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /* Release the drag */
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                if (mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRangeX(), 0, getScrollRangeY())) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();

        final int actionMasked = ev.getActionMasked();

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedXOffset = 0;
            mNestedYOffset = 0;
        }

        MotionEvent vtev = MotionEvent.obtain(ev);
        vtev.offsetLocation(mNestedXOffset, mNestedYOffset);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                if (getChildCount() == 0) {
                    return false;
                }
                if ((mIsBeingDragged = !mScroller.isFinished())) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    abortAnimatedScroll();
                }

                // Remember where the motion event started
                mLastMotionX = (int) ev.getX();
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL | ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int x = (int) ev.getX(activePointerIndex);
                final int y = (int) ev.getY(activePointerIndex);
                int deltaX = mLastMotionX - x;
                int deltaY = mLastMotionY - y;
                if (!mIsBeingDragged && (Math.abs(deltaY) > mTouchSlop || Math.abs(deltaX) > mTouchSlop)) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop;
                    } else {
                        deltaX += mTouchSlop;
                    }
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    // Start with nested pre scrolling
                    if (dispatchNestedPreScroll(deltaX, deltaY, mScrollConsumed, mScrollOffset,
                            ViewCompat.TYPE_TOUCH)) {
                        deltaX -= mScrollConsumed[0];
                        deltaY -= mScrollConsumed[1];
                        mNestedXOffset += mScrollOffset[0];
                        mNestedYOffset += mScrollOffset[1];
                    }

                    // Scroll to follow the motion event
                    mLastMotionX = x - mScrollOffset[0];
                    mLastMotionY = y - mScrollOffset[1];

                    final int oldX = getScrollX();
                    final int oldY = getScrollY();
                    final int rangeX = getScrollRangeX();
                    final int rangeY = getScrollRangeY();
                    final int overscrollMode = getOverScrollMode();
                    boolean canOverscrollX = overscrollMode == View.OVER_SCROLL_ALWAYS
                            || (overscrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && rangeX > 0);
                    boolean canOverscrollY = overscrollMode == View.OVER_SCROLL_ALWAYS
                            || (overscrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && rangeY > 0);

                    // Calling overScrollByCompat will call onOverScrolled, which
                    // calls onScrollChanged if applicable.
                    if (overScrollByCompat(deltaX, deltaY, getScrollX(), getScrollY(), rangeX, rangeY, 0,
                            0, true) && !hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)) {
                        // Break our velocity if we hit a scroll barrier.
                        mVelocityTracker.clear();
                    }

                    final int scrolledDeltaX = getScrollX() - oldX;
                    final int scrolledDeltaY = getScrollY() - oldY;
                    final int unconsumedX = deltaX - scrolledDeltaX;
                    final int unconsumedY = deltaY - scrolledDeltaY;

                    mScrollConsumed[0] = 0;
                    mScrollConsumed[1] = 0;

                    dispatchNestedScroll(scrolledDeltaX, scrolledDeltaY, unconsumedX, unconsumedY, mScrollOffset,
                            ViewCompat.TYPE_TOUCH, mScrollConsumed);

                    mLastMotionX -= mScrollOffset[0];
                    mLastMotionY -= mScrollOffset[1];
                    mNestedXOffset += mScrollOffset[0];
                    mNestedYOffset += mScrollOffset[1];

                    if (canOverscrollX) {
                        deltaX -= mScrollConsumed[0];
                        ensureGlows();
                        final int pulledToX = oldX + deltaX;
                        if (pulledToX < 0) {
                            EdgeEffectCompat.onPull(mEdgeGlowLeft, (float) deltaX / getWidth(),
                                    1.f - ev.getY(activePointerIndex) / getHeight());
                            if (!mEdgeGlowRight.isFinished()) {
                                mEdgeGlowRight.onRelease();
                            }
                        } else if (pulledToX > rangeX) {
                            EdgeEffectCompat.onPull(mEdgeGlowRight, (float) deltaX / getWidth(),
                                    ev.getY(activePointerIndex)
                                            / getHeight());
                            if (!mEdgeGlowLeft.isFinished()) {
                                mEdgeGlowLeft.onRelease();
                            }
                        }
                    }
                    if (canOverscrollY) {
                        deltaY -= mScrollConsumed[1];
                        ensureGlows();
                        final int pulledToY = oldY + deltaY;
                        if (pulledToY < 0) {
                            EdgeEffectCompat.onPull(mEdgeGlowTop, (float) deltaY / getHeight(),
                                    ev.getX(activePointerIndex) / getWidth());
                            if (!mEdgeGlowBottom.isFinished()) {
                                mEdgeGlowBottom.onRelease();
                            }
                        } else if (pulledToY > rangeY) {
                            EdgeEffectCompat.onPull(mEdgeGlowBottom, (float) deltaY / getHeight(),
                                    1.f - ev.getX(activePointerIndex)
                                            / getWidth());
                            if (!mEdgeGlowTop.isFinished()) {
                                mEdgeGlowTop.onRelease();
                            }
                        }
                    }
                    if (canOverscrollX || canOverscrollY) {
                        if (mEdgeGlowTop != null
                                && (!mEdgeGlowLeft.isFinished() || !mEdgeGlowTop.isFinished() || !mEdgeGlowRight.isFinished() || !mEdgeGlowBottom.isFinished())) {
                            ViewCompat.postInvalidateOnAnimation(this);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocityX = (int) velocityTracker.getXVelocity(mActivePointerId);
                int initialVelocityY = (int) velocityTracker.getYVelocity(mActivePointerId);
                if ((Math.abs(initialVelocityX) >= mMinimumVelocity) || (Math.abs(initialVelocityY) >= mMinimumVelocity)) {
                    if (!dispatchNestedPreFling(-initialVelocityX, -initialVelocityY)) {
                        dispatchNestedFling(-initialVelocityX, -initialVelocityY, true);
                        fling(-initialVelocityX, -initialVelocityY);
                    }
                } else if (mScroller.springBack(getScrollX(), getScrollY(),
                        0, getScrollRangeX(),
                        0, getScrollRangeY())) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && getChildCount() > 0) {
                    if (mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRangeX(), 0, getScrollRangeY())) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mLastMotionX = (int) ev.getX(index);
                mLastMotionY = (int) ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionX = (int) ev.getX(ev.findPointerIndex(mActivePointerId));
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();

        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = (int) ev.getX(newPointerIndex);
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDeviceCompat.SOURCE_CLASS_POINTER) != 0) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_SCROLL: {
                    if (!mIsBeingDragged) {
                        final float hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
                        final float vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                        if (hscroll != 0 || vscroll != 0) {
                            final int deltaX = (int) (hscroll * getHorizontalScrollFactorCompat());
                            final int deltaY = (int) (vscroll * getVerticalScrollFactorCompat());
                            final int rangeX = getScrollRangeX();
                            final int rangeY = getScrollRangeY();
                            int oldScrollX = getScrollX();
                            int oldScrollY = getScrollY();
                            int newScrollX = oldScrollX - deltaX;
                            int newScrollY = oldScrollY - deltaY;
                            if (newScrollX < 0) {
                                newScrollX = 0;
                            } else if (newScrollX > rangeX) {
                                newScrollX = rangeX;
                            }
                            if (newScrollY < 0) {
                                newScrollY = 0;
                            } else if (newScrollY > rangeY) {
                                newScrollY = rangeY;
                            }
                            if (newScrollX != oldScrollX || newScrollY != oldScrollY) {
                                super.scrollTo(newScrollX, newScrollY);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private float getHorizontalScrollFactorCompat() {
        if (mHorizontalScrollFactor == 0) {
            TypedValue outValue = new TypedValue();
            final Context context = getContext();
            if (!context.getTheme().resolveAttribute(
                    android.R.attr.listPreferredItemHeight, outValue, true)) {
                throw new IllegalStateException(
                        "Expected theme to define listPreferredItemHeight.");
            }
            mHorizontalScrollFactor = outValue.getDimension(
                    context.getResources().getDisplayMetrics());
        }
        return mHorizontalScrollFactor;
    }

    private float getVerticalScrollFactorCompat() {
        if (mVerticalScrollFactor == 0) {
            TypedValue outValue = new TypedValue();
            final Context context = getContext();
            if (!context.getTheme().resolveAttribute(
                    android.R.attr.listPreferredItemHeight, outValue, true)) {
                throw new IllegalStateException(
                        "Expected theme to define listPreferredItemHeight.");
            }
            mVerticalScrollFactor = outValue.getDimension(
                    context.getResources().getDisplayMetrics());
        }
        return mVerticalScrollFactor;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
            boolean clampedX, boolean clampedY) {
        super.scrollTo(scrollX, scrollY);
    }

    boolean overScrollByCompat(int deltaX, int deltaY,
            int scrollX, int scrollY,
            int scrollRangeX, int scrollRangeY,
            int maxOverScrollX, int maxOverScrollY,
            boolean isTouchEvent) {
        final int overScrollMode = getOverScrollMode();
        final boolean canScrollHorizontal =
                computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical =
                computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        // Clamp values if at the limits and record
        final int left = -maxOverScrollX;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        if ((clampedX || clampedY) && !hasNestedScrollingParent(ViewCompat.TYPE_NON_TOUCH)) {
            mScroller.springBack(newScrollX, newScrollY, 0, getScrollRangeX(), 0, getScrollRangeY());
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    int getScrollRangeX() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getWidth() + lp.leftMargin + lp.rightMargin;
            int parentSpace = getWidth() - getPaddingLeft() - getPaddingRight();
            scrollRange = Math.max(0, childSize - parentSpace);
        }
        return scrollRange;
    }

    int getScrollRangeY() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSpace = getHeight() - getPaddingTop() - getPaddingBottom();
            scrollRange = Math.max(0, childSize - parentSpace);
        }
        return scrollRange;
    }

    /**
     * <p>
     * Finds the next focusable component that fits in the specified bounds.
     * </p>
     *
     * @param topFocus look for a candidate is the one at the top of the bounds
     * if topFocus is true, or at the bottom of the bounds if topFocus is
     * false
     * @param top the top offset of the bounds in which a focusable must be
     * found
     * @param bottom the bottom offset of the bounds in which a focusable must
     * be found
     * @return the next focusable component in the bounds or null if none can
     * be found
     */
    private View findFocusableViewInBounds(boolean topFocus, int top, int bottom) {

        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;

        /*
         * A fully contained focusable is one where its top is below the bound's
         * top, and its bottom is above the bound's bottom. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
        boolean foundFullyContainedFocusable = false;

        int count = focusables.size();
        for (int i = 0; i < count; i++) {
            View view = focusables.get(i);
            int viewTop = view.getTop();
            int viewBottom = view.getBottom();

            if (top < viewBottom && viewTop < bottom) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                final boolean viewIsFullyContained = (top < viewTop) && (viewBottom < bottom);

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    final boolean viewIsCloserToBoundary =
                            (topFocus && viewTop < focusCandidate.getTop())
                                    || (!topFocus && viewBottom > focusCandidate.getBottom());

                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
                            focusCandidate = view;
                        }
                    } else {
                        if (viewIsFullyContained) {
                            /* Any fully contained view beats a partially contained view */
                            focusCandidate = view;
                            foundFullyContainedFocusable = true;
                        } else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
                            focusCandidate = view;
                        }
                    }
                }
            }
        }

        return focusCandidate;
    }

    /**
     * <p>Handles scrolling in response to a "page up/down" shortcut press. This
     * method will scroll the view by one page up or down and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link android.view.View#FOCUS_UP}
     * to go one page up or
     * {@link android.view.View#FOCUS_DOWN} to go one page down
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean pageScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        if (down) {
            mTempRect.top = getScrollY() + height;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) view.getLayoutParams();
                int bottom = view.getBottom() + lp.bottomMargin + getPaddingBottom();
                if (mTempRect.top + height > bottom) {
                    mTempRect.top = bottom - height;
                }
            }
        } else {
            mTempRect.top = getScrollY() - height;
            if (mTempRect.top < 0) {
                mTempRect.top = 0;
            }
        }
        mTempRect.bottom = mTempRect.top + height;

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    /**
     * <p>Handles scrolling in response to a "home/end" shortcut press. This
     * method will scroll the view to the top or bottom and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link android.view.View#FOCUS_UP}
     * to go the top of the view or
     * {@link android.view.View#FOCUS_DOWN} to go the bottom
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean fullScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        mTempRect.top = 0;
        mTempRect.bottom = height;

        if (down) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) view.getLayoutParams();
                mTempRect.bottom = view.getBottom() + lp.bottomMargin + getPaddingBottom();
                mTempRect.top = mTempRect.bottom - height;
            }
        }

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    /**
     * <p>Scrolls the view to make the area defined by <code>top</code> and
     * <code>bottom</code> visible. This method attempts to give the focus
     * to a component visible in this area. If no component can be focused in
     * the new visible area, the focus is reclaimed by this ScrollView.</p>
     *
     * @param direction the scroll direction: {@link android.view.View#FOCUS_UP}
     * to go upward, {@link android.view.View#FOCUS_DOWN} to downward
     * @param top the top offset of the new area to be made visible
     * @param bottom the bottom offset of the new area to be made visible
     * @return true if the key event is consumed by this method, false otherwise
     */
    private boolean scrollAndFocus(int direction, int top, int bottom) {
        boolean handled = true;

        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean up = direction == View.FOCUS_UP;

        View newFocused = findFocusableViewInBounds(up, top, bottom);
        if (newFocused == null) {
            newFocused = this;
        }

        if (top >= containerTop && bottom <= containerBottom) {
            handled = false;
        } else {
            int deltaY = up ? (top - containerTop) : (bottom - containerBottom);
            doScroll(0, deltaY);
        }

        if (newFocused != findFocus()) newFocused.requestFocus(direction);

        return handled;
    }

    /**
     * Handle scrolling in response to an arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was
     * pressed
     * @return True if we consumed the event, false otherwise
     */
    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) currentFocused = null;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);

        final int maxJumpX = getMaxScrollAmountX();
        final int maxJumpY = getMaxScrollAmountY();

        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJumpX, getWidth(), maxJumpY, getHeight())) {
            nextFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(nextFocused, mTempRect);
            int scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect);
            int scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect);
            doScroll(scrollDeltaX, scrollDeltaY);
            nextFocused.requestFocus(direction);
        } else {
            // no new focus
            int scrollDeltaX = maxJumpX;
            int scrollDeltaY = maxJumpY;

            if (direction == View.FOCUS_UP && getScrollY() < scrollDeltaY) {
                scrollDeltaY = getScrollY();
            } else if (direction == View.FOCUS_DOWN) {
                if (getChildCount() > 0) {
                    View child = getChildAt(0);
                    TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    int daBottom = child.getBottom() + lp.bottomMargin;
                    int screenBottom = getScrollY() + getHeight() - getPaddingBottom();
                    scrollDeltaY = Math.min(daBottom - screenBottom, maxJumpY);
                }
            } else if (direction == View.FOCUS_LEFT && getScrollX() < scrollDeltaX) {
                scrollDeltaX = getScrollX();
            } else if (direction == View.FOCUS_RIGHT) {
                if (getChildCount() > 0) {
                    View child = getChildAt(0);
                    TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    int daRight = child.getRight() + lp.rightMargin;
                    int screenRight = getScrollX() + getWidth() - getPaddingRight();
                    scrollDeltaX = Math.min(daRight - screenRight, maxJumpX);
                }
            }
            if (scrollDeltaX == 0 && scrollDeltaY == 0) {
                return false;
            }
            if (direction == View.FOCUS_RIGHT) {
                doScroll(scrollDeltaX, 0);
            } else if (direction == View.FOCUS_LEFT) {
                doScroll(-scrollDeltaX, 0);
            } else if (direction == View.FOCUS_DOWN) {
                doScroll(0, scrollDeltaY);
            } else if (direction == View.FOCUS_UP) {
                doScroll(0, -scrollDeltaY);
            } else {
                return false;
            }
        }

        if (currentFocused != null && currentFocused.isFocused()
                && isOffScreen(currentFocused)) {
            // previously focused item still has focus and is off screen, give
            // it up (take it back to ourselves)
            // (also, need to temporarily force FOCUS_BEFORE_DESCENDANTS so we are
            // sure to
            // get it)
            final int descendantFocusability = getDescendantFocusability();  // save
            setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            requestFocus();
            setDescendantFocusability(descendantFocusability);  // restore
        }
        return true;
    }

    /**
     * @return whether the descendant of this scroll view is scrolled off
     * screen.
     */
    private boolean isOffScreen(View descendant) {
        return !isWithinDeltaOfScreen(descendant, 0, getWidth(), 0, getHeight());
    }

    /**
     * @return whether the descendant of this scroll view is within delta
     * pixels of being on the screen.
     */
    private boolean isWithinDeltaOfScreen(View descendant, int deltaX, int width, int deltaY, int height) {
        descendant.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(descendant, mTempRect);

        return (mTempRect.bottom + deltaY) >= getScrollY()
                && (mTempRect.top - deltaY) <= (getScrollY() + height)
                && (mTempRect.right + deltaX) >= getScrollX()
                && (mTempRect.left - deltaX) <= (getScrollX() + width);
    }

    /**
     * Smooth scroll by a Y delta
     *
     * @param deltaX the number of pixels to scroll by on the X axis
     * @param deltaY the number of pixels to scroll by on the Y axis
     */
    private void doScroll(int deltaX, int deltaY) {
        if (deltaX != 0 || deltaY != 0) {
            if (mSmoothScrollingEnabled) {
                smoothScrollBy(deltaX, deltaY);
            } else {
                scrollBy(deltaX, deltaY);
            }
        }
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    public final void smoothScrollBy(int dx, int dy) {
        smoothScrollBy(dx, dy, DEFAULT_SMOOTH_SCROLL_DURATION, false);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     */
    public final void smoothScrollBy(int dx, int dy, int scrollDurationMs) {
        smoothScrollBy(dx, dy, scrollDurationMs, false);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    private void smoothScrollBy(int dx, int dy, int scrollDurationMs, boolean withNestedScrolling) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            View child = getChildAt(0);
            TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childWidth = child.getWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSpaceX = getWidth() - getPaddingLeft() - getPaddingRight();
            int parentSpaceY = getHeight() - getPaddingTop() - getPaddingBottom();
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            final int maxX = Math.max(0, childWidth - parentSpaceX);
            final int maxY = Math.max(0, childHeight - parentSpaceY);
            dx = Math.max(0, Math.min(scrollX + dx, maxX)) - scrollX;
            dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;
            mScroller.startScroll(scrollX, scrollY, dx, dy, scrollDurationMs);
            runAnimatedScroll(withNestedScrolling);
        } else {
            if (!mScroller.isFinished()) {
                abortAnimatedScroll();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, DEFAULT_SMOOTH_SCROLL_DURATION, false);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     */
    public final void smoothScrollTo(int x, int y, int scrollDurationMs) {
        smoothScrollTo(x, y, scrollDurationMs, false);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    // This should be considered private, it is package private to avoid a synthetic ancestor.
    void smoothScrollTo(int x, int y, boolean withNestedScrolling) {
        smoothScrollTo(x, y, DEFAULT_SMOOTH_SCROLL_DURATION, withNestedScrolling);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    // This should be considered private, it is package private to avoid a synthetic ancestor.
    void smoothScrollTo(int x, int y, int scrollDurationMs, boolean withNestedScrolling) {
        smoothScrollBy(x - getScrollX(), y - getScrollY(), scrollDurationMs, withNestedScrolling);
    }

    /**
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int parentSpace = getHeight() - getPaddingBottom() - getPaddingTop();
        if (count == 0) {
            return parentSpace;
        }

        View child = getChildAt(0);
        TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int scrollRange = child.getBottom() + lp.bottomMargin;
        final int scrollY = getScrollY();
        final int overscrollBottom = Math.max(0, scrollRange - parentSpace);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    /**
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeHorizontalScrollRange() {
        final int count = getChildCount();
        final int parentSpace = getWidth() - getPaddingRight() - getPaddingLeft();
        if (count == 0) {
            return parentSpace;
        }

        View child = getChildAt(0);
        TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int scrollRange = child.getRight() + lp.rightMargin;
        final int scrollX = getScrollX();
        final int overscrollRight = Math.max(0, scrollRange - parentSpace);
        if (scrollX < 0) {
            scrollRange -= scrollX;
        } else if (scrollX > overscrollRight) {
            scrollRange += scrollX - overscrollRight;
        }

        return scrollRange;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec,
            int parentHeightMeasureSpec) {
        int childMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        child.measure(childMeasureSpec, childMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                lp.leftMargin + lp.rightMargin, MeasureSpec.UNSPECIFIED);
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                lp.topMargin + lp.bottomMargin, MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public void computeScroll() {

        if (mScroller.isFinished()) {
            return;
        }

        mScroller.computeScrollOffset();
        final int x = mScroller.getCurrX();
        final int y = mScroller.getCurrY();
        int unconsumedX = x - mLastScrollerX;
        int unconsumedY = y - mLastScrollerY;
        mLastScrollerX = x;
        mLastScrollerY = y;

        // Nested Scrolling Pre Pass
        mScrollConsumed[0] = 0;
        mScrollConsumed[1] = 0;
        dispatchNestedPreScroll(unconsumedX, unconsumedY, mScrollConsumed, null,
                ViewCompat.TYPE_NON_TOUCH);
        unconsumedX -= mScrollConsumed[0];
        unconsumedY -= mScrollConsumed[1];

        final int rangeX = getScrollRangeX();
        final int rangeY = getScrollRangeY();

        if (unconsumedX != 0 || unconsumedY != 0) {
            // Internal Scroll
            final int oldScrollX = getScrollX();
            final int oldScrollY = getScrollY();
            overScrollByCompat(unconsumedX, unconsumedY, getScrollX(), oldScrollY, rangeX, rangeY, 0, 0, false);
            final int scrolledByMeX = getScrollX() - oldScrollX;
            final int scrolledByMeY = getScrollY() - oldScrollY;
            unconsumedX -= scrolledByMeX;
            unconsumedY -= scrolledByMeY;

            // Nested Scrolling Post Pass
            mScrollConsumed[0] = 0;
            mScrollConsumed[1] = 0;
            dispatchNestedScroll(scrolledByMeX, scrolledByMeY, unconsumedX, unconsumedY, mScrollOffset,
                    ViewCompat.TYPE_NON_TOUCH, mScrollConsumed);
            unconsumedX -= mScrollConsumed[0];
            unconsumedY -= mScrollConsumed[1];
        }

        if (unconsumedX != 0 || unconsumedY != 0) {
            final int mode = getOverScrollMode();
            final boolean canOverscrollX = mode == OVER_SCROLL_ALWAYS
                    || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && rangeX > 0);
            final boolean canOverscrollY = mode == OVER_SCROLL_ALWAYS
                    || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && rangeY > 0);
            if (canOverscrollX) {
                ensureGlows();
                if (unconsumedX < 0) {
                    if (mEdgeGlowLeft.isFinished()) {
                        mEdgeGlowLeft.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                } else {
                    if (mEdgeGlowRight.isFinished()) {
                        mEdgeGlowRight.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }
            if (canOverscrollY) {
                ensureGlows();
                if (unconsumedY < 0) {
                    if (mEdgeGlowTop.isFinished()) {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                } else {
                    if (mEdgeGlowBottom.isFinished()) {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }
            abortAnimatedScroll();
        }

        if (!mScroller.isFinished()) {
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
    }

    private void runAnimatedScroll(boolean participateInNestedScrolling) {
        if (participateInNestedScrolling) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL | ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
        mLastScrollerX = getScrollX();
        mLastScrollerY = getScrollY();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void abortAnimatedScroll() {
        mScroller.abortAnimation();
        stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
    }

    /**
     * Scrolls the view to the given child.
     *
     * @param child the View to scroll to
     */
    private void scrollToChild(View child) {
        child.getDrawingRect(mTempRect);

        /* Offset from child's local coordinates to ScrollView coordinates */
        offsetDescendantRectToMyCoords(child, mTempRect);

        int scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect);
        int scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect);

        if (scrollDeltaX != 0 || scrollDeltaY != 0) {
            scrollBy(scrollDeltaX, scrollDeltaY);
        }
    }

    /**
     * If rect is off screen, scroll just enough to get it (or at least the
     * first screen size chunk of it) on screen.
     *
     * @param rect The rectangle.
     * @param immediate True to scroll immediately without animation
     * @return true if scrolling was performed
     */
    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        final int deltaX = computeScrollDeltaToGetChildRectOnScreenX(rect);
        final int deltaY = computeScrollDeltaToGetChildRectOnScreenY(rect);
        final boolean scrollX = deltaX != 0;
        final boolean scrollY = deltaY != 0;
        if (scrollX || scrollY) {
            if (immediate) {
                scrollBy(deltaX, deltaY);
            } else {
                smoothScrollBy(deltaX, deltaY);
            }
        }
        return scrollX || scrollY;
    }

    /**
     * Compute the amount to scroll in the X direction in order to get
     * a rectangle completely on the screen (or, if wider than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected int computeScrollDeltaToGetChildRectOnScreenX(Rect rect) {
        if (getChildCount() == 0) return 0;

        int width = getWidth();
        int screenLeft = getScrollX();
        int screenRight = screenLeft + width;
        int actualScreenRight = screenRight;

        int fadingEdge = getHorizontalFadingEdgeLength();

        // TODO: screenTop should be incremented by fadingEdge * getTopFadingEdgeStrength (but for
        // the target scroll distance).
        // leave room for top fading edge as long as rect isn't at very top
        if (rect.left > 0) {
            screenLeft += fadingEdge;
        }

        // TODO: screenBottom should be decremented by fadingEdge * getBottomFadingEdgeStrength (but
        // for the target scroll distance).
        // leave room for bottom fading edge as long as rect isn't at very bottom
        View child = getChildAt(0);
        final TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (rect.right < child.getWidth() + lp.leftMargin + lp.rightMargin) {
            screenRight -= fadingEdge;
        }

        int scrollXDelta = 0;

        if (rect.right > screenRight && rect.left > screenLeft) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.width() > width) {
                // just enough to get screen size chunk on
                scrollXDelta += (rect.left - screenLeft);
            } else {
                // get entire rect at bottom of screen
                scrollXDelta += (rect.right - screenRight);
            }

            // make sure we aren't scrolling beyond the end of our content
            int right = child.getRight() + lp.rightMargin;
            int distanceToRight = right - actualScreenRight;
            scrollXDelta = Math.min(scrollXDelta, distanceToRight);

        } else if (rect.left < screenLeft && rect.right < screenRight) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.width() > width) {
                // screen size chunk
                scrollXDelta -= (screenRight - rect.right);
            } else {
                // entire rect at top
                scrollXDelta -= (screenLeft - rect.left);
            }

            // make sure we aren't scrolling any further than the top our content
            scrollXDelta = Math.max(scrollXDelta, -getScrollX());
        }
        return scrollXDelta;
    }

    /**
     * Compute the amount to scroll in the Y direction in order to get
     * a rectangle completely on the screen (or, if taller than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected int computeScrollDeltaToGetChildRectOnScreenY(Rect rect) {
        if (getChildCount() == 0) return 0;

        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;
        int actualScreenBottom = screenBottom;

        int fadingEdge = getVerticalFadingEdgeLength();

        // TODO: screenTop should be incremented by fadingEdge * getTopFadingEdgeStrength (but for
        // the target scroll distance).
        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }

        // TODO: screenBottom should be decremented by fadingEdge * getBottomFadingEdgeStrength (but
        // for the target scroll distance).
        // leave room for bottom fading edge as long as rect isn't at very bottom
        View child = getChildAt(0);
        final TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (rect.bottom < child.getHeight() + lp.topMargin + lp.bottomMargin) {
            screenBottom -= fadingEdge;
        }

        int scrollYDelta = 0;

        if (rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.height() > height) {
                // just enough to get screen size chunk on
                scrollYDelta += (rect.top - screenTop);
            } else {
                // get entire rect at bottom of screen
                scrollYDelta += (rect.bottom - screenBottom);
            }

            // make sure we aren't scrolling beyond the end of our content
            int bottom = child.getBottom() + lp.bottomMargin;
            int distanceToBottom = bottom - actualScreenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.height() > height) {
                // screen size chunk
                scrollYDelta -= (screenBottom - rect.bottom);
            } else {
                // entire rect at top
                scrollYDelta -= (screenTop - rect.top);
            }

            // make sure we aren't scrolling any further than the top our content
            scrollYDelta = Math.max(scrollYDelta, -getScrollY());
        }
        return scrollYDelta;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (!mIsLayoutDirty) {
            scrollToChild(focused);
        } else {
            // The child may not be laid out yet, we can't compute the scroll yet
            mChildToScrollTo = focused;
        }
        super.requestChildFocus(child, focused);
    }


    /**
     * When looking for focus in children of a scroll view, need to be a little
     * more careful not to give focus to something that is scrolled off screen.
     * <p>
     * This is more expensive than the default {@link android.view.ViewGroup}
     * implementation, otherwise this behavior might have been made the default.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect) {

        // convert from forward / backward notation to up / down / left / right
        // (ugh).
        if (direction == View.FOCUS_FORWARD) {
            direction = View.FOCUS_DOWN;
        } else if (direction == View.FOCUS_BACKWARD) {
            direction = View.FOCUS_UP;
        }

        final View nextFocus = previouslyFocusedRect == null
                ? FocusFinder.getInstance().findNextFocus(this, null, direction)
                : FocusFinder.getInstance().findNextFocusFromRect(
                this, previouslyFocusedRect, direction);

        if (nextFocus == null) {
            return false;
        }

        if (isOffScreen(nextFocus)) {
            return false;
        }

        return nextFocus.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle,
            boolean immediate) {
        // offset into coordinate space of this scroll view
        rectangle.offset(child.getLeft() - child.getScrollX(),
                child.getTop() - child.getScrollY());

        return scrollToChildRect(rectangle, immediate);
    }

    @Override
    public void requestLayout() {
        mIsLayoutDirty = true;
        super.requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mIsLayoutDirty = false;
        // Give a child focus if it needs it
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
            scrollToChild(mChildToScrollTo);
        }
        mChildToScrollTo = null;

        if (!mIsLaidOut) {
            // If there is a saved state, scroll to the position saved in that state.
            if (mSavedState != null) {
                scrollTo(mSavedState.scrollPositionX, mSavedState.scrollPositionY);
                mSavedState = null;
            } // mScrollY default value is "0"

            // Make sure current scrollY position falls into the scroll range.  If it doesn't,
            // scroll such that it does.
            int childWidth = 0;
            int childHeight = 0;
            if (getChildCount() > 0) {
                View child = getChildAt(0);
                TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
                childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            }
            int parentSpaceX = r - l - getPaddingLeft() - getPaddingRight();
            int parentSpaceY = b - t - getPaddingTop() - getPaddingBottom();
            int currentScrollX = getScrollX();
            int currentScrollY = getScrollY();
            int newScrollX = clamp(currentScrollX, parentSpaceX, childWidth);
            int newScrollY = clamp(currentScrollY, parentSpaceY, childHeight);
            if (newScrollX != currentScrollX || newScrollY != currentScrollY) {
                scrollTo(newScrollX, newScrollY);
            }
        }

        // Calling this with the present values causes it to re-claim them
        scrollTo(getScrollX(), getScrollY());
        mIsLaidOut = true;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mIsLaidOut = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        View currentFocused = findFocus();
        if (null == currentFocused || this == currentFocused) {
            return;
        }

        // If the currently-focused view was visible on the screen when the
        // screen was at the old height, then scroll the screen to make that
        // view visible with the new screen height.
        if (isWithinDeltaOfScreen(currentFocused, 0, oldw, 0, oldh)) {
            currentFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
            int scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect);
            int scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect);
            doScroll(scrollDeltaX, scrollDeltaY);
        }
    }

    /**
     * Return true if child is a descendant of parent, (or equal to the parent).
     */
    private static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }

        final ViewParent theParent = child.getParent();
        return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
    }

    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     * numbers mean that the finger/cursor is moving down the screen,
     * which means we want to scroll towards the top.
     */
    public void fling(int velocityX, int velocityY) {
        if (getChildCount() > 0) {

            mScroller.fling(getScrollX(), getScrollY(), // start
                    velocityX, velocityY, // velocities
                    Integer.MIN_VALUE, Integer.MAX_VALUE, // x
                    Integer.MIN_VALUE, Integer.MAX_VALUE, // y
                    0, 0); // overscroll
            runAnimatedScroll(true);
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;

        recycleVelocityTracker();
        stopNestedScroll(ViewCompat.TYPE_TOUCH);

        if (mEdgeGlowTop != null) {
            mEdgeGlowLeft.onRelease();
            mEdgeGlowTop.onRelease();
            mEdgeGlowRight.onRelease();
            mEdgeGlowBottom.onRelease();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This version also clamps the scrolling to the bounds of our child.
     */
    @Override
    public void scrollTo(int x, int y) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final TwoWayNestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int parentSpaceHorizontal = getWidth() - getPaddingLeft() - getPaddingRight();
            int childSizeHorizontal = child.getWidth() + lp.leftMargin + lp.rightMargin;
            int parentSpaceVertical = getHeight() - getPaddingTop() - getPaddingBottom();
            int childSizeVertical = child.getHeight() + lp.topMargin + lp.bottomMargin;
            x = clamp(x, parentSpaceHorizontal, childSizeHorizontal);
            y = clamp(y, parentSpaceVertical, childSizeVertical);
            if (x != getScrollX() || y != getScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }

    private void ensureGlows() {
        if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
            if (mEdgeGlowTop == null) {
                Context context = getContext();
                mEdgeGlowLeft = new EdgeEffect(context);
                mEdgeGlowTop = new EdgeEffect(context);
                mEdgeGlowRight = new EdgeEffect(context);
                mEdgeGlowBottom = new EdgeEffect(context);
            }
        } else {
            mEdgeGlowTop = null;
            mEdgeGlowBottom = null;
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mEdgeGlowTop != null) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            if (!mEdgeGlowLeft.isFinished()) {
                final int restoreCount = canvas.save();
                int width = getWidth();
                int height = getHeight();
                int xTranslation = Math.min(0, scrollX);
                int yTranslation = scrollY;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || getClipToPadding()) {
                    width -= getPaddingLeft() + getPaddingRight();
                    xTranslation += getPaddingLeft();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getClipToPadding()) {
                    height -= getPaddingTop() + getPaddingBottom();
                    yTranslation += getPaddingTop();
                }
                canvas.translate(xTranslation, yTranslation);
                canvas.rotate(-90, height / 2f, height / 2f);
                mEdgeGlowLeft.setSize(height, width);
                if (mEdgeGlowLeft.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowTop.isFinished()) {
                final int restoreCount = canvas.save();
                int width = getWidth();
                int height = getHeight();
                int xTranslation = scrollX;
                int yTranslation = Math.min(0, scrollY);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || getClipToPadding()) {
                    width -= getPaddingLeft() + getPaddingRight();
                    xTranslation += getPaddingLeft();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getClipToPadding()) {
                    height -= getPaddingTop() + getPaddingBottom();
                    yTranslation += getPaddingTop();
                }
                canvas.translate(xTranslation, yTranslation);
                mEdgeGlowTop.setSize(width, height);
                if (mEdgeGlowTop.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowRight.isFinished()) {
                final int restoreCount = canvas.save();
                int width = getWidth();
                int height = getHeight();
                int xTranslation = Math.max(getScrollRangeX(), scrollX) + width;
                int yTranslation = scrollY;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || getClipToPadding()) {
                    width -= getPaddingLeft() + getPaddingRight();
                    xTranslation += getPaddingLeft();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getClipToPadding()) {
                    height -= getPaddingTop() + getPaddingBottom();
                    yTranslation -= getPaddingBottom();
                }
                canvas.translate(xTranslation - width, yTranslation);
                canvas.rotate(90, width / 2f, width / 2f);
                mEdgeGlowRight.setSize(height, width);
                if (mEdgeGlowRight.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowBottom.isFinished()) {
                final int restoreCount = canvas.save();
                int width = getWidth();
                int height = getHeight();
                int xTranslation = scrollX;
                int yTranslation = Math.max(getScrollRangeY(), scrollY) + height;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || getClipToPadding()) {
                    width -= getPaddingLeft() + getPaddingRight();
                    xTranslation += getPaddingLeft();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getClipToPadding()) {
                    height -= getPaddingTop() + getPaddingBottom();
                    yTranslation -= getPaddingBottom();
                }
                canvas.translate(xTranslation - width, yTranslation);
                canvas.rotate(180, width, 0);
                mEdgeGlowBottom.setSize(width, height);
                if (mEdgeGlowBottom.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                canvas.restoreToCount(restoreCount);
            }
        }
    }

    private static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            /* my >= child is this case:
             *                    |--------------- me ---------------|
             *     |------ child ------|
             * or
             *     |--------------- me ---------------|
             *            |------ child ------|
             * or
             *     |--------------- me ---------------|
             *                                  |------ child ------|
             *
             * n < 0 is this case:
             *     |------ me ------|
             *                    |-------- child --------|
             *     |-- mScrollX --|
             */
            return 0;
        }
        if ((my + n) > child) {
            /* this case:
             *                    |------ me ------|
             *     |------ child ------|
             *     |-- mScrollX --|
             */
            return child - my;
        }
        return n;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mSavedState = ss;
        requestLayout();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.scrollPositionX = getScrollX();
        ss.scrollPositionY = getScrollY();
        return ss;
    }

    static class SavedState extends BaseSavedState {
        public int scrollPositionX;
        public int scrollPositionY;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel source) {
            super(source);
            scrollPositionX = source.readInt();
            scrollPositionY = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(scrollPositionX);
            dest.writeInt(scrollPositionY);
        }

        @NonNull
        @Override
        public String toString() {
            return "HorizontalScrollView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " scrollPositionX=" + scrollPositionX + "}"
                    + " scrollPositionY=" + scrollPositionY + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    static class AccessibilityDelegate extends AccessibilityDelegateCompat {
        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
            if (super.performAccessibilityAction(host, action, arguments)) {
                return true;
            }
            final TwoWayNestedScrollView nsvHost = (TwoWayNestedScrollView) host;
            if (!nsvHost.isEnabled()) {
                return false;
            }
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                case android.R.id.accessibilityActionScrollDown: {
                    final int viewportHeight = nsvHost.getHeight() - nsvHost.getPaddingBottom()
                            - nsvHost.getPaddingTop();
                    final int targetScrollY = Math.min(nsvHost.getScrollY() + viewportHeight,
                            nsvHost.getScrollRangeY());
                    if (targetScrollY != nsvHost.getScrollY()) {
                        nsvHost.smoothScrollTo(0, targetScrollY, true);
                        return true;
                    }
                }
                return false;
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                case android.R.id.accessibilityActionScrollUp: {
                    final int viewportHeight = nsvHost.getHeight() - nsvHost.getPaddingBottom()
                            - nsvHost.getPaddingTop();
                    final int targetScrollY = Math.max(nsvHost.getScrollY() - viewportHeight, 0);
                    if (targetScrollY != nsvHost.getScrollY()) {
                        nsvHost.smoothScrollTo(0, targetScrollY, true);
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            final TwoWayNestedScrollView nsvHost = (TwoWayNestedScrollView) host;
            info.setClassName(ScrollView.class.getName());
            if (nsvHost.isEnabled()) {
                final int scrollRange = nsvHost.getScrollRangeY();
                if (scrollRange > 0) {
                    info.setScrollable(true);
                    if (nsvHost.getScrollY() > 0) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_BACKWARD);
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_UP);
                    }
                    if (nsvHost.getScrollY() < scrollRange) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_FORWARD);
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_DOWN);
                    }
                }
            }
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            final TwoWayNestedScrollView nsvHost = (TwoWayNestedScrollView) host;
            event.setClassName(ScrollView.class.getName());
            final boolean scrollable = nsvHost.getScrollRangeY() > 0;
            event.setScrollable(scrollable);
            event.setScrollX(nsvHost.getScrollX());
            event.setScrollY(nsvHost.getScrollY());
            AccessibilityRecordCompat.setMaxScrollX(event, nsvHost.getScrollRangeX());
            AccessibilityRecordCompat.setMaxScrollY(event, nsvHost.getScrollRangeY());
        }
    }
}
