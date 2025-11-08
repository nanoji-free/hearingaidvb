package com.github.nanoji_free.hearingaidvb;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

public class infoActivity extends Activity {
    private Button addButton;
    private Button infoButton;
    private Button noticeButton;
    private Button infoReturnButton;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            DispHelper.applySavedBackground(this, rootLayout);
        }
        //「お知らせ」ボタン
        addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            //ここに、お知らせボタンの内容を実装する（作業中）
            Intent intent = new Intent(infoActivity.this, NoticeActivity.class);
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
                            "ポケットにスマートフォンを入れて使用する場合などは、アプリの詳細画面から外部マイクを使用しない設定に切り替えるか、ノイズ除去スイッチをオンにした状態で使用することをお勧めします。\n"+
                            "また、その際は音声の遅延防止の観点などから有線のイヤホンをご利用をおすすめします。\n\n"+
                            "屋外で使用するシチュエーションなど様々な状況に対する対応を目的に「外部マイクの使用」「左右スピーカーのバランス調整」「音声強調の強化」「音量の増強」などの機能を設けています。必要に応じた調整をしてご利用ください。\n\n"+
                            "ノイズ除去機能を搭載したスマホでは、その機能を利用することもできるようにしています。必要に応じてご活用ください。\n\n"+
                            "イヤホンからの音漏れや振動の漏れが原因でハウリングする場合があります。\n"+
                            "スマホとイヤホンの距離や、音量の調整をすることで改善する場合があります。\n\n"+
                            "スマホのメモリが不足した場合、自動的に状況を改善する機能を設けています。その機能が働いた場合には１～２秒程度音声が停止し、ノイズ除去機能が停止することがあります。その場合には必要に応じてノイズ除去スイッチを再度操作してご利用ください\n\n"+
                            "メモリ使用量が高まると、音声処理を一時停止することで安定性を保っています。\n" +
                            "状況が改善しない場合は、アプリの再起動をおすすめします。。\n\n"+
                            "画面に表示する画像を選択することができますが、端末のメモリの状況によっては画像がリアルタイムで変更されない場合があります。その場合は一度アプリを終了し、改めて再開してください。\n\n\n"+
                            "本アプリに起因する損害は、その一切を当方では負いかねますのでご了承の上、本アプリをご利用下さい。\n\n"
            );

            // ダイアログにレイアウトを設定
            builder.setTitle("このアプリについて")
                    .setView(dialogView)
                    .setPositiveButton("OK", null)
                    .show();
        });
//「戻る」ボタン
        infoReturnButton = findViewById(R.id.infoReturnButton);
        infoReturnButton.setOnClickListener(v -> {
            finish(); // 現在のActivityを終了して、前の画面（MainActivity）に戻る
        });

    }
}
