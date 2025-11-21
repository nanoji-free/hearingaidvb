package com.littlebit.hearingaid.b;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class infoActivity extends AppCompatActivity {
    private Button addButton;
    private Button infoButton;
    private Button noticeButton;
    private Button micNoticeButton;
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
            Intent intent = new Intent(infoActivity.this, NoticeActivity.class);
            startActivity(intent);
        });

        // 「使い方ガイド」ボタン
        infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(v -> {
            Intent intent = new Intent(infoActivity.this, GuideActivity.class);
            startActivity(intent);
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
            guideMessage.setText(getString(R.string.about_app_text));

            // ダイアログにレイアウトを設定
            builder.setTitle("このアプリについて")
                    .setView(dialogView)
                    .setPositiveButton("OK", null)
                    .show();
        });

        // 「音が出ないときは？」ボタン
        micNoticeButton = findViewById(R.id.micNoticeButton);
        micNoticeButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(infoActivity.this);
            builder.setTitle("マイクの使用について")
                    .setMessage(getString(R.string.mic_permission_notice))
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
