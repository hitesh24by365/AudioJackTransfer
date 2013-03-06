package com.slk.androidaudio;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.slk.androidaudio.receiver.AudioReceiver;

public class ReceiverActivity extends Activity {
	private static final String TAG = "MainActivity";
	private AudioReceiver mAudioReceiver;

	public ReceiverActivity() {
		this.mAudioReceiver = new AudioReceiver(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_receiver);
		final RadioButton radio = (RadioButton) findViewById(R.id.RadioButton01);

		final Button button = (Button) findViewById(R.id.Button);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (radio.isChecked()) {
					mAudioReceiver.stop();
				} else
					mAudioReceiver.start();
			}
		});
	}

	public void showDebugMessage(String message, boolean showToast) {
		try {
			if (showToast) {
				Toast.makeText(this.getApplicationContext(), message,
						Toast.LENGTH_SHORT).show();
			} else {
				TextView txt = (TextView) findViewById(R.id.DebugText);
				String info = txt.getText().toString();
				if (info.length() > 300)
					info = info.substring(0, 30);
				info = message + "\n" + info;
				txt.setText(info);
			}
		} catch (Exception e) {
			Log.e(TAG, "ERROR showDebugMessage()=" + message, e);
		}
	}

}
