package com.github.nanoji_free.hearingaidvb;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Splash);
        setContentView(R.layout.activity_splash);

        boolean isLowMemory = isLowMemoryDevice();

        if (isLowMemory) {
            Toast.makeText(this, "メモリの量が低下しています。\nセーフモードで起動します。", Toast.LENGTH_LONG).show();
        }

        // 1.5秒後に MainActivity へ遷移
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("SAFE_MODE", isLowMemory); // セーフモードフラグを渡す
            startActivity(intent);
            finish(); // スプラッシュ画面を終了
        }, 1500);
    }
    private boolean isLowMemoryDevice() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            int memoryClass = activityManager.getMemoryClass(); // 単位：MB
            return memoryClass < 200; // 200MB未満ならセーフモード
        }
        return false;
    }
}
