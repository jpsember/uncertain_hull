package uhull;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import base.*;
import testbed.*;
import uhull.SegEnvOper.*;

public class COper3 implements TestBedOperation, Globals {
  /*! .enum  .private 5300  startnode _ _ _ _ chainmode
    numedges dbfull
  */

  private static final int STARTNODE = 5300;//!
  private static final int CHAINMODE = 5305;//!
  private static final int NUMEDGES = 5306;//!
  private static final int DBFULL = 5307;//!
  /*!*/

  private COper3() {
  }
  public static COper3 singleton = new COper3();

  public void addControls() {
    C.sOpenTab("Poly+Pt");
    {
      C.sStaticText(//
      "Calculate possible hull of polygon and point");
      C.sCheckBox(DBFULL, "trace more", "add more tracing information", false);
      C.sCheckBox(CHAINMODE, "chain mode",
          "extract polygonal chain from polygon", false);
      C.sIntSpinner(NUMEDGES, "chain edges",
          "maximum number of edges (only if in chain mode)", 0, 50, 10, 1);
      C.sIntSpinner(STARTNODE, "start vertex", "select starting vertex", 0, 20,
          0, 1);
    }

    C.sCloseTab();
  }
  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      }
    }
  }

  /**
   * Build linked list of points from polygon
   * @param poly source polygon
   * @return rightmost vertex
   */
  private static PtEntry build(EdPolygon poly) {

    PtEntry ent = new PtEntry(poly.getPoint(0));
    ent.setSource(poly);
    PtEntry.join(ent, ent);

    for (int i = 1; i < poly.nPoints(); i++) {
      PtEntry ent2 = new PtEntry(poly.getPoint(i));
      ent2.setSource(poly);
      ent = ent.insert(ent2, true);
    }

    // use rightmost entry as base
    {
      PtEntry first = ent;
      for (PtEntry ent2 = ent.next(true); ent2 != first; ent2 = ent2.next(true)) {
        if (ent2.x > ent.x)
          ent = ent2;
      }
    }

    // choose start node
    int k = C.vi(STARTNODE);
    while (k-- > 0)
      ent = ent.next(true);

    // only limit edges if chain mode set
    if (C.vb(CHAINMODE)) {
      PtEntry ent2 = ent;
      int i = C.vi(NUMEDGES);
      if (i <= 0)
        i = -1;

      while (true) {
        if (ent2.next(true) == ent || i == 0) {
          ent2.setNext(null);
          ent.setPrev(null);

          if (i != 0) {
            // create a copy of the final entry
            FPoint2 dup = new FPoint2(ent);
            PtEntry.join(ent2, new PtEntry(dup));
          }
          break;
        }
        ent2 = ent2.next(true);
        i--;
      }
    }
    return ent;
  }

  public void runAlgorithm() {
    dbClear();

    EdPolygon origPoly = null;
    FPoint2 kernelPt = null;

    DArray a = Editor.readObjects(null, false, true);
    for (int i = 0; i < a.size(); i++) {
      EdObject obj = (EdObject) a.get(i);
      if (origPoly == null && obj instanceof EdPolygon)
        origPoly = (EdPolygon) obj;
      if (kernelPt == null && obj instanceof EdPoint)
        kernelPt = obj.getPoint(0);
    }
    if (origPoly == null || kernelPt == null)
      return;
    PtEntry ent = build(origPoly);
    if (C.vb(CHAINMODE)) {
      tracePoly = ent;
    }
    traceKernel = kernelPt;

    PtEntry hullPt = buildHullAux(ent, kernelPt);
    T.show(hullPt.toPolygon(), MyColor.cDARKGREEN, STRK_THICK, -1);
  }

  public void paintView() {

    EdPolygon p = null;
    //    if (oldContour != null) {
    //      p = oldContour.toPolygon();
    //      p.fill(MyColor.cLIGHTGRAY);
    //    }

    if (tracePoly == null) {
      Editor.render();
    } else {
      if (traceKernel != null)
          T.render(traceKernel);
     T.render(tracePoly.toPolygon()); //new MyPolygon(tracePoly));
    }

    dbPaint();
    if (p != null)
      T.render(p, Color.black);
  }

  public static void dbClear() {
    traceCEntrySet = null;
    traceStart = null;
    tracePoly = null;
    traceKernel = null;
  }

  public static void dbPaint() {
    if (traceCEntrySet != null)
      PtEntry.renderSet(traceKernel, traceCEntrySet);
    if (traceStart != null) {
      V.pushColor(MyColor.cDARKGREEN);
      V.mark(traceStart, MARK_CIRCLE, .7);
      V.pop();
    }
  }

  /**
   * Build hull given loop of points
   * @param handle one of the points in the loop
   * @param kernelPt
   * @return one of the points in the hull
   */
  public static PtEntry buildHullForChain(PtEntry handle, FPoint2 kernelPt) {
    return buildHullAux(handle, kernelPt);
  }

  public static boolean right(FPoint2 s0, FPoint2 s1, FPoint2 pt, boolean ccw) {
    if (ccw)
      return right(s0, s1, pt);
    else
      return left(s0, s1, pt);
  }

  public static boolean left(FPoint2 s0, FPoint2 s1, FPoint2 pt, boolean ccw) {
    if (ccw)
      return left(s0, s1, pt);
    else
      return right(s0, s1, pt);
  }

  private static boolean right(FPoint2 s0, FPoint2 s1, FPoint2 pt) {
    return MyMath.sideOfLine(s0, s1, pt) < -EPS;
  }

  private static boolean left(FPoint2 s0, FPoint2 s1, FPoint2 pt) {
    return MyMath.sideOfLine(s0, s1, pt) > EPS;
  }
  private static final double EPS = 1e-3;
  private static boolean samePoint(FPoint2 p1, FPoint2 p2) {
    return FPoint2.distanceSquared(p1, p2) < EPS * EPS;
  }
  private static String tl(FPoint2 rayStart, FPoint2 rayEnd) {
    return EdSegment.showDirected(rayStart, rayEnd, null, -1);
  }
  private static String tp(FPoint2 pt) {
    if (pt == null)
      return "<null>";
    return T.show(pt, null, -1, MARK_DISC);
  }
  public static FPoint2 segIntersection(FPoint2 s0, FPoint2 s1, FPoint2 t0,
      FPoint2 t1, boolean mustExist) {
    FPoint2 isect = MyMath.lineSegmentIntersection(s0, s1, t0, t1, null);

    if (isect == null && mustExist)
      T.err("can't find intersection");
    if (isect != null) {
      if (FPoint2.distanceSquared(isect, s0) < EPS * EPS)
        isect = s0;
      else if (FPoint2.distanceSquared(isect, s1) < EPS * EPS)
        isect = s1;
    }
    return isect;
  }

  private static PtEntry buildHullAux(PtEntry anyEntry, FPoint2 kernelPt) {

    final boolean db = true;
    final boolean db2 = C.vb(DBFULL);

    Inf inf = new Inf();

    PtEntry startEnt = anyEntry;
    traceStart = startEnt;

    // u will be the point on the current edge
    FPoint2 u = startEnt;

    // v will be the endpoint of the current edge
    PtEntry v = startEnt.next(true);

    if (db && T.update())
      T.msg("buildHull begins" + tl(u, v));

    PtEntry c = null;
    boolean prevWindDir = false;
    boolean windDir = false;

    while (true) {
      inf.update();

      if (c == null) {
        // 1) initialize

        // construct initial hull list from first edge, and kernel point
        c = constructSet(kernelPt, u, v);
        windDir = left(u, v, kernelPt);
        u = v;

        traceCEntrySet = c;

        if (db && T.update())
          T.msg("initialized hull" + tl(u, v.next(true)));
      }

      // calculate endpoint of current edge, if necessary
      if (u == v)
        v = v.next(true);

      // 2) are we done?
      if (v == null || samePoint(u, startEnt))
        break;

      // 3) update winding direction
      prevWindDir = windDir;
      windDir = left(u, v, kernelPt);

      // 4) does P enter the interior of H at u?
      if (!right(u, v, c.next(windDir), windDir)) {

        // 5)
        if (db && T.update())
          T.msg("outside hull" + tl(u, v) + T.show(c));

        PtEntry prev = c;
        PtEntry curr = c.next(windDir);
        while (true) {
          inf.update();

          // is chain still within triangle?
          if ((!right(kernelPt, u, curr, windDir)
              && !right(u, v, curr, windDir) && !right(v, kernelPt, curr,
              windDir))) {

            if (db2 && T.update())
              T.msg("vertex was within triangle" + T.show(curr)
                  + tl(kernelPt, u) + tl(kernelPt, v));
            prev = curr;
            curr = prev.next(windDir);

            // case (i)
            // if we have wrapped around to the first entry, then
            // the entire hull is invalid; throw it out, and build
            // a new, triangular hull from this segment only
            if (curr == c) {
              c = null;
              break;
            }

          } else

          // determine if old endpoint is off the left side of the triangle
          // (add projection to old edge)
          // or above the top (enter radial edge).

          // It is a radial edge only if it is not off to either of the radial sides
          // of the triangle.

          if (!right(v, kernelPt, curr, windDir)
              && !right(kernelPt, u, curr, windDir)) {

            // case (iii)
            FPoint2 crossPt = segIntersection(u, v, prev, curr, true);

            while (c.next(windDir) != curr) {
              if (db2 && T.update())
                T.msg("deleting shadowed vertex" + T.show(c.next(windDir)));
              c.next(windDir).delete(windDir);
              // in case we deleted the trace handle, reset it
              traceCEntrySet = c;
            }

            c = c.insert(new PtEntry(crossPt), windDir);
            if (db2 && T.update())
              T.msg("extending to enter point");

            u = crossPt;
            break;
          } else {
            // case (ii)
            FPoint2 crossPt = segIntersection(kernelPt, v, prev, curr, true);

            if (db2 && T.update())
              T.msg("found projection to old edge" + tl(u, v) + tl(v, crossPt)
                  + tp(crossPt));
            u = v;
            while (c.next(windDir) != curr) {
              if (db2 && T.update())
                T.msg("deleting shadowed vertex" + T.show(c.next(windDir)));
              c.next(windDir).delete(windDir);
              traceCEntrySet = c;
              }
            c = c.insert(new PtEntry(u), windDir);
            c.insert(new PtEntry(crossPt), windDir);

            if (db && T.update())
              T.msg("extending hull" + tp(crossPt));
            break;

          }
        }
      } else {

        // 6) traversing interior of hull

        // if direction just changed, gate we entered
        // starts at previous vertex.
        if (windDir != prevWindDir)
          c = c.prev(windDir);

        if (db && T.update())
          T.msg("entering hull" + T.show(c) + tl(u, v));

        PtEntry altGate = null;
        if (samePoint(c, kernelPt))
          altGate = c.prev(windDir);
        else if (samePoint(c.next(windDir), kernelPt))
          altGate = c.next(windDir);

        if (db && T.update()) {
          if (altGate == null)
            T.msg("exit edge" + T.show(c) + tl(c, c.next(windDir)));
          else
            T.msg("exit edge" + T.show(c) + tl(c, c.next(windDir))
                + " and alternate" + T.show(altGate)
                + tl(altGate, altGate.next(windDir)));
        }

        // find point where boundary of polygon exits gate, or stop if return to start
        // vertex without exiting
        while (true) {
          inf.update();

          if (u == v)
            v = v.next(true);

          if (v == null || u == startEnt)
            break;

          // find point where crossing primary or alternate exit gates
          FPoint2 bestPt = null;
          double bestDist = 0;
          PtEntry bestGate = null;
          for (int gateIndex = 0; gateIndex < 2; gateIndex++) {
            PtEntry gate = (gateIndex == 0) ? c : altGate;
            if (gate == null)
              continue;

            FPoint2 crossPt = segIntersection(gate, gate.next(windDir), u, v,
                false);
            // ignore crossing point of p, since this is where we entered gate
            if (crossPt == null || samePoint(crossPt, u))
              continue;

            double crossDist = FPoint2.distance(crossPt, u);
            if (bestPt == null || crossDist < bestDist) {
              bestDist = crossDist;
              bestPt = crossPt;
              bestGate = gate;
            }
          }
          if (bestGate == null) {
            if (db && T.update())
              T.msg("still inside hull" + tl(u, v));
            u = v;
            continue;
          }

          u = bestPt;
          c = bestGate;
          if (samePoint(u, startEnt)) {
            if (db && T.update())
              T.msg("leaving hull interior at starting point" + T.show(c));
          } else {
            c = c.insert(new PtEntry(u), windDir);
            if (db && T.update())
              T.msg("leaving hull interior" + T.show(c));
          }
          break;
        }
      }
    }
    dbClear();

    // delete last point if same as first, in case we're
    // in polygon mode, or building a chain that loops.
    {
      if (samePoint(c, c.next(windDir))) {
        if (db2 && T.update())
          T.msg("removing last vertex, same as first");
        c = c.delete(windDir);
        traceCEntrySet = c;
        }
    }
    return c;
  }

  /**
  * Build an initial loop of entries for polygon
  * @param kernelPt 
  * @param p0 initial edge of polygon
  * @param p1
  * @return entry corresponding to p0
  */
  private static PtEntry constructSet(FPoint2 kernelPt, FPoint2 p0, FPoint2 p1) {
    final boolean db = false;
    boolean ccw = left(kernelPt, p0, p1);
    if (!ccw) {
      FPoint2 temp = p0;
      p0 = p1;
      p1 = temp;
    }
    PtEntry v0 = new PtEntry(p0);
    PtEntry.join(v0, v0);

    PtEntry v1 = v0.insert(new PtEntry(p1), true);
    if (db && T.update())
      T.msg("construct set, v0=" + v0 + " v1=" + v1);
    v1.insert(new PtEntry(kernelPt), true);

    return ccw ? v1 : v0;
  }

  // if not null, CEntry set to render
  public static PtEntry traceCEntrySet;
  private static FPoint2 traceKernel;
  private static FPoint2 traceStart;
  private static PtEntry tracePoly;
}