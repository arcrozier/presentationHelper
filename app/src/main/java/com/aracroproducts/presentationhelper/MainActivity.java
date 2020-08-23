package com.aracroproducts.presentationhelper;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getName();

    private final static String FRAGMENT_RECORDING_TAG = "com.aracroproducts.presentation_helper.fragments.recording";
    private final static String FRAGMENT_WAITING_TAG = "com.aracroproducts.presentation_helper.fragments.waiting";
    private final static int PERMISSION_CODE_AUDIO = 1;

    private WhileRecording recordingFragment;
    private StartRecording baseFragment;

    private static final int RECORD_RATE = 44100;
    private static int BUFFER_SIZE;

    private SpeechRecognizer recognizer;
    private LinkedList<Float> volumes;
    private Warnings warnings;

    private StringBuilder resultBuilder;

    //private AudioRecord audioRecord;

    private class Warnings {

        private Map<View, Integer> warnings;

        public Warnings() {
            warnings = new HashMap<>();
            resetWarnings();
        }

        public void resetWarnings() {
            warnings.put(findViewById(R.id.too_much_filler), View.GONE);
            warnings.put(findViewById(R.id.too_fast), View.GONE);
            warnings.put(findViewById(R.id.too_slow), View.GONE);
        }

        public void makeVisible(int id) {
            warnings.put(findViewById(id), View.VISIBLE);
        }

        public Map<View, Integer> getWarnings() {
            return warnings;
        }
    }

    private RecognitionListener listener = new RecognitionListener() {

        private String[] lastResult = new String[0];
        private LinkedList<Long> timesOfWords = new LinkedList<>();
        private long startTime;

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Chronometer chronometer = findViewById(R.id.time_elapsed);
            chronometer.setCountDown(false);
            chronometer.start();
        }

        @Override
        public void onBeginningOfSpeech() {
            startTime = System.currentTimeMillis();
        }

        @Override
        public void onRmsChanged(float v) {
            reportVolumeWarning(v);
        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int i) {

        }

        @Override
        public void onResults(Bundle bundle) {
            resultBuilder.append(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0));
            //recognizer.destroy();
            startTranscription();
            Log.w(TAG, "Transcription stopped");
        }

        @Override
        public void onPartialResults(Bundle bundle) {
            ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            TextView output = MainActivity.this.findViewById(R.id.transcription);

            if (output == null) return;

            Log.d(TAG, "Got partial results");

            String bestResult = resultBuilder.toString() + results.get(0);

            output.setText(bestResult);

            String[] words = bestResult.split(" ");

            StringBuilder sb = new StringBuilder();

            double sampleWords = Math.max(words.length - 30, 0);

            for (int i = words.length - 1; i > sampleWords; i--) {
                sb.append(words[i]);
            }

            String sampleText = sb.toString();

            double percentFiller = (occurrences(sampleText, "I'm") /* the transcription tends to think "um" is "I'm"*/ + occurrences(sampleText, "um") + occurrences(sampleText, "so") + occurrences(sampleText, "ok") + occurrences(sampleText, "you know") + occurrences(sampleText, "uh")) / sampleWords;

            int newWords = words.length - lastResult.length;
            long currentTime = System.currentTimeMillis();
            for (int i = 0; i < newWords; i++) {
                timesOfWords.add(currentTime);
            }
            while (timesOfWords.peekFirst() < currentTime - 10000) { // if the first item is more than 10 seconds old
                timesOfWords.removeFirst();
            }

            if (currentTime - 10000 > startTime) {
                reportWarnings(timesOfWords.toArray().length, percentFiller);
            }

            lastResult = results.get(0).split(" ");

        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    };
/*
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {


        @Override
        public void onMarkerReached(AudioRecord audioRecord) {

        }

        @Override
        public void onPeriodicNotification(AudioRecord audioRecord) {
            short[] audioData = new short[BUFFER_SIZE];
            audioRecord.read(audioData, 0, BUFFER_SIZE);
            double volumeSum = 0;
            for (short audioDatum: audioData) {
                volumeSum += Math.pow((double) audioDatum, 2);
            }
            rmsVolume += Math.pow(volumeSum, 0.5);
            rmsVolume /= 2;
        }
    }; */

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            updateSeekBar(progress);

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };


    private void updateSeekBar(int progress) {
        TextView roomType = findViewById(R.id.textview_current_room_type);
        String[] environments = getResources().getStringArray(R.array.environments);
        if (progress < 4) {
            roomType.setText(environments[0]);
        } else if (progress < 7) {
            roomType.setText(environments[1]);
        } else if (progress < 10) {
            roomType.setText(environments[2]);
        } else {
            roomType.setText(environments[3]);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        warnings = new Warnings();
        updateWarningVisibilities(warnings);
        SeekBar seekBar = findViewById(R.id.volume_level);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        updateSeekBar(seekBar.getProgress());
        resultBuilder = new StringBuilder();
        volumes = new LinkedList<>();
    }

    public void record(View view) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment, recordingFragment = WhileRecording.newInstance(), FRAGMENT_RECORDING_TAG);
        transaction.commit();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE_AUDIO);
        } else {
            initRecording();
        }
    }

    public void stopRecording(View view) {
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
        }
        //audioRecord.stop();
        warnings.resetWarnings();
        updateWarningVisibilities(warnings);
        Chronometer chronometer = findViewById(R.id.time_elapsed);
        chronometer.stop();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment, StartRecording.newInstance(), FRAGMENT_WAITING_TAG);
        transaction.commit();
    }

    private void initRecording() {
        startTranscription();
/*
        BUFFER_SIZE = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT) * 10;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORD_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        audioRecord.setRecordPositionUpdateListener(updateListener);
        audioRecord.setPositionNotificationPeriod(5000);
        audioRecord.startRecording();
        */
    }

    private void startTranscription() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(listener);
        Intent intent = new Intent();
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 20000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 20000);
        recognizer.startListening(intent);
    }

    private void reportWarnings(int words, double percentFiller) {
        if (words < 15) {
            warnings.makeVisible(R.id.too_slow);
        } else if (words > 23) {
            warnings.makeVisible(R.id.too_fast);
        }
        if (percentFiller > 10.0) {
            warnings.makeVisible(R.id.too_much_filler);
        }
        updateWarningVisibilities(warnings);

    }

    private void reportVolumeWarning(float volume) {
        SeekBar seekBar = findViewById(R.id.volume_level);
        int volumeLevel = seekBar.getProgress();
        float maxVol = 15;
        float minVol = 4;
        volume *= 1 + volumeLevel * 0.1;
        if (volume < 0) return;
        volumes.add(volume);
        float volumeTotal = 0;
        for (Float vol: volumes.toArray(new Float[0])) {
            volumeTotal += vol;
        }
        volume = volumeTotal / volumes.toArray(new Float[0]).length;
        if (volume > maxVol) {
            findViewById(R.id.too_loud).setVisibility(View.VISIBLE);
            findViewById(R.id.too_quiet).setVisibility(View.GONE);
        } else if ((volume >= minVol && volume <= maxVol) || volume <= 0) {
            findViewById(R.id.too_loud).setVisibility(View.GONE);
            findViewById(R.id.too_quiet).setVisibility(View.GONE);
        } else if (volume > 0 && volume < minVol) {
            findViewById(R.id.too_quiet).setVisibility(View.VISIBLE);
            findViewById(R.id.too_loud).setVisibility(View.GONE);
        }

        if (volumes.toArray(new Float[0]).length > 400) {
            volumes.removeFirst();
            volumes.removeFirst();
            volumes.removeFirst();
        }
    }

    private void updateWarningVisibilities(Warnings warnings) {
        for (View warn: warnings.getWarnings().keySet()) {
            warn.setVisibility(warnings.getWarnings().get(warn));
        }
        warnings.resetWarnings();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CODE_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initRecording();
                } else {
                    stopRecording(findViewById(R.id.button_stop_recording));
                }
        }
    }

    public int occurrences(String source, String match) {
        int matches = 0;
        for (int i = 0; i < source.length(); i++) {
            for (int j = 0; j < match.length(); j++) {
                if (source.charAt(Math.min(i + j, source.length() - 1)) != match.charAt(j)) {
                    matches--; // Net effect is 0 when matches is incremented at end of loop
                    break;
                }
            }
            matches++; // Signals that the sub loop was completed. If no match was found, cancels out the subtraction
        }
        return matches;
    }
}