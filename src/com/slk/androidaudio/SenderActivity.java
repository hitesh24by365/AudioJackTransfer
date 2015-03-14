package com.slk.androidaudio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.slk.androidaudio.fsk.ErrorDetection;
import com.slk.androidaudio.fsk.FSKModule;

public class SenderActivity extends Activity {

	EditText msgEditText;
	Button btnOK;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sender);

		msgEditText = (EditText) findViewById(R.id.msgEditText);
		btnOK = (Button) findViewById(R.id.buttonSend);

		btnOK.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				write(Integer.parseInt(msgEditText.getText().toString()));
			}
		});

	}

	public synchronized void write(int message) {
		encodeMessage(message);
	}

	private static final int AUDIO_SAMPLE_FREQ = 44100;

	private void encodeMessage(int value) {
		// audio initialization
		int AUDIO_BUFFER_SIZE = 4096;
		int minBufferSize = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_FREQ,
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		if (AUDIO_BUFFER_SIZE < minBufferSize)
			AUDIO_BUFFER_SIZE = minBufferSize;
		AudioTrack aT = new AudioTrack(AudioManager.STREAM_MUSIC,
				AUDIO_SAMPLE_FREQ, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, AUDIO_BUFFER_SIZE,
				AudioTrack.MODE_STREAM);
		aT.play();

		// error detection encoding
		Log.i("TAG", "encodeMessage() value=" + value);
		value = ErrorDetection.createMessage(value);
		Log.i("TAG", "encodeMessage() message=" + value);
		// sound encoding
		double[] sound = FSKModule.encode(value);

		ByteBuffer buf = ByteBuffer.allocate(4 * sound.length);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < sound.length; i++) {
			int yInt = (int) sound[i];
			buf.putInt(yInt);
		}
		byte[] tone = buf.array();
		// play message
		int nBytes = aT.write(tone, 0, tone.length);
		aT.stop();
		aT.release();
	}

}
