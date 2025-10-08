package com.github.nanoji_free.hearingaidvb;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.media.audiofx.NoiseSuppressor;
import android.util.Pair;
import android.os.Process;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.github.nanoji_free.hearingaidvb.PrefKeys;


public class AudioStreamService extends Service {
    private static final String TAG = "AudioStreamService";
    private static final String CHANNEL_ID = "channel_id";

    // SharedPreferences 用フィールド
    private SharedPreferences prefs;
    //public static final String EXTRA_APP_VOLUME = "APP_VOLUME";
    //public static final String EXTRA_NOISE_FILTER = "NOISE_FILTER";
    //public static final String EXTRA_EMPHASIS      = "EXTRA_EMPHASIS";
    //public static final String EXTRA_BALANCE = "extra_balance";
    public static final String EXTRA_VOLUME = "extra_volume";
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;

    private float mBalance = 0f;
    private Thread streamThread;
    private volatile boolean isStreaming = false;
    private boolean noiseFilterEnabled = false;// NoiseSuppressor 用フラグ
    private NoiseSuppressor noiseSuppressor;
    private boolean superEmphasis = false;
    /** バッファを short 単位で扱う */
    private int bufferSizeInShorts;
    private short[] pcmBuffer;//出力側のバッファ（buffer[]変数は入力時のバッファの値を保持）
    private short[] stereoOut; // ステレオ出力用バッファ
    private float manualGain = 2.50f;// PCM再生時に乗算するゲインとクリッピング用パラメータ
    private float xManualGain = 1.20f;// PCM再生時に乗算するゲインとクリッピング用パラメータ
    // バンドパス用フラグ＆パラメータ
    private boolean emphasisEnabled = false;
    private double lowCutFreq  = 800.0;   // 下限周波数(Hz)
    private double highCutFreq = 3200.0;   // 上限周波数(Hz)
    private float threshold = 0.015f;  //小さな音は雑音として外すための閾値、初期値は0.02f// （これ以下は切る）
    // IIRフィルタ状態
    private double hpAlpha, lpAlpha;
    private double hpPrevInput, hpPrevOutput, lpPrevOutput;

    // --- 低音域 減衰用（1次HPF→1次LPFの状態・係数）---
    private double cutHpAlpha = 0.0;       // 1次HPF用の係数（MONO用）
    private double cutLpAlpha = 0.0;       // 1次LPF用の係数（MONO用）
    private double cutHpPrevIn,  cutHpPrevOut,  cutLpPrevOut;// MONO用の状態
    private double cutLowHz  = 15.0;
    private double cutHighHz = 220.0;
    private volatile double bandCutDepth = 0.90; // 0.0=無効, 1.0=最大カット,初期は0.8で仮設

    // --- 高域減衰用（例: 7.5kHz～Nyquist）---
    private double highCutHpAlpha = 0.0;
    private double highCutLpAlpha = 0.0;
    private double highCutHpPrevIn, highCutHpPrevOut, highCutLpPrevOut;
    private double highCutLowHz  = 6000.0;//広域減衰の幅の下側（初期値は7500）
    private double highCutHighHz = 20000.0;//広域元帥の幅の上側（初期値は18000）

    private volatile double highBandCutDepth = 0.97; // 高域減衰の深さ(初期値は0.90）

    //　volumeBoost関係（音量の限界突破のようなブースト）、SettingsActivityから反映
    private float XvolG = 7.0f; // DSPゲイン強化係数（xManualGainを増強する数値の係数）
    private float XvolZ = 0.2f; // 再生音量強化係数（appVolumeに追加する数値の係数）
    private float volumeBoost = 0.0f;
    private float boostGainMore = 0.5f; // 補正用の追加ゲイン

    // ===== 音量＆ブーストマッピング用の定数 =====
    private static final float MAX_APP_VOLUME = 0.5f;   // 全体音量の上限
    private static final float BOOST_START_VOL = 0.25f; // ブーストが効き始める位置
    private static final float BOOST_BASE = 4.0f;       // ブーストの基準値
    private static final float MAX_BOOST_GAIN = 15.0f;   // ブーストの安全上限

    // --- 単一帯域で出力をブースト（疑似的に音圧を上げる） ---
    private boolean boostEnabled = true; // 有効/無効フラグ
    private double boostLowHz  = 200.0;//増幅する帯域の範囲（下側）
    private double boostHighHz = 3200.0;//増幅する帯域の範囲（上側）
    private double boostGain   = 4.0; // 倍率(初期値2.5）
    private double boostHpAlpha, boostLpAlpha;
    private double boostHpPrevIn, boostHpPrevOut, boostLpPrevOut;

    // コンフォートノイズ生成用の状態
    //private float pinkState = 0f;//実装はひとまず見送ったのでコメントアウト。

    // バランス比（等電力パン用の左右比）
    private volatile float leftRatio  = 0.7071f; // center = 1/√2
    private volatile float rightRatio = 0.7071f;

    private Pair<Integer, Integer> findBestSampleRate() {
        final int[] candidates = {48000, 44100, 22050, 16000, 11025, 8000};
        int channelIn = AudioFormat.CHANNEL_IN_MONO;
        int channelOut = AudioFormat.CHANNEL_OUT_STEREO;
        int format = AudioFormat.ENCODING_PCM_16BIT;

        for (int rate : candidates) {
            int recBuf = AudioRecord.getMinBufferSize(rate, channelIn, format);
            int playBuf = AudioTrack.getMinBufferSize(rate, channelOut, format);
            if (recBuf > 0 && playBuf > 0) {
                return new Pair<>(rate, Math.max(recBuf, playBuf));
            }
        }
        // フォールバック: 16kHz + 最小バッファ
        int fb = AudioRecord.getMinBufferSize(16000, channelIn, format);
        return new Pair<>(16000, fb > 0 ? fb : 4096);
    }

    private float appVolume = 0.5f;  // 0.0f～1.0f //ホワイトノイズ対策で上限を0.5で反映
    private float balance    = 0f;
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        // SharedPreferences を取得し、保存値をロード
        prefs = getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE);
        appVolume         = prefs.getFloat(PrefKeys.PREF_VOLUME, appVolume);
        balance           = prefs.getFloat(PrefKeys.PREF_BALANCE, balance);
        setBalance(balance);
        noiseFilterEnabled = prefs.getBoolean(PrefKeys.PREF_NOISE_FILTER, noiseFilterEnabled);
        emphasisEnabled   = prefs.getBoolean(PrefKeys.PREF_EMPHASIS, emphasisEnabled);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification());

        SharedPreferences prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);

        // Intentが空でもSharedPreferencesから補完
        float appVolume = intent != null && intent.hasExtra(PrefKeys.EXTRA_APP_VOLUME)
                ? intent.getFloatExtra(PrefKeys.EXTRA_APP_VOLUME, 0.65f)
                : prefs.getFloat(PrefKeys.PREF_VOLUME, 0.65f);

        float balance = intent != null && intent.hasExtra(PrefKeys.EXTRA_BALANCE)
                ? intent.getFloatExtra(PrefKeys.EXTRA_BALANCE, 0f)
                : prefs.getFloat(PrefKeys.PREF_BALANCE, 0f);

        boolean noiseFilterEnabled = intent != null && intent.hasExtra(PrefKeys.EXTRA_NOISE_FILTER)
                ? intent.getBooleanExtra(PrefKeys.EXTRA_NOISE_FILTER, false)
                : prefs.getBoolean(PrefKeys.PREF_NOISE_FILTER, false);

        boolean emphasisEnabled = intent != null && intent.hasExtra(PrefKeys.EXTRA_EMPHASIS)
                ? intent.getBooleanExtra(PrefKeys.EXTRA_EMPHASIS, false)
                : prefs.getBoolean(PrefKeys.PREF_EMPHASIS, false);
        this.emphasisEnabled = emphasisEnabled;

        boolean micType = intent != null && intent.hasExtra(PrefKeys.EXTRA_OPTION_MIC)
                ? intent.getBooleanExtra(PrefKeys.EXTRA_OPTION_MIC, false)
                : prefs.getBoolean(PrefKeys.PREF_MIC_TYPE, false);

        float volumeBoost = intent != null && intent.hasExtra(PrefKeys.EXTRA_VOLUME_BOOST)
                ? intent.getFloatExtra(PrefKeys.EXTRA_VOLUME_BOOST, 0.0f)
                : prefs.getFloat(PrefKeys.PREF_VOLUME_BOOST, 0.0f);
        this.volumeBoost = volumeBoost;

        boolean superEmphasis = intent != null && intent.hasExtra(PrefKeys.EXTRA_SUPER_EMPHASIS)
                ? intent.getBooleanExtra(PrefKeys.EXTRA_SUPER_EMPHASIS, false)
                : prefs.getBoolean(PrefKeys.PREF_SUPER_EMPHASIS, false);
        this.superEmphasis = superEmphasis;

        // Intentがあれば状態を更新（prefsにも反映）
        if (intent != null) {
            if (intent.hasExtra(PrefKeys.EXTRA_EMPHASIS)) {
                prefs.edit().putBoolean(PrefKeys.PREF_EMPHASIS, emphasisEnabled).apply();
            }
            if (intent.hasExtra(PrefKeys.EXTRA_APP_VOLUME)) {
                mapSliderToGains(appVolume);
            }
            if (intent.hasExtra(PrefKeys.EXTRA_NOISE_FILTER) && noiseSuppressor != null) {
                noiseSuppressor.setEnabled(noiseFilterEnabled);
            }
            if (intent.hasExtra(PrefKeys.EXTRA_OPTION_MIC)) {
                prefs.edit().putBoolean(PrefKeys.PREF_MIC_TYPE, micType).apply();
            }
            if (intent.hasExtra(PrefKeys.EXTRA_SUPER_EMPHASIS)) {
                prefs.edit().putBoolean(PrefKeys.PREF_SUPER_EMPHASIS, superEmphasis).apply();
            }
            if (intent.hasExtra(PrefKeys.EXTRA_VOLUME_BOOST)) {
                prefs.edit().putFloat(PrefKeys.PREF_VOLUME_BOOST, volumeBoost).apply();
            }
            if (intent.hasExtra(PrefKeys.EXTRA_BALANCE)) {
                setBalance(balance);
                prefs.edit().putFloat(PrefKeys.PREF_BALANCE, balance).apply();
            }

            boolean requestStreaming = intent.getBooleanExtra(PrefKeys.EXTRA_REQUEST_STREAMING, false);
            if (requestStreaming && !isStreaming) {
                startStreaming();
            }
        }

        // 音量を反映（Intentが空でも補完済み）
        setAppVolume(appVolume);

        return START_STICKY;
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("耳みみエイド")
                .setContentText("作動中")
                .setSmallIcon(R.drawable.notification_icon)
                .setOngoing(true)
                .build();
    }
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Audio Streaming Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(channel);
        }
    }
    private void startStreaming() {
        if (isStreaming) return;
        // サンプルレート＆バッファを自動選択
        Pair<Integer, Integer> best = findBestSampleRate();
        final int sampleRate = best.first;
        final int channelIn = AudioFormat.CHANNEL_IN_MONO;
        final int channelOut = AudioFormat.CHANNEL_OUT_STEREO;
        final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        //　マイクを選択（内部マイクか外部マイクか）
        int micType = prefs.getBoolean(PrefKeys.PREF_MIC_TYPE, false)
                ? MediaRecorder.AudioSource.CAMCORDER  // 外部マイク
                : MediaRecorder.AudioSource.MIC;     // 内蔵マイク
        // それぞれの最小バッファを正しく取得（入力/出力で別々に計算）
        int recMin  = AudioRecord.getMinBufferSize(sampleRate, channelIn, audioFormat);
        int playMin = AudioTrack.getMinBufferSize(sampleRate, channelOut, audioFormat);
        if (recMin <= 0 || playMin <= 0) {
            stopSelf();
            return;
        }
        // フレームサイズに整列
        int inFrameBytes  = inChannelCount(channelIn) * 2; // 16bit = 2 bytes
        int outFrameBytes = 1 * 2; // MONO出力
        int recBufBytes   = alignToFrameSize(recMin,  inFrameBytes)  * 2; // 余裕を持たせ2倍
        int playBufBytes  = alignToFrameSize(playMin, outFrameBytes) * 2;
        bufferSizeInShorts = recBufBytes / 2;

        // AudioRecord の初期化をする場合はここに書き込む
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        audioRecord = new AudioRecord(
                //MediaRecorder.AudioSource.VOICE_COMMUNICATION,//メインマイクのみを選択ならMic
                micType,//　メインマイクからのみの作りから更新
                sampleRate,
                channelIn,
                audioFormat,
                recBufBytes
        );
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord の初期化に失敗");
            stopSelf();
            return;
        }

        // 雑踏ノイズ抑制 (NoiseSuppressor)
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
            noiseSuppressor.setEnabled(noiseFilterEnabled);
        }

        // AudioTrack の初期化
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelOut)
                        .setEncoding(audioFormat)
                        .build())
                .setBufferSizeInBytes(playBufBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack の初期化に失敗");
            stopSelf();
            return;
        }

        // 再生の準備
        audioRecord.startRecording();
        audioTrack.play();
        applyBalanceToTrack();//複数回呼び出している？デバッグ時などに確認のこと
        setBalance(balance);

        // 入力（ステレオ）と処理用（モノラル）のバッファを確保
        pcmBuffer = new short[bufferSizeInShorts];          // ステレオデータ用に確保（LRLR...）
        final short[] monoBuffer = new short[bufferSizeInShorts]; // モノラル処理用(frames)
        stereoOut  = new short[bufferSizeInShorts];     // 出力: ステレオ（LRLR...）
        // アプリ内ボリュームを反映
        setAppVolume(appVolume);//+volumeBoost*XvolZ);//XvolZはvolumeBoostに乗じる係数
        isStreaming = true;

        // 限界突破モードの初期化をスレッド外で一度だけ行う
        if (superEmphasis && emphasisEnabled) {
            applySuperEmphasisPreset(sampleRate);
        } else {
            xManualGain = manualGain;
            boostGainMore = 6.0f;
            bandCutDepth = 0.85;
            lowCutFreq = 800.0;
            highCutFreq = 3200.0;
            initBandPass(sampleRate);
            initBandCut(sampleRate, cutLowHz, cutHighHz);
            initBoostRange(sampleRate, boostLowHz, boostHighHz);
        }

        // ストリーミングスレッド開始
        streamThread = new Thread(() -> {
            // オーディオ優先度を高める
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

            short[] buffer = pcmBuffer;
            while (isStreaming) {
                int readCount = audioRecord.read(buffer, 0, buffer.length);
                Log.d(TAG, "readCount = " + readCount);
                Log.d(TAG, "rawInput[0] = " + buffer[0]);

                if (readCount > 0) {
                    /*
                    // ステレオ → モノラル中間値（(L+R)>>1）へ変換
                    int frames = readCount / 2; // ステレオ：2サンプル=1フレーム
                    for (int f = 0; f < frames; f++) {
                        int i = f * 2;
                        int l = buffer[i];
                        int r = buffer[i + 1];
                        int m = (l + r) >> 1; // 中間値（オーバーフロー回避のためintで加算→シフト）
                        // まずは 80–300Hz の減衰をモノラルで適用
                        double cut = bandcutFilterMono(m);
                        // 高域減衰を適用
                        cut = highBandcutFilterMono(cut);
                        // 音声の増幅（ブースト）
                        if (boostEnabled) {
                            cut = boostRangeMono(cut);
                        }
                        // クリップ
                        if (cut > Short.MAX_VALUE)      cut = Short.MAX_VALUE;
                        else if (cut < Short.MIN_VALUE) cut = Short.MIN_VALUE;
                        monoBuffer[f] = (short) Math.round(cut);
                    }
                    */
                    int frames;
                    if (channelIn == AudioFormat.CHANNEL_IN_MONO) {
                        // MONO入力の場合はそのまま処理
                        frames = readCount;
                        for (int f = 0; f < frames; f++) {
                            double cut = buffer[f];
                            if (boostEnabled) {
                                cut = boostRangeMono(cut);      // 帯域ブースト
                            }
                            cut = bandcutFilterMono(cut);
                            cut = highBandcutFilterMono(cut);

                            // ピーキー音の抑制（簡易リミッター）
                            if (Math.abs(cut) > 30000) {
                                cut *= 0.8;
                            }
                           //クリップ処理
                            if (cut > Short.MAX_VALUE) cut = Short.MAX_VALUE;
                            else if (cut < Short.MIN_VALUE) cut = Short.MIN_VALUE;
                            monoBuffer[f] = (short) Math.round(cut);
                        }
                    } else {
                        // ステレオ入力の場合は左右を平均してモノラル化
                        frames = readCount / 2;
                        for (int f = 0; f < frames; f++) {
                            int i = f * 2;
                            int l = buffer[i];
                            int r = buffer[i + 1];
                            int m = (l + r) >> 1;

                            double cut = m;
                            if (boostEnabled) {
                                cut = boostRangeMono(cut);      // 帯域ブースト
                            }
                            cut = bandcutFilterMono(cut);
                            cut = highBandcutFilterMono(cut);

                            // ピーキー音の抑制（簡易リミッター）
                            if (Math.abs(cut) > 30000) {
                                cut *= 0.8;
                            }
                            //クリップ処理
                            if (cut > Short.MAX_VALUE) cut = Short.MAX_VALUE;
                            else if (cut < Short.MIN_VALUE) cut = Short.MIN_VALUE;
                            monoBuffer[f] = (short) Math.round(cut);
                        }
                    }

                    // バンドパス強調（必要時のみ）— モノラル版

                    if (emphasisEnabled) {
                        applyBandPassFilterMono(monoBuffer, frames, threshold);
                    }

                    /*
                    // --- 簡易ノイズゲート ---
                    // gateThreshold: 16bit PCMの絶対値で判定（例: 300 ≈ -40dB付近）
                    int gateThreshold = 300;
                    for (int i = 0; i < frames; i++) {
                        if (Math.abs(monoBuffer[i]) < gateThreshold) {
                            monoBuffer[i] = 0;
                        }
                    }
                    */

                    // ゲイン＆クリップ（モノラル）
                    applyGainAndClamp(monoBuffer, frames, xManualGain* (1 + volumeBoost *XvolG) );
                    //XvolGはxManualGainをブーストするときにvolumeBoostに乗算する係数

                    /*
                    // --- コンフォートノイズ追加 ---
                    // 無音時に極小のピンクノイズを混ぜて耳詰まり感を防ぐ
                    // noiseLevel: 0.002f ≈ -54dBFS
                    float noiseLevel = 0.002f;
                    for (int i = 0; i < frames; i++) {
                        if (monoBuffer[i] == 0) {
                            // 簡易ピンクノイズ生成（ホワイトノイズに1次LPFをかける）
                            float white = (float)(Math.random() * 2.0 - 1.0);
                            pinkState = pinkState + 0.05f * (white - pinkState);
                            monoBuffer[i] = (short)(pinkState * noiseLevel * 32767f);
                        }
                    }
                    */

                    // stereoOut のサイズを frames*2 に合わせる
                    //　必要に応じて再確保（null またはサイズ不足時）
                    if (stereoOut == null || stereoOut.length < frames * 2) {
                        stereoOut = new short[frames * 2];
                    }
                    for (int f = 0; f < frames; f++) {
                        short s = monoBuffer[f];
                        int i = f << 1;
                        stereoOut[i]     = s; // L
                        stereoOut[i + 1] = s; // R
                    }

                    // ステレオトラックへ書き込み（サンプル数は frames*2）
                    Log.d(TAG, "stereoOut[0] = " + stereoOut[0] + ", stereoOut[1] = " + stereoOut[1]);
                    audioTrack.write(stereoOut, 0, frames * 2);
                }
            }
            // スレッドループ終了後にリソース解放
            audioRecord.stop();
            audioRecord.release();
            audioTrack.stop();
            audioTrack.release();
        }, "AudioStreamThread");
        streamThread.start();
    }

    @Override
    public void onDestroy() {
        // ストリーミング停止リクエスト
        isStreaming = false;
        super.onDestroy();

        // NoiseSuppressor の解放
        if (noiseSuppressor != null) {
            noiseSuppressor.setEnabled(false);
            noiseSuppressor.release();
            noiseSuppressor = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //アプリ内再生音量を設定する
    private void setAppVolume(float vol) {
        appVolume = Math.max(0f, Math.min(1f, vol));
        // 全体音量とバランス（左右比）を合成して反映
        applyBalanceToTrack();
    }

    @SuppressWarnings("deprecation")
    private void applyBalanceToTrack() {
        if (audioTrack == null) return;
        float lv = clamp01(appVolume * leftRatio);
        float rv = clamp01(appVolume * rightRatio);
        audioTrack.setStereoVolume(lv, rv); // setVolumeでは左右差が出せない
    }

    private static float clamp01(float x) {
        return (x < 0f) ? 0f : (x > 1f ? 1f : x);
    }

    // 等電力パン（-1=左, 0=中央, +1=右）→ 左右比率を更新して即反映
    public void setBalance(float b) {
        balance = Math.max(-1f, Math.min(1f, b));
        double theta = (balance + 1.0) * Math.PI / 4.0; // [-π/4, π/4] → [0, π/2]
        leftRatio  = (float) Math.cos(theta);
        rightRatio = (float) Math.sin(theta);
        applyBalanceToTrack();
    }

    private void initBandPass(double sampleRate) {
        double dt = 1.0 / sampleRate;

        // 1次低域フィルタ（LPF）用α
        double rcLow  = 1.0 / (2 * Math.PI * highCutFreq);
        lpAlpha = dt / (dt + rcLow);

        // 1次高域フィルタ（HPF）用α
        double rcHigh = 1.0 / (2 * Math.PI * lowCutFreq);
        hpAlpha = rcHigh / (dt + rcHigh);

        // 前回値リセット
        hpPrevInput = 0.0;
        hpPrevOutput = 0.0;
        lpPrevOutput = 0.0;
    }

    //（MONO版）バンドパス本体：length はモノラルのサンプル数
    private void applyBandPassFilterMono(short[] buffer, int length, float threshold) {
        for (int i = 0; i < length; i++) {
            double x = buffer[i];

            // ノイズゲート処理
            if (Math.abs(x) < threshold) {
                buffer[i] = 0;
                continue;  // フィルタ処理をスキップ
            }
            // HPF
            double hpOut = hpAlpha * (hpPrevOutput + x - hpPrevInput);
            hpPrevInput  = x;
            hpPrevOutput = hpOut;
            // LPF
            double lpOut = lpPrevOutput + lpAlpha * (hpOut - lpPrevOutput);
            lpPrevOutput = lpOut*0.85;//ホワイトノイズ対策で高音域の部分を減衰させている
            // クリップ
            int v = (int) Math.round(lpOut);
            if (v > Short.MAX_VALUE) v = Short.MAX_VALUE;
            else if (v < Short.MIN_VALUE) v = Short.MIN_VALUE;
            buffer[i] = (short) v;
        }
    }

   private void applyGainAndClamp(short[] buffer, int length, float gain) {
       for (int i = 0; i < length; i++) {
           // -1.0～1.0 に正規化してゲイン乗算
           float v =(buffer[i] / 32768f) * gain;
           // クリッピング
           if (v > 1f) v = 1f;
           else if (v < -1f) v = -1f;
           // 元の short 範囲に戻す
           buffer[i] =  (short) Math.round(v * 32767f);
       }
   }

    //低音域の減衰用：係数の初期化（既存の1次HP/LPと同じ式）---
    private void initBandCut(double sampleRate, double cutlowHz, double cuthighHz) {
        double dt = 1.0 / sampleRate;
        // LPF α（上限=highHz）
        double rcLow  = 1.0 / (2 * Math.PI * cuthighHz);
        cutLpAlpha = dt / (dt + rcLow);
        // HPF α（下限=lowHz）
        double rcHigh = 1.0 / (2 * Math.PI * cutlowHz);
        cutHpAlpha = rcHigh / (dt + rcHigh);
        // 状態リセット(MONO)
        cutHpPrevIn  = 0.0;
        cutHpPrevOut = 0.0;
        cutLpPrevOut = 0.0;
    }

    // 高域減衰用の初期化
    private void initHighBandCut(double sampleRate, double cutlowHz, double cuthighHz) {
        double dt = 1.0 / sampleRate;
        double rcLow  = 1.0 / (2 * Math.PI * cuthighHz);
        highCutLpAlpha = dt / (dt + rcLow);
        double rcHigh = 1.0 / (2 * Math.PI * cutlowHz);
        highCutHpAlpha = rcHigh / (dt + rcHigh);
        highCutHpPrevIn  = 0.0;
        highCutHpPrevOut = 0.0;
        highCutLpPrevOut = 0.0;
    }

    // 低音域を減衰して返す
    private double bandcutFilterMono(double sample) {
        // HPF
        double hpOut = cutHpAlpha * (cutHpPrevOut + sample - cutHpPrevIn);
        cutHpPrevIn  = sample;
        cutHpPrevOut = hpOut;
        // LPF
        double lpOut = cutLpPrevOut + cutLpAlpha * (hpOut - cutLpPrevOut);
        cutLpPrevOut = lpOut;
        // バンドカット成分を差し引く
        return sample - bandCutDepth * lpOut;
    }

    // 高域減衰用のフィルタ
    private double highBandcutFilterMono(double sample) {
        double hpOut = highCutHpAlpha * (highCutHpPrevOut + sample - highCutHpPrevIn);
        highCutHpPrevIn  = sample;
        highCutHpPrevOut = hpOut;
        double lpOut = highCutLpPrevOut + highCutLpAlpha * (hpOut - highCutLpPrevOut);
        highCutLpPrevOut = lpOut;
        return sample - highBandCutDepth * lpOut;
    }

    // 単一帯域ブースト用の初期化
    private void initBoostRange(double sampleRate, double lowHz, double highHz) {
        double dt = 1.0 / sampleRate;
        double rcLow  = 1.0 / (2 * Math.PI * highHz);
        boostLpAlpha = dt / (dt + rcLow);
        double rcHigh = 1.0 / (2 * Math.PI * lowHz);
        boostHpAlpha = rcHigh / (dt + rcHigh);
        boostHpPrevIn = boostHpPrevOut = boostLpPrevOut = 0.0;
    }

    // 単一帯域ブースト処理
    private double boostRangeMono(double sample) {
        // HPF
        double hpOut = boostHpAlpha * (boostHpPrevOut + sample - boostHpPrevIn);
        boostHpPrevIn  = sample;
        boostHpPrevOut = hpOut;
        // LPF
        double lpOut = boostLpPrevOut + boostLpAlpha * (hpOut - boostLpPrevOut);
        boostLpPrevOut = lpOut;
        // 元信号にゲイン付きで加算
        return sample + lpOut * ((boostGain+boostGainMore) - 1.0);
    }

    // UIから減衰深さを変えたい時のセッター（0.0～1.0）
    public void setBandCutDepth(double depth01) {
        bandCutDepth = Math.max(0.0, Math.min(1.0, depth01));
    }

    // 入力チャンネル数（CHANNEL_IN_MONO/CHANNEL_IN_STEREOの簡易判別）
    private static int inChannelCount(int channelMask) {
        return (channelMask == AudioFormat.CHANNEL_IN_MONO) ? 1 : 2;
    }
    // バッファサイズをフレームサイズの倍数に丸める
    private static int alignToFrameSize(int bytes, int frameSizeBytes) {
        int rem = bytes % frameSizeBytes;
        return rem == 0 ? bytes : (bytes + (frameSizeBytes - rem));
    }

    //スライダー値(0.0〜1.0)から appVolume と boostGain を同時に設定
    private void mapSliderToGains(float vol) {
        // appVolume の設定（上限あり）
        if (vol > MAX_APP_VOLUME) {
            appVolume = MAX_APP_VOLUME;
        } else {
            appVolume = vol;
        }

        // boostGain の設定（BOOST_START_VOL を超えたら可変）
        if (vol > BOOST_START_VOL) {
            boostGain = (((vol - 0.2f) * 10f) / 2f) + BOOST_BASE;
            if (boostGain > MAX_BOOST_GAIN) {
                boostGain = MAX_BOOST_GAIN;
            }
        } else {
            boostGain = BOOST_BASE;
        }

        setAppVolume(appVolume);
        // boostGain は DSP 側で直接参照される想定
        Log.d("AudioStreamService",
              String.format("Slider=%.2f AppVol=%.2f BoostGain=%.2f", vol, appVolume, boostGain));
    }
    //superEmphasisの挙動に関するメソッド
    private void applySuperEmphasisPreset(int sampleRate) {

        xManualGain       = manualGain + 0.5f;//出力する音量の強調
        boostGainMore     = 10.0f;

        cutLowHz          = 15.0;//bandCutする帯域の下限
        cutHighHz         = 425.0;//bandCutする帯域の上限
        bandCutDepth      = 0.98f;//bandCutの深さ

        lowCutFreq         = 150.0;//bandPassする帯域指定の下側
        highCutFreq        = 900.0;//bandPassする帯域指定の上側

        highCutLowHz       = 4000.0;
        highCutHighHz      = 20000.0;
        highBandCutDepth   = 0.995f;//4000～20000、広域バンドカットの深さ

        boostLowHz         = 1000.0;//音声強調を行う帯域（下側）
        boostHighHz        = 2800.0;//音声強調を行う帯域（上側）

        threshold          = 0.045f;//閾値より小さい音はノイズとしてカット
        //2800～4000Hz帯域は現時点では生音のまま
        //lpPrevOutputの値を調整したカットも検討してもいいかも。

        //　↓　フィルタの初期化
        initBandCut(sampleRate, cutLowHz, cutHighHz);
        initBandPass(sampleRate);
        initBoostRange(sampleRate, boostLowHz, boostHighHz);
        initHighBandCut(sampleRate, highCutLowHz, highCutHighHz);
    }

}
