package com.github.nanoji_free.hearingaidvb;

// TODO:  「お知らせ」ボタンの中身の実装が必要！　-> アドレスを取得する必要あり（WebViewでgoogleドライブへの誘導を検討中）
// TODO: バージョンアップのお知らせを把握するためのコードも実装する（MainActivity.javaかな？）

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.github.nanoji_free.hearingaidvb.PrefKeys;
import com.github.nanoji_free.hearingaidvb.SettingsUtils;


public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private boolean isStreaming = false;
    float volumeBoost = 0.00f;
    private Switch noiseFilterSwitch;
    private Switch emphasisSwitch;
    private Switch optionMic;
    private Switch superEnphasSwitch;
    private TextView volBoostText;
    private SeekBar volumeBoostSeekBar;
    private SeekBar stSeekBar;
    private Button balPresetButton;
    private Button presetButton;
    private Button addButton;
    private Button infoButton;
    private Button noticeButton;
    private Button returnButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        noiseFilterSwitch = findViewById(R.id.noiseFilterSwitch);
        emphasisSwitch = findViewById(R.id.emphasisSwitch);

        prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);
        float savedBalance = prefs.getFloat(PrefKeys.PREF_BALANCE, 0f);
        isStreaming = prefs.getBoolean("isStreaming", false);

        //アプリ起動時にもサービスへ初期バランスを通知(作動中のときのみバランスを通知）
        if (isStreaming) {
            startService(new Intent(this, AudioStreamService.class)
                    .putExtra(PrefKeys.EXTRA_BALANCE, savedBalance));
        }

        //「オプションマイク　on/off」スイッチ
        optionMic = findViewById(R.id.optionMic);
        optionMic.setOnCheckedChangeListener((btn, isChecked) -> {
            //外部マイクの切り替えを組み込む
            //　設定の保存
            prefs.edit()//状態の保存のみ先行して作成20250906
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

                prefs.edit()
                        .putFloat(PrefKeys.PREF_VOLUME_BOOST, volumeBoost)
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
                    .putBoolean(PrefKeys.PREF_MIC_TYPE, false)
                    .putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false)
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
            boolean isStreaming = prefs.getBoolean("isStreaming", false);
            if (isStreaming) {
                startService(buildStreamingIntent(false));
            }
        });

        //「お知らせ」ボタン
        addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            //ここに、お知らせボタンの内容を実装する（作業中）
            Intent intent = new Intent(SettingsActivity.this, NoticeActivity.class);
            startActivity(intent);
        });

        // 「使い方ガイド」ボタン
        infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();

            // 作成したレイアウトを取得
            View dialogView = inflater.inflate(R.layout.custom_dialog, null);

            // TextView に内容を設定
            TextView guideMessage = dialogView.findViewById(R.id.guide_message);
            guideMessage.setText(
                    "音声が再生できない場合はスマホの設定を確認してください。\n\n" +
                            "設定＞アプリ＞耳みみエイド＞許可＞マイクの権限を許可する。\n\n" +
                            "音量の調整はアプリの音量以外に本体の音量やイヤホンの音量もご確認ください。\n\n"
            );

            // ダイアログにレイアウトを設定
            builder.setTitle("使い方ガイド")
                    .setView(dialogView)
                    .setPositiveButton("OK", null)
                    .show();
        });

        // 「このアプリについて」ボタン
        noticeButton = findViewById(R.id.noticeButton);
        noticeButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();

            // 作成したレイアウトを取得
            View dialogView = inflater.inflate(R.layout.custom_dialog, null);

            // TextView に内容を設定
            TextView guideMessage = dialogView.findViewById(R.id.guide_message);
            guideMessage.setText(
                    "\n"+
                            "「聞こえるって、すばらしい！」\n\n" +
                            "このアプリはスマホの周囲の音をリアルタイムで集音し再生する聴覚支援ツールです。\n" +
                            "特に骨伝導ワイヤレスイヤホンと組み合わせによる聴覚支援補助を目指しています。\n\n" +
                            "集音ツールとしてテーブルの上やテレビの近くにスマートフォンを置いてご活用ください。\n" +
                            "(ワイヤレスイヤホンを使用すると、音声を転送する関係上、音声の遅延が発生することがあります。)\n\n"+
                            "ポケットにスマートフォンを入れて使用する場合などは、アプリの詳細画面から外部マイクを使用しない設定に切り替えることをお勧めします。\n"+
                            "また、その際は音声の遅延防止の観点などから有線のイヤホンをご利用をおすすめします。\n\n"+
                            "屋外で使用するシチュエーションなど様々な状況に対する対応を目的に「外部マイクの使用」「左右スピーカーのバランス調整」「音声強調の強化」「音量の増強」などの機能を設けています。必要に応じた調整をしてご利用ください。\n\n"+
                            "ノイズ除去機能を搭載したスマホでは、その機能を利用することもできるようにしています。必要に応じてご活用ください。\n\n"+
                            "イヤホンからの音漏れや振動の漏れが原因でハウリングする場合があります。\n"+
                            "スマホとイヤホンの距離や、音量の調整をすることで改善する場合があります。\n\n\n"+
                            "本アプリに起因する損害は、その一切を当方では負いかねますのでご了承の上、本アプリをご利用下さい。\n\n"
            );

            // ダイアログにレイアウトを設定
            builder.setTitle("このアプリについて")
                    .setView(dialogView)
                    .setPositiveButton("OK", null)
                    .show();
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
        startService(buildStreamingIntent(false));
    }

    private Intent buildStreamingIntent(boolean requestStreaming) {
        return new Intent(SettingsActivity.this, AudioStreamService.class)
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




















