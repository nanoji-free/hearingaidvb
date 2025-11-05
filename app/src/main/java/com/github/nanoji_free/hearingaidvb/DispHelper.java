package com.github.nanoji_free.hearingaidvb;

        import android.content.Context;
        import android.content.SharedPreferences;
        import androidx.constraintlayout.widget.ConstraintLayout;

public class DispHelper {
    public static void applySavedBackground(Context context, ConstraintLayout layout) {
        if (layout == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE);
        int savedId = prefs.getInt(PrefKeys.PREF_BACKGROUND_SELECTION, R.id.bgDefault);

        if (savedId == R.id.bgNone) {
            layout.setBackground(null);
        } else if (savedId == R.id.bgDefault) {
            layout.setBackgroundResource(R.drawable.buckofone);
        } else if (savedId == R.id.bgRecommended) {
            layout.setBackgroundResource(R.drawable.onerecback);
        }
    }
}

