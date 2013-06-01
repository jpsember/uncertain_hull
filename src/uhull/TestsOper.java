package uhull;

import java.awt.*;
import java.util.*;
import java.util.List;
import base.*;
import testbed.*;

public class TestsOper implements TestBedOperation, Globals {
  /*! .enum  .private 2650 left oper
  */

    private static final int LEFT             = 2650;//!
    private static final int OPER             = 2651;//!
/*!*/

  public void paintView() {
    Editor.render();
    T.render(trace);
  }
  private static final boolean db = true;

  private static final Color C1 = MyColor.cPURPLE, C2 = MyColor.get(
      MyColor.CYAN, .3);

  public static TestsOper singleton = new TestsOper();
  private TestsOper() {
  }

  public void processAction(TBAction a) {
  }

  public void addControls() {
    C.sOpenTab("Test");
    C.sStaticText("Tests various procedures");
    C.sOpenTabSet(OPER);

    {
      C.sOpenTab("logsearch");
      C.sStaticText("Log(n) search for tangent line to convex polygon."
          + " Create a polygon, and a point to search from.");
      C.sCheckBox(LEFT, "left tangent", null, true);
      C.sCloseTab();
    }
    {
      C.sOpenTab("combine");
      C.sStaticText("Combine two convex polygons into third, using rotating calipers method.  Start with"
          + " two convex polygons.");
      C.sCloseTab();
    }
    {
      C.sOpenTab("Melkman");
      C
          .sStaticText("Calculate convex hull of polygon using Melkman's algorithm."
              + "  Start with a simple polygon.");
      C.sCloseTab();
    }
    {
      C.sOpenTab("kernel");
      C
          .sStaticText("Search for point in kernel of possible convex hull of a pair of polygons.");
      C.sCloseTab();
    }
    {
      C.sOpenTab("bitangent");
      C.sStaticText("Search for bitangent of disjoint hulls.");
      C.sCloseTab();
    }
    C.sCloseTabSet();

    C.sCloseTab();
  }

  // ------------------------------------------------------------------------
  private void logSearchTest() {
    do {
      DArray a = Editor.readObjects(EdPoint.FACTORY, false, true);

      EdPolygon[] polys = UHullMain.getPolygons();
      if (a.size() < 1 || polys.length < 1)
        break;
      FPoint2 srcPoint = ((EdPoint) a.get(0)).getOrigin();
      EdPolygon hull = polys[0];

      FPoint2 targ = findTangentVertex(hull, srcPoint, !C.vb(LEFT));
      if (targ != null)
        EdSegment.show(srcPoint, targ, null, STRK_RUBBERBAND);
      else
        T.show(new EdDisc(srcPoint, 5), MyColor.cRED);
    } while (false);
  }

  private void kernelTest() {
    do {
      EdPolygon[] polys = UHullMain.getPolygons();
      if (polys.length < 2)
        break;
      EdPolygon hulla = polys[0].getConvexHull();
      EdPolygon hullb = polys[1].getConvexHull();

      HullUtil.findKernelPoint(hulla, hullb, db);
    } while (false);
  }

  private static int tangentCode(FPoint2 l1, FPoint2 l2, FPoint2 prev,
      FPoint2 next) {
    int sideP = MyMath.sign(MyMath.sideOfLine(l1, l2, prev));
    int sideN = MyMath.sign(MyMath.sideOfLine(l1, l2, next));
    if (sideP < 0)
      sideP = 0;
    if (sideN < 0)
      sideN = 0;
    return (sideN << 1) | sideP;
  }

  private void bitangentSearch() {
    do {
      EdPolygon[] polys = UHullMain.getPolygons();
      if (polys.length < 2)
        break;
      EdPolygon pa = polys[0].getConvexHull();
      EdPolygon pb = polys[1].getConvexHull();

      int ai = 0, bi = 0;

      Inf inf = new Inf();
      while (true) {
        inf.update();

        FPoint2 a1 = pa.getPointMod(ai);
        FPoint2 a0 = pa.getPointMod(ai - 1);
        FPoint2 a2 = pa.getPointMod(ai + 1);
        FPoint2 b1 = pb.getPointMod(bi);
        FPoint2 b0 = pb.getPointMod(bi - 1);
        FPoint2 b2 = pb.getPointMod(bi + 1);

        int ac = tangentCode(a1, b1, a0, a2);
        int bc = tangentCode(b1, a1, b0, b2);
        if (db && T.update())
          T.msg("bitangent search" + EdSegment.showDirected(a1, b1)
              + T.show("" + ac, null, a1.x + 3, a1.y + 3, TX_BGND, .7)
              + T.show("" + bc, null, b1.x + 3, b1.y + 3, TX_BGND, .7));

        if (ac == 3 && bc == 3)
          break;
        if (ac == 2)
          ai--;
        else if (ac < 2)
          ai++;
        else if (bc == 2)
          bi--;
        else
          bi++;

        ai = MyMath.mod(ai, pa.nPoints());
        bi = MyMath.mod(bi, pb.nPoints());
      }
    } while (false);
  }
  /**
   * Find vertex of a convex polygon that is tangent to the directed line from a
   * point outside the polygon to the vertex
   * @param poly array of FPoint2s forming a convex polygon
   * @param src point outside the polygon
   * @param rightTangent true if polygon is to be to the right of the tangent line,
   *  false if left
   * @return vertex, or null if failed (polygon isn't convex, or point is inside)
   */
  private static FPoint2 findTangentVertex(EdPolygon poly, FPoint2 src,
      boolean rightTangent) {

    poly = poly.getConvexHull();
    if (db && T.update())
      T.msg("findTangentVertex, rightTangent=" + Tools.f(rightTangent)
          + T.show(src) + T.show(poly, MyColor.cRED));

    int tv = -1;

    int seek = (rightTangent ? TCODE_RR : TCODE_LL);
    // int seek2 = rightTangent ? TCODE_RL : TCODE_LR;

    do {
      // start search with min and max vertices
      int vert1 = 0;
      int vert3 = poly.nPoints() - 1;

      // calc tangent codes for these vertices
      int tCode1 = tCode(src, poly, vert1, rightTangent);
      int tCode3 = tCode(src, poly, vert3, rightTangent);

      // if one of them is target, stop
      if (tCode1 == seek) {
        tv = vert1;
        break;
      }
      if (tCode3 == seek) {
        tv = vert3;
        break;
      }

      Inf inf = null;
      if (db)
        inf = new Inf("target", poly.nPoints());

      // repeat while more vertices to search
      while (vert1 != vert3) {
        if (db)
          inf.update();

        // find vertex that is halfway between them
        int vert2 = MyMath.mod(vert1
            + MyMath.mod(vert3 - vert1, poly.nPoints()) / 2, poly.nPoints());
        int tCode2 = tCode(src, poly, vert2, rightTangent);

        if (db && T.update())
          T.msg("v1=" + vert1 + ":" + tCode(tCode1) + " v2=" + vert2 + ":"
              + tCode(tCode2) + " v3=" + vert3 + ":" + tCode(tCode3));

        // if it's the target vertex, stop
        if (tCode2 == seek) {
          tv = vert2;
          break;
        }

        // determine which half has the target
        boolean side2;

        // if all three points' codes are equal, we must
        // disambiguate further
        if (tCode1 == tCode2 && tCode2 == tCode3) {
          FPoint2 loc1 = poly.getPoint(vert1);
          FPoint2 loc2 = poly.getPoint(vert2);
          boolean left = MyMath.sideOfLine(src, loc1, loc2) > 0;
          side2 = (tCode1 == TCODE_LR) ^ !left;
          if (db && T.update())
            T.msg("tCode1:" + tCode(tCode1) + " left=" + left
                + EdSegment.show(src, loc1) + T.show(loc2) + " side2=" + side2);
        } else
          side2 = (tCode2 != (seek ^ 1));

        if (!side2) {
          tCode3 = tCode2;
          vert3 = vert2;
        } else {
          tCode1 = tCode2;
          vert1 = vert2;
        }
      }
    } while (false);

    FPoint2 target = null;
    if (tv >= 0) {
      target = poly.getPoint(tv);
      if (db && T.update())
        T.msg("target" + EdSegment.show(src, target, MyColor.cRED, -1));
    }
    return target;
  }
  private static final int TCODE_LL = 0, TCODE_LR = 1, TCODE_RL = 2,
      TCODE_RR = 3;

  private static String tCode(int c) {
    return "LLLRRLRR".substring(c * 2, c * 2 + 2);
  }

  private static int tCode(FPoint2 srcVert, EdPolygon hull, int index,
      boolean rightTangent) {
    FPoint2 p1 = hull.getPointMod(index);
    FPoint2 p0 = hull.getPointMod(index - 1);
    FPoint2 p2 = hull.getPointMod(index + 1);
    int ret = 0;
    boolean nextLeft = MyMath.sideOfLine(srcVert, p1, p2) > 0;
    boolean prevLeft = MyMath.sideOfLine(srcVert, p1, p0) > 0;

    if (!prevLeft)
      ret |= 1;
    if (!nextLeft)
      ret |= 2;
    if (db && T.update())
      T.msg("tCode v#" + index + EdSegment.show(srcVert, p1)
          + EdSegment.show(srcVert, p0) + EdSegment.show(srcVert, p2)
          + T.show("p1:" + index, null, p1.x + 3, p1.y, 0)
          + T.show("p0:" + (index - 1), null, p0.x + 3, p0.y, 0)
          + T.show("p2:" + (index + 1), null, p2.x + 3, p2.y, 0)
          + "\nnextLeft=" + Tools.f(nextLeft) + " prevLeft="
          + Tools.f(prevLeft) + "\nret=" + tCode(ret)
          + T.show(tCode(ret), null, p1.x + 15, p1.y, 0));

    if (rightTangent) {
      if (ret == TCODE_LL)
        ret = TCODE_RL;
    } else {
      if (ret == TCODE_RR)
        ret = TCODE_RL;
    }
    return ret;
  }

  // ------------------------------------------------------------------------
  private void combinedHullTest() {
    do {
      EdPolygon[] p = UHullMain.getPolygons();
      if (p.length < 2)
        break;
      EdPolygon a = p[0].getConvexHull();
      EdPolygon b = p[1].getConvexHull();
      DArray hull = hullOfConvex(a.getPts(), b.getPts());
      T.show(new EdPolygon(hull), MyColor.cRED, -1, MARK_DISC);
    } while (false);
  }

  /**
   * Add point to hull list.
   * If point is same as last, does nothing.
   * If previous point lies between new and its previous, removes last.
   * @param pts list of hull points
   * @param pt new point
   * @param debugMsg for debugging only
   * @return true if point was not same as last but was same as first,
   *   i.e. hull is complete
   */
  private static boolean addPoint(DArray pts, FPoint2 pt, String debugMsg) {

    if (db && T.update())
      T.msg("adding point " + pt + T.show(pt) + ": " + debugMsg);
    boolean done = false;
    do {
      if (pts.size() > 1 && pt == pts.get(0)) {
        done = true;
        break;
      }

      if (!pts.isEmpty() && pt == pts.last())
        break;
      if (pts.size() >= 2) {
        FPoint2 p0 = (pts.size() >= 2) ? (FPoint2) pts.peek(1) : pt;
        FPoint2 p1 = (pts.size() >= 1) ? (FPoint2) pts.peek(0) : pt;
        double s = MyMath.sideOfLine(p0, p1, pt);
        //        if (db && T.update())
        //          T.msg("sideOfLine " + p0 + " " + p1 + " is " + s);
        if (s == 0) {
          pts.pop();
        }
      }
      pts.add(pt);
    } while (false);
    return done;
  }
  private static int rightMostVertex(List a) {
    int ex = -1;
    for (int i = 0; i < a.size(); i++) {
      if (ex < 0 || FPoint2.compareLex(pt(a, i), pt(a, ex), false) > 0)
        ex = i;
    }
    return ex;
  }

  /**
   * Determine convex hull of two convex polygons in linear time,
   * using rotating calipers
   * @param a first polygon
   * @param b second polygon
   * @return convex hull of the two
   */
  public static DArray hullOfConvex(List a, List b) {
    if (db && T.update())
      T.msg("hullOfConvex" + T.show(a, C1) + T.show(b, C2));
    DArray ret = new DArray();

    int aIndex = rightMostVertex(a);
    int bIndex = rightMostVertex(b);

    double theta = Math.PI / 2;
    FPoint2 aVert = pt(a, aIndex);
    FPoint2 bVert = pt(b, bIndex);
    LineEqn aLine = new LineEqn(aVert, theta);
    LineEqn bLine = new LineEqn(bVert, theta);

    boolean bActive = false;
    boolean bActiveDefined = false;

    if (a.size() < 3 || b.size() < 3)
      throw new IllegalArgumentException();

    Inf inf = null;
    if (db)
      inf = new Inf("hull", 100);

    while (true) {
      if (db)
        Inf.update(inf);

      // determine which is the limit pivot
      double anga = MyMath.polarAngle(aVert, pt(a, aIndex + 1));
      double angb = MyMath.polarAngle(bVert, pt(b, bIndex + 1));
      double aDiff = MyMath.normalizeAngle(anga - angb);
      boolean bIsPivot = aDiff > 0;

      if (bIsPivot) {
        theta = angb;
        bIndex++;
        bVert = pt(b, bIndex);
      } else {
        theta = anga;
        aIndex++;
        aVert = pt(a, aIndex);
      }

      aLine = new LineEqn(aVert, theta);
      bLine = new LineEqn(bVert, theta);

      if (db) {
        trace.clear();
        T.make(trace, aLine, C1);
        T.make(trace, bLine, C2);
        T.make(trace, aVert, C1);
        T.make(trace, bVert, C2);
        T.make(trace, EdPolygon.constructPath(ret), MyColor.cRED, STRK_THICK,
            -1);

        if (db && T.update())
          T.msg("combining hulls");
      }
      //      + T.show(aLine, C1) + T.show(bLine, C2)
      //            + T.show(aVert, C1) + T.show(bVert, C2) + "\naLine=" + aLine
      //            + "\nbLine=" + bLine);

      int bSide = aLine.sideOfLine(bVert);
      boolean bActiveNew = bSide < 0;
      if (bSide == 0)
        bActiveNew = bVert.y > aVert.y;

      if (!bActiveDefined) {
        bActive = bActiveNew;
        bActiveDefined = true;
      }

      if (bActiveNew != bActive) {
        bActive = bActiveNew;
        if (bIsPivot == bActive)
          if (addPoint(ret, bIsPivot ? pt(b, bIndex - 1) : pt(a, aIndex - 1),
              "crossover aux"))
            break;
        if (addPoint(ret, bActive ? pt(b, bIndex) : pt(a, aIndex), "crossover"))
          break;
      }

      if (bIsPivot == bActive)
        if (addPoint(ret, bIsPivot ? bVert : aVert, "right pivot"))
          break;

    }
    trace.clear();
    //    if (db && T.update())
    //      T.msg(" hullOfConvex" + T.show(a, C1) + T.show(b, C2) + " returning"
    //          + T.show(ret, MyColor.cRED));
    return ret;
  }

  // ------------------------------------------------------------------------
  private void hullOfPolyTest() {
    do {
      EdPolygon[] p = UHullMain.getPolygons();
      if (p.length < 1)
        break;
      EdPolygon a = p[0];
      DArray h = convexHullOfPoly(a.getPts());
      EdPolygon b = new EdPolygon();
      for (int i = 0; i < h.size(); i++)
        b.addPoint(a.getPoint(h.getInt(i)));
      T.show(b, MyColor.cRED, -1, MARK_DISC);
    } while (false);
  }

  public void runAlgorithm() {
    trace = new DArray();
    switch (C.vi(OPER)) {
    case 0:
      logSearchTest();
      break;
    case 1:
      combinedHullTest();
      break;
    case 2:
      hullOfPolyTest();
      break;
    case 3:
      kernelTest();
      break;
    case 4:
      bitangentSearch();
      break;
    }
  }

  // ------------------------------------------------------------------------
  private static void show(List polygon, DQueue q) {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    DArray a = new DArray();
    for (int i = 0; i < q.size(); i++) {
      int index = q.peekInt(i, false);
      sb.append(' ');
      sb.append(index);

      FPoint2 pt = (FPoint2) polygon.get(index);
      a.add(pt);
      T.make(trace, "" + index, MyColor.cRED, pt.x + 4, pt.y, TX_FRAME);
    }
    sb.append("]");

    T.make(trace, EdPolygon.constructPath(a), MyColor.cPURPLE, STRK_THICK,
        MARK_DISC);
    T.make(trace, sb.toString(), MyColor.cRED, 0, 0, TX_CLAMP);
  }

  /**
   * Note: takes index mod size
   * @param poly
   * @param index
   * @return
   */
  private static FPoint2 pt(List poly, int index) {
    return (FPoint2) poly.get(MyMath.mod(index, poly.size()));
  }
  private static int bottom(DQueue q, int dist) {
    return q.peekInt(dist, false);
  }
  private static int top(DQueue q, int dist) {
    return q.peekInt(dist, true);
  }

  private static boolean left(FPoint2 p0, FPoint2 p1, FPoint2 p2) {
    return MyMath.sideOfLine(p0, p1, p2) > 0;
  }

  /**
   * Compute the convex hull of a simple polygon, in linear time,
   * using Melkman's algorithm.
   * 
   * @param polygon list of FPoint2's
   * @return indexes of points on convex hull
   */
  public static DArray convexHullOfPoly(List polygon) {
    final boolean db = true;
    DQueue q = new DQueue();

    int size = polygon.size();

    // if < 3 vertices, polygon is already convex, and has ccw winding
    if (size <= 3) {
      for (int i = 0; i < polygon.size(); i++)
        q.push(i);
    } else {
      if (left(pt(polygon, 0), pt(polygon, 1), pt(polygon, 2))) {
        q.push(2);
        q.push(1);
        q.push(0);
        q.push(2);
      } else {
        q.push(2);
        q.push(0);
        q.push(1);
        q.push(2);
      }

      for (int j = 3; j < polygon.size(); j++) {

        FPoint2 vi = pt(polygon, j);
        FPoint2 dt1 = pt(polygon, top(q, 1));
        FPoint2 dt0 = pt(polygon, top(q, 0));
        FPoint2 db0 = pt(polygon, bottom(q, 0));
        FPoint2 db1 = pt(polygon, bottom(q, 1));

        if (db) {
          trace.clear();
          T.make(trace, new LineEqn(dt1, dt0), MyColor.cBLUE, STRK_THICK, -1);
          T.make(trace, new LineEqn(db1, db0), MyColor.cDARKGREEN, STRK_THICK,
              -1);
          show(polygon, q);
          T.make(trace, vi);
        }

        if (db && T.update())
          T.msg("convexHullOfPoly, j=" + j);
        if (left(dt1, dt0, vi) && left(vi, db0, db1))
          continue;

        while (!left(dt1, dt0, vi)) {
          int k = q.popInt(true);
          if (db && T.update())
            T
                .msg("popped " + k + " from top of queue"
                    + T.show(pt(polygon, k)));
          dt0 = dt1;
          dt1 = pt(polygon, top(q, 1));
        }
        q.push(j, true);

        while (!left(vi, db0, db1)) {
          int k = q.popInt(false);
          if (db && T.update())
            T.msg("popped " + k + " from bottom of queue"
                + T.show(pt(polygon, k)));
          db0 = db1;
          db1 = pt(polygon, bottom(q, 1));
        }
        q.push(j, false);
      }
    }
    trace.clear();

    // pop the duplicate vertex
    q.pop(false);

    DArray ret = new DArray();
    while (!q.isEmpty())
      ret.add(q.pop(false));

    return ret;
  }
  // ------------------------------------------------------------------------

  private static DArray trace;
}
