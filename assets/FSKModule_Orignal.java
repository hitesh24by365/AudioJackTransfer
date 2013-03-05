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

import android.util.Log;

public class FSKModule_Orignal {
	private static boolean isLogging = true;
	// Experimental results
	// Arduino sends "a" character (97) 1100001
	// The generated message is 0110.0001
	// 136 samples per encoded bit
	// total message = 166 bits: 155(high)+1(low)+8bit+stop(high)+end(high)

	private static int SAMPLING_FREQUENCY = 44100; // Hz
	private static double SAMPLING_TIME = 1.0 / SAMPLING_FREQUENCY; // ms

	// reading zero+155(high)+1(low)+8bit+stop+end+zero
	private static int FREQUENCY_HIGH = 3150;
	private static int FREQUENCY_LOW = 1575;

	// high: 7 samples/peak
	// low : 14 samples/peak
	// 1492 samples/message low+8bits+stop+end
	// 136 samples/bit (=1492/11)
	private static int SAMPLES_PER_BIT = 136;

	private static int ENCODING_SAMPLES_PER_BIT = SAMPLES_PER_BIT / 2; // 68

	// bit-high = 22 peaks
	// bit-low = 6 peaks
	private static int HIGH_BIT_N_PEAKS = 12;
	private static int LOW_BIT_N_PEAKS = 7;

	private static int SLOTS_PER_BIT = 4; // 4 parts: determines the size of the
											// part analyzed to count peaks
	private static int N_POINTS = SAMPLES_PER_BIT / SLOTS_PER_BIT; // 34=136/4

	private static double PEAK_AMPLITUDE_TRESHOLD = 60; // significant sample
														// (not noise)
	private static int NUMBER_SAMPLES_PEAK = 4; // minimum number of significant
												// samples to be considered a
												// peak

	private static int MINUMUM_NPEAKS = 100; // if lower it means that there is
												// no signal/message

	private static final int BIT_HIGH_SYMBOL = 2;
	private static final int BIT_LOW_SYMBOL = 1;
	private static final int BIT_NONE_SYMBOL = 0;

	private static final int CARRIER_MIN_HIGH_BITS = 12;

	private static final int SOUND_AMPLITUDE = 31000;

	private static final String TAG = "FSKModule";

	private FSKModule_Orignal() {
	}

	private static void debugInfo(String message) {
		if (isLogging) {
			// System.out.println(">>" + message);
			Log.w(TAG, "FSKModule:" + message);
		}
	}

	// -----------------------------------------
	// ENCODING FUNCTIONS
	// -----------------------------------------

	public static double[] encode(int number) {
		FSKModule m = new FSKModule();
		debugInfo("encode: number=" + number + " binary="
				+ Integer.toBinaryString(number));
		int[] bits = m.get8Bits(number);
		// for (int i = 0; i < bits.length; i++) {
		// debugInfo("encode: number bit=" + bits[i] );
		// }
		return m.encodeMessage(bits);
	}

	private int[] get8Bits(int number) {
		int[] bits = new int[8];
		for (int i = 0; i < 8; i++) {
			int mask = (int) Math.pow(2, i);
			int value = (number & mask) >>> i;
			// debugInfo("encode: value=" + value+ " binary=" +
			// Integer.toBinaryString(value));
			if (value == 1)
				bits[i] = BIT_HIGH_SYMBOL;
			else
				bits[i] = BIT_LOW_SYMBOL;
		}
		return bits;
	}

	private double[] encodeMessage(int[] bits) {

		// reading zero+155(high)+1(low)+8bit+stop+end+zero
		double[] sound = new double[0];
		// generate zeros
		double[] zeros = new double[10 * ENCODING_SAMPLES_PER_BIT];
		sound = concatenateArrays(sound, zeros);
		debugInfo("encodeMessage: zeros: nsamples=" + zeros.length);

		// generate carrier
		double duration = 28 * ENCODING_SAMPLES_PER_BIT * SAMPLING_TIME; // experimental
																			// adjustment
																			// carrier
																			// duration
																			// =
																			// 28
																			// bits
		double[] carrier = generateTone(FREQUENCY_HIGH, duration);
		sound = concatenateArrays(sound, carrier);
		debugInfo("encodeMessage: carrier: nsamples=" + carrier.length);

		// generate message
		duration = ENCODING_SAMPLES_PER_BIT * SAMPLING_TIME;
		// start-bit
		double[] message = new double[0];
		double[] bitArray = generateTone(FREQUENCY_LOW, duration);
		message = concatenateArrays(message, bitArray);
		// message bits
		for (int i = 0; i < bits.length; i++) {
			int freq = FREQUENCY_LOW;
			if (bits[i] > 1)
				freq = FREQUENCY_HIGH;
			bitArray = generateTone(freq, duration);
			message = concatenateArrays(message, bitArray);
		}
		sound = concatenateArrays(sound, message);
		debugInfo("encodeMessage: message: nsamples=" + message.length);

		// generate stop+end
		duration = duration * 2;
		double[] end = generateTone(FREQUENCY_HIGH, duration);
		sound = concatenateArrays(sound, end);
		debugInfo("encodeMessage: end: nsamples=" + end.length);

		// zeros
		sound = concatenateArrays(sound, zeros);
		debugInfo("encodeMessage: sount total: nsamples=" + sound.length);
		return sound;
	}

	private double[] concatenateArrays(double[] a1, double[] a2) {
		double[] data = new double[a1.length + a2.length];
		for (int i = 0; i < a1.length; i++) {
			data[i] = a1[i];
		}
		for (int i = 0; i < a2.length; i++) {
			data[i + a1.length] = a2[i];
		}
		return data;
	}

	public static double[] generateTone(int frequency, double duration) {
		// int duration = 1; // s
		int samplingRate = SAMPLING_FREQUENCY; // Hz
		int numberOfSamples = (int) (duration * samplingRate);
		double samplingTime = 1.0 / samplingRate;
		samplingTime = 2 * samplingTime;

		double[] tone = new double[numberOfSamples];

		for (int i = 0; i < numberOfSamples; i++) {
			double y = Math.sin(2 * Math.PI * frequency * i * samplingTime);
			tone[i] = y * SOUND_AMPLITUDE;
		}
		return tone;
	}

	// -----------------------------------------
	// DECODING FUNCTIONS
	// -----------------------------------------

	public static boolean signalAvailable(double[] sound) {
		FSKModule m = new FSKModule();

		int nPoints = N_POINTS;
		int nParts = sound.length / nPoints;
		int nPeaks = 0;
		int startIndex = 0;
		int i = 0;
		do {
			int endIndex = startIndex + nPoints;
			int n = m.countPeaks(sound, startIndex, endIndex);
			nPeaks += n;
			i++;
			startIndex = endIndex;
			if (nPeaks > MINUMUM_NPEAKS)
				return true;
		} while (i < nParts);
		if (nPeaks > 3)
			debugInfo("signalAvailable() nPeaks=" + nPeaks);
		return false;
	}

	public static int decodeSound(double[] sound) {
		FSKModule m = new FSKModule();
		// processing sound in parts and
		// Log.w(TAG, "ENTRO EN processSound");
		int[] nPeaks = m.processSound(sound);
		if (nPeaks.length == 0) // exit: no signal detected
			return -1;

		// debugInfo("decodeSound nPeaks=" + nPeaks.length);
		// transform number of peaks into bits
		// Log.w(TAG, "ENTRO EN parseBits");
		int[] bits = m.parseBits(nPeaks);// -------------------------> OK!!
		// debugInfo("decodeSound nBits=" + bits.length);
		// extract message from the bit array
		int message = m.decodeUniqueMessage(bits, sound, nPeaks);
		debugInfo("decodeSound(): message=" + message + ":"
				+ Integer.toBinaryString(message));
		return message;
	}

	private int decodeUniqueMessageCorrected(int[] nPeaks, int startBit) {
		int message = 0;

		// process nPeaks starting from the end
		int index = (startBit + 12) * SLOTS_PER_BIT;

		// find zero -> non zero transition
		for (int i = 0; i < index; i++) {
			int i2 = nPeaks[index - i];
			int i1 = nPeaks[index - i - 1];
			debugInfo("zero->nonzero index=" + (index - i) + ": i2=" + i2
					+ ":i1=" + i1);
			if ((i1 - i2) > 2) {
				index = index - i - 1;
				break;
			}
		}
		debugInfo("zero->nonzero index=" + index);
		int[] bits = new int[2 + 8 + 1 + 2];
		for (int i = 0; i < bits.length; i++) {
			int peakCounter = 0;
			for (int j = 0; j < 4; j++) {
				peakCounter += nPeaks[index - j];
			}
			debugInfo("decode corrected: peakCounter=" + i + ":" + peakCounter);
			if (peakCounter > 7) { // LOW_BIT_N_PEAKS)
				bits[i] = BIT_LOW_SYMBOL;
			}
			if (peakCounter > 12) { // LOW_BIT_N_PEAKS)
				bits[i] = BIT_HIGH_SYMBOL;
				message += Math.pow(2, i);
			}
			debugInfo("bit=" + bits[i] + ":" + message);
			index = index - 4;
		}
		debugInfo("decode corrected: message=" + message + ":"
				+ Integer.toBinaryString(message));
		message = 0;
		for (int i = 2; i < 10; i++) {
			if (bits[i] == BIT_HIGH_SYMBOL) {
				message += Math.pow(2, 7 - (i - 2));
			}
		}
		return message;
	}

	private int decodeUniqueMessage(int[] bits, double[] sound, int[] nPeaks) {
		// start bit
		int index = findStartBit(bits, 0);
		debugInfo("decodeUniqueMessage():start bit=" + index);
		if (index == -1)
			return -1; // no start-bit detected
		if (index + 8 + 2 > bits.length)
			throw new AndroinoException(
					"Message cutted, start bit at " + index,
					AndroinoException.TYPE_FSK_DECODING_ERROR);

		/*
		 * // debugging information int number = 16; // n bits to debug for (int
		 * i = index-5; i < index-5+number; i++) {
		 * debugInfo("decodeUniqueMessage(): bits=" + i +":" + bits[i] ); } for
		 * (int i = 0; i < number*SLOTS_PER_BIT; i++) { int position = i +
		 * (index-5)*SLOTS_PER_BIT ; debugInfo("decodeUniqueMessage(): npeaks="
		 * + position+ ":" + nPeaks[position] ); }
		 * 
		 * // 8bits message int value = 0; for (int i = 0; i < 8; i++) { int bit
		 * = bits[index+i]; if (bit==BIT_HIGH_SYMBOL) value+=Math.pow(2, i); }
		 * // stop bit: do nothing // end bit: do nothing
		 * debugInfo("MESSAGE          =" + Integer.toBinaryString(value) + ":"
		 * + value);
		 */
		int correctedMessage = decodeUniqueMessageCorrected(nPeaks, index);
		debugInfo("MESSAGE corrected="
				+ Integer.toBinaryString(correctedMessage) + ":"
				+ correctedMessage);
		return correctedMessage;

	}

	private int findStartBit(int[] bits, int startIndex) {
		// find carrier and start bit
		int index = startIndex;
		int highCounter = 0;
		boolean startBitDetected = false;
		do {
			int bit = bits[index];
			switch (bit) {
			case BIT_HIGH_SYMBOL:
				highCounter++; // carrier high bit

				break;
			case BIT_LOW_SYMBOL:
				if (highCounter > CARRIER_MIN_HIGH_BITS) { // start-bit detected
					startBitDetected = true;
				} else
					highCounter = 0; // reset carrier counter
				break;
			case BIT_NONE_SYMBOL:
				highCounter = 0;// reset carrier counter
				break;
			}
			index++;
			if (index >= bits.length)
				return -1;
		} while (!startBitDetected);
		return index;
	}

	private int[] parseBits(int[] peaks) {
		// from the number of peaks array decode into an array of bits (2=bit-1,
		// 1=bit-0, 0=no bit)
		//
		int i = 0;
		int lowCounter = 0;
		int highCounter = 0;
		int nBits = peaks.length / SLOTS_PER_BIT;
		int[] bits = new int[nBits];
		// i = findNextZero(peaks,i); // do not search for silence
		i = findNextNonZero(peaks, i);
		int nonZeroIndex = i;
		if (i + SLOTS_PER_BIT >= peaks.length) // non-zero not found
			return bits;
		do {
			// int nPeaks = peaks[i]+peaks[i+1]+peaks[i+2]+peaks[i+3];
			int nPeaks = 0;
			for (int j = 0; j < SLOTS_PER_BIT; j++) {
				nPeaks += peaks[i + j];
			}
			int position = i / SLOTS_PER_BIT;
			bits[position] = BIT_NONE_SYMBOL;

			debugInfo("npeaks:i=" + i + ":pos=" + position + ": nPeaks="
					+ nPeaks);
			if (nPeaks >= LOW_BIT_N_PEAKS) {
				// Log.w(TAG, "parseBits NPEAK=" + nPeaks);
				bits[position] = BIT_LOW_SYMBOL;
				lowCounter++;
			}
			if (nPeaks >= HIGH_BIT_N_PEAKS) {
				bits[position] = BIT_HIGH_SYMBOL;
				highCounter++;
			}

			// if (nPeaks>5) bits[position] = 1;
			// if (nPeaks>12) bits[position] = 2;
			i = i + SLOTS_PER_BIT;

		} while (SLOTS_PER_BIT + i < peaks.length);
		lowCounter = lowCounter - highCounter;
		debugInfo("parseBits nonZeroIndex=" + nonZeroIndex);
		debugInfo("parseBits lows=" + lowCounter);
		debugInfo("parseBits highs=" + highCounter);
		return bits;
	}

	private int findNextNonZero(int[] peaks, int startIndex) {
		// returns the position of the next value != 0 starting form startIndex
		int index = startIndex;
		int value = 1;
		do {
			value = peaks[index];
			index++;
		} while (value == 0 && index < peaks.length - 1);
		return index - 1;
	}

	private int[] processSound(double[] sound) {
		// split the sound array into slots of N_POINTS and calculate the number
		// of peaks

		int nPoints = N_POINTS;
		int nParts = sound.length / nPoints;
		int[] nPeaks = new int[nParts];
		int startIndex = 0;
		int i = 0;
		int peakCounter = 0;
		do {
			int endIndex = startIndex + nPoints;
			int n = this.countPeaks(sound, startIndex, endIndex);
			nPeaks[i] = n;
			peakCounter += n;
			i++;
			startIndex = endIndex;
		} while (i < nParts);
		// } while (startIndex+nPoints<sound.length);
		debugInfo("processSound() peaksCounter=" + peakCounter);
		if (peakCounter < MINUMUM_NPEAKS) {
			nPeaks = new int[0];
		}
		return nPeaks;
	}

	private int countPeaks(double[] sound, int startIndex, int endIndex) {
		// count the number of peaks in the selected interval
		// peak identification criteria: sign changed and several significant
		// samples (>PEAK_AMPLITUDE_TRESHOLD)

		int index = startIndex;
		int signChangeCounter = 0;
		int numberSamplesGreaterThresdhold = 0;
		int sign = 0; // initialized at the first significant value
		do {
			double value = sound[index];
			if (Math.abs(value) > PEAK_AMPLITUDE_TRESHOLD)
				numberSamplesGreaterThresdhold++; // significant value
			// sign initialization: take the sign of the first significant value
			if (sign == 0 & numberSamplesGreaterThresdhold > 0)
				sign = (int) (value / Math.abs(value));
			boolean signChanged = false;
			if (sign < 0 & value > 0)
				signChanged = true;
			if (sign > 0 & value < 0)
				signChanged = true;

			if (signChanged
					& numberSamplesGreaterThresdhold > NUMBER_SAMPLES_PEAK) {
				signChangeCounter++; // count peak
				sign = -1 * sign; // change sign
			}
			index++;
			// debugInfo(">>>>>>>index=" + index + " sign=" + sign +
			// " signChangeCounter=" + signChangeCounter + " value=" + value +
			// " numberSamplesGreaterThresdhold=" +
			// numberSamplesGreaterThresdhold);
		} while (index < endIndex);
		return signChangeCounter;
	}

}
