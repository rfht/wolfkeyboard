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

public class TracedKey implements Comparable<TracedKey>
{
  public int key;
  public float nearestDistance;
  public long nearestTime;

  public TracedKey(int key, float initialDistance, long initialTime)
  {
    this.key = key;
    nearestDistance = initialDistance;
    nearestTime = initialTime;
  }

  public int compareTo(TracedKey tracedKey)
  {
    if (nearestTime < tracedKey.nearestTime)
      return -1;
    if (nearestTime > tracedKey.nearestTime)
      return 1;
    return 0;
  }
}
