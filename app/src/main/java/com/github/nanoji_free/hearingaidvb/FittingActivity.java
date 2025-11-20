package com.github.nanoji_free.hearingaidvb;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FittingActivity extends AppCompatActivity {
    private AudioTrack toneTrack;

    private final int[] bands = {250, 500, 1000, 2000, 4000};
    private int currentBandIndex = 0;
    private boolean isPartialMode = false;

    private TextView fittingTitle;
    private TextView fittingModeTitle;
    private Button startFullFittingButton;
    private Button startPartialFittingButton;
    private TextView fittingInstruction;
    private Button playToneButton;
    private SeekBar fittingSlider;
    private TextView currentValue;
    private Button confirmBandButton;
    private TextView progressIndicator;
    private Button fittingReturnButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fitting);

        fittingTitle = findViewById(R.id.fittingTitle);
        fittingModeTitle = findViewById(R.id.fittingModeTitle);
        playToneButton = findViewById(R.id.playToneButton);
        fittingSlider = findViewById(R.id.fittingSlider);
        confirmBandButton = findViewById(R.id.confirmBandButton);

        startFullFittingButton = findViewById(R.id.startFullFittingButton);
        startPartialFittingButton = findViewById(R.id.startPartialFittingButton);
        currentValue = findViewById(R.id.currentValue);
        fittingInstruction = findViewById(R.id.fittingInstruction);
        progressIndicator = findViewById(R.id.progressIndicator);
        fittingReturnButton = findViewById(R.id.fittingReturnButton);

        // 初期状態では操作不可にする（グレーアウトする）
        playToneButton.setEnabled(false);
        fittingSlider.setEnabled(false);
        confirmBandButton.setEnabled(false);

        //「全体を調整する」ボタン
        startFullFittingButton.setOnClickListener(v -> {
            currentBandIndex = 0;
            updateBandUI(); // 250Hzの表示や進行状況を更新するメソッドを呼ぶ
            playToneButton.setEnabled(true);
            fittingSlider.setEnabled(true);
            confirmBandButton.setEnabled(true);

        });

        //「特定の帯域を調整する」ボタン
        startPartialFittingButton.setOnClickListener(v -> {
            String[] bandLabels = {"250Hz", "500Hz", "1000Hz", "2000Hz", "4000Hz"};
            new AlertDialog.Builder(FittingActivity.this)
                    .setTitle("調整する帯域を選んでください")
                    .setItems(bandLabels, (dialog, which) -> {
                        currentBandIndex = which;
                        isPartialMode = true;

                        updateBandUI(); // 選択した帯域に切り替え
                        playToneButton.setEnabled(true);
                        fittingSlider.setEnabled(true);
                        confirmBandButton.setEnabled(true);
                    })
                    .show();
        });


        //「音を流す」ボタン
        playToneButton.setOnClickListener(v -> {
            if (toneTrack != null) {
                toneTrack.stop();
                toneTrack.release();
                toneTrack = null;
            }
            int freq = bands[currentBandIndex];
            float volume = fittingSlider.getProgress() / 30.0f; // 0.0〜1.0
            playTone(freq, volume);
        });

        //補正値のテキスト、ユーザーがスライダーを動かしたとき、補正値をリアルタイムで表示する
        //ビューの順序が前後するがスライダー値の変更に際してヌルポが出ることを想定して先に宣言
        //currentValue = findViewById(R.id.currentValue);　←　冒頭でバインド済み

        //調整用スライダー
        fittingSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 補正値表示を更新
                int percent = (int)(progress / 30.0f * 100); // 0〜100%
                currentValue.setText("補正値: " + percent + "%");
                // 純音のボリュームを調整
                if (toneTrack != null && toneTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    float volume = progress / 30.0f;
                    toneTrack.setVolume(volume);
                }
            }
            @Override
            public void onStartTrackingTouch (SeekBar seekBar){}

            @Override
            public void onStopTrackingTouch (SeekBar seekBar) {}

        });

        //帯域設定のOKボタン
        confirmBandButton.setOnClickListener(v -> {
            int freq = bands[currentBandIndex];

            //　まず純音を停止する
            if (toneTrack != null) {
                toneTrack.stop();
                fittingInstruction.setText(freq + "Hz の音を10秒ほど再生します。\n聞こえるまでスライダーを少しずつ上げてください。");
                toneTrack.release();
                toneTrack = null;
            }
            // スライダー値を補正倍率に変換（0〜30 → 0.0〜3.0）
            float gain = fittingSlider.getProgress() * 0.1f;
            // 現在の帯域に対応する補正値を保存
            SharedPreferences prefs = getSharedPreferences("hearing_prefs", MODE_PRIVATE);
            switch (freq) {
                case 250:
                    prefs.edit().putFloat(PrefKeys.CORRECTION_250, gain).apply();
                    break;
                case 500:
                    prefs.edit().putFloat(PrefKeys.CORRECTION_500, gain).apply();
                    break;
                case 1000:
                    prefs.edit().putFloat(PrefKeys.CORRECTION_1000, gain).apply();
                    break;
                case 2000:
                    prefs.edit().putFloat(PrefKeys.CORRECTION_2000, gain).apply();
                    break;
                case 4000:
                    prefs.edit().putFloat(PrefKeys.CORRECTION_4000, gain).apply();
                    break;
            }
            if (isPartialMode) {
                new AlertDialog.Builder(this)
                        .setMessage(freq + "Hz の補正を保存しました。\n\n他の帯域も調整できます。")
                        .setPositiveButton(" OK ", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                currentBandIndex++;
                if (currentBandIndex < bands.length) {
                    updateBandUI();
                } else {
                    new AlertDialog.Builder(this)
                            .setMessage("すべての帯域の補正が完了し、状態を保存しました。\n新しい音の世界をお楽しみください。\n\n（前の画面に戻ります）")
                            .setPositiveButton(" OK ", (dialog, which) -> dialog.dismiss())
                            .show();
                    finish();
                }
            }
        });

        //何番目の帯域かの表示
        //progressIndicator = findViewById(R.id.progressIndicator);←バインド済み

        // 「戻る」ボタンの初期化とリスナー設定
        fittingReturnButton.setOnClickListener(v -> {
            finish(); // アクティビティを終了して前の画面に戻る
        });
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (toneTrack != null) {
            toneTrack.stop();
            toneTrack.release();
            toneTrack = null;
        }
    }

    private void updateBandUI() {
        int freq = bands[currentBandIndex];
        fittingInstruction.setText(freq + "Hz の音を10秒ほど再生します。\n聞こえるまでスライダーを少しずつ上げてください。");//ビープ音の長さを明示
        progressIndicator.setText((currentBandIndex + 1) + " / " + bands.length + " 帯域");
        fittingSlider.setProgress(0);
        currentValue.setText("補正値: 0%");
    }

    private void playTone(int freqHz, float volume) {
        int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
        int durationMs = 10000;//　ビープ音の鳴動時間
        int numSamples = durationMs * sampleRate / 1000;
        double[] sample = new double[numSamples];
        byte[] generatedSnd = new byte[2 * numSamples];

        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqHz));
        }

        int idx = 0;
        for (final double dVal : sample) {
            short val = (short) ((dVal * 32767.0) * volume);
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        toneTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.length,
                AudioTrack.MODE_STREAM);

        fittingInstruction.setText(freqHz + "Hz の音を再生中です。\n聞こえるまでスライダーを少しずつ上げてください。");

        toneTrack.play();
        toneTrack.setVolume(volume); // API 21以降
        toneTrack.write(generatedSnd, 0, generatedSnd.length);

        new Handler().postDelayed(() -> {
            if (toneTrack != null && toneTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                fittingInstruction.setText(freqHz + "Hz の音を10秒ほど再生します。\n聞こえるまでスライダーを少しずつ上げてください。");
            }
        }, 10000);


    }
}
