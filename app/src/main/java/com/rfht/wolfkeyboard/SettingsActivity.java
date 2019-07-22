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

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.provider.*;
import android.widget.*;

public class SettingsActivity extends PreferenceActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
    findPreference("userDictionary").setOnPreferenceClickListener(new ActionListener(Settings.ACTION_USER_DICTIONARY_SETTINGS));
    findPreference("popupDuration").setOnPreferenceClickListener(new ValueListener("popupDuration", R.string.popupDuration, 3, 18));
    findPreference("longPressDelay").setOnPreferenceClickListener(new ValueListener("longPressDelay", R.string.longPressDelay, 40, 70));
    findPreference("backspaceRate").setOnPreferenceClickListener(new ValueListener("backspaceRate", R.string.backspaceRate, 6, 15));
    findPreference("keyboardSize").setOnPreferenceClickListener(new DocumentListener(AdjustSizeActivity.class));
    findPreference("about").setOnPreferenceClickListener(new DocumentListener(About.class));
    findPreference("manual").setOnPreferenceClickListener(new DocumentListener(Manual.class));
    findPreference("tutorial").setOnPreferenceClickListener(new DocumentListener(Tutorial.class));
  }

  private class ValueListener implements Preference.OnPreferenceClickListener
  {
    private String pref;
    private int title, defaultValue, maxValue;

    public ValueListener(String pref, int title, int defaultValue, int maxValue)
    {
      this.pref = pref;
      this.title = title;
      this.defaultValue = defaultValue;
      this.maxValue = maxValue;
    }

    public boolean onPreferenceClick(Preference preference)
    {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
      AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
      builder.setTitle(title);
      final SeekBar slider = new SeekBar(SettingsActivity.this);
      slider.setMax(maxValue);
      slider.setProgress(prefs.getInt(pref, defaultValue));
      builder.setView(slider);
      builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialogInterface, int i)
        {
        }
      });
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface dialogInterface, int i)
        {
          SharedPreferences.Editor editor = prefs.edit();
          editor.putInt(pref, slider.getProgress());
          editor.commit();
        }
      });
      builder.create().show();
      return true;
    }
  }

  private class DocumentListener implements Preference.OnPreferenceClickListener
  {
    private Class docClass;

    public DocumentListener(Class docClass)
    {
      this.docClass = docClass;
    }

    public boolean onPreferenceClick(Preference preference)
    {
      startActivity(new Intent(SettingsActivity.this, docClass));
      return true;
    }
  }

  private class ActionListener implements Preference.OnPreferenceClickListener
  {
    private String action;

    public ActionListener(String action)
    {
      this.action = action;
    }

    public boolean onPreferenceClick(Preference preference)
    {
      try
      {
        startActivity(new Intent(action));
      }
      catch (Exception ex)
      {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle("Error");
        builder.setMessage("Failed to open user dictionary!  You can access it from Settings > Language & input.");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.create().show();
      }
      return true;
    }
  }
}
