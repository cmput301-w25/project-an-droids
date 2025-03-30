package com.example.an_droids;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class VoiceNoteUtil {

    private static final String TAG = "VOICE";

    private MediaRecorder recorder;
    private MediaPlayer player;
    private File outputFile;

    // ===== 🎙️ RECORDING =====
    public void startRecording(Context context) throws IOException {
        stopPlayback();

        File dir = context.getCacheDir();
        outputFile = new File(dir, "voice_note_temp.3gp");

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(outputFile.getAbsolutePath());

        recorder.prepare();
        recorder.start();

        Log.d(TAG, "Recording started: " + outputFile.getAbsolutePath());
    }

    public byte[] stopRecording() throws IOException {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            Log.d(TAG, "Recording stopped");
        }

        byte[] audioBytes = new byte[(int) outputFile.length()];
        try (FileInputStream fis = new FileInputStream(outputFile)) {
            fis.read(audioBytes);
        }

        Log.d(TAG, "Recording saved, bytes = " + audioBytes.length);
        return audioBytes;
    }

    // ===== 🔊 PLAYBACK =====
    public void startPlayback(Context context, byte[] audioBytes) throws IOException {
        stopPlayback();

        // Create temp file
        File tempFile = new File(context.getCacheDir(), "playback_temp.3gp");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(audioBytes);
            fos.flush();
        }

        Log.d(TAG, "Temp audio file created at: " + tempFile.getAbsolutePath());

        player = new MediaPlayer();

        // Force speaker ON
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);

        player.setDataSource(tempFile.getAbsolutePath());

        player.setOnPreparedListener(mp -> {
            Log.d(TAG, "MediaPlayer prepared, starting playback");
            mp.start();
        });

        player.setOnCompletionListener(mp -> {
            Log.d(TAG, "Playback completed");
            stopPlayback();
        });

        player.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Playback error: what=" + what + " extra=" + extra);
            return true;
        });

        player.setOnInfoListener((mp, what, extra) -> {
            Log.d(TAG, "Playback info: what=" + what + " extra=" + extra);
            return false;
        });

        player.prepareAsync(); // Non-blocking
        Log.d(TAG, "MediaPlayer preparing...");
    }

    public void stopPlayback() {
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
                Log.d(TAG, "MediaPlayer stopped");
            }
            player.release();
            player = null;
            Log.d(TAG, "MediaPlayer released");
        }
    }
}
