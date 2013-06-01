package uhull;

import java.awt.*;
import java.util.*;
import testbed.*;
import uhull.SegEnvOper.*;
import base.*;

public class EnvTools {

  public static final Color ACOLOR = MyColor.get(MyColor.BLUE, .6);
  public static final Color BCOLOR = MyColor.get(MyColor.GREEN, .4);

  public static final int F_RNDORDER = 1 << 0;
  public static final int F_TRACE = 1 << 1;
  /**
   * @deprecated
   */
  public static final int F_NOTRACE = 1 << 3;
  public static final int F_SIMP = 1 << 2;

  private static boolean db = true;

  /**
   * Calculate upper envelope of a set of generalized segments
   * @param segs array of GenSegs
   * @return array of GenSegs marking upper envelope
   */
  public static DArray calcUpper(DArray segs, int flags) {
    prepare(flags);
    DArray ret = calcEnv(flags, segs, 0, segs.size(), true);
    return ret;
  }

  private static void prepare(int flags) {
    rnd = null;
    if ((flags & F_RNDORDER) != 0) {
      rnd = new Random(1965);
    }

  }

  /**
   * Calculate upper envelope of a set of generalized segments
   * @param segs array of GenSegs
   * @return array of GenSegs marking upper envelope
   */
  public static DArray calcLower(DArray segs, int flags) {
    prepare(flags);
    DArray ret = calcEnv(flags, segs, 0, segs.size(), false);
    return ret;
  }

  private static Random rnd;

  /**
   * Calculate envelope of a set of generalized segments
   * @param segs array of GenSegs
   * @param start index of first segment to process
   * @param size number of segments to process
   * @param upper true to calc upper envelope, false for lower
   * @return array of GenSegs marking upper envelope
   */
  private static DArray calcEnv(int flags, DArray segs, int start, int size,
      boolean upper) {

    db = (flags & F_TRACE) != 0;

    DArray ret = null;
    if (size > 1) {
      if (db && T.update())
        T.msg("calcEnvelope" + T.show(segs.subset(start, size), MyColor.cRED)
            //+ "\n" + segs.subset(start, size).toString(true)
            );

      if (rnd != null) {
        int[] p = MyMath.permutation(size, rnd);
        for (int i = 0; i < size; i++)
          segs.swap(start + i, start + p[i]);
      }

      if ((flags & F_SIMP) != 0) {
        ret = simpleEnv(segs, start, size, upper);
      } else {

        int half = size / 2;
        DArray set1 = calcEnv(flags, segs, start, half, upper);
        DArray set2 = calcEnv(flags, segs, start + half, size - half, upper);
        ret = merge(set1, set2, upper);
      }

      if (db && T.update())
        T.msg("calculated envelope"
            + T.show(ret, MyColor.cRED, -1, Globals.MARK_DISC)
        //   + "\n"+ ret.toString(true)
            );

    } else {
      ret = segs.subset(start, size);
    }
    return ret;
  }

  private static Double calcNextInt(GenSeg a, GenSeg b, double x) {

    boolean db = false;

    if (a.id() > b.id()) {
      GenSeg tmp = a;
      a = b;
      b = tmp;
    }

    Double ni = a.nextIntersection(a, b, x);
    if (db && T.update())
      T.msg("nextIntersection:\n" + a + "\n" + b + "\n ahead of x:"+x+"\nyielded " + ni
          + T.show(a) + T.show(b));
    if (ni != null) {
      double xi = ni.doubleValue();
      if (xi > a.right() || xi > b.right())
        T.err("intersection out of range: " + xi + "\n a=" + a + "\n b=" + b);
      if (db) {
        StringBuilder sb = new StringBuilder();
        sb.append("calcNextInt\n a=" + a + "\n b=" + b + "\n from x=" + x
            + "\n");
        while (true) {
          sb.append(" " + xi + "\n");
          if (xi == a.right() || xi == b.right())
            break;
          Double n2 = a.nextIntersection(a, b, xi);
          if (n2 == null)
            break;
          xi = n2.doubleValue();
        }
        if (db && T.update())
          T.msg(sb);
      }

    }

    return ni;
  }

  private static DArray merge(DArray a1, DArray a2, boolean upper) {
    boolean db = EnvTools.db && (a1.size() > 0 && a2.size() > 0);
    DArray ret = new DArray();

    GenSeg render = null;
    if (a1.size() > 0)
      render = (GenSeg) a1.get(0);
    else if (a2.size() > 0)
      render = (GenSeg) a2.get(0);
    if (render == null)
      return ret;

    GenSeg aSeg = null;
    GenSeg bSeg = null;

    // index into merge lists
    int aInd = 0, bInd = 0;

    // envelope segment currently being built, or null
    GenSeg prevEnvSeg = null;
    double prevEnvSegStart = 0;

    double XMIN = -1e10, XMAX = 1e10;

    // sweep position
    double xSweep = XMIN;

    while (xSweep != XMAX) {
      GenSeg pNext = null, sNext = null;
      if (aInd < a1.size())
        pNext = (GenSeg) a1.get(aInd);
      if (bInd < a2.size())
        sNext = (GenSeg) a2.get(bInd);

      double xNext = XMAX;
      if (aSeg != null)
        xNext = Math.min(xNext, aSeg.right());
      if (bSeg != null)
        xNext = Math.min(xNext, bSeg.right());
      if (pNext != null)
        xNext = Math.min(xNext, pNext.left());
      if (sNext != null)
        xNext = Math.min(xNext, sNext.left());
      if (aSeg != null && bSeg != null) {
        Double nextint = calcNextInt(aSeg, bSeg, xSweep);
        double xi = XMAX;
        if (nextint != null) {
          xi = nextint.doubleValue();
        }
        if (xi > xSweep)
          xNext = Math.min(xNext, xi);
      }
      if (false && db && T.update())
        T.msg("sweep x=" + Tools.f(xSweep) + " xNext=" + Tools.f(xNext)
            + T.show(pNext) + T.show(sNext));

      GenSeg nextEnvSeg = (aSeg == null) ? bSeg : aSeg;
      if (aSeg != null && bSeg != null) {
        double tx = (xSweep + xNext) * .5;
        GenSeg t = higherOf(aSeg, bSeg, tx, upper);
        if (t != null)
          nextEnvSeg = t;
      }

      GenSeg newseg = null;

      if (nextEnvSeg != prevEnvSeg) {
        if (prevEnvSeg != null) {
          newseg = prevEnvSeg.clipTo(prevEnvSegStart, xSweep);
          if (newseg != null) {
            ret.add(newseg);
          }
        }
        prevEnvSeg = nextEnvSeg;
        prevEnvSegStart = xSweep;
      }

      if (render != null && db && T.update())
        T.msg("merging"
            + render.showSweepLine(xSweep)
            + render.showSweepLine(xNext)

            + T.show(aSeg, ACOLOR, Globals.STRK_THICK, -1)
            + T.show(bSeg, BCOLOR, Globals.STRK_THICK, -1)

            + T.show(ret, MyColor.cLIGHTGRAY, Globals.STRK_THICK,
                Globals.MARK_DISC) + T.show(a1, ACOLOR, -1, -1)
            + T.show(a2, BCOLOR, -1, -1)
            + T.show(newseg, MyColor.cRED, Globals.STRK_THICK, -1) 
            // + ":\n\n" + a1.toString(true) + "\n" + a2.toString(true)
        );

      xSweep = xNext;
      // throw out segments we have swept to end of
      if (bSeg != null && xSweep >= bSeg.right())
        bSeg = null;

      if (aSeg != null && xSweep >= aSeg.right())
        aSeg = null;

      // pull in segments we have swept to start of
      if (pNext != null && xSweep == pNext.left()) {
        aSeg = pNext;
        aInd++;
      }
      if (sNext != null && xSweep == sNext.left()) {
        bSeg = sNext;
        bInd++;
      }
    }
    return ret;
  }

  private static GenSeg higherOf(GenSeg a, GenSeg b, double x, boolean upper) {
    GenSeg ret = null;
    int s = MyMath.sign(a.id() - b.id());
    switch (s) {
    case -1:
      ret = a.higherOf(a, b, x);
      break;
    case 1:
      ret = a.higherOf(b, a, x);
      break;
    }
    if (ret != null && !upper)
      ret = (ret == a ? b : a);
    return ret;
  }

  private static GenSeg envSeg(GenSeg a, GenSeg b, double x, boolean upper) {
    GenSeg ret = higherOf(a, b, x, upper);
    //
    //    if (a.id() < b.id())
    //      ret = a.higherOf2(a, b, x);
    //    else if (a.id() > b.id())
    //      ret = a.higherOf2(b, a, x);
    //    if (ret != null) {
    //      if (!upper)
    //        ret = (ret == a) ? b : a;
    //    }
    return ret;
  }

  private static GenSeg leavingSeg(GenSeg s1, GenSeg s2, double x, boolean upper) {
    double endX = Math.min(s1.right(), s2.right());
    Double nextX = calcNextInt(s1, s2, x);
    if (nextX != null)
      endX = nextX.doubleValue();
    GenSeg env = envSeg(s1, s2, (x + endX) * .5, upper);
    if (env == null)
      T.err("unexpected:\ns1=" + s1 + "\ns2=" + s2 + "\nx=" + x + "\nendX="
          + endX + "\nnextX=" + nextX + T.show(s1, MyColor.cBLUE)
          + T.show(s2, MyColor.cDARKGREEN));
    return env;
  }

  /**
   * Calculate envelope of a set of generalized segments, using
   * simple O(nh) method described in Chan thesis
   * @param segs array of GenSegs
   * @param start index of first segment to process
   * @param size number of segments to process
   * @param upper true to calc upper envelope, false for lower
   * @return array of GenSegs marking envelope
   */
  private static DArray simpleEnv(DArray segs, int start, int size,
      boolean upper) {

    if (db && T.update())
      T.msg("calculating envelope using simple method");

    Inf inf = new Inf("simpleEnv", 1000);

    DArray ret = new DArray();

    do {
      // determine min, max x 
      GenSeg sMin = null, sMax = null;
      for (int i = 0; i < size; i++) {
        GenSeg s = (GenSeg) segs.get(start + i);
        if (sMin == null || sMin.left() > s.left())
          sMin = s;
        if (sMax == null || sMax.right() < s.right())
          sMax = s;
      }
      if (sMin == null)
        break;
      if (db && T.update())
        T.msg("minX=" + sMin.left() + T.show(sMin) + " maxX=" + sMax.right()
            + T.show(sMax));

      double x = sMin.left();

      while (x < sMax.right()) {
        Inf.update(inf);

        // find envelope segment extending to right from x
        GenSeg extRight = null;
        for (int i = 0; i < size; i++) {
          GenSeg s = (GenSeg) segs.get(start + i);
          if (s.left() > x || s.right() <= x)
            continue;
          if (extRight == null) {
            extRight = s;
          } else {
            // determine which segment is higher leaving current x
            extRight = leavingSeg(s, extRight, x, upper);
          }
        }
        if (db && T.update())
          T.msg("simple, x=" + x + "\n extRight=" + extRight + "\n"
              + T.show(segs.subList(start, start + size), MyColor.cBLUE)
              + T.show(ret, MyColor.cRED, Globals.STRK_THICK, -1)
              + T.show(extRight, MyColor.cRED) + sMin.showSweepLine(x));

        // if no seg found, advance to next starting seg
        if (extRight == null) {
          GenSeg nextStart = null;

          for (int i = 0; i < size; i++) {
            GenSeg s = (GenSeg) segs.get(start + i);
            if (s.left() > x) {
              if (nextStart == null || s.left() < nextStart.left())
                nextStart = s;
            }
          }
          x = nextStart.left();
          continue;
        }

        // find extent of this segment
        if (db && T.update())
          T.msg("finding extent of segment" + T.show(extRight, MyColor.cRED));
        double xEnd = extRight.right();
        for (int i = 0; i < size; i++) {
          GenSeg s = (GenSeg) segs.get(start + i);
          if (db && T.update())
            T.msg("testing segment #" + i + " of " + size
                + T.show(extRight, MyColor.cRED)
                + T.show(s, MyColor.cDARKGREEN) + " xEnd=" + xEnd + " s.left="
                + s.left() + " s.right=" + s.right());
          if (s == extRight)
            continue;
          if (s.left() >= xEnd || s.right() <= x)
            continue;

          //          Double nextInt = null;

          if (s.left() > x) {
            GenSeg leaving = leavingSeg(extRight, s, s.left(), upper);
            if (db && T.update())
              T.msg("leavingSeg was " + T.show(leaving));
            if (leaving == s) {
              xEnd = Math.min(xEnd, s.left());
              continue;
            }

          }

          //          if (s.left() <= x) 
          {
            // find next intersection point
            Double nextInt = calcNextInt(extRight, s, x);
            if (db && T.update())
              T.msg("nextInt for " + T.show(extRight, MyColor.cPURPLE)
                  + T.show(s, MyColor.cDARKGREEN) + " is " + nextInt);
            if (nextInt != null)
              xEnd = Math.min(xEnd, nextInt.doubleValue());
            if (db && T.update())
              T.msg("nextInt was " + nextInt);
          }
          //          else {
          //            GenSeg leaving = leavingSeg(extRight, s, s.left(), upper);
          //            if (db && T.update())
          //              T.msg("leavingSeg was " + T.show(leaving));
          //            if (leaving == s)
          //              xEnd = Math.min(xEnd, s.left());
          //          }
        }
        if (db && T.update())
          T.msg("adding segment clipped to " + x + " " + xEnd);
        ret.add(extRight.clipTo(x, xEnd));

        x = xEnd;
        if (db && T.update())
          T.msg("x now " + x);
      }
    } while (false);
    return ret;
  }
  static {
    final double width = 95;
    final double height = 55;
    final double SCLY = .12;
    final double HT = 115;
    final double maxHeight = 100;

    double s = Math.min(1.0, height / (SCLY * (2 * maxHeight)));
    Matrix trans = Matrix.getTranslate(new FPoint2((100 - width) / 2, HT
        - height / 2), false);
    Matrix scale = Matrix.getScale(s * (width / (2 * Math.PI)), s * SCLY);
    transform = Matrix.mult(trans, scale, null);
  }

  public static FPoint2 transform(FPoint2 pt) {
    return transform.apply(pt);
  }

  private static Color[] colors = { //
    MyColor.get(MyColor.BLUE, .8), //
  MyColor.get(MyColor.DARKGREEN, .6), //
      MyColor.get(MyColor.BROWN, .6), //
      MyColor.get(MyColor.MAGENTA, .4),//
  };

  public static Color multicolor(int i) {
    return colors[i % colors.length];
  }

  private static Matrix transform;
}
