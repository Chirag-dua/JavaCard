package com.gotrust.hcedemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class HceDemoAdapter extends BaseAdapter {

	private LayoutInflater layoutInflater;
	private List<String> messages = new ArrayList<String>(512);
	private int messageCounter;

	public HceDemoAdapter(LayoutInflater layoutInflater) {
		this.layoutInflater = layoutInflater;
	}

	public void addMessage(String message) {
		messageCounter++;
		messages.add("Message [" + messageCounter + "]: " + message);
		notifyDataSetChanged();
	}

	public void removeAllMsg() {
		messageCounter = 0;
		messages.clear();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return messages == null ? 0 : messages.size();
	}

	@Override
	public Object getItem(int position) {
		return messages.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = layoutInflater.inflate(
					android.R.layout.simple_list_item_1, parent, false);
		}
		TextView view = (TextView) convertView.findViewById(android.R.id.text1);
		view.setTextSize(12);
		view.setText((CharSequence) getItem(position));
		return convertView;
	}

	public void saveLog(String strLogFilePath) throws IOException {
		File file = new File(strLogFilePath);
		OutputStream output = new FileOutputStream(file);

		for (int i = 0; i < messages.size(); i++) {
			output.write(((String)messages.get(i)).getBytes());
			output.write("\n".getBytes());
		}

	}
}
