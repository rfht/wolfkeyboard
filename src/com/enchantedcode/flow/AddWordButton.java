package com.enchantedcode.flow;

/**
 * Copyright 2015 by Peter Eastman
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
import android.provider.*;
import android.view.*;
import android.widget.*;

public class AddWordButton extends View
{
  private FlowInputMethod im;
  private Paint paint;
  private String word;

  public AddWordButton(FlowInputMethod context)
  {
    super(context);
    im = context;
    paint = new Paint();
    paint.setTextSize(getResources().getDisplayMetrics().density*paint.getTextSize()*1.5f);
    paint.setAntiAlias(true);
  }
  
  public void setWord(String word)
  {
    this.word = word;
    requestLayout();
    invalidate();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    if (event.getAction() != MotionEvent.ACTION_DOWN)
      return true;
    AlertDialog.Builder builder = new AlertDialog.Builder(im);
    String message = String.format(getResources().getString(R.string.confirmAddWord), word);
    builder.setTitle(R.string.addWord);
    builder.setMessage(message);
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
        if (word.equals(word.toLowerCase()))
          addWordToDictionary(word);
        else
          confirmCapitalization();
      }
    });
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
      }
    });
    im.showDialog(builder.create());
    return true;
  }
  
  private void confirmCapitalization()
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(im);
    String message = String.format(getResources().getString(R.string.confirmCapitalization), word);
    builder.setTitle(message);
    builder.setItems(new String[] {word, word.toLowerCase()}, new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
        addWordToDictionary(which == 0 ? word : word.toLowerCase());
      }
    });
    im.showDialog(builder.create());
  }
  
  private void addWordToDictionary(String wordToAdd)
  {
    UserDictionary.Words.addWord(im, wordToAdd, 250, UserDictionary.Words.LOCALE_TYPE_ALL);
    String message = String.format(getResources().getString(R.string.wordAdded), wordToAdd);
    Toast toast = Toast.makeText(im, message, Toast.LENGTH_SHORT);
    toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
    toast.show();
    setWord(null);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
    int textSize = (int) paint.getTextSize();
    int height = metrics.bottom-metrics.top+textSize/2;
    if (word == null)
      height = 0;
    setMeasuredDimension(resolveSize(height, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    int width = getWidth();
    int height = getHeight();
    float centerx = 0.5f*width;
    float centery = 0.5f*height;
    float density = getResources().getDisplayMetrics().density;
    paint.setShader(new LinearGradient(0, 0, 0, height, new int[]{Color.LTGRAY, Color.argb(255, 250, 250, 250), Color.argb(255, 255, 255, 255)}, new float[] {0, 0.5f, 1}, Shader.TileMode.CLAMP));
    paint.setStyle(Paint.Style.FILL);
    canvas.drawRect(0, 0, width, height, paint);
    paint.setShader(null);
    paint.setColor(Color.BLACK);
    canvas.drawCircle(centerx, centery, 0.4f*width, paint);
    paint.setColor(Color.WHITE);
    paint.setStrokeWidth(4*density);
    float offset = 0.2f*width;
    canvas.drawLine(centerx-offset, centery, centerx+offset, centery, paint);
    canvas.drawLine(centerx, centery-offset, centerx, centery+offset, paint);
  }
}
