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

package com.slk.androidaudio.receiver;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.slk.androidaudio.ReceiverActivity;
import com.slk.androidaudio.fsk.ArduinoService;
import com.slk.androidaudio.fsk.ErrorDetection;

public class AudioReceiver {

	// arduino messages
	// [0-9] button events
	// [10-19] specific messages
	// [20-30] protocol codes
	private static boolean isLogging = true;
	public static final int ARDUINO_PROTOCOL_ARQ = 20; // Automatic repeat
														// request
	public static final int ARDUINO_PROTOCOL_ACK = 21; // Message received
														// acknowledgment

	public static final int LAST_MESSAGE_MAX_RETRY_TIMES = 5; // Number of times
																// that last
																// message is
																// repeated
	public static final int CONSECUTIVE_CHK_ERROR_LIMIT = 1; // Number of
																// consecutive
																// checksum
																// errors to
																// send an ARQ

	private Handler mHandler;
	private ArduinoService mArduinoS;
	private ReceiverActivity mActivity;
	private int lastMessage;
	private int lastMessageCounter = 0;
	private int checksumErrorCounter = 0;

	private static void debugInfo(String message) {
		if (isLogging) {
			Log.i("TAG", "AudioReceiver:" + message);
		}
	}

	public AudioReceiver(ReceiverActivity activity) {
		this.mActivity = activity;
		this.mHandler = new Handler() {
			public void handleMessage(Message msg) {
				messageReceived(msg);
			}
		};
	}

	public void stop() {
		debugInfo("stop()");
		// STOP the arduino service
		if (this.mArduinoS != null)
			this.mArduinoS.stopAndClean();
	}

	public void start() {
		// START the arduino service
		this.mArduinoS = new ArduinoService(this.mHandler);
		new Thread(this.mArduinoS).start();
	}

	private void messageReceived(Message msg) {
		int target = msg.what;
		int value = msg.arg1;
		int type = msg.arg2;		
		
		debugInfo("tic-tac-toe:messageReceived(): target=" + target + " value="
				+ value + " type=" + type);

		switch (target) {		
		case ArduinoService.HANDLER_MESSAGE_FROM_ARDUINO:
			switch (value) {
			case ARDUINO_PROTOCOL_ARQ:
				checksumErrorCounter = 0;
				sendLastMessage();
				break;
			case ErrorDetection.CHECKSUM_ERROR:
				checksumErrorCounter++;
				if (checksumErrorCounter > CONSECUTIVE_CHK_ERROR_LIMIT) {
					checksumErrorCounter = 0;
					sendMessage(ARDUINO_PROTOCOL_ARQ);// ARQ after two
														// consecutive CHK ERROR
														// received
				}
				break;

			/*
			 * case ARDUINO_MSG_START_GAME: this.mServer.startGameClick();
			 * break; case ARDUINO_MSG_END_GAME_WINNER:
			 * this.mServer.endGame("0"); break; case
			 * ARDUINO_MSG_END_GAME_LOSER: this.mServer.endGame("1"); break;
			 * default: this.mServer.buttonClick(""+value); break;
			 */
			default:
				checksumErrorCounter = 0;
				debugInfo("tic-tac-toe:messageReceived() ACK send");
				this.sendMessage(ARDUINO_PROTOCOL_ACK);
				break;
			}
			if(value!=31)
			this.mActivity.showDebugMessage("ARD: " + value, false);
			debugInfo("tic-tac-toe:messageReceived() from arduino value="
					+ value);
			break;
		default:
			// FIXME error happened handling messages
			break;
		}
	}

	private void sendLastMessage() {
		this.sendMessage(lastMessage);
		if (lastMessageCounter > LAST_MESSAGE_MAX_RETRY_TIMES) {
			// stop repeating last message, ERROR
			this.mActivity.showDebugMessage("ERROR MAX RETRY msg="
					+ this.lastMessage, false);
			debugInfo("tic-tac-toe:sendLastMessage() ERROR MAX RETRY value="
					+ lastMessage);
			this.sendMessage(ARDUINO_PROTOCOL_ACK); // send ack to avoid ARQ
			debugInfo("tic-tac-toe:sendLastMessage() ERROR MAX RETRY SENDING ACK instead");
			lastMessageCounter = 0;
		} else {
			debugInfo("tic-tac-toe:sendLastMessage() value=" + lastMessage);
			this.sendMessage(lastMessage);
			lastMessageCounter++;
		}
	}

	private void sendMessage(int number) {
		debugInfo("tic-tac-toe:sendMessage() number=" + number);
		// this.mArduinoS.write(number);
	}

	protected void developmentSendMessage(int number) {
		this.sendMessage(number);
	}

}
