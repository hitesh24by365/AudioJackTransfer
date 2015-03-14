/*		
 * Copyright (C) 2011 Androino authors		
 *		
 * Licensed under the Apache License, Version 2.0 (the "License");		
 * you may not use this file except in compliance with the License.		
 * You may obtain a copy of the License at		
 *		
 *      http://www.apache.org/licenses/LICENSE-2.0		
 *		
 * Unless required by applicable law or agreed to in writing, software		
 * distributed under the License is distributed on an "AS IS" BASIS,		
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.		
 * See the License for the specific language governing permissions and		
 * limitations under the License.		
 */

package com.slk.androidaudio.fsk;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

public class ArduinoService implements Runnable {
	private static boolean isLogging = true;
	private static final int EMULATOR_SAMPLE_FREQ = 44100; // emulator sample
															// freq 8000Hz
	private static final int GT540_SAMPLE_FREQ = 44100; // LG GT540
	private static final int AUDIO_SAMPLE_FREQ = GT540_SAMPLE_FREQ;
	public static int ACQ_AUDIO_BUFFER_SIZE = 4096; // 16000

	private static final String TAG = "ArduinoService";

	private Handler mClientHandler;
	private FSKDecoder mDecoder;
	private static boolean forceStop = false;
	public static final int HANDLER_MESSAGE_FROM_ARDUINO = 2000;
	public static final int RECORDING_ERROR = -1333;

	public ArduinoService(Handler handler) {
		this.mClientHandler = handler;
	}

	private static void debugInfo(String message) {
		if (isLogging) {
			Log.i(TAG, "ArduinoService:" + message);
		}
	}

	@Override
	public void run() {
		this.forceStop = false;
		// Decoder initialization
		this.mDecoder = new FSKDecoder(this.mClientHandler);
		this.mDecoder.start();

		// Sound recording loop
		this.audioRecordingRun();
	}

	public void stopAndClean() {
		debugInfo("STOP stopAndClean():");
		this.forceStop = true;
	}

	private void audioRecordingRun() {
		int AUDIO_BUFFER_SIZE = ACQ_AUDIO_BUFFER_SIZE; // 44000;//200000;//
														// 16000;
		int minBufferSize = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_FREQ,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		if (AUDIO_BUFFER_SIZE < minBufferSize)
			AUDIO_BUFFER_SIZE = minBufferSize;

		debugInfo("buffer size:" + AUDIO_BUFFER_SIZE);
		byte[] audioData = new byte[AUDIO_BUFFER_SIZE];

		AudioRecord aR = new AudioRecord(MediaRecorder.AudioSource.MIC,
				AUDIO_SAMPLE_FREQ,AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
				AUDIO_BUFFER_SIZE);

		// audio recording
		aR.startRecording();
		int nBytes = 0;
		int index = 0;
		this.forceStop = false;
		// continuous loop
		while (true) {
			nBytes = aR.read(audioData, index, AUDIO_BUFFER_SIZE);
			debugInfo("audio acq: length=" + nBytes);
			debugInfo("nBytes=" + nBytes);
			if (nBytes < 0) {
				debugInfo("audioRecordingRun() read error=" + nBytes);
				this.mClientHandler.obtainMessage(
						ArduinoService.HANDLER_MESSAGE_FROM_ARDUINO,
						RECORDING_ERROR, 0).sendToTarget();
			}
			this.mDecoder.addSound(audioData, nBytes);

			if (this.forceStop) {
				this.mDecoder.stopAndClean();
				break;
			}
		}

		aR.stop();
		aR.release();
		debugInfo("STOP audio recording stoped");

	}

}
