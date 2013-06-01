package uhull;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import base.*;
import testbed.*;
import uhull.SegEnvOper.*;

public class ValleyOper2 implements TestBedOperation, Globals {
  /*! .enum  .public 4850 db_initialhull oldalg db_insertvalley 
     _ _ db_contour _ skipcontour db_hullexpand oldmethod kernelvert
  */

    public static final int DB_INITIALHULL   = 4850;//!
    public static final int OLDALG           = 4851;//!
    public static final int DB_INSERTVALLEY  = 4852;//!
    public static final int DB_CONTOUR       = 4855;//!
    public static final int SKIPCONTOUR      = 4857;//!
    public static final int DB_HULLEXPAND    = 4858;//!
    public static final int OLDMETHOD        = 4859;//!
    public static final int KERNELVERT       = 4860;//!
/*!*/

  private ValleyOper2() {
  }
  public static ValleyOper2 singleton = new ValleyOper2();

  public void addControls() {
    C.sOpenTab("Valley");
    {
      C.sStaticText(//
          "Generate possible hull in O(n log k) time, using 'valley' algorithm");
      C.sCheckBox(DB_INITIALHULL, "db initial hull",
          "trace construction of initial convex hull of the two polygons",
          false);
      C.sCheckBox(DB_CONTOUR, "db pt/poly",
          "trace point / polygon hull construction", false);
      C.sCheckBox(DB_INSERTVALLEY, "db insert valley",
          "trace insertion of monotonic valley edges", false);
      C.sCheckBox(DB_HULLEXPAND, "db expand", "trace hull expansion procedure",
          false);
      C.sCheckBox(SKIPCONTOUR, "skip contour",
          "use original polygons and not their contours\n"
              + "(required to ensure monotonicity of tangent searches)", false);
      C.sCheckBox(OLDALG, "simple", "plot possible hull using old algorithm",
          false);
      C.sCheckBox(OLDMETHOD, "old", "use old code", false);
      C
          .sIntSpinner(
              KERNELVERT,
              "kernel vertex",
              "specifies which vertex of the other polygon to use as point in pt/poly step",
              0, 50, 0, 1);
    }

    C.sCloseTab();
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      }
    }
  }

  public void runAlgorithm() {
    //  traceHull = null;
    COper3.dbClear();

    traceHullPt = null;
    traceValley = null;
    simple = null;
    focusPt = null;
    possHull = null;
    valleyTri = null;
    PtEntry.resetIds();

    EdPolygon[] rgn = UHullMain.getPolygons();

    if (rgn.length < 2)
      return;

    if (C.vb(OLDALG)) {
      T.disable();
      simple = PossHullOper.buildPossibleHull(rgn);
      T.enable();
    }

    possHull = calcPHullRange(rgn, 0, rgn.length - 1);
  }

  /**
   * Calc possible hull from a set of polygons
   * @param polySet set of polygons
   * @param first first polygon to include
   * @param last last polygon to include
   * @return polygon containing possible hull
   */
  private static EdPolygon calcPHullRange(EdPolygon[] polySet, int first,
      int last) {
    final boolean db = true;
    int len = last + 1 - first;
    if (len == 1)
      return polySet[first];
    if (len > 2) {
      if (db && T.update()) {
        DArray a = new DArray();
        for (int i = first; i <= last; i++)
          a.add(polySet[i]);
        T.msg("calc possible hull of multiple polygons"
            + T.show(a, MyColor.cRED, STRK_THICK, -1));
      }

      int n = len / 2;
      return calcPHull(calcPHullRange(polySet, first, first + n - 1),
          calcPHullRange(polySet, first + n, last));
    }

    return calcPHull(polySet[first], polySet[first + 1]);
  }

  /**
   * Calculate possible hull of a pair of polygons
   * @param a
   * @param b
   * @return possible hull
   */
  private static EdPolygon calcPHull(EdPolygon aPoly, EdPolygon bPoly) {
    final boolean db = true;

    if (db && T.update())
      T.msg("calc possible hull of two polygons"
          + T.show(aPoly, MyColor.cBLUE, STRK_THICK, -1)
          + T.show(bPoly, MyColor.cDARKGREEN, STRK_THICK, -1));
    PtEntry a = PtEntry.buildFrom(aPoly);
    PtEntry b = PtEntry.buildFrom(bPoly);

    PtEntry aHull = PtEntry.convexHull(a);
    PtEntry bHull = PtEntry.convexHull(b);

    inf = Inf.create();

    if (db && T.update()) {
      T.msg("convex hulls"
          + T.show(aHull.toPolygon(), MyColor.cBLUE, STRK_THICK, -1)
          + T.show(bHull.toPolygon(), MyColor.cDARKGREEN, STRK_THICK, -1));
    }

    PtEntry ph = hullOfPolygons(a, b, aHull, bHull);
    traceHullPt = ph;

    insertValleys(ph, a.source(), b.source());

    expandHull(ph, aHull, bHull, true);
    expandHull(ph, aHull, bHull, false);

    return ph.toPolygon();
  }

  /**
   * Apply hull expansion procedure
   * @param convHullEntry an entry of the hull (should be on the convex hull, so it is not
   *   deleted or replaced and is still valid for subsequent calls)
   * @param aHull entry on convex hull of polygon A
   * @param bHull entry on convex hull of polygon B
   * @param ccw true to move in ccw direction, else cw
   */
  private static void expandHull(PtEntry convHullEntry, PtEntry aHull,
      PtEntry bHull, boolean ccw) {
    if (C.vb(OLDMETHOD)) {
      OLDexpandHull(convHullEntry, aHull, bHull, ccw);
      return;
    }
    boolean db = C.vb(DB_HULLEXPAND);

    if (db && T.update())
      T.msg("expandHull" + T.show(convHullEntry) + " ccw=" + ccw);

    // tangent points for A, B
    PtEntry[] tangentPoints = new PtEntry[2];
    tangentPoints[0] = aHull;
    tangentPoints[1] = bHull;

    PtEntry hEnt = convHullEntry;
    do {
      inf.update();

      // calculate tangent ray R
      PtEntry tangentPt = null;
      while (true) {
        int tanIndex = (hEnt.source() == tangentPoints[0].source()) ? 1 : 0;
        tangentPt = tangentPoints[tanIndex];

        if (!COper3.right(hEnt, tangentPt, tangentPt.next(true), ccw)
            && !COper3.right(hEnt, tangentPt, tangentPt.prev(true), ccw))
          break;

        tangentPt = tangentPt.next(ccw);

        if (db && T.update())
          T.msg("expandHull, advance tangent line"
              + T.show(tangentPt.toPolygon(), MyColor.cDARKGRAY, -1, MARK_DISC)
              + T.show(hEnt) + tl(hEnt, tangentPt));
        tangentPoints[tanIndex] = tangentPt;
      }

      if (COper3.left(hEnt, tangentPt, hEnt.next(ccw), ccw)) {
        DArray dispPts = new DArray();

        // delete points until cross tangent line
        PtEntry next = hEnt.next(ccw);
        while (true) {
          PtEntry prev = next;
          dispPts.add(prev);
          next = prev.delete(ccw);
          if (COper3.right(hEnt, tangentPt, next, ccw)) {
            FPoint2 cross = MyMath.linesIntersection(hEnt, tangentPt, prev,
                next, null);
            hEnt = hEnt.insert(new PtEntry(cross), ccw);
            if (db && T.update())
              T.msg("expandHull, clipped to shadow region"
                  + tl(hEnt, tangentPt) + T.show(hEnt) + T.show(dispPts));
            break;
          }
        }
      } else {
        if (db && T.update())
          T.msg("expandHull, not dipping into shadow region"
              + T.show(hEnt.next(ccw)) + tl(hEnt, tangentPt));
      }
      while (true) {
        hEnt = hEnt.next(ccw);
        if (COper3.left(hEnt.prev(ccw), hEnt, hEnt.next(ccw), ccw))
          break;
        if (db && T.update())
          T.msg("skipping reflex vertex" + T.show(hEnt));
      }
    } while (hEnt != convHullEntry);
  }

  private static void insertValleys(PtEntry hullPt, Object aSrc, Object bSrc) {// pa, MyPolygon pb) {
    boolean db = C.vb(DB_INSERTVALLEY);
    PtEntry ent = hullPt;

    if (db && T.update())
      T.msg("insertValleys");
    do {
      PtEntry next = ent.next(true);

      if (ent.source() == next.source()) {
        PtEntry orig = ent.orig();

        if (orig.next(true) != next.orig()) {

          PtEntry vPeak0 = ent;
          PtEntry vPeak1 = next;

          EdPolygon opp = (EdPolygon) (vPeak0.source() == aSrc ? bSrc : aSrc);

          FPoint2 kernelPt = opp.getPointMod(C.vi(KERNELVERT));

          // construct a chain from the vertices of the valley
          PtEntry handle = new PtEntry(vPeak0);
          PtEntry hNext = handle;
          PtEntry e = vPeak0.orig();
          while (e != vPeak1.orig()) {
            inf.update();
            e = e.next(true);
            hNext = hNext.insert(new PtEntry(e), true);
          }

          if (C.vb(SKIPCONTOUR)) {
            PtEntry h0 = handle.next(true);
            PtEntry h1 = hNext.prev(true);
            PtEntry.join(vPeak0, h0);
            PtEntry.join(h1, vPeak1);
            if (db && T.update())
              T.msg("inserted unmodified valley" + T.show(vPeak0)
                  + T.show(vPeak1));

          } else {
            if (!C.vb(DB_CONTOUR))
              T.disable();
            PtEntry hull = COper3.buildHullForChain(handle, kernelPt);
            if (!C.vb(DB_CONTOUR))
              T.enable();
            // find entries corresponding to start, end of hull
            PtEntry peak0 = null, peak1 = null;
            {
              PtEntry hEnt = hull;
              while (peak0 == null || peak1 == null) {
                inf.update();
                if (hEnt.orig() == vPeak0.orig())
                  peak0 = hEnt;
                if (hEnt.orig() == vPeak1.orig())
                  peak1 = hEnt;
                hEnt = hEnt.next(true);
              }
            }
            PtEntry.join(vPeak0, peak0.next(true));
            PtEntry.join(peak1.prev(true), vPeak1);
            if (db && T.update())
              T.msg("inserted monotonic valley" + T.show(vPeak0)
                  + T.show(vPeak1));
          }
        }
      }
      ent = next;
    } while (ent != hullPt);

  }
  public void paintView() {
    if (simple != null)
      simple.fill(MyColor.cLIGHTGRAY);

    Editor.render();
    //T.show(traceHull);

    COper3.dbPaint();

    if (traceHullPt != null) {
      renderHull(traceHullPt);
    }

    T.show(possHull, MyColor.cDARKGREEN, STRK_THICK, -1);
    T.show(valleyTri, MyColor.cDARKGREEN, STRK_RUBBERBAND, -1);
    T.show(traceValley, null, -1, -1);
    T.show(focusPt, MyColor.cRED, -1, MARK_DISC);
  }

  private static void renderHull(PtEntry pt) {
    Inf inf = Inf.create();
    int count = 30;
    PtEntry ptStart = pt;
    do {
      inf.update();
      if (pt.prev(true) != null) {
        boolean valley = false;

        valley = (pt.prev(true).source() == pt.source() && pt.prev(true).orig()
            .next(true) != pt.orig());

        V.pushColor(MyColor.cBLUE);
        V.pushStroke(valley ? STRK_RUBBERBAND : STRK_THICK);
        V.drawLine(pt.prev(true), pt);
        V.pop(2);
      }
      V.pushColor(MyColor.cDARKGREEN);
      V.mark(pt, MARK_DISC, .6);
      V.pop();
      if (Editor.withLabels(true)) {
        StringBuilder sb = new StringBuilder();
        sb.append(" #" + pt.id());
        if (pt.source() != null)
          sb.append(" <" + pt.source() + ">");

        V.pushScale(.6);
        V.draw(sb.toString(), MyMath.ptOnCircle(pt, MyMath.radians(30), 3),
            TX_FRAME | TX_BGND);
        V.pop();
      }
      pt = pt.next(true);
      if (count-- == 0)
        V.draw("too many points rendering!", 50, 50, TX_CLAMP);
    } while (pt != ptStart && count > 0);
  }

  private static String tr(FPoint2 rayStart, double theta) {
    return tl(rayStart, MyMath.ptOnCircle(rayStart, theta, 20));
  }
  private static String tp(FPoint2 p) {
    return T.show(p);
  }
  private static String tl(FPoint2 rayStart, FPoint2 rayEnd) {
    return EdSegment.showDirected(rayStart, rayEnd, null, -1);
  }

  private static PtEntry rightMostVertex(PtEntry p) {
    PtEntry first = p;
    PtEntry rightMost = p;
    while (true) {
      p = p.next(true);
      if (p == first)
        break;
      if (p.x > rightMost.x)
        rightMost = p;
    }
    return rightMost;
  }

  /**
   * Determine convex hull of two polygons, using rotating calipers method
   * @param pa first polygon
   * @param pb second polygon
   * @return convex hull structure
   */
  private static PtEntry hullOfPolygons(PtEntry pa, PtEntry pb, PtEntry aHull,
      PtEntry bHull) {

    boolean db = C.vb(DB_INITIALHULL);

    if (db && T.update())
      T.msg("construct convex hull of polygons"
          + T.show(pa, MyColor.cBLUE, STRK_THICK, -1)
          + T.show(pb, MyColor.cDARKGREEN, STRK_THICK, -1));

    PtEntry hullVertex = null;

    // A hull vertex and index
    PtEntry av = rightMostVertex(aHull);

    // B hull vertex and index
    PtEntry bv = rightMostVertex(bHull);

    double theta = Math.PI / 2;

    LineEqn aLine = new LineEqn(av, theta);

    int bSide = aLine.sideOfLine(bv);
    boolean bActive = (bSide == 0) ? (bv.y > av.y) : bSide < 0;

    if (db && T.update())
      T.msg("rightmost vertices" + T.show(av) + T.show(bv));

    // construct initial vertex of hull 
    hullVertex = new PtEntry(!bActive ? av : bv);

    //    if (db && T.update())
    //      T.msg("constructed initial hull vertex: " + hullVertex);

    PtEntry.join(hullVertex, hullVertex);
    PtEntry firstEnt = hullVertex;

    while (true) {
      Inf.update(inf);

      PtEntry av2 = av.next(true);
      PtEntry bv2 = bv.next(true);

      // next vertex is either A advance, B advance, or bridge
      double anga = MyMath.polarAngle(av, av2);
      double angb = MyMath.polarAngle(bv, bv2);

      double angBridge = bActive ? MyMath.polarAngle(bv, av) : MyMath
          .polarAngle(av, bv);

      double ta = MyMath.normalizeAnglePositive(anga - theta);
      double tb = MyMath.normalizeAnglePositive(angb - theta);
      double tc = MyMath.normalizeAnglePositive(angBridge - theta);

      // precision problem: if A and B tangent lines are parallel, both can
      // reach near zero simultaneously

      final double MAX = Math.PI * 2 - 1e-3;
      if (ta >= MAX)
        ta = 0;
      if (tb >= MAX)
        tb = 0;
      if (tc >= MAX)
        tc = 0;

      theta += Math.min(ta, Math.min(tb, tc));

      if (db && T.update())
        T.msg("caliper" + T.show(hullVertex) + tr(hullVertex, theta) + tp(av)
            + tp(bv));

      PtEntry newPoint = null;

      if (ta <= tb && ta <= tc) {
        if (db && T.update())
          T.msg("A vertex is nearest" + tl(av, av2));
        // ai++;
        av = av2;
        if (!bActive)
          newPoint = av;
      } else if (tb <= ta && tb <= tc) {
        if (db && T.update())
          T.msg("B vertex is nearest" + tl(bv, bv2));
        //bi++;
        bv = bv2;
        if (bActive)
          newPoint = bv;
      } else {
        if (db && T.update())
          T.msg("Bridge vertex is nearest"
              + tl(bActive ? bv : av, bActive ? av : bv));
        bActive ^= true;
        newPoint = bActive ? bv : av;
      }

      if (newPoint != null) {
        if (PtEntry.samePoint(newPoint, firstEnt)) {
          break;
        }

        // construct new vertex for hull of the two;
        // remember, use original vertex, not the convex hull 
        hullVertex = hullVertex.insert(new PtEntry(newPoint), true);
        if (db && T.update())
          T.msg("adding new caliper vertex " + T.show(hullVertex));
      }
    }
    return hullVertex;
  }

  private static void OLDexpandHull(PtEntry convHullEntry, PtEntry aHull,
      PtEntry bHull, boolean ccw) {
    boolean db__OLD = C.vb(DB_HULLEXPAND);
    if (db__OLD && T.update())
      T.msg("expandHull" + T.show(convHullEntry) + " ccw=" + ccw);

    PtEntry[] opp = new PtEntry[2];
    opp[0] = aHull;
    opp[1] = bHull;

    PtEntry old____hEnt = convHullEntry;
    boolean advanced = false;
    do {
      if (old____hEnt != convHullEntry)
        advanced = true;
      inf.update();
      if (old____hEnt.source() == null) {
        if (db__OLD && T.update())
          T.msg("expandHull, source unknown, guaranteed not convex"
              + T.show(old____hEnt));
        old____hEnt = old____hEnt.next(ccw);
        continue;
      }

      int w = (old____hEnt.source() == opp[0].source()) ? 1 : 0;
      PtEntry oppEnt = opp[w];

      boolean isTangent = !COper3.right(old____hEnt, oppEnt, oppEnt.next(true), ccw)
          && !COper3.right(old____hEnt, oppEnt, oppEnt.prev(true), ccw);

      if (!isTangent) {
        if (db__OLD && T.update())
          T.msg("expandHull, advance tangent line"
              + T.show(oppEnt.toPolygon(), MyColor.cDARKGRAY, -1, MARK_X)
              + T.show(old____hEnt) + tl(old____hEnt, oppEnt));
        opp[w] = oppEnt.next(ccw);
        continue;
      }

      if (COper3.left(old____hEnt, oppEnt, old____hEnt.next(ccw), ccw)
          && COper3.left(old____hEnt, oppEnt, old____hEnt.prev(ccw), ccw)) {
        DArray dispPts = new DArray();

        // delete points until cross tangent line
        PtEntry next = old____hEnt.next(ccw);
        while (true) {
          PtEntry prev = next;
          dispPts.add(prev);
          next = prev.delete(ccw);

          inf.update();
          if (COper3.right(old____hEnt, oppEnt, next, ccw)) {
            FPoint2 cross = MyMath.linesIntersection(old____hEnt, oppEnt, prev, next,
                null);
            old____hEnt = old____hEnt.insert(new PtEntry(cross), ccw);
            if (db__OLD && T.update())
              T.msg("expandHull, clipped to shadow region" + tl(old____hEnt, oppEnt)
                  + T.show(old____hEnt) + T.show(dispPts));
            break;
          }
        }
      } else {
        if (db__OLD && T.update())
          T.msg("expandHull, not dipping into shadow region"
              + T.show(old____hEnt.next(ccw)) + tl(old____hEnt, oppEnt));
      }
      old____hEnt = old____hEnt.next(ccw);
    } while (!advanced || old____hEnt != convHullEntry);
  }
  private static PtEntry traceHullPt;
  private static EdPolygon simple;
  private static EdPolygon possHull;
  private static EdPolygon valleyTri;
  private static Renderable traceValley;
  private static FPoint2 focusPt;

  private static Inf inf;

}
