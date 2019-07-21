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

public class ControlsToggle extends View
{
  private FlowInputMethod im;
  private Paint paint;
  private boolean selected;

  public ControlsToggle(FlowInputMethod context)
  {
    super(context);
    im = context;
    paint = new Paint();
    paint.setTextSize(getResources().getDisplayMetrics().density*paint.getTextSize()*1.5f);
    paint.setAntiAlias(true);
  }

  public boolean getSelected()
  {
    return selected;
  }

  public void setSelected(boolean select)
  {
    selected = select;
    im.getControlsPanel().setVisibility(selected ? VISIBLE : INVISIBLE);
    invalidate();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    if (event.getAction() == MotionEvent.ACTION_DOWN)
      setSelected(!selected);
    return true;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
    int textSize = (int) paint.getTextSize();
    int height = metrics.bottom-metrics.top+textSize/2;
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
    paint.setColor(Color.WHITE);
    canvas.drawCircle(centerx, centery, 0.5f*width, paint);
    paint.setColor(Color.BLACK);
    paint.setStrokeWidth(2*density);
    float dx = 0.2f*width;
    float dy = (selected ? -0.2f : 0.2f)*width;
    canvas.drawLine(centerx, centery, centerx-dx, centery-dy, paint);
    canvas.drawLine(centerx, centery, centerx+dx, centery-dy, paint);
    canvas.drawLine(centerx, centery+dy, centerx-dx, centery, paint);
    canvas.drawLine(centerx, centery+dy, centerx+dx, centery, paint);
    paint.setColor(Color.LTGRAY);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(density);
    canvas.drawCircle(centerx, centery, 0.5f*width, paint);
  }
}
