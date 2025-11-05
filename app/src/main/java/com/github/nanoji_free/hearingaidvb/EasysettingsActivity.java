package com.github.nanoji_free.hearingaidvb;
//TODO:アプデがあればその表示をする機能をこのアクティビティに実装予定

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class EasysettingsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private boolean isStreaming = false;

    private Button firstEasySetButton;
    private Button secondEasySetButton;
    private Button thirdEasySetButton;
    private Button callSConeButton;
    private Button recSConeButton;
    private Button callSCtwoButton;
    private Button recSCtwoButton;
    private Button EasySettingsReturnButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_easysettings);
        TextView versionNoticeView = findViewById(R.id.versionNoticeView);

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            DispHelper.applySavedBackground(this, rootLayout);
        }

        prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);
        float savedBalance = prefs.getFloat(PrefKeys.PREF_BALANCE, 0f);
        isStreaming = prefs.getBoolean("isStreaming", false);

        if (isStreaming) {
            startService(new Intent(this, AudioStreamService.class)
                    .putExtra(PrefKeys.EXTRA_BALANCE, savedBalance));
        }

        //卓上モードの設定
        firstEasySetButton = findViewById(R.id.firstEasySetButton);
        firstEasySetButton.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(PrefKeys.PREF_NOISE_FILTER, true)        // ノイズ除去ON
                    .putBoolean(PrefKeys.PREF_EMPHASIS, true)            // 音声強調ON
                    .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, true)      // 強調ブーストON
                    .apply();
            //サービスが動作中なら動作を止めて設定を読み込んで再起動する
            if (isStreaming) {
                stopService(new Intent(this, AudioStreamService.class));
                startService(new Intent(this, AudioStreamService.class)
                        .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, true));
            }
            //トースト表示で戻切りかえを伝達する
            Toast.makeText(this, "「卓上モード」に切り替えました。\n\nワイヤレス骨伝導マイクの利用をお勧めします。", Toast.LENGTH_SHORT).show();
        });

        //テレビ視聴モードのボタン
        secondEasySetButton = findViewById(R.id.secondEasySetButton);
        secondEasySetButton.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(PrefKeys.PREF_NOISE_FILTER, false)       // ノイズ除去OFF
                    .putBoolean(PrefKeys.PREF_MIC_TYPE, false)           // 内部マイク（外部マイクOFF）
                    .putBoolean(PrefKeys.PREF_EMPHASIS, true)            // 音声強調ON
                    .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, true)      // 強調ブーストON
                    .apply();

            if (isStreaming) {
                stopService(new Intent(this, AudioStreamService.class));
                startService(new Intent(this, AudioStreamService.class)
                        .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, true));
            }
            Toast.makeText(this, "「テレビ視聴モード」に切り替えました。\n\nワイヤレス骨伝導マイクの利用をお勧めします。", Toast.LENGTH_SHORT).show();
        });

        // お出かけモードボタン
        thirdEasySetButton = findViewById(R.id.thirdEasySetButton);
        thirdEasySetButton.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(PrefKeys.PREF_NOISE_FILTER, true)        // ノイズ除去ON
                    .putBoolean(PrefKeys.PREF_EMPHASIS, true)            // 音声強調ON
                    .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, true)      // 強調ブーストON
                    .apply();

            if (isStreaming) {
                stopService(new Intent(this, AudioStreamService.class));
                startService(new Intent(this, AudioStreamService.class)
                        .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, true));
            }
            Toast.makeText(this, "「お出かけモード」に切り替えました。\n\n有線式のマイクの利用をお勧めします。", Toast.LENGTH_SHORT).show();
        });

        // preset1を呼びだすボタン
        callSConeButton = findViewById(R.id.callSConeButton);
        callSConeButton.setOnClickListener(v -> {
            if (!prefs.contains(PrefKeys.PRESET1_VOLUME)) {
                Toast.makeText(this, "プリセット1がまだ記憶されていません。", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putFloat(PrefKeys.PREF_VOLUME, prefs.getFloat(PrefKeys.PRESET1_VOLUME, 0.65f))
                    .putBoolean(PrefKeys.PREF_NOISE_FILTER, prefs.getBoolean(PrefKeys.PRESET1_NOISE_FILTER, false))
                    .putBoolean(PrefKeys.PREF_EMPHASIS, prefs.getBoolean(PrefKeys.PRESET1_EMPHASIS, false))
                    .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, prefs.getBoolean(PrefKeys.PRESET1_SUPER_EMPHASIS, false))
                    .putFloat(PrefKeys.PREF_BALANCE, prefs.getFloat(PrefKeys.PRESET1_BALANCE, 0f))
                    .putBoolean(PrefKeys.PREF_MIC_TYPE, prefs.getBoolean(PrefKeys.PRESET1_MIC_TYPE, false))
                    .putFloat(PrefKeys.PREF_VOLUME_BOOST, prefs.getFloat(PrefKeys.PRESET1_VOLUME_BOOST, 0f))
                    .putFloat(PrefKeys.PREF_DEPTH_SCALER, prefs.getFloat(PrefKeys.PRESET1_DEPTH_SCALER, 1.0f))
                    .apply();

            if (isStreaming) {
                stopService(new Intent(this, AudioStreamService.class));
                startService(new Intent(this, AudioStreamService.class)
                        .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, true));
            }

            Toast.makeText(this, "プリセット1を呼び出しました。\n語りの芯を復元しました。", Toast.LENGTH_SHORT).show();
        });

        //　１のプリセットの記憶をするrecSConeButton
        recSConeButton = findViewById(R.id.recSConeButton);
        recSConeButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("プリセット1の保存")
                    .setMessage("現在の設定をプリセット1として記憶しますか？")
                    .setPositiveButton("はい", (dialog, which) -> {
                        prefs.edit()
                                .putFloat(PrefKeys.PRESET1_VOLUME, prefs.getFloat(PrefKeys.PREF_VOLUME, 0.65f))               // 音量
                                .putBoolean(PrefKeys.PRESET1_NOISE_FILTER, prefs.getBoolean(PrefKeys.PREF_NOISE_FILTER, false)) // ノイズ除去
                                .putBoolean(PrefKeys.PRESET1_EMPHASIS, prefs.getBoolean(PrefKeys.PREF_EMPHASIS, false))         // 音声強調
                                .putBoolean(PrefKeys.PRESET1_SUPER_EMPHASIS, prefs.getBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false)) // 強調ブースト
                                .putFloat(PrefKeys.PRESET1_BALANCE, prefs.getFloat(PrefKeys.PREF_BALANCE, 0f))                  // バランス
                                .putBoolean(PrefKeys.PRESET1_MIC_TYPE, prefs.getBoolean(PrefKeys.PREF_MIC_TYPE, false))        // マイク種別
                                .putFloat(PrefKeys.PRESET1_VOLUME_BOOST, prefs.getFloat(PrefKeys.PREF_VOLUME_BOOST, 0f))       // ブースト係数
                                .putFloat(PrefKeys.PRESET1_DEPTH_SCALER, prefs.getFloat(PrefKeys.PREF_DEPTH_SCALER, 1.0f))     // 深さスケーラ
                                .apply();

                        Toast.makeText(this, "プリセット1を記憶しました。\n今の状態をいつでも呼出すことができます。", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("いいえ", null)
                    .show();
        });

        // 2のプリセットを呼び出すボタン
        callSCtwoButton = findViewById(R.id.callSCtwoButton);
        callSCtwoButton.setOnClickListener(v -> {
            if (!prefs.contains(PrefKeys.PRESET2_VOLUME)) {
                Toast.makeText(this, "プリセット2がまだ記憶されていません。", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putFloat(PrefKeys.PREF_VOLUME, prefs.getFloat(PrefKeys.PRESET2_VOLUME, 0.65f))
                    .putBoolean(PrefKeys.PREF_NOISE_FILTER, prefs.getBoolean(PrefKeys.PRESET2_NOISE_FILTER, false))
                    .putBoolean(PrefKeys.PREF_EMPHASIS, prefs.getBoolean(PrefKeys.PRESET2_EMPHASIS, false))
                    .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, prefs.getBoolean(PrefKeys.PRESET2_SUPER_EMPHASIS, false))
                    .putFloat(PrefKeys.PREF_BALANCE, prefs.getFloat(PrefKeys.PRESET2_BALANCE, 0f))
                    .putBoolean(PrefKeys.PREF_MIC_TYPE, prefs.getBoolean(PrefKeys.PRESET2_MIC_TYPE, false))
                    .putFloat(PrefKeys.PREF_VOLUME_BOOST, prefs.getFloat(PrefKeys.PRESET2_VOLUME_BOOST, 0f))
                    .putFloat(PrefKeys.PREF_DEPTH_SCALER, prefs.getFloat(PrefKeys.PRESET2_DEPTH_SCALER, 1.0f))
                    .apply();

            if (isStreaming) {
                stopService(new Intent(this, AudioStreamService.class));
                startService(new Intent(this, AudioStreamService.class)
                        .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, true));
            }

            Toast.makeText(this, "プリセット2を呼び出しました。\n語りの芯を復元しました。", Toast.LENGTH_SHORT).show();
        });

        //　2のプリセットの記憶をするrecSCtwoButton
        recSCtwoButton = findViewById(R.id.recSCtwoButton);
        recSCtwoButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("プリセット2の保存")
                    .setMessage("現在の設定をプリセット2として記憶しますか？")
                    .setPositiveButton("はい", (dialog, which) -> {
                        prefs.edit()
                                .putFloat(PrefKeys.PRESET2_VOLUME, prefs.getFloat(PrefKeys.PREF_VOLUME, 0.65f))               // 音量
                                .putBoolean(PrefKeys.PRESET2_NOISE_FILTER, prefs.getBoolean(PrefKeys.PREF_NOISE_FILTER, false)) // ノイズ除去
                                .putBoolean(PrefKeys.PRESET2_EMPHASIS, prefs.getBoolean(PrefKeys.PREF_EMPHASIS, false))         // 音声強調
                                .putBoolean(PrefKeys.PRESET2_SUPER_EMPHASIS, prefs.getBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false)) // 強調ブースト
                                .putFloat(PrefKeys.PRESET2_BALANCE, prefs.getFloat(PrefKeys.PREF_BALANCE, 0f))                  // バランス
                                .putBoolean(PrefKeys.PRESET2_MIC_TYPE, prefs.getBoolean(PrefKeys.PREF_MIC_TYPE, false))        // マイク種別
                                .putFloat(PrefKeys.PRESET2_VOLUME_BOOST, prefs.getFloat(PrefKeys.PREF_VOLUME_BOOST, 0f))       // ブースト係数
                                .putFloat(PrefKeys.PRESET2_DEPTH_SCALER, prefs.getFloat(PrefKeys.PREF_DEPTH_SCALER, 1.0f))     // 深さスケーラ
                                .apply();

                        Toast.makeText(this, "プリセット2を記憶しました。\n今の状態をいつでも呼出すことができます。", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("いいえ", null)
                    .show();
        });

        //　「もどる」 ボタン
        EasySettingsReturnButton = findViewById(R.id.EasySettingsReturnButton);
        EasySettingsReturnButton.setOnClickListener(v -> {
            finish();
        });

        //新バージョンのお知らせ
        new Thread(() -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/nanoji-free/mimimimi-notice-data/main/mimimimi-notice-data.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONObject json = new JSONObject(result.toString());
                int remoteVersionCode = json.getInt("versionCode");

                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                long currentVersionCode = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        ? packageInfo.getLongVersionCode()
                        : packageInfo.versionCode;

                if (remoteVersionCode > currentVersionCode) {
                    //
                    String message = "新しいバージョンにアップデートできます！\n\n▶ 詳細はこちら: https://play.google.com/store/apps/details?id=com.github.nanoji_free.hearingaidvb";
                    runOnUiThread(() -> {
                        versionNoticeView.setText(message);
                        versionNoticeView.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            DispHelper.applySavedBackground(this, rootLayout);
        }
    }
}

