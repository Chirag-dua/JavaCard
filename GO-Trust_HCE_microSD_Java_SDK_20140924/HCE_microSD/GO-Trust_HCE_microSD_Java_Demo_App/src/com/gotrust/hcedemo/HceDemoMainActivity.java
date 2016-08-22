package com.gotrust.hcedemo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.gotrust.hce.GOTrustHCEmicroSDException;
import com.gotrust.hce.GOTrustmicroSDHandler;
import com.gotrust.hce.GOTrustHCEmicroSDMsgInfo;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.os.PowerManager;
import android.widget.Button;
import android.widget.ListView;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.NfcAdapter.ReaderCallback;

import com.gotrust.hcedemo.R;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class HceDemoMainActivity extends Activity implements GOTrustHCEmicroSDMsgInfo {

	private ListView listView;
	private HceDemoAdapter isoDepAdapter;
	private MenuItem menuItemAutoAct;
	private Button btnEnableSD, btnDisableSD;
	private ImageView gtrIcon, imgViewAnimation;

	private int imgAnimationIdx;
	private Timer imgAnimationTimer;
	private MyHandler imgAnimationHandler;

	private GOTrustmicroSDHandler gtruSDHandler;

	// When HCE microSD is enabled. We use power management to prevent mobile to
	// power off microSD.
	PowerManager pm;
	PowerManager.WakeLock wakeLock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		listView = (ListView) findViewById(R.id.listView);
		btnEnableSD = (Button) findViewById(R.id.enable_gtr_usd);
		btnDisableSD = (Button) findViewById(R.id.disable_gtr_usd);

		isoDepAdapter = new HceDemoAdapter(getLayoutInflater());
		listView.setAdapter(isoDepAdapter);

		gtruSDHandler = GOTrustmicroSDHandler.getInstance();
		gtruSDHandler.initMsgInfo(this);

		// Add a simple animation to show HCE microSD is activated.
		gtrIcon = (ImageView) findViewById(R.id.imageView1);
		gtrIcon.setImageResource(R.drawable.gotrust_trans);
		imgViewAnimation = (ImageView) findViewById(R.id.imgAnimationView);

		Bitmap bmp = null;
		try {
			bmp = BitmapFactory.decodeStream(this.getAssets().open(
					"hce_enable_ico_1.png"));

		} catch (IOException e) {
			e.printStackTrace();
		}

		imgViewAnimation.setImageBitmap(bmp);
		imgAnimationHandler = new MyHandler();
		imgAnimationIdx = 1;

		// Add power management code to control screen on/off.
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
				"GTR HCE Demo");

		btnEnableSD.setEnabled(true);
		btnDisableSD.setEnabled(false);

		btnEnableSD.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// If HCE microSD is enabled manually, the auto activation
				// function
				// has to be disabled.
				gtruSDHandler.setAutoActivateMicroSD(false);

				isoDepAdapter.removeAllMsg();

				try {
					gtruSDHandler.connectHCEmicroSD("SMART_IO.CRD");

					imgAnimationTimer = new Timer();
					imgAnimationTimer.schedule(new TickClass(), 500, 200);

					btnEnableSD.setEnabled(false);
					btnDisableSD.setEnabled(true);

					gtruSDHandler.setHCEmicroSDEnable(true);

					// Some device may power off microSD some time.
					// We need to use polling function to keep microSD power on.
					gtruSDHandler.enablemicroSDPolling(0);

					wakeLock.acquire();

				} catch (GOTrustHCEmicroSDException ex) {
					onMessage("Error when Init HCE microSD!");

				}
			}
		});

		btnDisableSD.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String strShowMsg;
				Bitmap bmp = null;

				imgAnimationTimer.cancel();
				gtruSDHandler.disablemicroSDPolling();

				try {
					bmp = BitmapFactory.decodeStream(HceDemoMainActivity.this
							.getAssets().open("hce_enable_ico_1.png"));
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					imgViewAnimation.setImageBitmap(bmp);

					// The auto activation function can be enabled again when
					// HCE microSD is disconnected.
					gtruSDHandler.setAutoActivateMicroSD(true);

					btnEnableSD.setEnabled(true);
					btnDisableSD.setEnabled(false);

					strShowMsg = "\n" + gtruSDHandler.disconnectHCEmicroSD();

					gtruSDHandler.setHCEmicroSDEnable(false);

					strShowMsg += "\n" + saveLog();

					isoDepAdapter.removeAllMsg();
					onMessage(strShowMsg);

					wakeLock.release();

				} catch (GOTrustHCEmicroSDException ex) {
					onMessage("Error when Disconnect HCE microSD!");

				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean zOnCreate;
		getMenuInflater().inflate(R.menu.optionmenu, menu);
		zOnCreate = super.onCreateOptionsMenu(menu);
		menuItemAutoAct = (MenuItem) menu.findItem(R.id.menu_auto_act);
		menuItemAutoAct.setChecked(gtruSDHandler.isAutoActivatedMicroSD());

		menuItemAutoAct = (MenuItem) menu.findItem(R.id.menu_enable_log);
		menuItemAutoAct.setChecked(gtruSDHandler.isLogEnable());

		return zOnCreate;
	}

	@Override
	public void onResume() {
		super.onResume();

	}

	@Override
	public void onPause() {
		super.onPause();

	}


	public void onMessage(final byte[] message) {
		runOnUiThread(new Runnable() {

			public void run() {
				isoDepAdapter.addMessage(new String(message));
			}
		});
	}

	@Override
	public void onMessage(final String strMessage) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				isoDepAdapter.addMessage(strMessage);
			}
		});
	}

	@Override
	public void onError(Exception exception) {
		onMessage(exception.getMessage().getBytes());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_auto_act:
			if (item.isChecked()) {
				item.setChecked(false);
				gtruSDHandler.setAutoActivateMicroSD(false);
			} else {
				item.setChecked(true);
				gtruSDHandler.setAutoActivateMicroSD(true);
			}

			return true;

		case R.id.menu_enable_log:
			if (item.isChecked()) {
				item.setChecked(false);
				gtruSDHandler.setLogEnable(false);
			} else {
				item.setChecked(true);
				gtruSDHandler.setLogEnable(true);
			}

			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private String saveLog() {
		String logFilePath;
		String strShowMsg = "";
		File[] fileArray = getExternalFilesDirs(null);

		if (fileArray.length < 2) {
			return "No microSD found!";
		}

		String tmp = fileArray[1].getPath();
		logFilePath = tmp.substring(0, tmp.indexOf("/files"));

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.ENGLISH);

		logFilePath = logFilePath + File.separator + "hcedemo_log_"
				+ sdf.format(new Date()) + ".txt";

		try {
			isoDepAdapter.saveLog(logFilePath);
			strShowMsg = "Save Log to " + logFilePath;
		} catch (IOException e) {
			e.printStackTrace();
			strShowMsg = e.getMessage();
		}

		return strShowMsg;

	}

	private class TickClass extends TimerTask {
		@Override
		public void run() {
			imgAnimationHandler.sendEmptyMessage(imgAnimationIdx);
			imgAnimationIdx++;
			if (imgAnimationIdx > 5) {
				imgAnimationIdx = 1;
			}
		}
	}

	private class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			try {
				Bitmap bmp = BitmapFactory.decodeStream(HceDemoMainActivity.this
						.getAssets().open(
								"hce_enable_ico_" + imgAnimationIdx + ".png"));
				imgViewAnimation.setImageBitmap(bmp);

			} catch (IOException e) {
				Log.v("Exception in Handler ", e.getMessage());
			}
		}
	}

}
