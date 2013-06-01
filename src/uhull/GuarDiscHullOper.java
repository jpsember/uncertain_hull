package uhull;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class GuarDiscHullOper implements TestBedOperation, Globals {
  /*! .enum  .private 4450
       standard
  */

    private static final int STANDARD         = 4450;//!
/*!*/

  public void addControls() {
    C.sOpenTab("DiscHull");
    C
        .sStaticText("Constructs upper envelope of guaranteed convex hull of uncertain discs,"
            + " using geometric n(log n) algorithm");
    C.sCheckBox(STANDARD, "plot hull", "plot guaranteed hull for verification",
        true);

    C.sCloseTab();
  }

  public static GuarDiscHullOper singleton = new GuarDiscHullOper();

  private GuarDiscHullOper() {
  }

  public void processAction(TBAction a) {
  }

  private static double sortVal(EdDisc disc, EdObject[] extremeDiscs) {
    double irad = disc.getRadius();
    if (disc == extremeDiscs[HullUtil.WEST])
      irad = -20;
    if (disc == extremeDiscs[HullUtil.EAST])
      irad = -30;
    return irad;
  }

  /**
   * Sort discs to [0] east [1] west [2...n] increasing radii
   * @param discs discs
   * @return true if valid; false if size < 2, or east = west
   */
  private boolean sortDiscs(EdDisc[] discs) {
    boolean valid = false;
    do {
      if (discs.length < 2)
        break;

      EdObject[] extremeDiscs = HullUtil.calcExtremeDiscs(discs, true);

      // find west, east discs
      EdDisc west = (EdDisc) extremeDiscs[HullUtil.WEST];
      EdDisc east = (EdDisc) extremeDiscs[HullUtil.EAST];
      if (west == east)
        break;

      for (int i = 0; i < discs.length; i++) {
        for (int j = i + 1; j < discs.length; j++) {
          if (sortVal(discs[i], extremeDiscs) > sortVal(discs[j], extremeDiscs)) {
            EdDisc tmp = discs[i];
            discs[i] = discs[j];
            discs[j] = tmp;
          }
        }
      }
      valid = true;
    } while (false);
    return valid;
  }
  private int size() {
    return btList.size();
  }
  private BiTangent bitan(int i) {
    return (BiTangent) btList.get(i);
  }
  private static final Color c0 = MyColor.cDARKGREEN, c1 = MyColor.get(
      MyColor.RED, .8), c2 = MyColor.get(MyColor.BROWN, .5);
  private static final double ENVMAX = Math.PI * 3 / 2;

  public void runAlgorithm() {

    final boolean db = true;

    btList = new DArray();
    simpleHull = null;
    currDisc = null;

    EdDisc[] discs = UHullMain.getDiscs();
    if (!sortDiscs(discs))
      return;

    EdDisc east0 = new EdDisc(discs[0]);
    east0.setPoint(0, east0.getOrigin().add(0, -200));
    EdDisc west0 = new EdDisc(discs[1]);
    west0.setPoint(0, west0.getOrigin().add(0, -200));

    if (C.vb(STANDARD)) {
      EdDisc[] d2 = new EdDisc[discs.length + 2];
      int i = 0;
      for (; i < discs.length; i++)
        d2[i] = discs[i];
      d2[i++] = east0;
      d2[i++] = west0;
      simpleHull = HullUtil.build(d2);
    }

    // construct initial bitangent list
    btList.add(new BiTangent(east0, discs[0]));
    btList.add(new BiTangent(discs[0], discs[1]));
    btList.add(new BiTangent(discs[1], west0));

    // process sorted discs
    outer: for (int i = 2; i < discs.length; i++) {
      EdDisc q = discs[i];
      currDisc = q;

      int remStart = -1;
      int postStart = -1;

      EdDisc prob_x = null;
      BiTangent prob_xq = null;

      for (int j = 0; j < size(); j++) {
        BiTangent b = bitan(j);

        // test if bitangent is invalidated
        if (b.lineEqn().signedDistanceFrom(q.getOrigin()) <= -q.getRadius()) {
          if (postStart >= 0)
            T.err("prob: j=" + j + " postStart=" + postStart + T.show(b));
          if (remStart < 0)
            remStart = j;
          continue;
        }

        EdDisc x = b.disc(0);
        BiTangent xq = new BiTangent(x, q);

        if (!xq.defined()) {
          if (db && T.update())
            T.msg("disc is contained" + T.show(q) + T.show(x));
          continue outer;
        }

        if (xq.thetaP() > b.thetaP() && xq.thetaP() < ENVMAX) {
          if (remStart >= 0) {
            if (prob_x == null) {
              prob_x = x;
              prob_xq = xq;
            }
          }
        } else {
          if (postStart < 0) {
            if (remStart < 0)
              remStart = j;
            postStart = j;
          }
        }
      }

      if (db && T.update()) {
        for (int j = 0; j < size(); j++) {
          BiTangent b = bitan(j);
          Color c = (j < remStart) ? c0 : (j < postStart ? c1 : c2);
          T.show(b, c);
        }
        btList = null;
        T.msg("binary search results");
      }

      if (remStart < 0)
        T.err("no interface found");

      BiTangent lastType0 = bitan(remStart - 1);
      BiTangent firstType2 = bitan(postStart);

      EdDisc x = lastType0.disc(1);
      EdDisc y = firstType2.disc(0);

      BiTangent xq = new BiTangent(x, q);

      if (xq.thetaP() <= lastType0.thetaP()
          || xq.thetaP() >= firstType2.thetaP()) {
        if (db && T.update())
          T.msg("new bitangent xq not valid:"
              + T.show(xq, MyColor.cRED, STRK_THICK, -1));
        continue outer;
      }

      if (prob_x != null)
        T.err("unexpected bitangent" + T.show(prob_x) + T.show(prob_xq));

      BiTangent qy = new BiTangent(q, y);

      if (db && T.update()) {
        T.msg("adding new bitangents" + T.show(xq, null, STRK_THICK, -1)
            + T.show(qy, null, STRK_THICK, -1));
      }

      // construct new array from old and new bitangents
      {
        DArray a = new DArray();
        for (int k = 0; k < remStart; k++)
          a.add(btList.get(k));
        a.add(xq);
        a.add(qy);
        for (int k = postStart; k < btList.size(); k++)
          a.add(btList.get(k));
        btList = a;
      }
    }
    currDisc = null;
  }

  public void paintView() {
    Editor.render();
    T.render(simpleHull, MyColor.cLIGHTGRAY, STRK_THICK, -1);
    T.render(btList, BiTangent.BITAN_COLOR, -1, -1);
    T.render(currDisc, MyColor.cRED);
  }

  private Object currDisc;
  private DArray btList;
  private EdPolygon simpleHull;
}
