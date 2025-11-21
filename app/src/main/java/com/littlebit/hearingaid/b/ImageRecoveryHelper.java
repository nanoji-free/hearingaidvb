package com.littlebit.hearingaid.b;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 表示失敗時に画像をBitmapで吸い上げてJPEGに変換し、
 * キャッシュ保存して再表示するクラス。
 */
public class ImageRecoveryHelper {

    public static void tryRecoverAndDisplay(Context context, String uriString, ImageView target, SharedPreferences prefs) {
        try {
            // ① URIをパースして元画像のサイズなどを取得
            Uri originalUri = Uri.parse(uriString);
            //InputStream inputStream = context.getContentResolver().openInputStream(originalUri);

            // ② 表示領域（160dp）に合わせて縮小率を計算し、Bitmapを軽量化して生成
            //Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            //inputStream.close();

            InputStream inputStream = context.getContentResolver().openInputStream(originalUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            int targetPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 160, context.getResources().getDisplayMetrics());
            int sampleSize = calculateInSampleSize(options, targetPx, targetPx);

            options.inSampleSize = sampleSize;
            options.inJustDecodeBounds = false;
            inputStream = context.getContentResolver().openInputStream(originalUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // ③ キャッシュディレクトリに軽量化されたBitmapをJPEG形式で保存
            File file = new File(context.getCacheDir(), "recovered_center_image.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream); // JPEG変換＋圧縮率90%
            outputStream.close();

            // ④ 保存したJPEGファイルのURIを取得
            Uri safeUri = Uri.fromFile(file);

            // ⑤ SharedPreferencesに安全な軽量化済みURIとして保存し、選択状態も更新
            prefs.edit().putString(PrefKeys.PREF_CENTER_IMAGE_URI, safeUri.toString()).apply();
            prefs.edit().putInt(PrefKeys.PREF_CENTER_SELECTION, R.id.chrChoiced).apply();

            // ⑥ ImageViewに再表示
            target.setImageURI(safeUri);

        } catch (Exception ex) {
            // ⑦ 軽量化や保存に失敗した場合は代替画像を表示し、選択状態を初期化
            target.setImageResource(R.drawable.betaimage);
            prefs.edit().putInt(PrefKeys.PREF_CENTER_SELECTION, R.id.chrRecommended).apply();
            prefs.edit().remove(PrefKeys.PREF_CENTER_IMAGE_URI).apply();

            // ⑧ ユーザーに通知
            Toast.makeText(context, "中央の画像の表示ができませんでした。代替画像として初期設定の表示にしています。", Toast.LENGTH_LONG).show();
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
}

