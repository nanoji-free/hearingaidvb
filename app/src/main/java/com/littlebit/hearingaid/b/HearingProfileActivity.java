package com.littlebit.hearingaid.b;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class HearingProfileActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private SeekBar slider250, slider500, slider1000, slider2000, slider4000;
    private TextView value250, value500, value1000, value2000, value4000;
    private Button toFittingButton;
    private Button hearingReturnButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("hearing_prefs", MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hearingprofile);

        //スライダー群と数値表示のバインド
        slider250 = findViewById(R.id.slider_250);
        slider500 = findViewById(R.id.slider_500);
        slider1000 = findViewById(R.id.slider_1000);
        slider2000 = findViewById(R.id.slider_2000);
        slider4000 = findViewById(R.id.slider_4000);

        value250 = findViewById(R.id.value_250);
        value500 = findViewById(R.id.value_500);
        value1000 = findViewById(R.id.value_1000);
        value2000 = findViewById(R.id.value_2000);
        value4000 = findViewById(R.id.value_4000);

        //スライダー250の挙動
        slider250.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float gain = progress *0.03f;
                int percent = (int)(gain / 3.0f * 100); // 0〜100%
                value250.setText(percent + "%");
                prefs.edit().putFloat(PrefKeys.CORRECTION_250, gain).apply();

                // AudioStreamService に補正値を送信
                boolean isStreaming = prefs.getBoolean(PrefKeys.PREF_IS_STREAMING, false);
                if (isStreaming) {
                    Intent intent = new Intent(HearingProfileActivity.this, AudioStreamService.class);
                    intent.putExtra(PrefKeys.EXTRA_CORRECTION_250, gain);
                    intent.putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false); // 通知のみ
                    startService(intent);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //スライダー500の挙動
        slider500.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float gain = progress *0.03f;
                int percent = (int)(gain / 3.0f * 100); // 0〜100%
                value500.setText(percent + "%");
                prefs.edit().putFloat(PrefKeys.CORRECTION_500, gain).apply();
                // AudioStreamService に補正値を送信
                boolean isStreaming = prefs.getBoolean(PrefKeys.PREF_IS_STREAMING, false);
                if (isStreaming) {
                    Intent intent = new Intent(HearingProfileActivity.this, AudioStreamService.class);
                    intent.putExtra(PrefKeys.EXTRA_CORRECTION_500, gain);
                    intent.putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false); // 通知のみ
                    startService(intent);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //スライダー1000の挙動
        slider1000.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float gain = progress *0.03f;
                int percent = (int)(gain / 3.0f * 100); // 0〜100%
                value1000.setText(percent + "%");
                prefs.edit().putFloat(PrefKeys.CORRECTION_1000, gain).apply();
                // AudioStreamService に補正値を送信
                boolean isStreaming = prefs.getBoolean(PrefKeys.PREF_IS_STREAMING, false);
                if (isStreaming) {
                    Intent intent = new Intent(HearingProfileActivity.this, AudioStreamService.class);
                    intent.putExtra(PrefKeys.EXTRA_CORRECTION_1000, gain);
                    intent.putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false); // 通知のみ
                    startService(intent);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //スライダー2000の挙動
        slider2000.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float gain = progress *0.03f;
                int percent = (int)(gain / 3.0f * 100); // 0〜100%
                value2000.setText(percent + "%");
                prefs.edit().putFloat(PrefKeys.CORRECTION_2000, gain).apply();
                // AudioStreamService に補正値を送信
                boolean isStreaming = prefs.getBoolean(PrefKeys.PREF_IS_STREAMING, false);
                if (isStreaming) {
                    Intent intent = new Intent(HearingProfileActivity.this, AudioStreamService.class);
                    intent.putExtra(PrefKeys.EXTRA_CORRECTION_2000, gain);
                    intent.putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false); // 通知のみ
                    startService(intent);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //スライダー4000の挙動
        slider4000.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float gain = progress *0.03f;
                int percent = (int)(gain / 3.0f * 100); // 0〜100%
                value4000.setText(percent + "%");
                prefs.edit().putFloat(PrefKeys.CORRECTION_4000, gain).apply();
                // AudioStreamService に補正値を送信
                boolean isStreaming = prefs.getBoolean(PrefKeys.PREF_IS_STREAMING, false);
                if (isStreaming) {
                    Intent intent = new Intent(HearingProfileActivity.this, AudioStreamService.class);
                    intent.putExtra(PrefKeys.EXTRA_CORRECTION_4000, gain);
                    intent.putExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false); // 通知のみ
                    startService(intent);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //「フィッテング画面」に遷移するボタン
        toFittingButton = findViewById(R.id.toFittingButton);
        toFittingButton.setOnClickListener(v -> {
        new AlertDialog.Builder(HearingProfileActivity.this)
                .setTitle("音声の設定に進みます")
                .setMessage("この操作により音声処理が停止されます。\nよろしいですか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    // サービス停止
                    stopService(new Intent(HearingProfileActivity.this, AudioStreamService.class));
                    // 運転状況の更新
                    prefs.edit().putBoolean(PrefKeys.PREF_IS_STREAMING, false).apply();
                    // 遷移
                    Intent intent = new Intent(HearingProfileActivity.this, FittingActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("いいえ", null)
                .show();
        });

        // 「戻る」ボタンの初期化とリスナー設定
        hearingReturnButton = findViewById(R.id.hearingReturnButton);
        hearingReturnButton.setOnClickListener(v -> {
            finish(); // アクティビティを終了して前の画面に戻る
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        float gain250 = prefs.getFloat(PrefKeys.CORRECTION_250, 1.0f);
        int progress250 = (int)(gain250 / 3.0f * 100);
        slider250.setProgress(progress250);
        value250.setText(progress250 + "%");

        float gain500 = prefs.getFloat(PrefKeys.CORRECTION_500, 1.0f);
        int progress500 = (int)(gain500 / 3.0f * 100);
        slider500.setProgress(progress500);
        value500.setText(progress500 + "%");

        float gain1000 = prefs.getFloat(PrefKeys.CORRECTION_1000, 1.0f);
        int progress1000 = (int)(gain1000 / 3.0f * 100);
        slider1000.setProgress(progress1000);
        value1000.setText(progress1000 + "%");

        float gain2000 = prefs.getFloat(PrefKeys.CORRECTION_2000, 1.0f);
        int progress2000 = (int)(gain2000 / 3.0f * 100);
        slider2000.setProgress(progress2000);
        value2000.setText(progress2000 + "%");

        float gain4000 = prefs.getFloat(PrefKeys.CORRECTION_4000, 1.0f);
        int progress4000 = (int)(gain4000 / 3.0f * 100);
        slider4000.setProgress(progress4000);
        value4000.setText(progress4000 + "%");

    }
}
