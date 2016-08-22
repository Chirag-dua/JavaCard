package com.example.epurse;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

public class DisplayMessageFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
		
		//Inflate the layout for this fragment
		return inflator.inflate(R.layout.display_view, container, false);
	}
	
}
