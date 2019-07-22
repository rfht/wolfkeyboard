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

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.provider.*;
import android.view.inputmethod.*;
import android.webkit.*;

public class Welcome extends Activity
{
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.web);
    WebView view = (WebView) findViewById(R.id.webView);
    view.loadUrl("file:///android_asset/welcome.html");
    view.setBackgroundColor(Color.BLACK);
    WebSettings ws = view.getSettings();
    ws.setJavaScriptEnabled(true);
    view.addJavascriptInterface(new Object()
    {
      public void performClick()
      {
        startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
      }
    }, "settings");
    view.addJavascriptInterface(new Object()
    {
      public void performClick()
      {
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
      }
    }, "select");
    view.addJavascriptInterface(new Object()
    {
      public void performClick()
      {
        startActivity(new Intent(Welcome.this, Manual.class));
      }
    }, "manual");
    view.addJavascriptInterface(new Object()
    {
      public void performClick()
      {
        startActivity(new Intent(Welcome.this, Tutorial.class));
      }
    }, "tutorial");
  }
}