package com.example.epurse;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;

import android.widget.TextView;

public class DisplayMessageActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TextView textView = new TextView(this);
		textView.setTextSize(20.0f);
		textView.setPadding(10, 10, 10, 10);
		textView.setText(getIntent().getStringExtra(MyActivity.EXTRA_MESSAGE));
		
		setContentView(textView);
	}
	
	public boolean onCreateMenu(Menu menu) {
		//Inflate menu
		getMenuInflater().inflate(R.menu.my, menu);
		return true;
	}
}
