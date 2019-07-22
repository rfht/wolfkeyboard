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
import android.preference.*;
import android.view.*;
import android.view.inputmethod.*;

public class ControlsPanel extends View
{
  private FlowInputMethod im;
  private int repeatKey;
  private RectF leftBounds, rightBounds, upBounds, downBounds, cutBounds, copyBounds, pasteBounds, settingsBounds, languageBounds;
  private final Handler handler;

  public ControlsPanel(FlowInputMethod context)
  {
    super(context);
    im = context;
    handler = new Handler();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    if (leftBounds == null)
      return true; // onDraw() hasn't been called yet
    float x = event.getX();
    float y = event.getY();
    if (event.getAction() == MotionEvent.ACTION_DOWN)
    {
      InputConnection ic = im.getCurrentInputConnection();
      repeatKey = -1;
      if (leftBounds.contains(x, y))
        repeatKey = KeyEvent.KEYCODE_DPAD_LEFT;
      else if (rightBounds.contains(x, y))
        repeatKey = KeyEvent.KEYCODE_DPAD_RIGHT;
      else if (upBounds.contains(x, y))
        repeatKey = KeyEvent.KEYCODE_DPAD_UP;
      else if (downBounds.contains(x, y))
        repeatKey = KeyEvent.KEYCODE_DPAD_DOWN;
      else if (languageBounds.contains(x, y))
      {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(im.getBaseContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(im);
        builder.setTitle(R.string.dictionary);
        final String allValues[] = im.getResources().getStringArray(R.array.dictionaryValues);
        final String currentValue = prefs.getString("dictionary", "american");
        int selectedIndex = 0;
        for (; selectedIndex < allValues.length && !currentValue.equals(allValues[selectedIndex]); selectedIndex++)
          ;
        builder.setSingleChoiceItems(R.array.dictionaryEntries, selectedIndex, new DialogInterface.OnClickListener()
        {
          public void onClick(DialogInterface dialog, int which)
          {
            dialog.dismiss();
            if (!currentValue.equals(allValues[which]))
            {
              prefs.edit().putString("dictionary", allValues[which]).commit();
              im.getControlsToggle().setSelected(false);
              im.getKeyboardView().invalidate();
              im.rebuildDictionary();
            }
          }
        });
        Dialog dialog = builder.create();
        showDialog(dialog);
      }
      else if (settingsBounds.contains(x, y))
        im.startActivity(new Intent(im, SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      else if (cutBounds.contains(x, y))
        ic.performContextMenuAction(android.R.id.cut);
      else if (copyBounds.contains(x, y))
        ic.performContextMenuAction(android.R.id.copy);
      else if (pasteBounds.contains(x, y))
        ic.performContextMenuAction(android.R.id.paste);
      if (repeatKey != -1) {
        if (repeatKey == KeyEvent.KEYCODE_DPAD_LEFT || repeatKey == KeyEvent.KEYCODE_DPAD_RIGHT || repeatKey == KeyEvent.KEYCODE_DPAD_UP || repeatKey == KeyEvent.KEYCODE_DPAD_DOWN)
          im.setPressedArrowKey();
        im.sendDownUpKeyEvents(repeatKey);
        handler.postDelayed(new Runnable() {
          public void run()
          {
            if (repeatKey != -1)
            {
              if (repeatKey == KeyEvent.KEYCODE_DPAD_LEFT || repeatKey == KeyEvent.KEYCODE_DPAD_RIGHT || repeatKey == KeyEvent.KEYCODE_DPAD_UP || repeatKey == KeyEvent.KEYCODE_DPAD_DOWN)
                im.setPressedArrowKey();
              im.sendDownUpKeyEvents(repeatKey);
              handler.postDelayed(this, im.getTouchListener().getBackspaceDelay());
            }
          }
        }, im.getTouchListener().getLongPressDelay());
      }
    }
    else if (event.getAction() == MotionEvent.ACTION_UP)
      repeatKey = -1;
    return true;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    // This is copied from KeyboardView to make this panel the same size as that one.

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    WindowManager wm = (WindowManager) im.getSystemService(Context.WINDOW_SERVICE);
    int width = getMeasuredWidth();
    int height = getMeasuredHeight();
    if (im.isFullscreenMode())
      height = Math.min(height, (int) (0.65f*wm.getDefaultDisplay().getHeight()));
    height = KeyboardView.computeHeight(width, height);
    SharedPreferences preferences = getContext().getSharedPreferences("Flow", Context.MODE_PRIVATE);
    height = Math.min(height, preferences.getInt("keyboardSize", height));
    setMeasuredDimension(width, height);
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    Paint paint = new Paint();
    int width = getWidth();
    int spacing = im.getKeyboardView().getKeySpacing();
    float gap = 0.07f*spacing;
    float cornerRadius = 0.2f*spacing;
    float textSize = 0.5f*spacing;
    float minx = width/2-3.5f*spacing;
    float maxx = width/2+3.5f*spacing;
    float bottom = 3*spacing+gap;
    paint.setColor(Color.WHITE);
    canvas.drawRect(0, 0, width, bottom-cornerRadius, paint);
    canvas.drawRoundRect(new RectF(0, 0, width, bottom), cornerRadius, cornerRadius, paint);
    paint.setAntiAlias(true);
    paint.setTextSize(textSize);
    paint.setTextAlign(Paint.Align.CENTER);
    float buttonWidth = (maxx-minx)/4;
    leftBounds = new RectF(minx+gap, gap, minx+buttonWidth-gap, spacing-gap);
    rightBounds = new RectF(minx+buttonWidth+gap, gap, minx+2*buttonWidth-gap, spacing-gap);
    upBounds = new RectF(minx+2*buttonWidth+gap, gap, minx+3*buttonWidth-gap, spacing-gap);
    downBounds = new RectF(minx+3*buttonWidth+gap, gap, maxx-gap, spacing-gap);
    buttonWidth = (maxx-minx)/3;
    cutBounds = new RectF(minx+gap, spacing+gap, minx+buttonWidth-gap, 2*spacing-gap);
    copyBounds = new RectF(minx+buttonWidth+gap, spacing+gap, minx+2*buttonWidth-gap, 2*spacing-gap);
    pasteBounds = new RectF(minx+2*buttonWidth+gap, spacing+gap, maxx-gap, 2*spacing-gap);
    buttonWidth = (maxx-minx)/2;
    languageBounds = new RectF(minx+gap, 2*spacing+gap, minx+buttonWidth-gap, 3*spacing-gap);
    settingsBounds = new RectF(minx+buttonWidth+gap, 2*spacing+gap, minx+2*buttonWidth-gap, 3*spacing-gap);
    paint.setShader(new LinearGradient(0, gap, 0, spacing-gap, new int[]{Color.rgb(210, 220, 210), Color.rgb(220, 230, 220), Color.rgb(192, 202, 192)}, new float[] {0, 0.4f, 1}, Shader.TileMode.CLAMP));
    canvas.drawRoundRect(leftBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(rightBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(upBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(downBounds, cornerRadius, cornerRadius, paint);
    paint.setShader(new LinearGradient(0, spacing+gap, 0, 2*spacing-gap, new int[]{Color.rgb(220, 220, 210), Color.rgb(230, 230, 220), Color.rgb(202, 202, 192)}, new float[]{0, 0.4f, 1}, Shader.TileMode.CLAMP));
    canvas.drawRoundRect(cutBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(copyBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(pasteBounds, cornerRadius, cornerRadius, paint);
    paint.setShader(new LinearGradient(0, 2*spacing+gap, 0, 3*spacing-gap, new int[]{Color.rgb(210, 210, 220), Color.rgb(220, 220, 230), Color.rgb(192, 192, 202)}, new float[]{0, 0.4f, 1}, Shader.TileMode.CLAMP));
    canvas.drawRoundRect(languageBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(settingsBounds, cornerRadius, cornerRadius, paint);
    paint.setShader(null);
    paint.setColor(Color.BLACK);
    paint.setStyle(Paint.Style.STROKE);
    canvas.drawRoundRect(leftBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(rightBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(upBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(downBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(cutBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(copyBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(pasteBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(languageBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(settingsBounds, cornerRadius, cornerRadius, paint);
    paint.setStyle(Paint.Style.FILL_AND_STROKE);
    canvas.drawText("\u2190", leftBounds.left+0.5f*(leftBounds.right-leftBounds.left), leftBounds.top+0.6f*spacing, paint);
    canvas.drawText("\u2192", rightBounds.left+0.5f*(rightBounds.right-rightBounds.left), rightBounds.top+0.6f*spacing, paint);
    canvas.drawText("\u2191", upBounds.left+0.5f*(upBounds.right-upBounds.left), upBounds.top+0.6f*spacing, paint);
    canvas.drawText("\u2193", downBounds.left+0.5f*(downBounds.right-downBounds.left), downBounds.top+0.6f*spacing, paint);
    paint.setTextSize(0.4f*spacing);
    canvas.drawText(im.getResources().getString(R.string.cut), cutBounds.left+0.5f*(cutBounds.right-cutBounds.left), cutBounds.top+0.6f*spacing, paint);
    canvas.drawText(im.getResources().getString(R.string.copy), copyBounds.left+0.5f*(copyBounds.right-copyBounds.left), copyBounds.top+0.6f*spacing, paint);
    canvas.drawText(im.getResources().getString(R.string.paste), pasteBounds.left+0.5f*(pasteBounds.right-pasteBounds.left), pasteBounds.top+0.6f*spacing, paint);
    canvas.drawText(im.getResources().getString(R.string.settings), settingsBounds.left+0.5f*(settingsBounds.right-settingsBounds.left), settingsBounds.top+0.6f*spacing, paint);
    canvas.drawText(im.getResources().getString(R.string.language), languageBounds.left+0.5f*(languageBounds.right-languageBounds.left), languageBounds.top+0.6f*spacing, paint);
    canvas.drawLine(cornerRadius, bottom+1, width-cornerRadius, bottom+1, paint);
  }

  private void showDialog(Dialog dlg)
  {
    Window window = dlg.getWindow();
    WindowManager.LayoutParams lp = window.getAttributes();
    lp.token = getWindowToken();
    lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
    window.setAttributes(lp);
    window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    dlg.show();
  }
}
