package com.littlebit.hearingaid.b;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HearingProfileMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hearing_profile_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button left = findViewById(R.id.button_left);
        Button right = findViewById(R.id.button_right);
        Button back = findViewById(R.id.button_back);

        left.setOnClickListener(v -> {
            startActivity(new Intent(this, HearingProfileActivityLeft.class));
        });

        right.setOnClickListener(v -> {
            startActivity(new Intent(this, HearingProfileActivityRight.class));
        });

        back.setOnClickListener(v -> finish());

    }
}