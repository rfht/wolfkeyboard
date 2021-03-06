package info.thfr.wolfkeyboard;

/**
 * Copyright 2011-2013 by Peter Eastman
 * Copyright 2019         Thomas Frohwein <11335318+rfht@users.noreply.github.com>
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
import android.graphics.*;
import android.os.Bundle;
import android.webkit.*;

public class About extends Activity
{
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.web);
    WebView view = (WebView) findViewById(R.id.webView);
    view.loadUrl("file:///android_asset/about.html");
    view.setBackgroundColor(Color.BLACK);
  }
}