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

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.util.*;
import android.view.*;

import java.util.*;

public class KeyboardView extends View
{
  private boolean needToCreateBackground;
  private final FlowInputMethod inputMethod;
  private final KeyboardLayout baseKeyboard, shiftKeyboard, altKeyboard, altShiftKeyboard, emojiKeyboard;
  private KeyboardLayout currentKeyboard, secondaryKeyboard;
  private Point keyPositions[];
  private int spacing, lastWidth, lastHeight, lastPosition;
  private ArrayList<Point> markers;
  private List<Point> trace;
  private ModifierMode shiftMode, altMode;
  private Path spacePath, enterPath, deletePath, forwardDeletePath, shiftPath, voicePath1, voicePath2, autoPath;
  private Bitmap overlay;
  private long overlayStartTime;
  private final Handler handler;
  private boolean showOverlay;
  private long overlayTime;
  private static final int vowelColor = Color.rgb(190, 255, 190);
  private static final int consonantColor = Color.rgb(190, 210, 255);
  private static final int punctuationColor = Color.rgb(200, 200, 200);
  private static final int numberColor = Color.rgb(255, 255, 255);
  private static final int controlColor = Color.rgb(255, 255, 190);
  private static final int emojiColor = Color.rgb(255, 255, 255);

  public enum ModifierMode {UP, DOWN, LOCKED, EMOJI, EMOJI_LOCKED}
  public static final String AUTO = "AUTO";
  public static final String NO_AUTO = "NO_AUTO";

  public KeyboardView(Context context, KeyboardLayout baseKeyboard, KeyboardLayout shiftKeyboard, KeyboardLayout altKeyboard, KeyboardLayout altShiftKeyboard, KeyboardLayout emojiKeyboard)
  {
    super(context);
    inputMethod = (context instanceof FlowInputMethod ? (FlowInputMethod) context : null);
    this.baseKeyboard = baseKeyboard;
    this.shiftKeyboard = shiftKeyboard;
    this.altKeyboard = altKeyboard;
    this.altShiftKeyboard = altShiftKeyboard;
    this.emojiKeyboard = emojiKeyboard;
    currentKeyboard = baseKeyboard;
    secondaryKeyboard = altKeyboard;
    keyPositions = new Point[baseKeyboard.keys.length];
    needToCreateBackground = true;
    shiftMode = ModifierMode.UP;
    altMode = ModifierMode.UP;
    handler = new Handler();
  }

  public KeyboardLayout getKeyboard()
  {
    return currentKeyboard;
  }
  
  public KeyboardLayout getBaseKeyboard()
  {
    return baseKeyboard;
  }

  public KeyboardLayout getSecondaryKeyboard()
  {
    return secondaryKeyboard;
  }

  public Point[] getKeyPositions()
  {
    return keyPositions;
  }

  public int getKeySpacing()
  {
    return spacing;
  }

  public ModifierMode getShiftMode()
  {
    return shiftMode;
  }

  public void setShiftMode(ModifierMode shiftMode)
  {
    if (this.shiftMode != shiftMode)
    {
      this.shiftMode = shiftMode;
      selectKeyboard();
    }
  }

  public ModifierMode getAltMode()
  {
    return altMode;
  }

  public void setAltMode(ModifierMode altMode)
  {
    if (this.altMode != altMode)
    {
      this.altMode = altMode;
      selectKeyboard();
    }
  }

  private void selectKeyboard()
  {
    boolean shift = (shiftMode != ModifierMode.UP);
    boolean alt = (altMode != ModifierMode.UP);
    if (shiftMode == ModifierMode.EMOJI || shiftMode == ModifierMode.EMOJI_LOCKED)
    {
      currentKeyboard = emojiKeyboard;
      secondaryKeyboard = emojiKeyboard;
    }
    else if (shift && alt)
    {
      currentKeyboard = altShiftKeyboard;
      secondaryKeyboard = shiftKeyboard;
    }
    else if (alt)
    {
      currentKeyboard = altKeyboard;
      secondaryKeyboard = baseKeyboard;
    }
    else if (shift)
    {
      currentKeyboard = shiftKeyboard;
      secondaryKeyboard = altShiftKeyboard;
    }
    else
    {
      currentKeyboard = baseKeyboard;
      secondaryKeyboard = altKeyboard;
    }
    redraw();
  }

  public void setShowOverlay(boolean showOverlay)
  {
    this.showOverlay = showOverlay;
  }

  public void setOverlayTime(long overlayTime)
  {
    this.overlayTime = overlayTime;
  }

  public void redraw()
  {
    needToCreateBackground = true;
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    int width = getMeasuredWidth();
    int height = getMeasuredHeight();
    if (inputMethod != null && inputMethod.isFullscreenMode())
      height = Math.min(height, (int) (0.65f*wm.getDefaultDisplay().getHeight()));
    height = computeHeight(width, height);
    SharedPreferences preferences = getContext().getSharedPreferences("Flow", Context.MODE_PRIVATE);
    height = Math.min(height, preferences.getInt("keyboardSize", height));
    int position = Math.min(height, preferences.getInt("keyboardPosition", 1));
    setMeasuredDimension(width, height);
    if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0 && (needToCreateBackground || width != lastWidth || height != lastHeight || position != lastPosition))
      createBackground(getMeasuredWidth(), getMeasuredHeight());
  }

  public static int computeHeight(int availableWidth, int availableHeight)
  {
    int spacing, offset;
    if (availableWidth/7 < availableHeight/5)
    {
      spacing = availableWidth/7;
      offset = (availableWidth-6*spacing)/2;
    }
    else
    {
      spacing = availableHeight/5;
      offset = (availableHeight-4*spacing)/2;
    }
    return (int) Math.min(availableHeight, 5.2f*spacing+offset/8);
  }

  public void createBackground(int width, int height)
  {
    spacing = (int) Math.min(width/7, height/5.3f);
    float density = getResources().getDisplayMetrics().density;
    int altColor = Color.rgb(80, 80, 80);
    createPaths();
    Path path = new Path();
    SharedPreferences preferences = getContext().getSharedPreferences("Flow", Context.MODE_PRIVATE);
    int keyboardPosition = Math.min(height, preferences.getInt("keyboardPosition", 1));
    int xoffset;
    if (keyboardPosition == 0)
      xoffset = spacing/2;
    else if (keyboardPosition == 1)
      xoffset = (width-6*spacing)/2;
    else
      xoffset = (int) (width-6.5*spacing);
    int yoffset = (int) (height-4.3f*spacing)/2;
    float radius = 0.47f*spacing;
    Bitmap background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(background);
    Paint backgroundPaint = new Paint();
    backgroundPaint.setColor(Color.argb(180, 0, 0, 0));
    canvas.drawRect(0, 0, width, height, backgroundPaint);
    Paint keyPaint = new Paint();
    keyPaint.setAntiAlias(true);
    keyPaint.setStrokeWidth(2*density);
    Paint textPaint = new Paint();
    textPaint.setAntiAlias(true);
    textPaint.setTextAlign(Paint.Align.CENTER);
    Paint pathPaint = new Paint();
    pathPaint.setAntiAlias(true);
    pathPaint.setStyle(Paint.Style.STROKE);
    pathPaint.setStrokeWidth(2*density);
    float textSize = 0.5f*spacing;
    for (int i = 0; i < 5; i++)
      for (int j = 0; j < 7; j++)
      {
        int index = i*7+j;
        int x = j*spacing+xoffset;
        int y = height-((4-i)*spacing+yoffset);
        keyPositions[index] = new Point(x, y);
        KeyboardLayout.KeyType type = currentKeyboard.keyType[index];
        int key = currentKeyboard.keys[index];
        if (type == KeyboardLayout.KeyType.CONTROL)
          keyPaint.setColor(controlColor);
        else if (type == KeyboardLayout.KeyType.NUMBER)
          keyPaint.setColor(numberColor);
        else if (type == KeyboardLayout.KeyType.VOWEL)
          keyPaint.setColor(vowelColor);
        else if (type == KeyboardLayout.KeyType.CONSONANT)
          keyPaint.setColor(consonantColor);
        else if (type == KeyboardLayout.KeyType.EMOJI)
          keyPaint.setColor(emojiColor);
        else
          keyPaint.setColor(punctuationColor);
        canvas.drawCircle(x, y, radius, keyPaint);
        if (key == ' ')
        {
          spacePath.offset(x, y+0.5f*textSize, path);
          canvas.drawPath(path, pathPaint);
        }
        else if (key == KeyboardLayout.ENTER)
        {
          enterPath.offset(x, y+0.4f*textSize, path);
          canvas.drawPath(path, pathPaint);
        }
        else if (key == KeyboardLayout.DELETE)
        {
          deletePath.offset(x, y+0.35f*textSize, path);
          canvas.drawPath(path, pathPaint);
        }
        else if (key == KeyboardLayout.FORWARD_DELETE)
        {
          forwardDeletePath.offset(x, y+0.35f*textSize, path);
          canvas.drawPath(path, pathPaint);
        }
        else if (key == KeyboardLayout.SHIFT)
        {
          shiftPath.offset(x, y+0.4f*textSize, path);
          if (shiftMode == ModifierMode.UP)
            canvas.drawPath(path, pathPaint);
          else if (shiftMode == ModifierMode.DOWN || shiftMode == ModifierMode.EMOJI)
          {
            pathPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawPath(path, pathPaint);
            pathPaint.setStyle(Paint.Style.STROKE);
          }
          else
          {
            pathPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            pathPaint.setColor(Color.BLUE);
            canvas.drawPath(path, pathPaint);
            pathPaint.setColor(Color.BLACK);
            pathPaint.setStyle(Paint.Style.STROKE);
          }
        }
        else if (key == KeyboardLayout.ALT)
        {
          textPaint.setTextSize(0.7f*textSize);
          String label = (altMode == ModifierMode.UP ? "123" : "ABC");
          int color = (altMode == ModifierMode.LOCKED ? Color.BLUE : Color.BLACK);
          textPaint.setColor(color);
          canvas.drawText(label, x, y+0.35f*textSize, textPaint);
          textPaint.setColor(Color.BLACK);
        }
        else if (key == KeyboardLayout.VOICE)
        {
          voicePath1.offset(x-0.1f, y+0.5f*textSize, path);
          pathPaint.setStyle(Paint.Style.FILL);
          canvas.drawPath(path, pathPaint);
          pathPaint.setStyle(Paint.Style.STROKE);
          voicePath2.offset(x-0.1f, y+0.5f*textSize, path);
          canvas.drawPath(path, pathPaint);
        }
        else if (key > 32)
        {
          textPaint.setColor(Color.BLACK);
          textPaint.setTextSize(textSize);
          if (type == KeyboardLayout.KeyType.EMOJI)
            canvas.drawText(new String(Character.toChars(key)), x, y+0.35f*textSize, textPaint);
          else
            canvas.drawText(new String(Character.toChars(key)), x-0.1f*textSize, y+0.5f*textSize, textPaint);
        }
        int altkey = secondaryKeyboard.keys[index];
        if (altkey != key || key == '.')
        {
          if (altkey == ' ')
          {
            Matrix m = new Matrix();
            m.postScale(0.55f, 0.55f);
            m.postTranslate(x+0.5f*textSize, y-0.05f*textSize);
            spacePath.transform(m, path);
            pathPaint.setStrokeWidth(1.5f*density);
            pathPaint.setColor(altColor);
            canvas.drawPath(path, pathPaint);
            pathPaint.setStrokeWidth(2*density);
            pathPaint.setColor(Color.BLACK);
          }
          else if (altkey == KeyboardLayout.VOICE)
          {
            Matrix m = new Matrix();
            m.postScale(0.55f, 0.55f);
            m.postTranslate(x+0.5f*textSize, y-0.05f*textSize);
            voicePath1.transform(m, path);
            pathPaint.setStyle(Paint.Style.FILL);
            pathPaint.setColor(altColor);
            canvas.drawPath(path, pathPaint);
            voicePath2.transform(m, path);
            pathPaint.setStyle(Paint.Style.STROKE);
            pathPaint.setStrokeWidth(1.5f*density);
            canvas.drawPath(path, pathPaint);
            pathPaint.setStrokeWidth(2*density);
            pathPaint.setColor(Color.BLACK);
          }
          else if (altkey == KeyboardLayout.FORWARD_DELETE)
          {
            Matrix m = new Matrix();
            m.postScale(0.55f, 0.55f);
            m.postTranslate(x+0.5f*textSize, y-0.15f*textSize);
            forwardDeletePath.transform(m, path);
            pathPaint.setStrokeWidth(1.0f*density);
            pathPaint.setColor(altColor);
            canvas.drawPath(path, pathPaint);
            pathPaint.setStrokeWidth(2*density);
            pathPaint.setColor(Color.BLACK);
          }
          else if (altkey == '.')
          {
            if (inputMethod != null && !inputMethod.isSimpleModePermanent())
            {
              if (inputMethod.isSimpleMode())
              {
                Matrix m = new Matrix();
                m.postScale(0.45f, 0.45f);
                m.postTranslate(x+0.4f*textSize, y-0.2f*textSize);
                autoPath.transform(m, path);
                pathPaint.setStrokeWidth(1.5f*density);
                pathPaint.setColor(Color.rgb(115, 0, 0));
                canvas.drawPath(path, pathPaint);
                pathPaint.setStrokeWidth(2*density);
                pathPaint.setColor(Color.BLACK);
              }
              else
              {
                textPaint.setTextSize(0.35f*textSize);
                textPaint.setColor(Color.rgb(115, 0, 0));
                Typeface t = textPaint.getTypeface();
                textPaint.setTypeface(Typeface.create(t, Typeface.BOLD));
                canvas.drawText("AUTO", x+0.25f*textSize, y-0.05f*textSize, textPaint);
                textPaint.setTypeface(t);
              }
            }
          }
          else if (altkey > 32)
          {
            textPaint.setTextSize(0.55f*textSize);
            textPaint.setColor(altColor);
            canvas.drawText(new String(Character.toChars(altkey)), x+0.5f*textSize, y-0.1f*textSize, textPaint);
          }
        }
      }
    setBackgroundDrawable(new BitmapDrawable(background));
    needToCreateBackground = false;
    lastWidth = width;
    lastHeight = height;
    lastPosition = keyboardPosition;
  }

  public void setMarkers(ArrayList<Point> markers)
  {
    this.markers = markers;
  }

  public void setTrace(List<Point> trace)
  {
    this.trace = trace;
    invalidate();
  }

  public void setOverlayWord(String word)
  {
    if (!showOverlay)
      return;
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTextSize(spacing);
    paint.setColor(Color.BLACK);
    Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
    Rect bounds = new Rect();
    paint.getTextBounds(word, 0, word.length(), bounds);
    int textWidth = bounds.right-bounds.left+spacing;
    int textHeight = bounds.bottom-bounds.top+spacing;
    if (word == NO_AUTO)
      textWidth = (int) (1.5f*textHeight);
    bounds.top = 0;
    bounds.bottom = textHeight;
    bounds.left = 0;
    bounds.right = textWidth;
    overlay = Bitmap.createBitmap(textWidth, textHeight, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(overlay);
    canvas.drawRoundRect(new RectF(bounds), 0.5f*spacing, 0.5f*spacing, paint);
    paint.setColor(Color.WHITE);
    if (word == NO_AUTO)
    {
      Matrix m = new Matrix();
      m.postScale(2, 2);
      m.postTranslate(overlay.getWidth()/2, overlay.getHeight()/2);
      Path path = new Path();
      autoPath.transform(m, path);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(0.1f*spacing);
      canvas.drawPath(path, paint);
    }
    else
      canvas.drawText(word, overlay.getWidth()/2, (spacing-metrics.top+metrics.bottom)/2, paint);
    overlayStartTime = System.currentTimeMillis();
    handler.post(new Runnable()
    {
      public void run()
      {
        invalidate();
        if (System.currentTimeMillis()-overlayStartTime < overlayTime)
            handler.postDelayed(this, 40);
      }
    });
    invalidate();
  }

  private void createPaths()
  {
    spacePath = new Path();
    spacePath.moveTo(-spacing/5, -spacing/6);
    spacePath.lineTo(-spacing/5, 0);
    spacePath.lineTo(spacing/5, 0);
    spacePath.lineTo(spacing/5, -spacing/6);

    enterPath = new Path();
    enterPath.moveTo(-spacing/6, -spacing/3);
    enterPath.lineTo(spacing/6, -spacing/3);
    enterPath.cubicTo(spacing/3, -spacing/3, spacing/3, 0, spacing/6, 0);
    enterPath.lineTo(-spacing/6, 0);
    enterPath.moveTo(-spacing/6+spacing/8, -spacing/8);
    enterPath.lineTo(-spacing/6, 0);
    enterPath.lineTo(-spacing/6+spacing/8, spacing/8);

    deletePath = new Path();
    float deleteHeight = spacing/3.0f;
    float deleteWidth = spacing/6.0f;
    deletePath.moveTo(-deleteWidth, -deleteHeight);
    deletePath.lineTo(1.8f*deleteWidth, -deleteHeight);
    deletePath.lineTo(1.8f*deleteWidth, 0);
    deletePath.lineTo(-deleteWidth, 0);
    deletePath.lineTo(-2*deleteWidth, -deleteHeight/2);
    deletePath.close();
    deletePath.moveTo(-0.25f*deleteWidth, -0.7f*deleteHeight);
    deletePath.lineTo(0.75f*deleteWidth, -0.3f*deleteHeight);
    deletePath.moveTo(0.75f*deleteWidth, -0.7f*deleteHeight);
    deletePath.lineTo(-0.25f*deleteWidth, -0.3f*deleteHeight);
    
    forwardDeletePath = new Path(deletePath);
    Matrix m = new Matrix();
    m.setTranslate(0, deleteHeight/2);
    m.postRotate(180);
    m.postScale(0.6f, 0.8f);
    m.postTranslate(0, -0.4f*deleteHeight);
    forwardDeletePath.transform(m);
    
    shiftPath = new Path();
    float shiftTop = -spacing*0.35f;
    float shiftOuterWidth = spacing/5.0f;
    float shiftInnerWidth = spacing/8.0f;
    float shiftMiddle = shiftTop+shiftOuterWidth;
    shiftPath.moveTo(0,  shiftTop);
    shiftPath.lineTo(shiftOuterWidth, shiftMiddle);
    shiftPath.lineTo(shiftInnerWidth, shiftMiddle);
    shiftPath.lineTo(shiftInnerWidth, 0);
    shiftPath.lineTo(-shiftInnerWidth, 0);
    shiftPath.lineTo(-shiftInnerWidth, shiftMiddle);
    shiftPath.lineTo(-shiftOuterWidth, shiftMiddle);
    shiftPath.close();

    voicePath1 = new Path();
    float voiceTop = spacing*0.4f;
    voicePath1.addRoundRect(new RectF(-0.15f*voiceTop, -1.1f*voiceTop, 0.15f*voiceTop, -0.5f*voiceTop), 0.15f*voiceTop, 0.15f*voiceTop, Path.Direction.CW);
    voicePath2 = new Path();
    voicePath2.moveTo(-0.15f*voiceTop, 0);
    voicePath2.lineTo(0.15f*voiceTop, 0);
    voicePath2.moveTo(0, 0);
    voicePath2.lineTo(0, -0.2f*voiceTop);
    voicePath2.moveTo(-0.3f*voiceTop, -0.6f*voiceTop);
    voicePath2.cubicTo(-0.25f*voiceTop, -0.2f*voiceTop, 0.25f*voiceTop, -0.2f*voiceTop, 0.3f*voiceTop, -0.6f*voiceTop);

    autoPath = new Path();
    float radius = 0.25f*spacing;
    float rSqrt2 = radius/FloatMath.sqrt(2);
    autoPath.addCircle(0, 0, radius, Path.Direction.CW);
    autoPath.moveTo(-rSqrt2, -rSqrt2);
    autoPath.lineTo(rSqrt2, rSqrt2);
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    if (getMeasuredWidth() == 0 || getMeasuredHeight() == 0)
      return;
    if (needToCreateBackground)
      createBackground(getMeasuredWidth(), getMeasuredHeight());
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    if (trace != null)
    {
      paint.setColor(Color.argb(192, 255, 255, 255));
      paint.setStrokeWidth(3*getResources().getDisplayMetrics().density);
      Point prev = null;
      for (Point p : trace)
      {
        if (prev != null)
          canvas.drawLine(prev.x, prev.y, p.x, p.y, paint);
        prev = p;
      }
    }
    if (markers != null)
    {
      float hsv[] = new float[] {0.0f, 1.0f, 1.0f};
      paint.setStrokeWidth(3*getResources().getDisplayMetrics().density);
      for (int i = 0; i < markers.size()-1; i++)
      {
        Point p = markers.get(i);
        hsv[0] = (i*180.0f)/markers.size();
        paint.setColor(Color.HSVToColor(hsv));
        canvas.drawLine(p.x, p.y, markers.get(i+1).x, markers.get(i+1).y, paint);
      }
    }
    if (overlay != null)
    {
      long time = System.currentTimeMillis()-overlayStartTime;
      if (time < overlayTime)
      {
        Rect bounds = new Rect((getWidth()-overlay.getWidth())/2, (getHeight()-overlay.getHeight())/2, (getWidth()+overlay.getWidth())/2, (getHeight()+overlay.getHeight())/2);
        paint.setColor(Color.argb((int) (200*(overlayTime-time)/(float) overlayTime), 255, 255, 255));
        canvas.drawBitmap(overlay, null, bounds, paint);
      }
    }
  }
}
