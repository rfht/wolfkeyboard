package com.enchantedcode.flow;

/* Copyright (C) 2013-2015 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class AdjustSizeActivity extends Activity implements SeekBar.OnSeekBarChangeListener
{
  private int minHeight, maxHeight;
  private KeyboardView keyboardView;
  private SeekBar sizeSlider, positionSlider;
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.adjustsize);
    FrameLayout view = (FrameLayout) findViewById(R.id.keyboardContainer);
    keyboardView = new KeyboardView(this, Flow.baseKeyboard, Flow.shiftKeyboard, Flow.altKeyboard, Flow.altShiftKeyboard, Flow.emojiKeyboard);
    view.addView(keyboardView);
    sizeSlider = (SeekBar) findViewById(R.id.sizeSlider);
    positionSlider = (SeekBar) findViewById(R.id.positionSlider);
    SharedPreferences preferences = getSharedPreferences("Flow", MODE_PRIVATE);
    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    int displayWidth = wm.getDefaultDisplay().getWidth();
    int displayHeight = wm.getDefaultDisplay().getHeight();
    maxHeight = KeyboardView.computeHeight(displayWidth, displayHeight);
    minHeight = maxHeight/3;
    sizeSlider.setMax(maxHeight-minHeight);
    sizeSlider.setProgress(preferences.getInt("keyboardSize", maxHeight)-minHeight);
    positionSlider.setProgress(preferences.getInt("keyboardPosition", 1));
    sizeSlider.setOnSeekBarChangeListener(this);
    positionSlider.setOnSeekBarChangeListener(this);
  }

  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
  {
    SharedPreferences.Editor preferences = getSharedPreferences("Flow", MODE_PRIVATE).edit();
    preferences.putInt("keyboardSize", sizeSlider.getProgress()+minHeight);
    preferences.putInt("keyboardPosition", positionSlider.getProgress());
    preferences.commit();
    keyboardView.requestLayout();
  }

  public void onStartTrackingTouch(SeekBar seekBar)
  {
  }

  public void onStopTrackingTouch(SeekBar seekBar)
  {
  }
}
