package com.github.nanoji_free.hearingaidvb;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

public class SettingsUtils {
    public static void resetDefaults(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE);
        boolean wasStreaming = prefs.getBoolean(PrefKeys.PREF_IS_STREAMING, false);//isStreaming を退避

        //　デフォルト値を再登録 (初回65％／ノイズOFF／強調OFF)
        prefs.edit()
                .putFloat(PrefKeys.PREF_VOLUME, 0.65f)
                .putBoolean(PrefKeys.PREF_NOISE_FILTER, false)
                .putBoolean(PrefKeys.PREF_EMPHASIS, false)
                .putFloat(PrefKeys.PREF_BALANCE, 0f)
                .putBoolean(PrefKeys.PREF_MIC_TYPE, false)
                .putFloat(PrefKeys.PREF_VOLUME_BOOST, 0.0f)
                .putFloat(PrefKeys.PREF_DEPTH_SCALER, 1.0f)
                .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false)
                .putBoolean(PrefKeys.PREF_IS_STREAMING,wasStreaming)
                .apply();

        // Service にもデフォルト値を通知
        boolean isStreamingNow = prefs.getBoolean("isStreaming", false);
        if (isStreamingNow) {

            Intent i = new Intent(context, AudioStreamService.class)
                    .putExtra(PrefKeys.EXTRA_APP_VOLUME, 0.65f)
                    .putExtra(PrefKeys.EXTRA_NOISE_FILTER, false)
                    .putExtra(PrefKeys.EXTRA_EMPHASIS, false)
                    .putExtra(PrefKeys.EXTRA_BALANCE, 0f)
                    .putExtra(PrefKeys.EXTRA_OPTION_MIC, false)
                    .putExtra(PrefKeys.EXTRA_VOLUME_BOOST, 0.0f)
                    .putExtra(PrefKeys.EXTRA_DEPTH_SCALER, 1.0f)
                    .putExtra(PrefKeys.EXTRA_SUPER_EMPHASIS, false)
                    .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false);
            context.startService(i);
        }

        // 各種のスライダーの内容を即時反映にする(AudioStreamServiceに渡すだけでなくボリュームと左右バランスを反映させる）
        if (isStreamingNow) {
           new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent confirmIntent = new Intent(context, AudioStreamService.class)
                        .putExtra(PrefKeys.EXTRA_APP_VOLUME, 0.65f)
                        .putExtra(PrefKeys.EXTRA_BALANCE, 0f)
                        .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false);
                context.startService(confirmIntent);
            }, 100);
        }
    }
}
