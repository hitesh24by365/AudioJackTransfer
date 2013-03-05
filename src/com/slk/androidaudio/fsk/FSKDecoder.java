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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import android.os.Handler;
import android.util.Log;

public class FSKDecoder extends Thread {

	private static boolean isLogging = true;
	private static int MINIMUM_BUFFER = 4; // 2 factor, 16000 acq
	private static int MAXIMUM_BUFFER = 6 * 2;

	private boolean signalDetected = false;
	private static boolean forceStop;
	private Handler mClientHandler;
	private Vector<byte[]> mSound;
	private static String TAG = "FSKDecoder";

	public FSKDecoder(Handler handler) {
		this.mClientHandler = handler;
		this.mSound = new Vector<byte[]>();
		this.forceStop = false;
	}

	private static void debugInfo(String message) {
		if (isLogging) {
			// System.out.println(">>" + message);
			Log.i(TAG, "FSKDecoder:" + message);
		}
	}

	public void run() {

		while (!this.forceStop) {

			try {
				// warning: if decode sound takes too much time/processing (as
				// debugging a long array)
				// this loop stops working

				if (signalDetected()) {
					if (messageAvailable()) {
						decodeSound();
					} else
						Thread.sleep(1 * 100); // wait to acq enough sound
				} else
					Thread.sleep(50); // wait for the next sound acq

			} catch (InterruptedException e) {
				debugInfo("FSKDecoder:run");
				e.printStackTrace();
			}
		}
		debugInfo("STOP run()");

	}

	public synchronized void stopAndClean() {
		debugInfo("STOP stopAndClean()");
		this.forceStop = true;
	}

	private synchronized boolean messageAvailable() {
		boolean available = false;
		if (this.mSound.size() >= MINIMUM_BUFFER)
			available = true;
		debugInfo("messageAvailable()=" + available);
		return available;
	}

	private boolean signalDetected() {
		if (!signalDetected) {
			byte[] sound = this.getSound();
			if (sound != null) {
				double data[] = this.byte2double(sound);
				signalDetected = FSKModule.signalAvailable(data);
				if (signalDetected)
					debugInfo("signalDetected() TRUE");
			}
		}
		debugInfo("signalDetected()=" + this.signalDetected);
		return signalDetected;
	}

	public synchronized void addSound(byte[] sound, int nBytes) {
		byte[] data = new byte[nBytes];
		for (int i = 0; i < nBytes; i++) {
			data[i] = sound[i];
		}
		this.mSound.add(data);
		debugInfo("addSound nBytes=" + nBytes + " accumulated="
				+ this.mSound.size());

		if (this.mSound.size() > MAXIMUM_BUFFER) {
			debugInfo("ERROR addSound() buffer overflow size="
					+ this.mSound.size());
			// reset state and cleaning the buffer
			this.signalDetected = false;
			this.mSound.clear();
		}
	}

	private synchronized byte[] getSound() {
		// returns the first sound part and removes it from the buffer
		// or null if there is no sound in the buffer
		if (this.mSound.size() > 0)
			return (byte[]) this.mSound.remove(0);
		else
			return null;
	}

	private synchronized byte[] consumeSoundMessage() {
		int counter = 0;
		for (int i = 0; i < MINIMUM_BUFFER; i++) {
			counter += this.mSound.elementAt(i).length;
		}
		byte[] sound = new byte[counter];

		counter = 0; // removing the first block (carrier)
		for (int i = 0; i < MINIMUM_BUFFER; i++) {
			byte[] s = this.mSound.elementAt(i);
			for (int j = 0; j < s.length; j++) {
				sound[counter + j] = s[j];
			}
			counter += s.length;
		}
		this.mSound.clear();
		this.signalDetected = false;
		debugInfo("FSKDEC:consumeSound() nBytes=" + sound.length);
		return sound;
	}

	private double[] byte2double(byte[] data) {
		double d[] = new double[data.length / 2];
		ByteBuffer buf = ByteBuffer.wrap(data, 0, data.length);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		int counter = 0;
		while (buf.remaining() >= 2) {
			double s = buf.getShort();
			d[counter] = s;
			counter++;
		}
		return d;
	}

	private void decodeSound() {
		byte[] sound = consumeSoundMessage();
		debugInfo("decodeSound: length=" + sound.length);
		// this.decodeAmplitude(sound, sound.length);
		this.decodeFSK(sound);
	}

	private void decodeFSK(byte[] audioData) {
		double[] sound = byte2double(audioData);

		debugInfo("decodeFSK: bytes length=" + audioData.length);
		debugInfo("decodeFSK: doubles length=" + sound.length);
		try {
			int message = FSKModule.decodeSound(sound);
			debugInfo("decodeFSK():message=" + message + ":"
					+ Integer.toBinaryString(message));
			// validate message integrity
			message = ErrorDetection.decodeMessage(message);
			debugInfo("decodeFSK():message number=" + message + ":"
					+ Integer.toBinaryString(message));
			this.mClientHandler.obtainMessage(
					ArduinoService.HANDLER_MESSAGE_FROM_ARDUINO, message, 0)
					.sendToTarget();
		} catch (AndroinoException ae) {
			debugInfo("decodeFSK():Androino ERROR=" + ae.getMessage());
			this.mClientHandler.obtainMessage(
					ArduinoService.HANDLER_MESSAGE_FROM_ARDUINO, ae.getType(),
					0).sendToTarget();
		} catch (Exception e) {
			debugInfo("decodeFSK():ERROR=" + e.getMessage());
			this.mClientHandler.obtainMessage(
					ArduinoService.HANDLER_MESSAGE_FROM_ARDUINO, -2, 0)
					.sendToTarget();
		}
	}

}
