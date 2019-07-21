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

import java.util.*;

public class TracePoint
{
  public final ArrayList<TracedKey> viaKeyList;
  public final float keyDistances[];
  public final int viaKeyIndices[];
  public float x, y, weight;
  public TracedKey viaKeys[];

  public TracePoint(float x, float y)
  {
    this.x = x;
    this.y = y;
    viaKeyList = new ArrayList<TracedKey>();
    keyDistances = new float[27];
    viaKeyIndices = new int[26];
    Arrays.fill(keyDistances, Float.MAX_VALUE);
    Arrays.fill(viaKeyIndices, -1);
    weight = 1.0f;
  }

  public void addViaKey(int key, float distance, long time)
  {
    for (TracedKey c : viaKeyList)
    {
      if (c.key == key)
      {
        if (distance < c.nearestDistance)
        {
          c.nearestDistance = distance;
          c.nearestTime = time;
        }
        return;
      }
    }
    viaKeyList.add(new TracedKey(key, distance, time));
  }

  public void addViaKeys(List<TracedKey> keys)
  {
    for (TracedKey key : keys)
      addViaKey(key.key, key.nearestDistance, key.nearestTime);
  }

  public void finalizeViaKeys()
  {
    viaKeys = viaKeyList.toArray(new TracedKey[viaKeyList.size()]);
    for (int i = 0; i < viaKeys.length; i++)
    {
      if (viaKeys[i].key >= 'a' && viaKeys[i].key <= 'z')
        viaKeyIndices[viaKeys[i].key-'a'] = i;
    }
  }

  public float getKeyDistance(int c)
  {
    if (c == '\'')
      return keyDistances[26];
    return keyDistances[c-'a'];
  }

  public int getViaKeyIndex(int key)
  {
    if (key >= 'a' && key <= 'z')
      return viaKeyIndices[key-'a'];
    TracedKey localViaKeys[] = viaKeys;
    for (int i = localViaKeys.length-1; i >= 0; i--)
      if (localViaKeys[i].key == key)
        return i;
    return -1;
  }

  public void mergePoint(TracePoint point)
  {
    addViaKeys(point.viaKeyList);
    for (int i = 0; i < keyDistances.length; i++)
      keyDistances[i] = Math.min(keyDistances[i], point.keyDistances[i]);
  }

  public float distance2(TracePoint point)
  {
    float dx = x-point.x;
    float dy = y-point.y;
    return dx*dx+dy*dy;
  }
}
