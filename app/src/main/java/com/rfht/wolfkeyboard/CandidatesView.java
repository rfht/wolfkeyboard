package com.enchantedcode.flow;

/**
 * Copyright 2011-2013 by Peter Eastman
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

import android.graphics.*;
import android.view.*;

public class CandidatesView extends View
{
  private FlowInputMethod im;
  private Paint paint;
  private String candidates[];
  private int dividers[];
  private int touchx, touchy;

  public CandidatesView(FlowInputMethod context)
  {
    super(context);
    im = context;
    paint = new Paint();
    paint.setTextSize(getResources().getDisplayMetrics().density*paint.getTextSize()*1.5f);
    paint.setAntiAlias(true);
    setOnClickListener(new ClickHandler());
  }

  public void setCandidates(String candidates[], boolean showOverlay)
  {
    this.candidates = candidates;
    requestLayout();
    invalidate();
    if (showOverlay && candidates != null && candidates[0] != null && candidates[0].length() > 1)
      im.getKeyboardView().setOverlayWord(candidates[0]);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    int width = 0;
    Rect bounds = new Rect();
    Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
    int textSize = (int) paint.getTextSize();
    if (candidates != null)
    {
      for (String s : candidates)
      {
        if (s != null)
        {
          paint.getTextBounds(s, 0, s.length(), bounds);
          width += bounds.right-bounds.left;
        }
      }
      width += 2*textSize*candidates.length;
    }
    width = Math.max(width, ((ViewGroup) getParent()).getMeasuredWidth());
    setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(metrics.bottom-metrics.top+textSize/2, heightMeasureSpec));
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    int width = getWidth();
    int height = getHeight();
    paint.setShader(new LinearGradient(0, 0, 0, height, new int[]{Color.LTGRAY, Color.argb(255, 250, 250, 250), Color.argb(255, 255, 255, 255)}, new float[] {0, 0.5f, 1}, Shader.TileMode.CLAMP));
    canvas.drawRect(0, 0, width, height, paint);
    paint.setShader(null);
    paint.setColor(Color.BLACK);
    if (candidates != null)
    {
      dividers = new int[candidates.length];
      Rect bounds = new Rect();
      Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
      int textSize = (int) paint.getTextSize();
      int start = textSize;
      for (int i = 0; i < candidates.length; i++)
      {
        String s = candidates[i];
        if (s != null)
        {
          canvas.drawText(s, start, -metrics.top+textSize/4, paint);
          paint.getTextBounds(s, 0, s.length(), bounds);
          start += bounds.right-bounds.left+textSize;
        }
        dividers[i] = start;
        start += textSize;
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    touchx = (int) event.getX();
    touchy = (int) event.getY();
    return super.onTouchEvent(event);
  }

  private class ClickHandler implements OnClickListener
  {
    public void onClick(View view)
    {
      if (dividers == null || candidates == null || im.getTouchListener() == null)
        return;
      int index;
      for (index = 0; index < dividers.length && touchx > dividers[index]; index++)
        ;
      if (index < dividers.length && candidates[index] != null)
        im.getTouchListener().selectCandidate(index, true);
    }
  }
}
