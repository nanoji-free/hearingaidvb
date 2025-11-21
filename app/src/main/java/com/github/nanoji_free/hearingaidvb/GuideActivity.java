package com.github.nanoji_free.hearingaidvb;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GuideActivity extends AppCompatActivity {
    private TextView guideTextView;
    private Button closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        guideTextView = findViewById(R.id.guideTextView);
        closeButton = findViewById(R.id.closeButton);

        // strings.xmlから全文を読み込んで表示
        guideTextView.setText(getString(R.string.guide_full_text));

        // 閉じるボタンで戻る
        closeButton.setOnClickListener(v -> finish());
    }
}

