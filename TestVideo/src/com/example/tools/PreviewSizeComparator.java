package com.example.tools;

import java.util.Comparator;


public class PreviewSizeComparator implements Comparator<PreviewSizeElement>
{
	  @Override
	  public int compare(PreviewSizeElement o1, PreviewSizeElement o2) {
          double i = o1.getPri();
          double j = o2.getPri();
          if (i > j)
              return -1;
          if (i < j)
              return 1;
          return 0;
      }
}