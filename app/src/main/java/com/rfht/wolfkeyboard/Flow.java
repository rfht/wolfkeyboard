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

public class Flow
{
  public static final int ENTER = 0;
  public static final int DELETE = 1;
  public static final int SHIFT = 2;
  public static final int ALT = 3;
  public static final int VOICE = 4;
  public static final int FORWARD_DELETE = 5;
  public static final int keys[] =
      {'?', 'x', 'w', 'v', 'y', 'b', DELETE,
       ',', 't', 'h', 'e', 'r', 'm', ' ',
       '.', 'c', 'a', 'i', 'o', 'l', 'p',
       ALT, 'k', 's', 'n', 'u', 'd', 'j',
       SHIFT, 'z', '\'', 'g', 'f', 'q', ENTER};
  public static final int shiftKeys[] =
      {'?', 'X', 'W', 'V', 'Y', 'B', DELETE,
       ',', 'T', 'H', 'E', 'R', 'M', ' ',
       '.', 'C', 'A', 'I', 'O', 'L', 'P',
       ALT, 'K', 'S', 'N', 'U', 'D', 'J',
       SHIFT, 'Z', '\'', 'G', 'F', 'Q', ENTER};
  public static final int altKeys[] =
      {'!', '@', '(', ')', '%', FORWARD_DELETE, DELETE,
       ',', ':', '1', '2', '3', '/', ' ',
       '.', ';', '4', '5', '6', '+', '&',
       ALT, '$', '7', '8', '9', '-', VOICE,
       SHIFT, '=', '"', '0', '#', '*', ENTER};
  public static final int emojiKeys[] = 
      {0x1F603, 0x1F601, 0x1F602, 0x1F605, 0x1F609, 0x1F60A, 0x1F60E,
       0x1F60D, 0x1F618, 0x1F610, 0x1F60F, 0x1F634, 0x1F60C, 0x1F61C,
       0x1F612, 0x1F614, 0x1F615, 0x1F61E, 0x1F61F, 0x1F62D, 0x1F633,
       0x1F620, 0x2764, 0x1F494, 0x1F495, 0x1F64F, 0x1F44C, 0x270C,
       SHIFT, 0x1F44D, 0x1F44E, 0x1F44F, 0x2728, 0x1F525, 0x1F3B6};
  public static final KeyboardLayout baseKeyboard = new KeyboardLayout(keys);
  public static final KeyboardLayout shiftKeyboard = new KeyboardLayout(shiftKeys);
  public static final KeyboardLayout altKeyboard = new KeyboardLayout(altKeys);
  public static final KeyboardLayout altShiftKeyboard = new KeyboardLayout(altKeys);
  public static final KeyboardLayout emojiKeyboard = new KeyboardLayout(emojiKeys);
  public static final HashMap<Integer, String[]> alternates = new HashMap<Integer, String[]>();

  static
  {
    alternates.put((int) 'a', new String[] {"á", "à", "ä", "â", "å", "æ"});
    alternates.put((int) 'A', new String[] {"Á", "À", "Ä", "Â", "Å", "Æ"});
    alternates.put((int) 'e', new String[] {"é", "è", "ë", "ê"});
    alternates.put((int) 'E', new String[] {"É", "È", "Ë", "Ê"});
    alternates.put((int) 'i', new String[] {"í", "ì", "ï", "î"});
    alternates.put((int) 'I', new String[] {"Í", "Ì", "Ï", "Î"});
    alternates.put((int) 'o', new String[] {"ó", "ò", "ö", "ô", "œ", "ø"});
    alternates.put((int) 'O', new String[] {"Ó", "Ò", "Ö", "Ô", "Œ", "Ø"});
    alternates.put((int) 'u', new String[] {"ú", "ù", "ü", "û"});
    alternates.put((int) 'U', new String[] {"Ú", "Ù", "Ü", "Û"});
    alternates.put((int) 'n', new String[] {"ñ"});
    alternates.put((int) 'N', new String[] {"Ñ"});
    alternates.put((int) 's', new String[] {"ß", "§"});
    alternates.put((int) 'S', new String[] {"ß", "§"});
    alternates.put((int) 'c', new String[] {"ç", "©"});
    alternates.put((int) 'C', new String[] {"Ç", "©"});
    alternates.put((int) 'p', new String[] {"π", "¶"});
    alternates.put((int) 'P', new String[] {"∏", "¶"});
    alternates.put((int) 'r', new String[] {"®"});
    alternates.put((int) 'R', new String[] {"®"});
    alternates.put((int) 't', new String[] {"™", "þ"});
    alternates.put((int) 'T', new String[] {"™", "Þ"});
    alternates.put((int) 'd', new String[] {"ð"});
    alternates.put((int) 'D', new String[] {"Ð"});
    alternates.put((int) '$', new String[] {"€", "£", "¢", "¥"});
    alternates.put((int) '+', new String[] {"±"});
    alternates.put((int) '-', new String[] {"–", "_"});
    alternates.put((int) '*', new String[] {"°", "^", "‡", "†"});
    alternates.put((int) '/', new String[] {"\\", "|", "÷"});
    alternates.put((int) '(', new String[] {"<", "[", "{", "≤", "«", " :-("});
    alternates.put((int) ')', new String[] {">", "]", "}", "≥", "»", " :-)"});
    alternates.put((int) '!', new String[] {"¡"});
    alternates.put((int) '?', new String[] {"¿"});
    alternates.put((int) '=', new String[] {"≠", "≈", "~"});
    alternates.put((int) '.', new String[] {"…"});
    alternates.put((int) ';', new String[] {" ;-)"});
  }

}
