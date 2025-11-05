package com.github.nanoji_free.hearingaidvb;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DisplaySettingsActivity extends AppCompatActivity {

    private Button returnToEasyButton;
    private Button centerPictSelect;
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView centerImageView;


    private ConstraintLayout rootLayout;
    private RadioGroup backgroundSelector;
    private RadioGroup centerSelector;
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_displaysettings);

        rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            DispHelper.applySavedBackground(this, rootLayout);
        }

        prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);
        int savedId = prefs.getInt(PrefKeys.PREF_BACKGROUND_SELECTION, R.id.bgDefault);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            File file = new File(getCacheDir(), "center_image.jpg");
                            OutputStream outputStream = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }
                            inputStream.close();
                            outputStream.close();

                            Uri safeUri = Uri.fromFile(file);
                            prefs.edit()
                                    .remove(PrefKeys.PREF_CENTER_IMAGE_URI)
                                    .putString(PrefKeys.PREF_CENTER_IMAGE_URI, safeUri.toString())
                                    .apply();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        //　中央の画像の選択処理
        centerImageView = findViewById(R.id.centerImageView);
        //中央の関係のラジオボタンの処理
        centerSelector = findViewById(R.id.centerSelector);
        if (centerSelector != null) {
            int savedCenterId = prefs.getInt(PrefKeys.PREF_CENTER_SELECTION, R.id.chrRecommended);
            centerSelector.check(savedCenterId);
            updateCenterImage(savedCenterId);

            centerSelector.setOnCheckedChangeListener((group, checkedId) -> {
                prefs.edit().putInt(PrefKeys.PREF_CENTER_SELECTION, checkedId).apply();
                updateCenterImage(checkedId);
            });
        }

        //ユーザーが写真を選択する処理
        //Button（android:id="@+id/centerPictSelect"）
        centerPictSelect = findViewById(R.id.centerPictSelect);
        centerPictSelect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });


        //centerImageViewの表示

        // 背景選択のラジオボタン処理
        backgroundSelector = findViewById(R.id.backgroundSelector);
        if (backgroundSelector != null) {
            backgroundSelector.check(savedId);
            backgroundSelector.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.bgNone) {
                    rootLayout.setBackground(null);
                } else if (checkedId == R.id.bgDefault) {
                    rootLayout.setBackgroundResource(R.drawable.buckofone);
                } else if (checkedId == R.id.bgRecommended) {
                    rootLayout.setBackgroundResource(R.drawable.onerecback);
                }
                prefs.edit().putInt(PrefKeys.PREF_BACKGROUND_SELECTION, checkedId).apply();

            });
        }

        //　「もどる」 ボタン
        returnToEasyButton = findViewById(R.id.returnToEasyButton);
        returnToEasyButton.setOnClickListener(v -> {
            finish();
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            DispHelper.applySavedBackground(this, rootLayout);
        }
        int selectedId = prefs.getInt(PrefKeys.PREF_CENTER_SELECTION, R.id.chrRecommended);
        updateCenterImage(selectedId);
    }
    private void updateCenterImage(int selectedId) {
        if (centerImageView == null) return;
        if (selectedId == R.id.chrNone) {
            centerImageView.setVisibility(View.GONE);
        } else {
            centerImageView.setVisibility(View.VISIBLE);
            if (selectedId == R.id.chrRecommended) {
                centerImageView.setImageResource(R.drawable.betaimage);
            } else if (selectedId == R.id.chrChoiced) {
                String uriString = prefs.getString(PrefKeys.PREF_CENTER_IMAGE_URI, null);
                if (uriString != null && !uriString.contains("com.google.android.apps.photos")) {
                    try {
                        centerImageView.setImageURI(Uri.parse(uriString));
                    } catch (Exception e) {
                        ImageRecoveryHelper.tryRecoverAndDisplay(this, uriString, centerImageView, prefs);
                    }
                } else {
                    centerImageView.setImageResource(R.drawable.betaimage);
                }
            }
        }
    }

}

