package com.android.mirror.assisttouch.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.android.mirror.assisttouch.R;
import com.android.mirror.assisttouch.utils.SystemsUtils;

import java.util.Timer;
import java.util.TimerTask;

public class AssistiveTouchService extends Service {

    private boolean isMoving;

    private float rawX;
    private float rawY;

    private int mScreenWidth;
    private int mScreenHeight;
    private int mStatusBarHeight;

    private int lastAssistiveTouchViewX;
    private int lastAssistiveTouchViewY;

    private View mAssistiveTouchView;
    private View mInflateAssistiveTouchView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams  mParams;
    private PopupWindow mPopupWindow;
    private AlertDialog.Builder mBulider;
    private AlertDialog mAlertDialog;
    private View mScreenShotView;

    private Timer mTimer;
    private Handler mHandler;

    private LayoutInflater mInflater;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        calculateForMyPhone();
        createAssistiveTouchView();
        inflateViewListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void init(){
        mTimer = new Timer();
        mHandler =  new MyHandler();
        mBulider = new AlertDialog.Builder(AssistiveTouchService.this);
        mAlertDialog = mBulider.create();
        mParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        mInflater = LayoutInflater.from(this);
        mAssistiveTouchView = mInflater.inflate(R.layout.assistive_touch_layout, null);
        mInflateAssistiveTouchView = mInflater.inflate(R.layout.assistive_touch_inflate_layout, null);
    }

    private void calculateForMyPhone(){
        DisplayMetrics displayMetrics = SystemsUtils.getScreenSize(this);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mStatusBarHeight = SystemsUtils.getStatusBarHeight(this);

        mInflateAssistiveTouchView.setLayoutParams(new WindowManager.LayoutParams((int) (mScreenWidth * 0.75), (int) (mScreenWidth * 0.75)));
    }

    public void createAssistiveTouchView(){
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.x = mScreenWidth;
        mParams.y = 520;
        mParams.gravity = Gravity.TOP|Gravity.LEFT;
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowManager.addView(mAssistiveTouchView, mParams);
        mAssistiveTouchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                rawX = event.getRawX();
                rawY = event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isMoving = false;
                        break;
                    case MotionEvent.ACTION_UP:
                        setAssitiveTouchViewAlign();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        isMoving = true;
                        mParams.x = (int) (rawX - mAssistiveTouchView.getMeasuredWidth() / 2);
                        mParams.y = (int) (rawY - mAssistiveTouchView.getMeasuredHeight() / 2 - mStatusBarHeight);
                        mWindowManager.updateViewLayout(mAssistiveTouchView, mParams);
                }
                if (isMoving)
                    return true;
                else
                    return false;
            }
        });
        mAssistiveTouchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAssistiveTouchView.setAlpha(0);
                lastAssistiveTouchViewX = mParams.x;
                lastAssistiveTouchViewY = mParams.y;
                myAssitiveTouchAnimator(mParams.x,
                        mScreenWidth / 2 - mAssistiveTouchView.getMeasuredWidth() / 2,
                        mParams.y,
                        mScreenHeight / 2 - mAssistiveTouchView.getMeasuredHeight() / 2,
                        false).start();

                mPopupWindow = new PopupWindow(mInflateAssistiveTouchView,
                        (int) (mScreenWidth * 0.75), (int) (mScreenWidth * 0.75));
                mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        myAssitiveTouchAnimator(mParams.x, lastAssistiveTouchViewX, mParams.y, lastAssistiveTouchViewY, true).start();
                        mAssistiveTouchView.setAlpha(1);
                    }
                });
                mPopupWindow.setFocusable(true);
                mPopupWindow.setTouchable(true);
                mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
                mPopupWindow.showAtLocation(mAssistiveTouchView, Gravity.CENTER, 0, 0);

                /*mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                mAlertDialog.show();
                WindowManager.LayoutParams alertDialogParams = mAlertDialog.getWindow().getAttributes();
                alertDialogParams.width = (int)(mScreenWidth*0.75);
                alertDialogParams.height = (int)(mScreenWidth*0.75);
                alertDialogParams.alpha = 0.85F;
                mAlertDialog.getWindow().setAttributes(alertDialogParams);
                mAlertDialog.getWindow().setContentView(mInflateAssistiveTouchView);
                mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mAssistiveTouchView.setAlpha(1);
                    }
                });*/
            }
        });
    }

    private void inflateViewListener(){
        ImageView shutdown = (ImageView)mInflateAssistiveTouchView.findViewById(R.id.shutdown);
        ImageView star = (ImageView)mInflateAssistiveTouchView.findViewById(R.id.star);
        ImageView screenshot = (ImageView)mInflateAssistiveTouchView.findViewById(R.id.screenshot);
        ImageView home = (ImageView)mInflateAssistiveTouchView.findViewById(R.id.home);

        shutdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SystemsUtils.shutDown(AssistiveTouchService.this);
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(0);
                    }
                }, 626);
            }
        });

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SystemsUtils.goHome(AssistiveTouchService.this);
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(0);
                    }
                }, 626);
            }
        });

        screenshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.sendEmptyMessage(0);
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        String filename = SystemsUtils.takeScreenShot(AssistiveTouchService.this);
                        Message msg = mHandler.obtainMessage();
                        msg.what = 1;
                        msg.obj = filename;
                        mHandler.sendMessage(msg);
                    }
                }, 626);
            }
        });
    }

    private ValueAnimator myAssitiveTouchAnimator(final int fromx, final int tox, int fromy, final int toy, final boolean flag){
        PropertyValuesHolder p1 = PropertyValuesHolder.ofInt("X", fromx, tox);
        PropertyValuesHolder p2 = PropertyValuesHolder.ofInt("Y", fromy, toy);
        ValueAnimator v1 = ValueAnimator.ofPropertyValuesHolder(p1, p2);
        v1.setDuration(100L);
        v1.setInterpolator(new DecelerateInterpolator());
        v1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer x = (Integer) animation.getAnimatedValue("X");
                Integer y = (Integer) animation.getAnimatedValue("Y");
                mParams.x = x;
                mParams.y = y;
                mWindowManager.updateViewLayout(mAssistiveTouchView, mParams);
            }
        });
        v1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (flag)
                    mAssistiveTouchView.setAlpha(0.85f);
            }
        });
        return v1;
    }

    /*private ValueAnimator mSceenShotAnimator(){
        PropertyValuesHolder p1 = PropertyValuesHolder.ofFloat("scaleX", 1, 0);
        PropertyValuesHolder p2 = PropertyValuesHolder.ofFloat("scaleY", 1, 0);
        ValueAnimator v1 = ValueAnimator.ofPropertyValuesHolder(p1, p2);
        v1.setDuration(5000L);
        v1.setInterpolator(new DecelerateInterpolator());
        v1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float scaleX = (Float) animation.getAnimatedValue("scaleX");
                Float scaleY = (Float) animation.getAnimatedValue("scaleY");
                *//*mScreenShotView.setScaleX(scaleX);
                mScreenShotView.setScaleY(scaleY);*//*
                *//*WindowManager.LayoutParams lp = mAlertDialog.getWindow().getAttributes();
                lp.width = (int)(scaleX * mScreenWidth);
                lp.height = (int)(scaleY * mScreenHeight);
                mAlertDialog.getWindow().setAttributes(lp);*//*
                *//*ViewGroup.LayoutParams lp = mScreenShotView.getLayoutParams();
                lp.width = (int)(scaleX * mScreenWidth);
                lp.height = (int)(scaleY * mScreenHeight);*//*
                //mScreenShotView
                mScreenShotView.setLayoutParams(new FrameLayout.LayoutParams((int)(scaleX * mScreenWidth), (int)(scaleY * mScreenHeight) ));
            }
        });
        return v1;
    }*/

    private void setAssitiveTouchViewAlign(){
        int mAssistiveTouchViewWidth = mAssistiveTouchView.getMeasuredWidth();
        int mAssistiveTouchViewHeight = mAssistiveTouchView.getMeasuredHeight();
        int top = mParams.y + mAssistiveTouchViewWidth/2;
        int left = mParams.x + mAssistiveTouchViewHeight/2;
        int right = mScreenWidth - mParams.x - mAssistiveTouchViewWidth/2;
        int bottom = mScreenHeight - mParams.y - mAssistiveTouchViewHeight/2;
        int lor = Math.min(left, right);
        int tob = Math.min(top, bottom);
        int min = Math.min(lor, tob);
        lastAssistiveTouchViewX = mParams.x;
        lastAssistiveTouchViewY = mParams.y;
        if(min == top) mParams.y = 0;
        if(min == left) mParams.x = 0;
        if(min == right) mParams.x = mScreenWidth - mAssistiveTouchViewWidth;
        if(min == bottom) mParams.y = mScreenHeight - mAssistiveTouchViewHeight;
        myAssitiveTouchAnimator(lastAssistiveTouchViewX, mParams.x, lastAssistiveTouchViewY, mParams.y, false).start();
    }

    private void showScreenshot(String name){
        String path = "/sdcard/Pictures/" + name + ".png";
        Bitmap bitmap = BitmapFactory.decodeFile(path);

        mScreenShotView = mInflater.inflate(R.layout.screen_shot_show, null);
        ImageView imageView = (ImageView)mScreenShotView.findViewById(R.id.screenshot);
        imageView.setImageBitmap(bitmap);

        mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mAlertDialog.show();
        WindowManager.LayoutParams alertDialogParams = mAlertDialog.getWindow().getAttributes();
        alertDialogParams.width = mScreenWidth;
        alertDialogParams.height = mScreenHeight;
        mAlertDialog.getWindow().setAttributes(alertDialogParams);
        mAlertDialog.getWindow().setContentView(mScreenShotView);

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = mHandler.obtainMessage();
                msg.what = 2;
                mHandler.sendMessage(msg);
            }
        }, 3000);

        /*mSceenShotAnimator().start();*/

        /*ObjectAnimator.ofFloat(mScreenShotView, "translationX", 0, mScreenWidth-mScreenShotView.getX());
        ObjectAnimator.ofFloat(mScreenShotView, "translationY", 0, mScreenHeight-mScreenShotView.getY());
        ObjectAnimator.ofFloat(mScreenShotView, "scaleX", 1, 0);
        ObjectAnimator.ofFloat(mScreenShotView, "scaleY", 1, 0);*/

        /*mScreenShotView.setPivotX();
        mScreenShotView.setPivotY();*/
        /*PropertyValuesHolder p1 = PropertyValuesHolder.ofFloat("X", 0, mScreenWidth);
        PropertyValuesHolder p2 = PropertyValuesHolder.ofFloat("Y", 0, mScreenHeight/2);
        PropertyValuesHolder p3 = PropertyValuesHolder.ofFloat("scaleX", 1, 0.5F);
        PropertyValuesHolder p4 = PropertyValuesHolder.ofFloat("scaleY", 1, 0.5F);
        ObjectAnimator.ofPropertyValuesHolder(mScreenShotView,p1,p2,p3,p4).setDuration(2000).start();*/
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    showScreenshot((String)msg.obj);
                    break;
                case 2:
                    mAlertDialog.dismiss();
                default:
                    mPopupWindow.dismiss();
                    break;
            }
            super.handleMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWindowManager.removeView(mAssistiveTouchView);
    }
}
