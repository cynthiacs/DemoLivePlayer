package com.leadcore.demoliveplayer;

import android.app.Application;

import com.alivc.player.AliVcMediaPlayer;

/**
 * Created by Shenqing on 2017/11/30.
 */

public class VideoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final String businessId = "";
        AliVcMediaPlayer.init(getApplicationContext(), businessId);
    }
}
