package com.github.nanoji_free.hearingaidvb;

//todo:HearingProfileActivityの次にここの完成を目指す
//todo:AudioStreamServiceに増幅させるメソッドを未実装なので検討の上で実装する（増強スイッチの挙動が未実装）
//todo:AudioStreamServiceの中の音声強調や音声強調ブーストの内容も点検が必要。
//todo:増幅スイッチの操作をしたときの挙動も検討が必要（減衰をどう扱うか）

import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FittingActivity extends AppCompatActivity {

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

        //「全体を調整する」ボタン
        startFullFittingButton = findViewById(R.id.startFullFittingButton);
        startFullFittingButton.setOnClickListener(v -> {

        });

        //「特定の帯域を調整する」ボタン
        startPartialFittingButton = findViewById(R.id.startPartialFittingButton);
        startPartialFittingButton.setOnClickListener(v -> {

        });

        fittingInstruction = findViewById(R.id.fittingInstruction);

        //「音を流す」ボタン
        playToneButton = findViewById(R.id.playToneButton);
        playToneButton.setOnClickListener(v -> {

        });

        //補正値のテキスト、ユーザーがスライダーを動かしたとき、補正値をリアルタイムで表示する
        //ビューの順序が前後するがスライダー値の変更に際してヌルポが出ることを想定して先に宣言
        currentValue = findViewById(R.id.currentValue);

        //調整用スライダー
        fittingSlider = findViewById(R.id.fittingSlider);
        fittingSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 補正値表示を更新
                currentValue.setText("補正値: " + progress + "dB");
                // 他の処置を記述予定
            }
            @Override
            public void onStartTrackingTouch (SeekBar seekBar){}

            @Override
            public void onStopTrackingTouch (SeekBar seekBar) {}

        });

        //帯域設定のOKボタン
        confirmBandButton = findViewById(R.id.confirmBandButton);
        confirmBandButton.setOnClickListener(v -> {

        });

        //何番目の帯域かの表示
        progressIndicator = findViewById(R.id.progressIndicator);

        // 「戻る」ボタンの初期化とリスナー設定
        fittingReturnButton = findViewById(R.id.fittingReturnButton);
        fittingReturnButton.setOnClickListener(v -> {
            finish(); // アクティビティを終了して前の画面に戻る
        });
    }
}
