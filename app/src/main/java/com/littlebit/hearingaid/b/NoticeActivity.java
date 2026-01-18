package com.littlebit.hearingaid.b;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NoticeActivity extends AppCompatActivity{

    private TextView noticeTextView;
    private Button backButton;
    private final String jsonUrl = "https://raw.githubusercontent.com/nanoji-free/mimimimi-notice-data/main/mimimimi-notice-data.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        noticeTextView = findViewById(R.id.noticeTextView);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());
        new Thread(() -> {
            try {
                URL url = new URL(jsonUrl);
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
                JSONArray notices = json.getJSONArray("notices");

                PackageInfo packageInfo = getPackageManager()
                        .getPackageInfo(getPackageName(), 0);
                long currentVersionCode;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    currentVersionCode = packageInfo.getLongVersionCode();
                } else {
                    currentVersionCode = packageInfo.versionCode;
                }

                StringBuilder messageBuilder = new StringBuilder();

                //無償配布版のコメントアウトはここから。
                //
                // 🔔 アップデート通知を先頭に追加
                if (remoteVersionCode > currentVersionCode) {
                    messageBuilder
                            .append("🔔 新しいバージョンがあります！\n")
                            .append("▶ 詳細: https://play.google.com/store/apps/details?id=com.littlebit.hearingaid.b\n\n");
                }
                //
                //必要に応じてここまでコメントアウト（無償配布版はコメントアウトが必要）


                // 通常のお知らせを追加
                for (int i = 0; i < notices.length(); i++) {
                    JSONObject notice = notices.getJSONObject(i);
                    String date = notice.getString("date");
                    String title = notice.getString("title");
                    String content = notice.getString("content");
                    String urlStr = notice.getString("url");

                    messageBuilder
                            .append("【").append(date).append("】").append("\n")
                            .append("「").append(title).append("」").append("\n")
                            .append(content).append("\n")
                            .append("▶ 詳細: ").append(urlStr).append("\n\n");
                }

                runOnUiThread(() -> {
                    noticeTextView.setText(messageBuilder.toString());
                    Linkify.addLinks(noticeTextView, Linkify.WEB_URLS);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> noticeTextView.setText("お知らせの取得に失敗しました"));
            }
        }).start();
    }
}
