package com.aracroproducts.presentationhelper;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WhileRecording#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WhileRecording extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_VOLUME = "volume";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private int mVolume;
    private Chronometer chronometer;

    public WhileRecording() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WhileRecording.
     */
    // TODO: Rename and change types and number of parameters
    public static WhileRecording newInstance() {
        WhileRecording fragment = new WhileRecording();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_while_recording, container, false);
    }
}