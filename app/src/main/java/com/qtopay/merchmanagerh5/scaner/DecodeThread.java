package com.qtopay.merchmanagerh5.scaner;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import com.qtopay.litemall.CaptureActivity;

import java.util.concurrent.CountDownLatch;

/**
 * 描述: 解码线程
 */
final class DecodeThread extends Thread {

    CaptureActivity activity;
    private Handler handler;
    private final CountDownLatch handlerInitLatch;

    DecodeThread(CaptureActivity activity) {
        this.activity = activity;
        handlerInitLatch = new CountDownLatch(1);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        Log.e("chenzefeng", "scan ing1");
        handler = new DecodeHandler(activity);
        Log.e("chenzefeng", "scan ing2");
        handlerInitLatch.countDown();
        Log.e("chenzefeng", "scan in3");
        Looper.loop();
    }

}
