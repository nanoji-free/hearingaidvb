package com.github.nanoji_free.hearingaidvb;

        import android.content.Context;
        import android.content.SharedPreferences;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.net.Uri;
        import android.util.TypedValue;

        import androidx.constraintlayout.widget.ConstraintLayout;

        import java.io.InputStream;

public class DispHelper {
    public static void applySavedBackground(Context context, ConstraintLayout layout) {
        if (layout == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE);
        int savedId = prefs.getInt(PrefKeys.PREF_BACKGROUND_SELECTION, R.id.bgDefault);

        if (savedId == R.id.bgNone) {
            layout.setBackground(null);
        } else if (savedId == R.id.bgDefault) {
            layout.setBackgroundResource(R.drawable.def_back);
        } else if (savedId == R.id.bgRecommended) {
            layout.setBackgroundResource(R.drawable.recommended_one);
        }
    }
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

// URIから軽量化されたBitmapを読み込むユーティリティ
    public static Bitmap loadOptimizedBitmap(Context context, Uri uri, int reqWidthDp, int reqHeightDp) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            int reqWidthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, reqWidthDp, context.getResources().getDisplayMetrics());
            int reqHeightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, reqHeightDp, context.getResources().getDisplayMetrics());
            int sampleSize = calculateInSampleSize(options, reqWidthPx, reqHeightPx);

            options.inSampleSize = sampleSize;
            options.inJustDecodeBounds = false;
            inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}

