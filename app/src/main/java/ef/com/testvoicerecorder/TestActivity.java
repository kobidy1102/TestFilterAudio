package ef.com.testvoicerecorder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.BassBoost;
import android.media.audiofx.NoiseSuppressor;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestActivity extends AppCompatActivity {

    String[] freqText = {"11.025 KHz (Lowest)", "16.000 KHz", "22.050 KHz", "44.100 KHz (Highest)"};
    Integer[] freqset = {11025, 16000, 22050, 44100};
    private ArrayAdapter<String> adapter;

    Spinner spFrequency;
    Button startRec, stopRec;

    Boolean recording;
    private final int channel_in = AudioFormat.CHANNEL_IN_MONO; //
    private final int channel_out = AudioFormat.CHANNEL_OUT_MONO;
    private final int format = AudioFormat.ENCODING_PCM_16BIT; //signed bit range from (-32768,32767)
    /** Called when the activity is first created. */
    AudioTrack audioTrack;
    AudioRecord audioRecord;
    int minBuffer;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        startRec = (Button)findViewById(R.id.startrec);
        stopRec = (Button)findViewById(R.id.stoprec);


        spFrequency = (Spinner)findViewById(R.id.frequency);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, freqText);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrequency.setAdapter(adapter);

        stopRec.setEnabled(false);

        initAudioTrack();


        startRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread recordThread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        recording = true;
                        startRecorder();

                    }

                });

                recordThread.start();
                startRec.setEnabled(false);
                stopRec.setEnabled(true);
            }
        });


        stopRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recording = false;
                startRec.setEnabled(true);
                stopRec.setEnabled(false);
            }
        });


        setupSeekBarPitch();


    }


    private void initAudioTrack(){
        int sample_rate = getSampleRate();
        minBuffer = AudioRecord.getMinBufferSize(sample_rate, channel_in, format);

        int selectedPos = spFrequency.getSelectedItemPosition();
        int sampleFreq = freqset[selectedPos];
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleFreq,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer,
                AudioTrack.MODE_STREAM);

        audioTrack.getPlaybackRate();
        Log.e("abc","pitch:"+audioTrack.getPlaybackRate());

     //   audioTrack.setPlaybackRate(22050);

    }

    private  void startRecorder(){
        int selectedPos = spFrequency.getSelectedItemPosition();

        int sampleFreq = freqset[selectedPos];

        int minBufferSize = AudioRecord.getMinBufferSize(sampleFreq,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleFreq,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize);

        int id = audioRecord.getAudioSessionId();
        if(AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler echo = AcousticEchoCanceler.create(id);
            echo.setEnabled(true);
            Log.d("Echo", "Off");
        }
        //reduce noise
        if(NoiseSuppressor.isAvailable()) {
            NoiseSuppressor noise = NoiseSuppressor.create(id);
            noise.setEnabled(true);
            Log.d(  "Noise", "Off");
        }

        audioRecord.startRecording();
        audioTrack.play();

        int read ,write;

        while(recording){
            short[] audioData = new short[minBuffer];
          read= audioRecord.read(audioData, 0, minBuffer);
           write= audioTrack.write(audioData, 0, read);

            Log.e("abc","read:"+read+" write:"+write);

        }

      //  audioRecord.stop();
    }



    public int getSampleRate() {
        //Find a sample rate that works with the device
        for (int rate : new int[] {8000, 11025, 16000,  22050, 44100, 48000}) {
            int buffer = AudioRecord.getMinBufferSize(rate, channel_in, format);
            if (buffer > 0)
                return rate;
        }
        return -1;
    }

    private void setupSeekBarPitch(){
        SeekBar seekBarPitch= findViewById(R.id.sb_pitch);
        seekBarPitch.setMax(10);
        seekBarPitch.setProgress(4);

        final BassBoost base = new BassBoost(1, audioTrack.getAudioSessionId()); //using the player id to

        seekBarPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                Log.e("abc","seek pitch:"+progress);
               // audioTrack.setPlaybackRate(progress*3000);

                base.setStrength((short) (250*progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}
