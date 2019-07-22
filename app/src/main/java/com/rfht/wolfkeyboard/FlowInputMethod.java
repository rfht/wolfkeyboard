package com.enchantedcode.flow;

/**
 * Copyright 2011-2015 by Peter Eastman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.app.*;
import android.content.*;
import android.database.*;
import android.inputmethodservice.*;
import android.os.*;
import android.preference.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

public class FlowInputMethod extends InputMethodService
{
  private String lastDictionaryName;
  private Dictionary dictionary;
  private KeyboardView keyboardView;
  private CandidatesView candidatesView;
  private TouchListener touchListener;
  private ControlsToggle toggle;
  private ControlsPanel controlsPanel;
  private AddWordButton addWordButton;
  private View extractView;
  private boolean simpleMode, temporarySimpleMode, passwordMode, pressedArrowKey;
  private int selectionStart, selectionEnd;

  @Override
  public void onCreate()
  {
    super.onCreate();
    getContentResolver().registerContentObserver(UserDictionary.Words.CONTENT_URI, true, new ContentObserver(new Handler()) {
      @Override
      public void onChange(boolean selfChange)
      {
        // The user dictionary has changed, so we'll need to reload it.

        rebuildDictionary();
      }
    });
  }
  @Override
  public View onCreateInputView()
  {
    keyboardView = new KeyboardView(this, Flow.baseKeyboard, Flow.shiftKeyboard, Flow.altKeyboard, Flow.altShiftKeyboard, Flow.emojiKeyboard);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    keyboardView.setShowOverlay(prefs.getBoolean("showPopup", true));
    keyboardView.setOverlayTime((2+prefs.getInt("popupDuration", 3))*100);
    candidatesView = new CandidatesView(this);
    toggle = new ControlsToggle(this);
    controlsPanel = new ControlsPanel(this);
    addWordButton = new AddWordButton(this);
    createListener();

    // Create the scroller for the candidates view.

    HorizontalScrollView scroll = new HorizontalScrollView(this);
    scroll.addView(candidatesView);
    scroll.requestDisallowInterceptTouchEvent(true);
    LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    scroll.setLayoutParams(layout);

    // Lay out the widgets.

    toggle.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
    LinearLayout row = new LinearLayout(this);
    row.addView(toggle);
    row.addView(scroll);
    row.addView(addWordButton);
    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    column.addView(row);
    FrameLayout frame = new FrameLayout(this);
    frame.addView(keyboardView);
    frame.addView(controlsPanel);
    column.addView(frame);
    controlsPanel.setVisibility(View.INVISIBLE);
    return column;
  }

  @Override
  public View onCreateExtractTextView()
  {
    extractView = super.onCreateExtractTextView();
    return extractView;
  }

  public View getExtractView()
  {
    return extractView;
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting)
  {
    super.onStartInput(attribute, restarting);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    String dictionaryName = prefs.getString("dictionary", "american");
    if (dictionary == null || !dictionaryName.equals(lastDictionaryName))
      dictionary = new Dictionary(this, dictionaryName);
    lastDictionaryName = dictionaryName;
    int type = getCurrentInputEditorInfo().inputType;
    int typeClass = type&InputType.TYPE_MASK_CLASS;
    int typeVariation = type&InputType.TYPE_MASK_VARIATION;
    simpleMode = (typeClass == InputType.TYPE_CLASS_TEXT && (typeVariation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
          typeVariation == InputType.TYPE_TEXT_VARIATION_URI || typeVariation == InputType.TYPE_TEXT_VARIATION_PASSWORD));
    passwordMode = (typeClass == InputType.TYPE_CLASS_TEXT && typeVariation == InputType.TYPE_TEXT_VARIATION_PASSWORD);
    if (keyboardView != null)
    {
      keyboardView.setShiftMode(KeyboardView.ModifierMode.UP);
      keyboardView.setShowOverlay(prefs.getBoolean("showPopup", true));
      keyboardView.setOverlayTime((2+prefs.getInt("popupDuration", 3))*100);
    }
    if (candidatesView != null)
      candidatesView.setCandidates(null, false);
    createListener();
  }

  @Override
  public boolean onEvaluateFullscreenMode()
  {
    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    int screenWidth = wm.getDefaultDisplay().getWidth();
    int screenHeight = wm.getDefaultDisplay().getHeight();

    // Find the desired height of the keyboard when the screen is in portrait mode.

    int desiredHeight = KeyboardView.computeHeight(Math.min(screenWidth, screenHeight), Math.max(screenWidth, screenHeight));
    SharedPreferences preferences = getSharedPreferences("Flow", Context.MODE_PRIVATE);
    desiredHeight = Math.min(desiredHeight, preferences.getInt("keyboardSize", desiredHeight));

    // Decide whether to use fullscreen mode.

    return desiredHeight > 0.65f*screenHeight;
  }

  private void createListener()
  {
    if (keyboardView != null && candidatesView != null)
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
      if (touchListener != null)
        touchListener.finish();
      keyboardView.setOnTouchListener(touchListener = new TouchListener(keyboardView, candidatesView, dictionary));
      touchListener.setInputMethodService(this);
      touchListener.setLongPressDelay(10*prefs.getInt("longPressDelay", 30)+300);
      touchListener.setBackspaceDelay(10*(15-prefs.getInt("backspaceRate", 6))+30);
      if (getCurrentInputEditorInfo() != null)
      {
        int type = getCurrentInputEditorInfo().inputType;
        if ((type&InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER)
          keyboardView.setAltMode(KeyboardView.ModifierMode.LOCKED);
      }
      updateShiftMode();
    }
  }

  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
  {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    if (touchListener == null)
      return;
    TouchListener.CandidatesType candidatesType = touchListener.getCandidatesType();
    boolean jumped = (newSelStart != oldSelStart && newSelStart != oldSelStart+1 && candidatesType != TouchListener.CandidatesType.Trace);
    selectionStart = newSelStart;
    selectionEnd = newSelEnd;
    updateShiftMode();
    if (candidatesEnd != -1 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd))
      touchListener.selectCandidate(0, true);
    if (pressedArrowKey)
    {
      touchListener.setCandidates(null, TouchListener.CandidatesType.None);
      candidatesView.setCandidates(null, false);
      pressedArrowKey = false;
    }
    else if (candidatesEnd == -1 && (candidatesType != TouchListener.CandidatesType.PrefixAfterDelete || newSelStart != oldSelStart-1) && (touchListener.getCandidates() == null || candidatesType == TouchListener.CandidatesType.ExistingWord || jumped))
      touchListener.suggestReplacementsForExistingWord();
  }

  @Override
  public void onWindowShown()
  {
    if (toggle != null)
      toggle.setSelected(false);
  }

  public TouchListener getTouchListener()
  {
    return touchListener;
  }

  public KeyboardView getKeyboardView()
  {
    return keyboardView;
  }

  public View getControlsPanel()
  {
    return controlsPanel;
  }

  public ControlsToggle getControlsToggle()
  {
    return toggle;
  }

  public AddWordButton getAddWordButton()
  {
    return addWordButton;
  }

  public boolean isSimpleMode()
  {
    return simpleMode || temporarySimpleMode;
  }

  public boolean isSimpleModePermanent()
  {
    return simpleMode;
  }

  public void setSimpleMode(boolean simple)
  {
    temporarySimpleMode = simple;
  }

  public boolean isPasswordMode()
  {
    return passwordMode;
  }

  public int getSelectionStart()
  {
    return selectionStart;
  }

  public int getSelectionEnd()
  {
    return selectionEnd;
  }

  public void updateShiftMode()
  {
    if (keyboardView == null || getCurrentInputConnection() == null)
      return;
    if (keyboardView.getShiftMode() != KeyboardView.ModifierMode.LOCKED && keyboardView.getShiftMode() != KeyboardView.ModifierMode.EMOJI_LOCKED && !isSimpleMode())
    {
      CharSequence before = getCurrentInputConnection().getTextBeforeCursor(5, 0);
      if (before == null)
        return;
      boolean capitalize = true;
      for (int i = before.length()-1; i >= 0; i--)
      {
        char c = before.charAt(i);
        if (c == '.' || c == '!' || c == '?' || c == '¡' || c == '¿')
          break;
        if (!Character.isSpace(c) && c != '\"' && c != '\'')
        {
          capitalize = false;
          break;
        }
      }
      keyboardView.setShiftMode(capitalize ? KeyboardView.ModifierMode.DOWN : KeyboardView.ModifierMode.UP);
    }
  }
  
  public void setPressedArrowKey()
  {
    pressedArrowKey = true;
  }

  public void rebuildDictionary()
  {
    dictionary = null;
    touchListener.setDictionary(null);
    if (isInputViewShown())
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
      lastDictionaryName = prefs.getString("dictionary", "american");
      dictionary = new Dictionary(FlowInputMethod.this, lastDictionaryName);
      touchListener.setDictionary(dictionary);
    }
  }

  public void showDialog(Dialog dlg)
  {
    Window window = dlg.getWindow();
    WindowManager.LayoutParams lp = window.getAttributes();
    lp.token = candidatesView.getWindowToken();
    lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
    window.setAttributes(lp);
    window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    dlg.show();
  }
}
