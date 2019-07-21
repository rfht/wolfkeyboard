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
import android.graphics.*;
import android.os.*;
import android.speech.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;

import java.util.*;

public class TouchListener implements View.OnTouchListener
{
  public enum CandidatesType  {None, Trace, Prefix, PrefixAfterDelete, LongPress, ExistingWord};

  private final KeyboardView keyboardView;
  private final CandidatesView candidatesView;
  private Dictionary dictionary;
  private final Handler handler;
  private final ArrayList<TracePoint> trace;
  private final LinkedList<Point> displayedPoints;
  private final LinkedList<Long> displayedTimes;
  private boolean dragInProgress, shouldInsertSpace, spaceBeforeCandidates, spaceAfterCandidates, candidateIsI, isDeleting, selectionEndsWithSpace, isCancelableLongPress;
  private CandidatesType candidatesType;
  private float lastx, lasty;
  private float minSpeed, maxSpeedSinceMin;
  private int numFinalized, skipCharacters, longPressDelay, backspaceDelay;
  private int existingWordStartOffset, existingWordEndOffset;
  private long lastTime, startTime;
  private FlowInputMethod inputMethod;
  private KeyboardView.ModifierMode shiftMode, altMode;
  private String candidates[];

  private static final char traceableKeys[] = "abcdefghijklmnopqrstuvwxyz\'".toCharArray();
  private static final long displayLength = 1500;

  public TouchListener(KeyboardView keyboardView, CandidatesView candidatesView, Dictionary dictionary)
  {
    this.keyboardView = keyboardView;
    this.candidatesView = candidatesView;
    this.dictionary = dictionary;
    handler = new Handler();
    trace =  new ArrayList<TracePoint>();
    displayedPoints = new LinkedList<Point>();
    displayedTimes = new LinkedList<Long>();
    candidatesType = CandidatesType.None;
  }

  public void setInputMethodService(FlowInputMethod inputMethod)
  {
    this.inputMethod = inputMethod;
  }

  public void setLongPressDelay(int longPressDelay)
  {
    this.longPressDelay = longPressDelay;
  }

  public int getLongPressDelay()
  {
    return longPressDelay;
  }

  public void setBackspaceDelay(int backspaceDelay)
  {
    this.backspaceDelay = backspaceDelay;
  }

  public int getBackspaceDelay()
  {
    return backspaceDelay;
  }

  public String[] getCandidates()
  {
    return candidates;
  }

  public CandidatesType getCandidatesType()
  {
    return candidatesType;
  }

  public void setCandidates(String newCandidates[], CandidatesType newType)
  {
    candidates = newCandidates;
    candidatesType = newType;
    selectionEndsWithSpace = false;
    InputConnection ic = inputMethod.getCurrentInputConnection();
    if (ic != null)
    {
      CharSequence selected = ic.getSelectedText(0);
      if (selected != null && selected.toString().endsWith(" "))
        selectionEndsWithSpace = true;
    }
  }

  public void setDictionary(Dictionary dictionary)
  {
    this.dictionary = dictionary;
  }

  public void finish()
  {
    isDeleting = false;
    dragInProgress = false;
  }
  
  public boolean onTouch(View view, MotionEvent ev)
  {
    Point keyPositions[] = keyboardView.getKeyPositions();
    if (keyPositions[0] == null)
      return true; // The keyboard hasn't been drawn yet.
    float x = ev.getX();
    float y = ev.getY();
    final long time = ev.getEventTime();
    if (ev.getAction() == MotionEvent.ACTION_DOWN)
    {
      dragInProgress = true;
      isDeleting = false;
      isCancelableLongPress = false;
      lastx = x;
      lasty = y;
      startTime = lastTime = time;
      numFinalized = 0;
      shiftMode = keyboardView.getShiftMode();
      altMode = keyboardView.getAltMode();
      trace.clear();
      displayedPoints.clear();
      displayedTimes.clear();
      displayedPoints.add(new Point((int) x, (int) y));
      displayedTimes.add(time);
      handler.postDelayed(new Runnable()
      {
        public void run()
        {
          if (startTime == time && numFinalized == 0)
          {
            if (dragInProgress)
            {
              if (trace.size() == 0)
                trace.add(new TracePoint(lastx, lasty));
              TracePoint first = trace.get(0);
              TracePoint last = trace.get(trace.size()-1);
              float dist2 = (first.x-last.x)*(first.x-last.x) + (first.y-last.y)*(first.y-last.y);
              if (dist2 < 0.25f*keyboardView.getKeySpacing()*keyboardView.getKeySpacing())
              {
                // Interpret this as a long press.

                finishTrace(true);
                if (isDeleting)
                  handler.postDelayed(this, backspaceDelay);
              }
            }
            else if (isDeleting && inputMethod != null)
            {
              inputMethod.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
              setCandidates(null, CandidatesType.None);
              handler.postDelayed(this, backspaceDelay);
            }
          }
        }
      }, longPressDelay);
      return true;
    }
    if (ev.getAction() == MotionEvent.ACTION_UP && isDeleting)
    {
      showCompletionsFromPrefix(true);
      isDeleting = false;
    }
    if (isCancelableLongPress && ev.getAction() == MotionEvent.ACTION_MOVE && inputMethod != null)
    {
      TracePoint first = trace.get(0);
      float dist2 = (first.x-x)*(first.x-x) + (first.y-y)*(first.y-y);
      if (dist2 > 0.25f*keyboardView.getKeySpacing()*keyboardView.getKeySpacing())
      {
        // We previously interpreted this as a long press, but the user has now moved further,
        // so probably they didn't really mean it to be a long press.  Cancel it and go back
        // to processing the drag.
        
        dragInProgress = true;
        isCancelableLongPress = false;
        InputConnection ic = inputMethod.getCurrentInputConnection();
        if (ic != null)
        {
          ic.commitText("", 0);
          setCandidates(null, CandidatesType.None);
          candidatesView.setCandidates(null, false);
        }
      }
    }
    if (!dragInProgress)
      return true;
    float dx = x-lastx;
    float dy = y-lasty;
    float speed = FloatMath.sqrt(dx*dx+dy*dy)/(time-lastTime);
    if (trace.size() == 0)
    {
      minSpeed = maxSpeedSinceMin = speed;
      trace.add(new TracePoint(lastx, lasty));
    }
    TracePoint current = trace.get(trace.size()-1);
    TracePoint previous = (trace.size() == 1 ? current : trace.get(trace.size()-2));
    float scale = 1.0f/(keyboardView.getKeySpacing()*keyboardView.getKeySpacing());
    float distances2[] = new float[27];
    findKeyDistances(keyboardView.getKeyboard(), distances2, x, y);
    if ((minSpeed < 0.5f*maxSpeedSinceMin && speed < 0.5f*maxSpeedSinceMin) || trace.size() == 1)
    {
      previous = current;
      current = new TracePoint(x, y);
      trace.add(current);
      for (int j = 0; j < distances2.length; j++)
        current.keyDistances[j] = Math.min(current.keyDistances[j], distances2[j]);
      minSpeed = maxSpeedSinceMin = speed;


      TracePoint p = trace.get(numFinalized);
      if (scale*p.distance2(previous) >= 0.25f || trace.size() == 1)
      {
        int first = numFinalized;
        int last = trace.size()-2;
        if (last > first+1)
        {
          float sumx = p.x;
          float sumy = p.y;
          for (int i = first+1; i < last; i++)
          {
            TracePoint p2 = trace.get(i);
            p.mergePoint(p2);
            sumx += p2.x;
            sumy += p2.y;
          }
          p.x = sumx/(last-first);
          p.y = sumy/(last-first);
          for (int i = last-1; i > first; i--)
            trace.remove(i);
        }
        numFinalized++;
        if (numFinalized == 1)
          updateModifiers();
      }
    }
    else if (speed < minSpeed)
    {
      current.x = x;
      current.y = y;
      previous.addViaKeys(current.viaKeyList);
      current.viaKeyList.clear();
      minSpeed = maxSpeedSinceMin = speed;
    }
    else if (speed > maxSpeedSinceMin)
      maxSpeedSinceMin = speed;
    for (int j = 0; j < distances2.length; j++)
      if (distances2[j] <= 0.5f)
        current.addViaKey(traceableKeys[j], distances2[j], time);


    if (ev.getAction() == MotionEvent.ACTION_UP)
      finishTrace(false);
    if (ev.getAction() == MotionEvent.ACTION_MOVE)
    {
      Long first = displayedTimes.peek();
      while (first != null && first < time-displayLength)
      {
        displayedPoints.removeFirst();
        displayedTimes.removeFirst();
        first = displayedTimes.peek();
      }
      displayedPoints.add(new Point((int) x, (int) y));
      displayedTimes.add(time);
      keyboardView.setTrace(displayedPoints);
    }
    lastx = x;
    lasty = y;
    lastTime = time;
    return true;
  }

  private void findKeyDistances(KeyboardLayout keyboard, float allDistances[], float x, float y)
  {
    Point keyPositions[] = keyboardView.getKeyPositions();
    Arrays.fill(allDistances, Float.MAX_VALUE);
    float scale = 1.0f/(keyboardView.getKeySpacing()*keyboardView.getKeySpacing());
    int numKeys = keyPositions.length;
    for (int i = 0; i < numKeys; i++)
    {
      if (keyboard.slideCharIndex[i] > -1)
      {
        Point pos = keyPositions[i];
        float dist = Math.max(0.0f, FloatMath.sqrt(scale*((pos.x-x)*(pos.x-x)+(pos.y-y)*(pos.y-y)))-0.5f);
        allDistances[keyboard.slideCharIndex[i]] = dist;
      }
    }
  }

  private void finishTrace(boolean isLongPress)
  {
    dragInProgress = false;
    keyboardView.setTrace(null);

    // Merge points that correspond to the same set of keys.

    for (int first = numFinalized; first < trace.size()-1; first++)
    {
      TracePoint p = trace.get(first);
      float scale = 1.0f/(keyboardView.getKeySpacing()*keyboardView.getKeySpacing());
      int last;
      for (last = first+1; last < trace.size() && scale*p.distance2(trace.get(last)) < 0.25f; last++)
        ;
      if (last > first+1)
      {
        float sumx = p.x;
        float sumy = p.y;
        for (int i = first+1; i < last; i++)
        {
          TracePoint p2 = trace.get(i);
          p.mergePoint(p2);
          sumx += p2.x;
          sumy += p2.y;
        }
        p.x = sumx/(last-first);
        p.y = sumy/(last-first);
        for (int i = last-1; i > first; i--)
          trace.remove(i);
      }
    }
    if (trace.size() == 1)
    {
      processSingleKey(isLongPress);
      return;
    }
    if (skipCharacters == 0 && candidatesType != CandidatesType.ExistingWord)
      selectCandidate(0, true);
    candidateIsI = false;

    // Compute weights for points.

    for (int i = 1; i < trace.size()-1; i++)
    {
      TracePoint p1 = trace.get(i-1);
      TracePoint p2 = trace.get(i);
      TracePoint p3 = trace.get(i+1);
      float dx1 = p2.x-p1.x;
      float dy1 = p2.y-p1.y;
      float dx2 = p3.x-p2.x;
      float dy2 = p3.y-p2.y;
      float len1 = FloatMath.sqrt(dx1*dx1+dy1*dy1);
      float len2 = FloatMath.sqrt(dx2*dx2+dy2*dy2);
      float dot = (dx1*dx2+dy1*dy2)/(len1*len2);
      if (dot > 0.95f && trace.size() > 10)
        trace.remove(i--);
      else
        p2.weight = 0.75f-0.25f*dot;
    }

    for (TracePoint point : trace)
    {
      Collections.sort(point.viaKeyList);
      findKeyDistances(keyboardView.getKeyboard(), point.keyDistances, point.x, point.y);
      point.finalizeViaKeys();
    }
    setCandidates(dictionary.guessWord(trace.toArray(new TracePoint[trace.size()]), shiftMode, 5), CandidatesType.Trace);
    ensureCandidatesAreUnique();
    spaceBeforeCandidates = false;
    if (inputMethod != null && candidates[0] != null)
    {
      candidatesView.setCandidates(candidates, true);
      skipCharacters = 0;
      spaceAfterCandidates = true;
      InputConnection ic = inputMethod.getCurrentInputConnection();
      if (ic != null)
      {
        CharSequence prev = ic.getTextBeforeCursor(1, 0);
        if (prev == null)
          prev = "";
        if (prev.length() > 0)
        {
          char prevChar = prev.charAt(0);
          if (prevChar == '*' || prevChar == '_' || prevChar == '"')
          {
            CharSequence prev2 = ic.getTextBeforeCursor(2, 0);
            if (prev2.length() == 2 && !Character.isWhitespace(prev2.charAt(0)))
              spaceBeforeCandidates = true;
          }
          else if (!(Character.isSpace(prevChar) || prevChar == '+' || prevChar == '-' || prevChar == '(' || prevChar == '/' || prevChar == '\\' || prevChar == '@' || prevChar == '¡' || prevChar == '¿'))
            spaceBeforeCandidates = true;
        }
      }
      selectCandidate(0, false);
    }
    if (shiftMode == KeyboardView.ModifierMode.DOWN)
      keyboardView.setShiftMode(KeyboardView.ModifierMode.UP);
    if (altMode == KeyboardView.ModifierMode.DOWN)
      keyboardView.setAltMode(KeyboardView.ModifierMode.UP);
  }

  private void processSingleKey(boolean longPress)
  {
    Point keyPositions[] = keyboardView.getKeyPositions();
    int nearest = -1;
    float nearestDistance = Float.MAX_VALUE;
    float x = trace.get(0).x;
    float y = trace.get(0).y;
    for (int i = 0; i < keyPositions.length; i++)
    {
      Point pos = keyPositions[i];
      float dist = FloatMath.sqrt(((pos.x-x)*(pos.x-x)+(pos.y-y)*(pos.y-y)));
      if (dist < nearestDistance)
      {
        nearest = i;
        nearestDistance = dist;
      }
    }
    if (nearestDistance > keyboardView.getKeySpacing())
      return;
    int key = (longPress ? keyboardView.getSecondaryKeyboard() : keyboardView.getKeyboard()).keys[nearest];
    if (candidateIsI && Character.isLetter(key))
    {
      // They previously typed an i or I.  We now see that was the start of a word, not the word "I".

      spaceAfterCandidates = false;
      if (candidates != null && candidates.length > 1)
        selectCandidate(1, true);
    }
    boolean showingExistingWordCandidates = (candidatesType == CandidatesType.ExistingWord);
    if (skipCharacters > 0 || showingExistingWordCandidates)
      setCandidates(null, CandidatesType.None);
    if (key != KeyboardLayout.DELETE && key != KeyboardLayout.SHIFT && key != KeyboardLayout.ALT)
      selectCandidate(0, true);
    candidateIsI = false;
    InputConnection ic = (inputMethod == null ? null : inputMethod.getCurrentInputConnection());
    if (key == KeyboardLayout.SHIFT)
    {
      if (longPress)
          shiftMode = KeyboardView.ModifierMode.EMOJI;
      else if (shiftMode == KeyboardView.ModifierMode.UP)
        shiftMode = KeyboardView.ModifierMode.DOWN;
      else if (shiftMode == KeyboardView.ModifierMode.DOWN)
        shiftMode = KeyboardView.ModifierMode.LOCKED;
      else if (shiftMode == KeyboardView.ModifierMode.EMOJI)
        shiftMode = KeyboardView.ModifierMode.EMOJI_LOCKED;
      else
        shiftMode = KeyboardView.ModifierMode.UP;
      keyboardView.setShiftMode(shiftMode);
      if (showingExistingWordCandidates)
        suggestReplacementsForExistingWord(); // Update the suggestions to reflect the new shift mode
    }
    else if (key == KeyboardLayout.ALT)
    {
      if (altMode == KeyboardView.ModifierMode.UP)
        altMode = KeyboardView.ModifierMode.DOWN;
      else if (altMode == KeyboardView.ModifierMode.DOWN)
        altMode = KeyboardView.ModifierMode.LOCKED;
      else
        altMode = KeyboardView.ModifierMode.UP;
      keyboardView.setAltMode(altMode);
    }
    else if (key == KeyboardLayout.DELETE)
    {
      if (inputMethod != null)
      {
        CharSequence prev = ic.getTextBeforeCursor(1, 0);
        if (candidates != null && ic != null)
        {
          ic.commitText("", 0);
          setCandidates(null, CandidatesType.None);
          candidatesView.setCandidates(null, false);
        }
        else
        {
          if (prev != null && prev.length() > 0 && prev.charAt(0) == ' ')
          {
            if (shiftMode == KeyboardView.ModifierMode.DOWN)
            {
              shiftMode = KeyboardView.ModifierMode.UP;
              keyboardView.setShiftMode(shiftMode);
            }
          }
          inputMethod.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        }
        showCompletionsFromPrefix(true);
      }
      if (longPress)
        isDeleting = true;
      shouldInsertSpace = false;
    }
    else if (key == KeyboardLayout.FORWARD_DELETE)
    {
      if (inputMethod != null)
      {
        inputMethod.sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL);
        setCandidates(null, CandidatesType.None);
        candidatesView.setCandidates(null, false);
      }
    }
    else if (key == KeyboardLayout.ENTER)
    {
      boolean result = inputMethod.sendDefaultEditorAction(true);
      if (!result && inputMethod != null)
        inputMethod.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
    }
    else if (key == '.' && longPress && !inputMethod.isSimpleModePermanent())
    {
      if (inputMethod != null)
      {
        inputMethod.setSimpleMode(!inputMethod.isSimpleMode());
        inputMethod.updateShiftMode();
        keyboardView.setOverlayWord(inputMethod.isSimpleMode() ? KeyboardView.NO_AUTO : KeyboardView.AUTO);
        keyboardView.redraw();
      }
    }
    else if (key == KeyboardLayout.VOICE)
    {
      if (!SpeechRecognizer.isRecognitionAvailable(inputMethod))
      {
        AlertDialog.Builder builder = new AlertDialog.Builder(inputMethod);
        builder.setTitle(R.string.voiceInput);
        builder.setMessage(R.string.voiceNotSupported);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        inputMethod.showDialog(builder.create());
      }
      final ProgressDialog dlg = new ProgressDialog(candidatesView.getContext());
      final SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(inputMethod);
      dlg.setIndeterminate(true);
      dlg.setTitle(R.string.voiceInput);
      dlg.setMessage(inputMethod.getResources().getString(R.string.recording));
      dlg.setOnCancelListener(new DialogInterface.OnCancelListener()
      {
        public void onCancel(DialogInterface dialogInterface)
        {
          recognizer.cancel();
        }
      });
      recognizer.setRecognitionListener(new RecognitionListener()
      {
        public void onReadyForSpeech(Bundle bundle)
        {
        }
        public void onBeginningOfSpeech()
        {
        }
        public void onRmsChanged(float v)
        {
        }
        public void onBufferReceived(byte[] bytes)
        {
        }
        public void onEndOfSpeech()
        {
          dlg.setMessage(inputMethod.getResources().getString(R.string.processing));
        }
        public void onError(int i)
        {
          dlg.dismiss();
          AlertDialog.Builder builder = new AlertDialog.Builder(inputMethod);
          builder.setTitle(R.string.voiceInput);
          builder.setIcon(android.R.drawable.ic_dialog_alert);
          if (i == SpeechRecognizer.ERROR_NETWORK)
            builder.setMessage(R.string.voiceNetworkError);
          else
            builder.setMessage(R.string.voiceError);
          inputMethod.showDialog(builder.create());
        }
        public void onResults(Bundle bundle)
        {
          dlg.dismiss();
          ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
          if (results.size() > 0)
          {
            setCandidates(new String[] {results.get(0)}, CandidatesType.Trace);
            if (shiftMode == KeyboardView.ModifierMode.DOWN)
            {
              char input[] = candidates[0].toCharArray();
              input[0] = Character.toUpperCase(input[0]);
              candidates[0] = new String(input);
            }
            else if (shiftMode == KeyboardView.ModifierMode.LOCKED)
              candidates[0] = candidates[0].toUpperCase();
            skipCharacters = 0;
            spaceBeforeCandidates = false;
            if (inputMethod != null)
            {
              spaceAfterCandidates = true;
              InputConnection ic = inputMethod.getCurrentInputConnection();
              if (ic != null)
              {
                CharSequence prev = ic.getTextBeforeCursor(1, 0);
                if (prev == null)
                  prev = "";
                if (prev.length() > 0 && !Character.isSpace(prev.charAt(0)))
                  spaceBeforeCandidates = true;
              }
              selectCandidate(0, false);
            }
          }
        }
        public void onPartialResults(Bundle bundle)
        {
        }
        public void onEvent(int i, Bundle bundle)
        {
        }
      });
      Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
      intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
      intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.enchantedcode.flow");
      inputMethod.showDialog(dlg);
      recognizer.startListening(intent);
    }
    else
    {
      if (ic != null)
      {
        spaceBeforeCandidates = false;
        boolean capitalizeNextI = false;
        CharSequence prev = ic.getTextBeforeCursor(1, 0);
        if (prev == null)
          prev = "";
        if (shouldInsertSpace && prev.length() > 0 && !Character.isSpace(prev.charAt(0)) && (Character.isLetter(key) || key == '('))
        {
          spaceBeforeCandidates = true;
          capitalizeNextI = true;
        }
        else if (prev.length() == 0 || Character.isSpace(prev.charAt(0)) || prev.charAt(0) == '(')
          capitalizeNextI = true;
        else if (Character.isLetter(key) && (prev.charAt(0) == '.' || prev.charAt(0) == ',' || prev.charAt(0) == '?' || prev.charAt(0) == '!') && !inputMethod.isSimpleMode())
        {
          spaceBeforeCandidates = true;
          capitalizeNextI = true;
        }
        if (inputMethod.isSimpleMode())
          capitalizeNextI = false;
        if (longPress)
        {
          ArrayList<String> alternates = new ArrayList<String>();
          alternates.add(new String(Character.toChars(key)));
          String alt[] = Flow.alternates.get(keyboardView.getKeyboard().keys[nearest]);
          if (alt != null)
            for (int i = 0; i < alt.length; i++)
              alternates.add(alt[i]);
          alt = Flow.alternates.get(keyboardView.getSecondaryKeyboard().keys[nearest]);
          if (alt != null)
            for (int i = 0; i < alt.length; i++)
              alternates.add(alt[i]);
          if (alternates.size() > 1)
          {
            setCandidates(alternates.toArray(new String[alternates.size()]), CandidatesType.LongPress);
            ensureCandidatesAreUnique();
            candidatesView.setCandidates(candidates, false);
            skipCharacters = 0;
            spaceAfterCandidates = false;
            isCancelableLongPress = true;
          }
        }
        else if (key == 'i' && capitalizeNextI)
        {
          setCandidates(new String[] {"I", "i"}, CandidatesType.Prefix);
          candidatesView.setCandidates(candidates, false);
          skipCharacters = 0;
          spaceAfterCandidates = true;
          candidateIsI = true;
        }
        else if (key == 'I' && capitalizeNextI)
        {
          setCandidates(new String[] {"I"}, CandidatesType.Prefix);
          skipCharacters = 0;
          spaceAfterCandidates = true;
          candidateIsI = true;
        }
        if (candidates == null)
        {
          if (spaceBeforeCandidates && !inputMethod.isSimpleMode())
            ic.commitText(" ", 1);
          ic.commitText(new String(Character.toChars(key)), 1);
          showCompletionsFromPrefix(false);
        }
        else
          selectCandidate(0, false);
        shouldInsertSpace = false;
      }
      updateModifiers();
    }
  }
  
  private String getPrefixBeforeCursor()
  {
    InputConnection ic = (inputMethod == null ? null : inputMethod.getCurrentInputConnection());
    if (ic == null)
      return null;
    int maxSearchDistance = 30;
    CharSequence prev = ic.getTextBeforeCursor(maxSearchDistance, 0);
    if (prev == null)
      return null;
    for (int i = prev.length()-1; i >= 0; i--)
    {
      char c = prev.charAt(i);
      if (!Character.isLetter(c) && c != '\'')
      {
        if (i == prev.length()-1)
          prev = "";
        else
          prev = prev.subSequence(i+1, prev.length());
        break;
      }
    }
    if (prev.length() == maxSearchDistance)
      return null;
    return prev.toString();
  }
  
  private String getSuffixAfterCursor()
  {
    InputConnection ic = (inputMethod == null ? null : inputMethod.getCurrentInputConnection());
    if (ic == null)
      return null;
    int maxSearchDistance = 30;
    CharSequence next = ic.getTextAfterCursor(maxSearchDistance, 0);
    if (next == null)
      return null;
    for (int i = 0; i < next.length(); i++)
    {
      char c = next.charAt(i);
      if (!Character.isLetter(c) && c != '\'')
      {
        if (i == 0)
          next = "";
        else
          next = next.subSequence(0, i);
        break;
      }
    }
    if (next.length() == maxSearchDistance)
      return null;
    return next.toString();
  }

  private void showCompletionsFromPrefix(boolean afterDelete)
  {
    updateAddWordButton();
    InputConnection ic = (inputMethod == null ? null : inputMethod.getCurrentInputConnection());
    if (ic == null)
      return;
    String prev = getPrefixBeforeCursor();
    if (prev != null && prev.length() > 0)
    {
      setCandidates(dictionary.findWordsStartingWith(prev), afterDelete ? CandidatesType.PrefixAfterDelete : CandidatesType.Prefix);
      ensureCandidatesAreUnique();
      if (!inputMethod.isPasswordMode())
        candidatesView.setCandidates(candidates, false);
      skipCharacters = prev.length();
    }
    else
    {
      setCandidates(null, CandidatesType.None);
      candidatesView.setCandidates(null, false);
      skipCharacters = 0;
    }
  }

  private void ensureCandidatesAreUnique()
  {
    if (candidates == null || candidates.length < 2 || candidates[0] == null)
      return;
    for (int i = 1; i < candidates.length; i++)
      if (candidates[i] != null)
      {
        for (int j = 0; j < i; j++)
          if (candidates[j].equals(candidates[i]))
          {
            // This candidate is identical to another one.

            for (int k = i; k < candidates.length-1; k++)
              candidates[k] = candidates[k+1];
            candidates[candidates.length-1] = null;
          }
      }
  }

  public void selectCandidate(int index, boolean confirm)
  {
    if (candidates == null)
      return;
    if (inputMethod != null && candidates[index] != null)
    {
      InputConnection ic = inputMethod.getCurrentInputConnection();
      if (ic != null)
      {
        String text = candidates[index];
        if (candidatesType == CandidatesType.ExistingWord)
        {
          ic.deleteSurroundingText(existingWordStartOffset, existingWordEndOffset);
          ic.commitText(text, 1);
        }
        else
        {
          if (skipCharacters > 0 && skipCharacters <= text.length())
            text = text.substring(skipCharacters);
          if (spaceBeforeCandidates && !inputMethod.isSimpleMode() && skipCharacters == 0)
            text = " "+text;
          if (selectionEndsWithSpace)
            text = text+" "; // Double-tap selects a word plus the following space.  We want to preserve that space.
          if (confirm && index == 0 && skipCharacters == 0)
            ic.finishComposingText();
          else if (confirm)
            ic.commitText(text, 1);
          else
            ic.setComposingText(text, 1);
        }
        shouldInsertSpace = spaceAfterCandidates;
      }
    }
    if (confirm)
    {
      setCandidates(null, CandidatesType.None);
      candidatesView.setCandidates(null, false);
    }
  }

  public void suggestReplacementsForExistingWord()
  {
    updateAddWordButton();
    shiftMode = keyboardView.getShiftMode();
    InputConnection ic = inputMethod.getCurrentInputConnection();
    if (ic == null || isDeleting)
      return;
    String prev = getPrefixBeforeCursor();
    String next = getSuffixAfterCursor();
    if (prev == null || next == null)
      return;
    String word = prev+next;
    if (word.length() == 0)
      return;
    String lowerCaseWord = word.toLowerCase();

    // Create a trace that represents the existing word.

    KeyboardLayout baseKeyboard = keyboardView.getBaseKeyboard();
    ArrayList<Integer> keyIndices = new ArrayList<Integer>();
    ArrayList<TracePoint> trace = new ArrayList<TracePoint>();
    Point keyPositions[] = keyboardView.getKeyPositions();
    int slideCharIndex[] = baseKeyboard.slideCharIndex;
    for (int i = 0; i < lowerCaseWord.length(); i++)
    {
      int c = lowerCaseWord.charAt(i);
      if (c != '\'' && !(c >= 'a' && c <= 'z'))
      {
        if (dictionary.replacements.containsKey(c))
          c = dictionary.replacements.get(c);
        else
          c = '\'';
      }
      int charIndex = (c == '\'' ? 26 : c-'a');
      int index = -1;
      for (int j = 0; j <  slideCharIndex.length && index == -1; j++)
        if (slideCharIndex[j] == charIndex)
          index = j;
      if (index != -1)
      {
        keyIndices.add(index);
        trace.add(new TracePoint(keyPositions[index].x, keyPositions[index].y));
      }
    }
    if (trace.size() == 0)
      return;

    // Add via keys to the trace.

    for (int i = 1; i < trace.size(); i++)
    {
      TracePoint point1 = trace.get(i-1);
      TracePoint point2 = trace.get(i);
      int index1 = keyIndices.get(i-1);
      int index2 = keyIndices.get(i);
      float dirx = point2.x-point1.x;
      float diry = point2.y-point1.y;
      float len = FloatMath.sqrt(dirx*dirx+diry*diry);
      dirx /= len;
      diry /= len;
      for (int j = 0; j < keyPositions.length; j++)
      {
        if (slideCharIndex[j] == index1 || slideCharIndex[j] == index2)
          continue;
        float dx = keyPositions[j].x-point1.x;
        float dy = keyPositions[j].y-point1.y;
        float parallelDist = dx*dirx + dy*diry;
        if (parallelDist < 0 || parallelDist > len)
          continue;
        float dxperp = dx - parallelDist*dirx;
        float dyperp = dy - parallelDist*diry;
        float perpDist = Math.max(0.0f, FloatMath.sqrt(dxperp*dxperp+dyperp*dyperp)-0.5f);
        if (perpDist < 0.5f)
          point2.addViaKey(baseKeyboard.keys[j], perpDist, (long) (parallelDist*1000));
      }
    }
    for (TracePoint point : trace)
    {
      Collections.sort(point.viaKeyList);
      findKeyDistances(baseKeyboard, point.keyDistances, point.x, point.y);
      point.finalizeViaKeys();
    }

    // Determine the list of candidates.

    setCandidates(dictionary.guessWord(trace.toArray(new TracePoint[trace.size()]), shiftMode, 6), CandidatesType.ExistingWord);
    int nextCandidate = 0;
    for (int i = 0; i < candidates.length; i++)
    {
      if (candidates[i] != null && !candidates[i].equals(word))
      {
        candidates[nextCandidate] = candidates[i];
        nextCandidate++;
      }
    }
    for (int i = nextCandidate; i < candidates.length; i++)
      candidates[i] = null;
    ensureCandidatesAreUnique();
    candidatesView.setCandidates(candidates, false);
    existingWordStartOffset = prev.length();
    existingWordEndOffset = next.length();
  }

  private void updateAddWordButton()
  {
    String prev = getPrefixBeforeCursor();
    String next = getSuffixAfterCursor();
    boolean shouldEnable = false;
    String word = null;
    if (prev != null && next != null && !inputMethod.isPasswordMode())
    {
      word = prev+next;
      if (word.length() > 1)
      {
        String lowerCaseWord = word.toLowerCase();
        shouldEnable = true;
        for (String candidate : dictionary.findWordsStartingWith(word))
          if (candidate != null && candidate.toLowerCase().equals(lowerCaseWord))
            shouldEnable = false;
      }
    }
    inputMethod.getAddWordButton().setWord(shouldEnable ? word : null);
  }
  
  private void updateModifiers()
  {
    if (shiftMode == KeyboardView.ModifierMode.DOWN)
      keyboardView.setShiftMode(KeyboardView.ModifierMode.UP);
    if (altMode == KeyboardView.ModifierMode.DOWN)
      keyboardView.setAltMode(KeyboardView.ModifierMode.UP);
  }
}
