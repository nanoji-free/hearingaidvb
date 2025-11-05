package com.github.nanoji_free.hearingaidvb;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class MainActivity extends AppCompatActivity {
            private SharedPreferences prefs;
            private ConstraintLayout rootLayout;
            private float savedBalance;// 現在はService通知用。将来的にUI表示にも使う可能性あり

            // SharedPreferences 用フィールド
            private static final int REQUEST_AUDIO_PERMISSION = 200;

            // UI コンポーネント
            private Button toggleButton;
            private TextView statusText;
            private SeekBar volumeSeekBar;
            private Button presetButtonMA;
            private TextView tvVolumeValue;
            private Switch noiseFilterSwitch;
            private Switch emphasisSwitch;
            private Button button_settings;
            private Button button_easysettings;
            private ImageView centerImage;

            // 録音・再生状態ほか
            private boolean isStreaming = false;
            private static final String TAG = "MainActivity";
            private static final float  DEFAULT_VOLUME_FLOAT = 0.65f;
            private static final float    DEFAULT_BALANCE = 0f;  // SeekBar progress の中央

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                // View のバインド
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);

                rootLayout = findViewById(R.id.rootLayout);
                if (rootLayout != null) {
                    DispHelper.applySavedBackground(this, rootLayout);
                }
                toggleButton = findViewById(R.id.toggleButton);
                statusText = findViewById(R.id.statusText);
                volumeSeekBar = findViewById(R.id.volumeSeekBar);
                presetButtonMA = findViewById(R.id.presetButtonMA);
                tvVolumeValue = findViewById(R.id.tvVolumeValue);
                noiseFilterSwitch = findViewById(R.id.noiseFilterSwitch);
                emphasisSwitch   = findViewById(R.id.emphasisSwitch);
                centerImage = findViewById(R.id.centerImage);

                prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);
                savedBalance = prefs.getFloat(PrefKeys.PREF_BALANCE, DEFAULT_BALANCE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                1001);
                    }
                    NotificationChannel channel = new NotificationChannel(
                        "channel_id",
                        "静かな語りかけ",
                        NotificationManager.IMPORTANCE_DEFAULT
                    );
                    channel.setDescription("音声処理の状態をそっとお知らせします");
                    NotificationManager manager = getSystemService(NotificationManager.class);
                    if (manager != null) {
                        manager.createNotificationChannel(channel);
                    }
                }

                //アプリ起動時にもサービスへ初期バランスを通知
                if (isStreaming) {
                    startService(new Intent(this, AudioStreamService.class)
                            .putExtra(PrefKeys.EXTRA_BALANCE, savedBalance));
                }

                // トグル状態を復元
                isStreaming = prefs.getBoolean("isStreaming", false);
                updateUi();

                // 前回ONだったら自動的に開始
                if (isStreaming) {
                    checkPermissionAndStart();
                }

                // 初回起動時デフォルトを65％に設定
                if (!prefs.contains(PrefKeys.PREF_VOLUME)) {
                    prefs.edit()
                            .putFloat(PrefKeys.PREF_VOLUME, 0.65f)
                            .apply();
                }

                //保存済み設定の読み込み（デフォルト値あり）
                float savedVolume     = prefs.getFloat(PrefKeys.PREF_VOLUME, 0.65f);
                boolean savedNoise = prefs.getBoolean(PrefKeys.PREF_NOISE_FILTER, false);
                boolean savedEmphasis= prefs.getBoolean(PrefKeys.PREF_EMPHASIS, false);

        /*
        // 読み込んだ値を UI に反映
        int savedProg = (int)(savedVolume * 100);
        volumeSeekBar.setProgress(savedProg);
        tvVolumeValue.setText(String.format("音量調整: %.2f×", savedVolume));
        noiseFilterSwitch.setChecked(savedNoise);
        emphasisSwitch.setChecked(savedEmphasis);

        // SeekBar の最大値設定（Progress は保存値を使う）
        volumeSeekBar.setMax(100);
        */
                // UI更新メソッドで一括反映
                updateUiFromPrefs();
                updateUi();

                // トグルボタン：開始／停止
                toggleButton.setOnClickListener(v -> {
                    boolean isStreaming = prefs.getBoolean(PrefKeys.PREF_IS_STREAMING, false);
                    if (!isStreaming) {
                        checkPermissionAndStart();
                    } else {
                        stopStreaming();
                    }
                });

                // 音量 SeekBar
                volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
                        if (!fromUser) return;
                        float vol = prog / 100f;
                        tvVolumeValue.setText(String.format("音量調整: %.2f×", vol));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar sb) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar sb) {
                        int prog = sb.getProgress();
                        float vol = prog / 100f;

                        // 設定は常に保存
                        prefs.edit()
                                .putFloat(PrefKeys.PREF_VOLUME, vol)
                                .apply();

                        // 作動中のときだけサービスに通知
                        if (isStreaming) {
                           startService(buildStreamingIntent(false)
                           );
                        }
                    }
                });

        //「全体の設定を戻す」ボタン（MainActivity側）
        presetButtonMA.setOnClickListener(v -> {

            // SharedPreferencesを初期化
            SettingsUtils.resetDefaults(this);
            //UIを再描画
            updateUiFromPrefs();

            if (isStreaming) {
                // サービスを一度停止して再起動することでマイク設定を反映
                 if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    stopService(new Intent(MainActivity.this, AudioStreamService.class));
                    ContextCompat.startForegroundService(this, buildStreamingIntent(true));
                } else {
                    checkPermissionAndStart(); // 権限がない場合は再要求
                }
           }
        });

        // ノイズフィルター Switch
        noiseFilterSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            //設定を保存
            prefs.edit()
                     .putBoolean(PrefKeys.PREF_NOISE_FILTER, isChecked)
                     .apply();
            // Service にノイズ抑制の ON/OFF を通知
            if (isStreaming) {
                stopService(new Intent(MainActivity.this, AudioStreamService.class));
                ContextCompat.startForegroundService(this, buildStreamingIntent(true));
            }
        });

        // 音声強調 Switch
        emphasisSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit()
                    .putBoolean(PrefKeys.PREF_EMPHASIS, isChecked)
                    .apply();

            // 音声強調がOFFになったら限界突破もOFFにする
            if (!isChecked) {
                prefs.edit()
                    .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false)
                    .apply();
            }

            if (isStreaming) {
                startService(buildStreamingIntent(false));
            }
        });

        //遷移ボタン（詳細設定）
        button_settings = findViewById(R.id.button_settings);
        button_settings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        //遷移ボタン（かんたん設定）
        button_easysettings = findViewById(R.id.button_easysettings);
        button_easysettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EasysettingsActivity.class);
            startActivity(intent);
        });
            }

        private void checkPermissionAndStart() {
        // （ここに startStreaming を呼ぶロジックを入れる）
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO_PERMISSION
            );
        } else {
            startStreaming();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            // 許可された場合は録音開始
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStreaming();

            // 一度拒否のみなら再リクエスト
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.RECORD_AUDIO)) {
                showRationaleDialog();

            // 永続拒否ユーザーは設定／ガイドへ誘導
            } else {
                showSettingsOrGuideDialog();
            }
        }else if (requestCode == 1001) { // 通知権限のリクエストコードを仮設20251027　↓
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 通知が許可された場合の処理（通知表示）
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setSmallIcon(R.drawable.rednotification_icon)
                .setContentTitle("通知の準備が整いました")
                .setContentText("音声処理の状態を通知でお知らせします")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);
            } else {
                // 通知が拒否された場合の処理（例：案内やToastなど）
            }
        }//　←　ここまでを仮設
    }

    private void showRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("マイク権限が必要です")
                .setMessage("録音にはマイクの許可が必要です。もう一度許可しますか？")
                .setPositiveButton("はい", (d, w) -> {
                    ActivityCompat.requestPermissions(
                        this,
                        new String[]{ Manifest.permission.RECORD_AUDIO },
                        REQUEST_AUDIO_PERMISSION
                    );
                })
                .setNegativeButton("いいえ", null)
                .show();
    }

    private void showSettingsOrGuideDialog() {
        new AlertDialog.Builder(this)
                .setTitle("マイク権限を設定で許可してください")
                .setMessage("設定→アプリ→耳みみエイド→権限 でマイクをオンにしてください。")
                .setPositiveButton("設定を開く", (d, w) -> {
                    Intent i = new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(i);
                })
                .setNegativeButton("ガイドを見る", (d, w) -> {
                    // 既存の「使い方ガイド」を開く処理
                    showUsageGuideDialog();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (rootLayout == null) {
            rootLayout = findViewById(R.id.rootLayout);
        }

        DispHelper.applySavedBackground(this, rootLayout);
        updateUiFromPrefs(); // ← prefsからUIを再描画

        if (isStreaming) {
            startService(buildStreamingIntent(false));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //AudioStreamServiceの停止
        boolean isStreaming = prefs.getBoolean("isStreaming", false);
        if (!isStreaming) {
            stopService(new Intent(this, AudioStreamService.class));
        }
    }

    private void startStreaming() {
        ContextCompat.startForegroundService(this, buildStreamingIntent(true));
        isStreaming = true;
        prefs.edit().putBoolean("isStreaming", true).apply();
        updateUi();
    }

    private void stopStreaming() {
        Intent intent = new Intent(this, AudioStreamService.class);
        stopService(intent);
        isStreaming = false;
        prefs.edit().putBoolean("isStreaming", false).apply();
        updateUi();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "マイク権限がないため、startStreaming を中止します");
            return;
            }
        }

    private void updateUi() {
        if (statusText != null) {
            statusText.setText(isStreaming ? "作動中" : "動作停止中");
        }
        if (toggleButton != null) {
            toggleButton.setText(isStreaming ? "停止" : "開始");
        }
        // 必要なら他のUIの有効/無効を切り替える
    }
    private void showUsageGuideDialog() {
        new AlertDialog.Builder(this)
                .setTitle("使い方ガイド")
                .setMessage("このアプリを利用する際にはスマホのマイクの権限を「ON」にする必要があります。\n　\n（現在は「OFF」なので切り替えが必要です。）\n　\n設定からマイクの権限を許可するようにして下さい。")  //ここも更新！
                .setPositiveButton("OK", null)
                .show();
    }
    private void updateUiFromPrefs() {
        float savedVolume = prefs.getFloat(PrefKeys.PREF_VOLUME, DEFAULT_VOLUME_FLOAT);
        if(volumeSeekBar != null){
            volumeSeekBar.setProgress((int)(savedVolume * 100));
        }
        if(tvVolumeValue != null){
            tvVolumeValue.setText(String.format("音量調整: %.2f×", savedVolume));
        }
        if(noiseFilterSwitch != null){
            noiseFilterSwitch.setChecked(prefs.getBoolean(PrefKeys.PREF_NOISE_FILTER, false));
        }
        if (emphasisSwitch != null) {
            emphasisSwitch.setChecked(prefs.getBoolean(PrefKeys.PREF_EMPHASIS, false));
        }
        int selectedId = prefs.getInt(PrefKeys.PREF_CENTER_SELECTION, R.id.chrRecommended);
        if (centerImage != null) {
            if (selectedId == R.id.chrNone) {
                centerImage.setVisibility(View.GONE);
            } else {
                centerImage.setVisibility(View.VISIBLE);
                if (selectedId == R.id.chrRecommended) {
                    centerImage.setImageResource(R.drawable.betaimage);
                } else if (selectedId == R.id.chrChoiced) {
                    String uriString = prefs.getString(PrefKeys.PREF_CENTER_IMAGE_URI, null);
                    if (uriString != null && !uriString.contains("com.google.android.apps.photos")) {
                        try {
                            centerImage.setImageURI(Uri.parse(uriString));
                        } catch (Exception e) {
                            ImageRecoveryHelper.tryRecoverAndDisplay(this, uriString, centerImage, prefs);
                        }
                    } else {
                        centerImage.setImageResource(R.drawable.betaimage);
                    }
                }
            }
        }
    }
    private Intent buildStreamingIntent(boolean requestStreaming) {
        return new Intent(this, AudioStreamService.class)
             .putExtra(PrefKeys.EXTRA_APP_VOLUME, prefs.getFloat(PrefKeys.PREF_VOLUME, 0.65f))
             .putExtra(PrefKeys.EXTRA_BALANCE, prefs.getFloat(PrefKeys.PREF_BALANCE, 0f))
             .putExtra(PrefKeys.EXTRA_NOISE_FILTER, prefs.getBoolean(PrefKeys.PREF_NOISE_FILTER, false))
             .putExtra(PrefKeys.EXTRA_EMPHASIS, prefs.getBoolean(PrefKeys.PREF_EMPHASIS, false))
             .putExtra(PrefKeys.EXTRA_OPTION_MIC, prefs.getBoolean(PrefKeys.PREF_MIC_TYPE, false))
             .putExtra(PrefKeys.EXTRA_VOLUME_BOOST, prefs.getFloat(PrefKeys.PREF_VOLUME_BOOST, 0.0f))
             .putExtra(PrefKeys.EXTRA_SUPER_EMPHASIS, prefs.getBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false))
             .putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, requestStreaming);
        }

}
