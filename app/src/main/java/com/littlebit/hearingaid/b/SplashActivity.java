package com.littlebit.hearingaid.b;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private BillingClient billingClient;
    private boolean premiumUnlocked = false; // ← Billing 判定結果を保持

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        saveFirstLaunchDateIfNeeded();

        //セーフモードの導入関連コード（初期化）
        getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(PrefKeys.PREF_SAFE_MODE_ENABLED, false)
                .apply();

        // Billing 初期化
        setupBillingClient();

        //スプラッシュ表示
        setTheme(R.style.Theme_Splash);
        setContentView(R.layout.activity_splash);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (getWindow().getInsetsController() != null) {
                getWindow().getInsetsController().hide(
                        android.view.WindowInsets.Type.navigationBars()
                );
                getWindow().getInsetsController().setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        }

        // セーフモード導入判定し、さらに一定時間後に MainActivity へ遷移
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            boolean isLowMemory = isLowMemoryDevice();

            if (isLowMemory) {
                Toast.makeText(this, "メモリの量が低下しています。\nセーフモードで起動します。", Toast.LENGTH_LONG).show();
            }

            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("SAFE_MODE", isLowMemory); // セーフモードフラグを渡す
            startActivity(intent);
            finish(); // スプラッシュ画面を終了
        }, 1500);
    }
    // BillingClient 初期化
    private void setupBillingClient() {

        billingClient = BillingClient.newBuilder(this)
                .setListener((billingResult, purchases) -> {
                    // 購入更新時のコールバック（今回は特に処理不要）
                })
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()   // 単発購入を有効化
                                .build()
                )
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    checkPurchaseState();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // 切断時は無料扱いでOK（premiumUnlocked は false のまま）
            }
        });
    }

    // 購入状態のチェック
    private void checkPurchaseState() {

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchasesList) -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                for (Purchase purchase : purchasesList) {
                    if (purchase.getProducts().contains("premium_monthly") &&//★★ここに商品IDが仮設されていた（別アプリ作成時も参考に！）★★
                            purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        premiumUnlocked = true;
                        break;
                    }
                }
            }
            // Billing 判定結果を保存
            savePremiumState(premiumUnlocked);
        });
    }

    // フラグ保存
    private void savePremiumState(boolean isPremium) {

        // Premium でない場合は聴力プロファイル補正を強制 OFF
        if (!isPremium) {
            getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PrefKeys.PREF_HEARING_PROFILE_CORRECTION, false)
                    .apply();
        }
        // Premium 状態を保存
        getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(PrefKeys.PREF_PREMIUM_UNLOCKED, isPremium)
                .apply();
    }

    private boolean isLowMemoryDevice() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(info);

            long availMem = info.availMem; // 空きメモリ（バイト）
            long threshold = 128L * 1024L * 1024L; // 128MB を閾値にする

            return availMem < threshold;
        }
        return false;
    }
    private void saveFirstLaunchDateIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);

        if (!prefs.contains(PrefKeys.PREF_TRIAL_START)) {
            prefs.edit()
                    .putLong(PrefKeys.PREF_TRIAL_START, System.currentTimeMillis())
                    .apply();
        }
    }
}
