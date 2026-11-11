package com.campusbill.app.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 消息列表铺在球下面。球体以外的点击和滑动交给列表，WebView 方框不吃这些事件。
 */
public class ChatBallStage extends FrameLayout {

    private View ballPanel;
    private View messageList;
    private View passTip;
    private boolean passToList;

    public ChatBallStage(Context context) {
        super(context);
    }

    public ChatBallStage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChatBallStage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void bind(View ballPanel, View messageList, View passTip) {
        this.ballPanel = ballPanel;
        this.messageList = messageList;
        this.passTip = passTip;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (ballPanel == null || messageList == null) {
            return super.dispatchTouchEvent(event);
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            passToList = shouldPassToList(event.getX(), event.getY());
        }
        if (!passToList) {
            return super.dispatchTouchEvent(event);
        }
        MotionEvent copy = MotionEvent.obtain(event);
        copy.offsetLocation(-messageList.getLeft(), -messageList.getTop());
        boolean handled = messageList.dispatchTouchEvent(copy);
        copy.recycle();
        return handled;
    }

    private boolean shouldPassToList(float x, float y) {
        if (passTip != null && passTip.getVisibility() == VISIBLE
                && x >= passTip.getLeft() && x < passTip.getRight()
                && y >= passTip.getTop() && y < passTip.getBottom()) {
            return false;
        }
        float localX = x - ballPanel.getLeft();
        float localY = y - ballPanel.getTop();
        return !ChatBallFade.insideSphere(localX, localY,
                ballPanel.getWidth(), ballPanel.getHeight());
    }
}
