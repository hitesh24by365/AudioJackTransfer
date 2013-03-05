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

public class ErrorDetection {

	public static final int CHECKSUM_ERROR = -1000;
	private static final int PARITY_EVEN = 64;
	private static final int PARITY_ODD = 32;

	private static int checkSum(int number) {
		int counter = Integer.bitCount(number);
		int sign = (int) Math.pow(-1, counter);
		if (sign > 0)
			return PARITY_EVEN; // even
		else
			return PARITY_ODD; // odd
	}

	public static int createMessage(int number) {
		int cSum = checkSum(number);
		int message = number + cSum;
		return message;
	}

	public static int decodeMessage(int message) {
		String msg = "";
		msg += "decodeMessage : " + message;
		int mask = 31;// 00011111
		int number = message & mask;
		int check = message - number;
		msg += " check : " + check;
		int cSum = checkSum(number);
		msg += " cSum : " + cSum;

		Log.i("TAG", msg);

		return number;
		// if (cSum != check)
		// return CHECKSUM_ERROR;
		// else
		// return number;
	}

	public static void main(String[] args) {
		System.out.println("Start");
		int n = 1;
		System.out.println("Start" + n);
		System.out.println("number=" + n + " binary="
				+ Integer.toBinaryString(n));
		n = createMessage(n);
		System.out.println("msg=" + n + " binary=" + Integer.toBinaryString(n));
		n = decodeMessage(n);
		System.out.println("decoded=" + n + " binary="
				+ Integer.toBinaryString(n));
		n = 68;
		n = decodeMessage(n);
		System.out.println("decoded=" + n + " binary="
				+ Integer.toBinaryString(n));
		// testing encoding-decoding
		for (int i = 0; i < 32; i++) {
			n = i;
			System.out.print("number=" + n + " binary="
					+ Integer.toBinaryString(n));
			n = createMessage(i);
			System.out.println("msg=" + n + " binary="
					+ Integer.toBinaryString(n));
			n = decodeMessage(n);
			System.out.println("decoded=" + n + " binary="
					+ Integer.toBinaryString(n));
		}
	}

}
