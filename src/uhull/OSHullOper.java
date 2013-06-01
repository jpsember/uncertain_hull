package uhull;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class OSHullOper implements TestBedOperation, Globals {
  /*! .enum  .private 4750
        mvalue   plotsimp   verify  dbclipbitan dbjarvis
        upperonly printenv mtest ctest
  */

  private static final int MVALUE = 4750;//!
  private static final int PLOTSIMP = 4751;//!
  private static final int VERIFY = 4752;//!
  private static final int DBCLIPBITAN = 4753;//!
  private static final int DBJARVIS = 4754;//!
  private static final int UPPERONLY = 4755;//!
  private static final int PRINTENV = 4756;//!
  private static final int MTEST = 4757;//!
  private static final int CTEST = 4758;//!
  /*!*/

  // private static final boolean NEW = true;
  public void addControls() {
    C.sOpenTab("OSHull");
    C.sStaticText("Output-sensitive guaranteed convex hull of discs algorithm"
        + " (an adaptation of Chan's convex hull algorithm)");

    C.sIntSpinner(MVALUE, "m:",
        "value for m (if 0, iterates with larger values each time)", 0, 100, 4,
        1);

    C.sCheckBox(DBCLIPBITAN, "trace clip",
        "Trace clipping bitangents -> hull polygon", false);
    C.sCheckBox(DBJARVIS, "trace jarvis", "Trace Jarvis march", false);
    {
      C.sOpen();

      C.sCheckBox(PLOTSIMP, "simple",
          "Plot guaranteed hull, for testing purposes, "
              + "using simple, robust algorithm", false);
      C.sNewColumn();
      C.sCheckBox(VERIFY, "verify", "Verify that generated hull "
          + "matches the simple, slow algorithm's output", false);
      C.sClose();
    }

    C.sCheckBox(UPPERONLY, "upper only",//
        "Construct only the upper hull", false);
    C.sCheckBox(PRINTENV, "print env", "Display upper envelope as string",
        false);
    C.sCheckBox(MTEST, "monotone test",
        "tests if subset bitangents are monotone\nw.r.t. query disc", false);
    C.sCheckBox(CTEST, "contiguous test",
        "tests if entries popped by query disc are contiguous", false);
    C.sCloseTab();
  }

  public static OSHullOper singleton = new OSHullOper();

  private OSHullOper() {
  }

  public void processAction(TBAction a) {
  }

  private EdPolygon simpleHull() {
    if (simpleHull == null)
      simpleHull = HullUtil.build(UHullMain.getDiscs());
    return simpleHull;
  }

  private boolean db;

  public void runAlgorithm() {
    hullPolygon = null;
    simpleHull = null;
    subsets = null;
    hullBitan = new DArray();
    workBitan = null;
    prevDisc = null;

    for (int side = 0; side < 2; side++) {
      if (side == 1 && C.vb(UPPERONLY) && !C.vb(VERIFY))
        break;
      db = side == 0;
      if (false) {
        Tools.warn("turning off db");
        db = false;
      }
      EdDisc[] ds = UHullMain.getDiscs();

      if (ds.length == 0)
        return;

      // if second side, replace discs with rotated versions, and 
      // map them to their originals
      if (side == 1) {
        discMap = new HashMap();
        ds = HullUtil.transformDiscs(ds, discMap);
      }
      DArray discs = new DArray(ds);

      int mValue = C.vi(MVALUE);
      workBitan = new DArray();
      prevDisc = null;
      if (mValue == 0) {
        for (int t = 0;; t++) {
          int m = (int) Math.pow(2, Math.pow(2, t));
          if (chanEnv(workBitan, discs, m, m))
            break;
          if (t > 5)
            T.err("t too large");
        }
      } else {
        chanEnv(workBitan, discs, mValue, -1);
      }
      prevDisc = null;
      subsets = null;

      {
        if (db && T.update())
          T.msg("finished bitangents for envelope");

        for (int i = 0; i < workBitan.size(); i++) {
          BiTangent b = (BiTangent) workBitan.get(i);
          if (side == 1) {
            EdDisc d0 = (EdDisc) discMap.get(b.disc(0));
            EdDisc d1 = (EdDisc) discMap.get(b.disc(1));
            if (d0 != d1)
              b = new BiTangent(d0, d1);
          }
          hullBitan.add(b);
        }
        workBitan = null;
      }

      if (side == 0 && C.vb(PRINTENV)) {
        T.show(HullUtil.bitangentsString(hullBitan), null, 0, 100);
      }
    }

    if (!(C.vb(UPPERONLY) && !C.vb(VERIFY))) {
      hullPolygon = HullUtil.hullFromBiTangents(hullBitan, null);
    }

    subsets = null;

    if (C.vb(VERIFY) && hullPolygon != null) {
      EdPolygon simp = simpleHull();
      if (simp != null) {
        String diff = HullUtil.compareHulls(hullPolygon, simp);
        if (diff != null)
          T.err("Generated hull and simple version don't agree:\n"
              + T.show(simpleHull(), MyColor.cLIGHTGRAY, STRK_THICK, -1) + diff);
      }
    }
  }

  public void paintView() {

    if (subsets == null)
      Editor.render();

    if (C.vb(PLOTSIMP))
      T.render(simpleHull(), MyColor.cLIGHTGRAY, STRK_THICK, -1);

    T.render(subsets);

    T.render(workBitan, BiTangent.BITAN_COLOR, STRK_NORMAL, -1);
    T.render(prevDisc, null, STRK_THICK, -1);

    if (C.vb(UPPERONLY))
      T.render(hullBitan, MyColor.cDARKGREEN, -1, -1);
    HullUtil.render(hullPolygon);
  }

  /**
   * Test if remaining items on stack have monotone property
   * (see June 10:1)
   * @param query
   * @param set
   */
  private void monotoneTest(EdDisc query, BitanStack set) {
    EdDisc prev = null;
    double prevTheta = 0;
    BiTangent bprev = null;

    for (int i = 0; i < set.size(); i++) {
      EdDisc next = set.peek(i);
      if (next == query)
        continue;
      BiTangent b = new BiTangent(query, next);
      if (!b.defined())
        continue;
      double theta = b.thetaP();
      if (prev != null && theta < prevTheta) {
        T.msg("monotonicity failed; query=" + query
            + T.show(query, MyColor.cRED) + " prev=" + prev
            + T.show(bprev, MyColor.cDARKGREEN) //, STRK_THICK, -1)
            + " curr=" + next + T.show(b, MyColor.cPURPLE)); //, STRK_THICK, -1));
      }
      prev = next;
      prevTheta = theta;
      bprev = b;
    }
  }
  /**
   * Test if items on stack that would be popped, were they at the
   * top of the stack, are contiguous
   * (see June 10:1)
   * @param query
   * @param set
   */
  private void contiguousTest(EdDisc query, BitanStack set, double theta) {
    StringBuilder sb = new StringBuilder();

    sb.append("Query: " + query);
    sb.append(" [");

    int state = 0;

    for (int i = 0; i < set.size() - 1; i++) {
      EdDisc d1 = set.peek(i);
      EdDisc d2 = set.peek(i + 1);
      if (i > 0)
        sb.append(' ');
      sb.append(d1);
      if (d1 == query)
        continue;

      BiTangent b = new BiTangent(query, d1);
      BiTangent be = new BiTangent(d1, d2);

      boolean popped = (!b.defined() || b.thetaP() <= theta || b.thetaP() >= be
          .thetaP());

      switch (state) {
      case 0:
        if (popped)
          state++;
        break;
      case 1:
        if (!popped)
          state++;
        break;
      case 2:
        if (popped) {
          state++;
        }
        break;
      }
      if (popped)
        sb.append('*');
      else
        sb.append(' ');
      //  sb.append(state);

    }
    sb.append(']');
    if (state == 3) {
      T.msg("contiguity failed; query=" + query + T.show(query, MyColor.cRED)
          + T.show(sb.toString(), null, 0, 100, Globals.TX_CLAMP));
    }
    if (db && T.update())
      T.msg("contiguity test: " + sb + T.show(sb.toString()));
  }

  /**
   * Calculate upper hull bitangents using Chan's algorithm
   * @param hb where to store bitangents
   * @param discs
   * @param m  value of m to use
   * @param maxHullDiscs  max # hull discs to generate; if we exceed this amount,
   *  the algorithm stops and returns null
   */
  private boolean chanEnv(DArray hb, DArray discs, int m, int maxHullDiscs) {

    boolean finished = false;
    if (db && T.update())
      T.msg("chanHull, m=" + m + " maxHullDiscs=" + maxHullDiscs);

    hb.clear();
    constructSubsets(discs, m);

    EdObject[] extr = HullUtil.calcExtremeDiscs(discs, true);
    prevDisc = (EdDisc) extr[HullUtil.EAST];
    double prevTheta = BSEnt.THETA_MIN - .1;

    if (db && T.update())
      T.msg("first hull disc");

    Inf inf = Inf.create();

    // phase 2
    while (true) {

      Inf.update(inf);
      if (maxHullDiscs > 0 && hb.size() > maxHullDiscs) {
        prevDisc = null;
        if (db && T.update())
          T.msg("maximum # hull discs " + maxHullDiscs + " exceeded, aborting");
        break;
      }

      // perform Jarvis march step 
      BiTangent bNext = null;

      for (int setIndex = 0; setIndex < subsets.size(); setIndex++) {

        BitanStack set = (BitanStack) subsets.get(setIndex);
        if (set.isEmpty())
          continue;

        if (db && T.update())
          T.msg("looking for best candidate from set #" + setIndex
              + T.show(set, null, STRK_THICK, -1));

        if (db && C.vb(CTEST)) {
          contiguousTest(prevDisc, set, prevTheta);
        }

        EdDisc candidate = set.find(prevDisc, prevTheta);

        if (db && C.vb(MTEST)) {
          monotoneTest(prevDisc, set);
        }

        if (candidate == null) {
          if (db && T.update())
            T.msg("set #" + setIndex + " no disc found");
          continue;
        }

        BiTangent b = new BiTangent(prevDisc, candidate);
        if (db && T.update())
          T.msg("set #" + setIndex + " disc found=" + candidate
              + T.show(candidate, set.color(), STRK_THICK, -1)
              + T.show(b, null, -1, -1));
        if (!b.defined()) {
          if (db && T.update())
            T.msg("bitangent undefined");
          continue;
        }

        if (!BSEnt.isUpperEnv(b.thetaP())) {
          if (db && T.update())
            T.msg("not upper envelope");
          continue;
        }

        if (b.thetaP() < prevTheta) {
          if (db && T.update())
            T.msg("angle less than previous");
          continue;
        }

        if (bNext == null || b.thetaP() < bNext.thetaP()) {
          bNext = b;
        }

      }
      if (bNext == null || bNext.thetaP() <= prevTheta) {
        finished = true;
        break;
      }

      if (db && T.update())
        T.msg("next hull bitangent" + T.show(bNext, null, STRK_THICK, -1)
            + T.show(bNext.disc(0)));

      hb.add(bNext);
      prevDisc = bNext.disc(1);
      prevTheta = bNext.theta();
      //prevDisc = prev;
    }
    return finished;
  }

  private static int[] colors = {//
  0, 0, 102,//
      20, 183, 10,//
      255, 99, 20,//
      204, 0, 255,//
      61, 100, 255,//
      255, 10, 133, //
      163, 82, 0,//
      0, 163, 163,//
  };

  /**
   * Construct subsets
   * @param discs2
   * @param m
   */
  private void constructSubsets(DArray discs2, int m) {
    subsets = new DArray();
    if (db && T.update())
      T.msg("constructing subsets of size " + m + T.show(discs2));
    int k = 0;
    while (k < discs2.size()) {
      int len = Math.min(m, discs2.size() - k);

      EdDisc[] discs = new EdDisc[len];
      for (int i = 0; i < len; i++)
        discs[i] = (EdDisc) discs2.get(k + i);
      k += len;

      BitanSet bitanSet = new BitanSet();

      int ci = (subsets.size() % (colors.length / 3)) * 3;

      bitanSet.setColor(new Color(colors[ci + 0], colors[ci + 1],
          colors[ci + 2]));
      bitanSet.construct(discs);

      BitanStack stack = new BitanStack(bitanSet);
      stack.setTrace(db);

      subsets.add(stack);
      if (db && T.update())
        T.msg("subset #" + (subsets.size() - 1) + "\n" + stack);
    }
  }

  private DArray subsets;
  // polygon representing guaranteed hull, or null if empty
  private EdPolygon hullPolygon;
  private EdPolygon simpleHull;
  private DArray hullBitan;
  private DArray workBitan;
  private EdDisc prevDisc;
  // map of transformed=>original discs, for constructing lower hull
  private Map discMap;
}
