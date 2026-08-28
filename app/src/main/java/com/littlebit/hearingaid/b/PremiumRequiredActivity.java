package com.littlebit.hearingaid.b;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
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

        billingClient.queryProductDetailsAsync(params, (billingResult, queryProductDetailsResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && queryProductDetailsResult != null
                    && !queryProductDetailsResult.getProductDetailsList().isEmpty()) {

                premiumProductDetails = queryProductDetailsResult.getProductDetailsList().get(0);
            }
        });
    }

    private void launchPurchaseFlow() {
        if (premiumProductDetails == null) {
            Toast.makeText(this, "購入情報を取得できませんでした。しばらくしてからお試しください。", Toast.LENGTH_SHORT).show();
            return;
        }

        String offerToken = null;
        if (premiumProductDetails.getSubscriptionOfferDetails() != null
                && !premiumProductDetails.getSubscriptionOfferDetails().isEmpty()) {
            offerToken = premiumProductDetails
                    .getSubscriptionOfferDetails()
                    .get(0)
                    .getOfferToken();
        }

        BillingFlowParams.ProductDetailsParams productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(premiumProductDetails)
                        .setOfferToken(offerToken)
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();   // ★ 重要：Activity終了後のコールバック防止
        }
    }
}


