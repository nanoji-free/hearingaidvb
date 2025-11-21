package com.littlebit.hearingaid.b;

// TODO: BillingLibruaryの関連コードの実装
// TODO: 本番用JSONの準備、現状のテスト用のコードからの更新
// TODO: GooglePlayConsoleの準備

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private ConstraintLayout rootLayout;
    private boolean isStreaming = false;
    float volumeBoost = 0.00f;
    private float depthScaler = 1.0f;
    private Switch noiseFilterSwitch;
    private Switch emphasisSwitch;
    private Switch optionMic;
    private Switch superEnphasSwitch;
    private TextView volBoostText;
    private SeekBar volumeBoostSeekBar;
    private Switch hearingProfileSwitch;
    private SeekBar stSeekBar;
    private Button balPresetButton;
    private Button presetButton;
    private Button toChangeViewButton;
    private Button toChangeHearingButton;
    private Switch safeModeSwitch;
    private Button returnButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            DispHelper.applySavedBackground(this, rootLayout);
        }

        noiseFilterSwitch = findViewById(R.id.noiseFilterSwitch);
        emphasisSwitch = findViewById(R.id.emphasisSwitch);

        prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);
        float savedBalance = prefs.getFloat(PrefKeys.PREF_BALANCE, 0f);
        isStreaming = prefs.getBoolean("isStreaming", false);

        //アプリ起動時にもサービスへ初期バランスを通知(作動中のときのみバランスを通知）
        //if (isStreaming) {
        //    startService(new Intent(this, AudioStreamService.class)
        //            .putExtra(PrefKeys.EXTRA_BALANCE, savedBalance));
        //}　　//20251105遷移時にサービスが動作してしまう誤動作の防止のためコメントアウト

        //「オプションマイク　on/off」スイッチ
        optionMic = findViewById(R.id.optionMic);
        optionMic.setOnCheckedChangeListener((btn, isChecked) -> {
            //外部マイクの切り替えを組み込む
            //　設定の保存
            prefs.edit()
                    .putBoolean(PrefKeys.PREF_MIC_TYPE, isChecked)
                    .apply();
            // サービスへの通知
            //boolean isStreaming = prefs.getBoolean("isStreaming", false);
            if (isStreaming) {
                // 一度サービスを停止
                stopService(new Intent(SettingsActivity.this, AudioStreamService.class));
                // 再起動リクエストを送信
                Intent intent = new Intent(SettingsActivity.this, AudioStreamService.class)
                        .putExtra(PrefKeys.EXTRA_OPTION_MIC, isChecked)
                        .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, true);
                startService(intent);
            }
        });

        //「音声強調機能ブースト」スイッチ
        superEnphasSwitch = findViewById(R.id.superEmphasisSwitch);
        superEnphasSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit()
                    .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, isChecked)
                    .apply();
            //MainActivityの音声強調スイッチもオンにする
            if (isChecked) {
                prefs.edit()
                        .putBoolean(PrefKeys.PREF_EMPHASIS, true)
                        .apply();
            }
            boolean isStreaming = prefs.getBoolean("isStreaming", false);
            if (isStreaming) {
                startService(buildStreamingIntent(false));
            }
        });

        //「聴力プロファイル補正」スイッチ
        hearingProfileSwitch = findViewById(R.id.hearingProfileSwitch);
        boolean isCorrectionEnabled = prefs.getBoolean(PrefKeys.PREF_HEARING_PROFILE_CORRECTION, false);
        hearingProfileSwitch.setChecked(isCorrectionEnabled);
        hearingProfileSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit()
                    .putBoolean(PrefKeys.PREF_HEARING_PROFILE_CORRECTION, isChecked)
                    .apply();
            //サービスに通知する必要があるので追記する。20251111、内容が実装されていないので検討別途

        });

        //音量増幅シークバーのテキスト
        volBoostText = findViewById(R.id.volBoost);
        // 初期表示（Prefsから取得して反映）
        float savedBoost = prefs.getFloat(PrefKeys.PREF_VOLUME_BOOST, 0f);
        if (volBoostText != null) {
            volBoostText.setText("音量増量ブースター（" + Math.round(savedBoost * 100) + "%）");
        }

        //音量増幅シークバー
        volumeBoostSeekBar = findViewById(R.id.volumeBoostSeekBar);
        volumeBoostSeekBar.setMax(100);
        volumeBoostSeekBar.setProgress(0); // 仮の初期値

        volumeBoostSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //音量増幅シークバーの中身を実装する。
            //変数を　５０～１００（上限値は１００でいいのか検討別途）の範囲で値を返す。
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (volBoostText != null) {
                    volBoostText.setText("音量増量ブースター（" + progress + "%）");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 必要ならここに処理を書く（未使用なら空でOK）
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 設定保存やサービス通知をここに追加
                int progress = seekBar.getProgress(); // 0〜100
                volumeBoost = progress / 100f;        // 0.0〜1.0 に正規化
                depthScaler = Math.max(0.3f, 1.0f - volumeBoost);

                prefs.edit()
                        .putFloat(PrefKeys.PREF_VOLUME_BOOST, volumeBoost)
                        .putFloat(PrefKeys.PREF_DEPTH_SCALER, depthScaler)
                        .apply();

                startService(buildStreamingIntent(false));
            }
        });

        //左右バランスシークバー
        stSeekBar = findViewById(R.id.stSeekBar);
        stSeekBar.setMax(200);
        int initProg = Math.round((savedBalance + 1f) * stSeekBar.getMax() / 2f);
        stSeekBar.setProgress(initProg);

        stSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) { }
            @Override public void onStartTrackingTouch(SeekBar sb) { }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                int prog = sb.getProgress();                  // 0〜200
                // float バランス値にマッピング（-1..+1）
                float balance = (prog / (float)sb.getMax()) * 2f - 1f;
                // 永続化（float 型で保存）
                prefs.edit()
                        .putFloat(PrefKeys.PREF_BALANCE, balance)
                        .apply();
                // サービスへバランス更新を通知
                Intent intent = new Intent(SettingsActivity.this, AudioStreamService.class)
                        .putExtra(PrefKeys.EXTRA_BALANCE, balance)
                        .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false);
                startService(intent);
            }
        });

        //左右バランスを初期化するボタン
        balPresetButton =findViewById(R.id.balPresetButton);
        balPresetButton.setOnClickListener(v -> {
            // デフォルト値を再登録
            prefs.edit()
                    .putFloat(PrefKeys.PREF_BALANCE, 0f)
                    .apply();
            //  UI に反映
            stSeekBar.setProgress(stSeekBar.getMax() / 2);
            // Service にもデフォルト値を通知
            Intent i = new Intent(SettingsActivity.this, AudioStreamService.class)
                    .putExtra(PrefKeys.EXTRA_BALANCE, 0f)
                    .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false);
            startService(i);
        });

        //設定全体を初期化するボタン
        presetButton =findViewById(R.id.presetButton);
        presetButton.setOnClickListener(v -> {
            // SharedPreferencesを初期化
            //SettingsUtils.resetDefaults(this);
            prefs.edit()
                    .putFloat(PrefKeys.PREF_VOLUME, 0.65f)
                    .putBoolean(PrefKeys.PREF_NOISE_FILTER, false)
                    .putBoolean(PrefKeys.PREF_EMPHASIS, false)
                    .putFloat(PrefKeys.PREF_BALANCE, 0f)
                    .putFloat(PrefKeys.PREF_VOLUME_BOOST, 0f)
                    .putFloat(PrefKeys.PREF_DEPTH_SCALER, 1.0f)
                    .putBoolean(PrefKeys.PREF_MIC_TYPE, false)
                    .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false)
                    .putBoolean(PrefKeys.PREF_HEARING_PROFILE_CORRECTION, false)
                    .apply();
            // volumeBoost の変数も初期化
            volumeBoost = 0f;

            //  UI に反映(nullチェックをして反映）
            if (noiseFilterSwitch != null) {
                noiseFilterSwitch.setChecked(false);
            }
            if (emphasisSwitch != null) {
                emphasisSwitch.setChecked(false);
            }
            if (stSeekBar != null) {
                stSeekBar.setProgress(stSeekBar.getMax() / 2);
            }
            if (optionMic != null) {
                optionMic.setChecked(false);
            }
            if (superEnphasSwitch != null) {
                superEnphasSwitch.setChecked(false);
            }
            if (volumeBoostSeekBar != null) {
                volumeBoostSeekBar.setProgress(Math.round(volumeBoost * 100));
                if (volBoostText != null) {
                    volBoostText.setText("音量増量ブースター（" + Math.round(volumeBoost * 100) + "%）");
                }
            }
            if (hearingProfileSwitch != null) {
                hearingProfileSwitch.setChecked(false);
            }
            boolean isStreaming = prefs.getBoolean("isStreaming", false);
            if (isStreaming) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    startService(buildStreamingIntent(false));
                } else {
                    Toast.makeText(this, "マイク権限が必要です。設定から許可してください。", Toast.LENGTH_LONG).show();
                }
            }
        });

        //「画面設定へ」遷移ボタン
        toChangeViewButton = findViewById(R.id.toChangeViewButton);
        toChangeViewButton.setOnClickListener(v -> {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("画像設定に進みます")
                    .setMessage("この操作により音声処理が一時停止されます。\nよろしいですか？")
                    .setPositiveButton("はい", (dialog, which) -> {
                        // サービス停止
                        stopService(new Intent(SettingsActivity.this, AudioStreamService.class));
                        // 運転状況の更新
                        prefs.edit().putBoolean(PrefKeys.PREF_IS_STREAMING, false).apply();
                        // 遷移
                        Intent intent = new Intent(SettingsActivity.this, DisplaySettingsActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton("いいえ", null)
                    .show();
        });
        //「聞こえ方の設定へ」遷移ボタン　toChangeHearingButton
        toChangeHearingButton = findViewById(R.id.toChangeHearingButton);
        toChangeHearingButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, HearingProfileActivity.class);
            startActivity(intent);
        });

        //「safeModeSwitch」スイッチ
        safeModeSwitch = findViewById(R.id.safeModeSwitch);
        boolean isSafeMode = prefs.getBoolean(PrefKeys.PREF_SAFE_MODE_ENABLED, false);
        safeModeSwitch.setChecked(isSafeMode);
        updateSafeModeUi(isSafeMode); // UI制御（toChangeViewButtonなど）

        safeModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PrefKeys.PREF_SAFE_MODE_ENABLED, isChecked).apply();

            // UI制御を即時反映
            updateSafeModeUi(isChecked);

            // トースト通知で語りを補足
            Toast.makeText(this,
                    isChecked ? "セーフモードを有効にしました（安定性優先）" : "セーフモードを解除しました（通常モード）",
                    Toast.LENGTH_SHORT).show();
        });

        //「戻る」ボタン
        returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(v -> {
            finish(); // 現在のActivityを終了して、前の画面（MainActivity）に戻る
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (rootLayout == null) {
            rootLayout = findViewById(R.id.rootLayout);
        }

        DispHelper.applySavedBackground(this, rootLayout);

        isStreaming = prefs.getBoolean("isStreaming", false);

        if (optionMic != null) {
            optionMic.setChecked(prefs.getBoolean(PrefKeys.PREF_MIC_TYPE, false));
        }
        if (superEnphasSwitch != null) {
            superEnphasSwitch.setChecked(prefs.getBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false));
        }

        float balance = prefs.getFloat(PrefKeys.PREF_BALANCE, 0f);
        if (stSeekBar != null) {
            stSeekBar.setProgress(Math.round((balance + 1f) * stSeekBar.getMax() / 2f));
        }

        volumeBoost = prefs.getFloat(PrefKeys.PREF_VOLUME_BOOST, 0f);
        if (volumeBoostSeekBar != null) {
            volumeBoostSeekBar.setProgress(Math.round(volumeBoost * 100));
        }
        if (volBoostText != null) {
            volBoostText.setText("音量増量ブースター（" + Math.round(volumeBoost * 100) + "%）");
        }
        if (prefs.contains(PrefKeys.PREF_DEPTH_SCALER)) {
            depthScaler = prefs.getFloat(PrefKeys.PREF_DEPTH_SCALER, 1.0f);
        } else {
            depthScaler = 1.0f;
        }

        boolean isSafeMode = prefs.getBoolean(PrefKeys.PREF_SAFE_MODE_ENABLED, false);
        if (safeModeSwitch != null) {
            safeModeSwitch.setChecked(isSafeMode);
        }
        if (hearingProfileSwitch != null) {
            hearingProfileSwitch.setChecked(prefs.getBoolean(PrefKeys.PREF_HEARING_PROFILE_CORRECTION, false));
        }



        updateSafeModeUi(isSafeMode);

    }
    private void updateSafeModeUi(boolean isSafeMode) {
        if (toChangeViewButton != null) {
            toChangeViewButton.setEnabled(!isSafeMode);
            toChangeViewButton.setAlpha(isSafeMode ? 0.5f : 1.0f);
            toChangeViewButton.setText(isSafeMode
                    ? "セーフモード中は画像の変更はできません"
                    : "画面構成を変更する");
        }
    }
    private Intent buildStreamingIntent(boolean requestStreaming) {
        return new Intent(SettingsActivity.this, AudioStreamService.class)
                .putExtra(PrefKeys.EXTRA_APP_VOLUME, prefs.getFloat(PrefKeys.PREF_VOLUME, 0.65f))
                .putExtra(PrefKeys.EXTRA_BALANCE, prefs.getFloat(PrefKeys.PREF_BALANCE, 0f))
                .putExtra(PrefKeys.EXTRA_NOISE_FILTER, prefs.getBoolean(PrefKeys.PREF_NOISE_FILTER, false))
                .putExtra(PrefKeys.EXTRA_EMPHASIS, prefs.getBoolean(PrefKeys.PREF_EMPHASIS, false))
                .putExtra(PrefKeys.EXTRA_OPTION_MIC, prefs.getBoolean(PrefKeys.PREF_MIC_TYPE, false))
                .putExtra(PrefKeys.EXTRA_VOLUME_BOOST, prefs.getFloat(PrefKeys.PREF_VOLUME_BOOST, 0.0f))
                .putExtra(PrefKeys.EXTRA_DEPTH_SCALER, prefs.getFloat(PrefKeys.PREF_DEPTH_SCALER, 1.0f))
                .putExtra(PrefKeys.EXTRA_SUPER_EMPHASIS, prefs.getBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false))
                .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, requestStreaming);
    }
}














