package com.slk.androidaudio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		
		Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
		//		editTextTotal.setText("Total amount");
			}
		};
		
		handler.sendEmptyMessage(0);

		((Button) findViewById(R.id.buttonSender))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this,
								SenderActivity.class));

					}
				});
		((Button) findViewById(R.id.buttonReceiver))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this,
								ReceiverActivity.class));

					}
				});
	}

}
