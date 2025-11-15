package com.github.nanoji_free.hearingaidvb;

public class PrefKeys {
    public static final String PREFS_NAME        = "hearing_aid_prefs";
    public static final String PREF_IS_STREAMING = "isStreaming";
    public static final String PREF_VOLUME       = "pref_volume";
    public static final String PREF_BALANCE      = "pref_balance";
    public static final String PREF_NOISE_FILTER = "pref_noise_filter";
    public static final String PREF_EMPHASIS     = "pref_emphasis";
    public static final String PREF_MIC_TYPE     = "pref_mic_type";
    public static final String PREF_SUPER_EMPHASIS="pref_super_emphasis";
    public static final String PREF_VOLUME_BOOST = "volumeBoost";
    public static final String PREF_DEPTH_SCALER = "pref_depth_scaler";
    public static final String PREF_BACKGROUND_SELECTION = "pref_background_selection";
    public static final String PREF_CENTER_IMAGE_URI = "pref_center_image_uri";
    public static final String PREF_CENTER_SELECTION = "pref_center_selection";
    public static final String PREF_SAFE_MODE_ENABLED = "safe_mode_enabled";
    public static final String PREF_HEARING_PROFILE_CORRECTION = "pref_hearing_profile_correction";


    public static final String PREF_CURRENT_VERSION_CODE = "currentVersionCode";//削除を検討中20251024、バ-ジョンコードの動的対応が不要なら削除
    public static final String PREF_INITIALIZED  = "pref_initialized";  // 初回起動フラグ//利用の可否が保留中

    public static final String EXTRA_APP_VOLUME = "extra_app_volume";
    public static final String EXTRA_BALANCE = "extra_balance";
    public static final String EXTRA_NOISE_FILTER = "extra_noise_filter";
    public static final String EXTRA_EMPHASIS = "extra_emphasis";
    public static final String EXTRA_OPTION_MIC = "extra_option_mic";
    public static final String EXTRA_VOLUME_BOOST = "extraVolumeBoost";
    public static final String EXTRA_DEPTH_SCALER = "extra_depth_scaler";
    public static final String EXTRA_SUPER_EMPHASIS= "extraSuperEmphasis";

    public static final String EXTRA_REQUEST_STREAMING = "extra_request_streaming";


    // プリセット1保存用キー
    public static final String PRESET1_VOLUME           = "preset1_volume";
    public static final String PRESET1_NOISE_FILTER     = "preset1_noise";
    public static final String PRESET1_EMPHASIS         = "preset1_emphasis";
    public static final String PRESET1_SUPER_EMPHASIS   = "preset1_super_emphasis";
    public static final String PRESET1_BALANCE          = "preset1_balance";
    public static final String PRESET1_MIC_TYPE         = "preset1_mic";
    public static final String PRESET1_VOLUME_BOOST     = "preset1_boost";
    public static final String PRESET1_DEPTH_SCALER     = "preset1_depth";

    // プリセット2保存用キー
    public static final String PRESET2_VOLUME           = "preset2_volume";
    public static final String PRESET2_NOISE_FILTER     = "preset2_noise";
    public static final String PRESET2_EMPHASIS         = "preset2_emphasis";
    public static final String PRESET2_SUPER_EMPHASIS   = "preset2_super_emphasis";
    public static final String PRESET2_BALANCE          = "preset2_balance";
    public static final String PRESET2_MIC_TYPE         = "preset2_mic";
    public static final String PRESET2_VOLUME_BOOST     = "preset2_boost";
    public static final String PRESET2_DEPTH_SCALER     = "preset2_depth";

    //増幅設定用のバンドごとのパラメータ
    public static final String CORRECTION_250 = "correction_250";
    public static final String CORRECTION_500 = "correction_500";
    public static final String CORRECTION_1000 = "correction_1000";
    public static final String CORRECTION_2000 = "correction_2000";
    public static final String CORRECTION_4000 = "correction_4000";
    // 聴力補正値をサービスに渡すためのキー
    public static final String EXTRA_CORRECTION_250 = "extra_correction_250";
    public static final String EXTRA_CORRECTION_500 = "extra_correction_500";
    public static final String EXTRA_CORRECTION_1000 = "extra_correction_1000";
    public static final String EXTRA_CORRECTION_2000 = "extra_correction_2000";
    public static final String EXTRA_CORRECTION_4000 = "extra_correction_4000";

}
