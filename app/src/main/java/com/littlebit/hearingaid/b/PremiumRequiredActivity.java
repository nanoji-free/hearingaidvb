package com.littlebit.hearingaid.b;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;

import java.util.Collections;
import java.util.List;

public class PremiumRequiredActivity extends AppCompatActivity {

    private BillingClient billingClient;
    private ProductDetails premiumProductDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_required);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        Button upgradeButton = findViewById(R.id.upgradeButton);
        upgradeButton.setOnClickListener(v -> launchPurchaseFlow());

        // Billing 初期化
        setupBillingClient();
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && purchases != null) {
                        handlePurchaseUpdate(purchases);
                    }
                })
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // 再接続は任意
            }
        });
    }

    private void queryProductDetails() {
        QueryProductDetailsParams.Product product =
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("premium_monthly")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build();

        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(Collections.singletonList(product))
                        .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && productDetailsList != null
                    && !productDetailsList.isEmpty()) {

                premiumProductDetails = productDetailsList.get(0);
            }
        });
    }

    private void launchPurchaseFlow() {
        if (premiumProductDetails == null) {
            Toast.makeText(this, "購入情報を取得できませんでした。しばらくしてからお試しください。", Toast.LENGTH_SHORT).show();
            return;
        }

        BillingFlowParams.ProductDetailsParams productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(premiumProductDetails)
                        .build();

        BillingFlowParams billingFlowParams =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Collections.singletonList(productDetailsParams))
                        .build();

        billingClient.launchBillingFlow(this, billingFlowParams);
    }

    private void handlePurchaseUpdate(List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
            if (purchase.getProducts().contains("premium_monthly")
                    && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

                // Premium 解放
                getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean(PrefKeys.PREF_PREMIUM_UNLOCKED, true)
                        .apply();

                // この画面は保険なので閉じるだけでOK
                finish();
            }
        }
    }
}


