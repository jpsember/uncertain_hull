package uhull;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class PossHullOper implements TestBedOperation, Globals {
  /*! .enum  .private 3200
      reset sample _ db_starcent   
        db_potedges  db_polyfromenv leftonly db_walkenv showstarcent
  */

    private static final int RESET            = 3200;//!
    private static final int SAMPLE           = 3201;//!
    private static final int DB_STARCENT      = 3203;//!
    private static final int DB_POTEDGES      = 3204;//!
    private static final int DB_POLYFROMENV   = 3205;//!
    private static final int LEFTONLY         = 3206;//!
    private static final int DB_WALKENV       = 3207;//!
    private static final int SHOWSTARCENT     = 3208;//!
/*!*/

  private boolean db;
  private static final double EPS = 1e-5;
  private static final Color C1 = MyColor.cPURPLE, C2 = MyColor.get(
      MyColor.CYAN, .3);
  private static final boolean WITH_SNAP = false;

  public void addControls() {
    C.sOpenTab("PHull");
    C.sStaticText("Generates possible convex hull of uncertain polygons, in O(n log^2 n) time, using envelope method");

    C
        .sCheckBox(
            LEFTONLY,
            "left only",
            "calculate only left tangent lines for candidate type II edges; insufficient!",
            false);
    C.sCheckBox(SHOWSTARCENT, "plot star center", null, true);
    {
      C.sOpen("Trace");
      C.sCheckBox(DB_STARCENT, "star center", null, false);
      C.sCheckBox(DB_POTEDGES, "potential edges", null, false);
      C.sCheckBox(DB_WALKENV, "envelope generation", null, false);
      C.sCheckBox(DB_POLYFROMENV, "envelope->poly transform", null, false);
      C.sClose();
    }
    {
      C.sOpen("Samples");
      C.sCheckBox(SAMPLE, "active", null, false);
      C.sNewColumn();
      C.sButton(RESET, "reset", null);
      C.sClose();
    }
    C.sCloseTab();
  }

  public static PossHullOper singleton = new PossHullOper();
  private PossHullOper() {
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      case RESET:
      case SAMPLE:
        sampledHull = null;
        break;
      }
    }
  }

  public void paintView() {
    T.render(sampledHull);
    //    if (C.vb(PLOTCOMBEDGES))
    //      T.renderAll(HullUtil.buildCombEdges(), MyColor.cDARKGREEN,
    //          STRK_RUBBERBAND, -1);

    if (renderPolyA == null && renderPolyB == null)
      Editor.render();

    T.render(possHull, MyColor.cDARKGREEN, -1, -1); //C.vb(PLOTCOMBEDGES) ? STRK_THICK
    //  : -1, -1);

    T.render(renderPolyA, C1, STRK_THICK, -1);
    T.render(renderPolyB, C2, STRK_THICK, -1);
    if (C.vb(SHOWSTARCENT))
      T.render(starCenter, MyColor.cRED, -1, MARK_DISC);
  }

  /**
   * Build the possible hull of a set of uncertain polygons
   * @param polygons
   * @return EdPolygon representing possible convex hull, or null if
   *  set is empty
   */
  public static EdPolygon buildPossibleHull(EdPolygon[] polygons) {
    singleton.db = false;
    singleton.starCenter = null;
    EdPolygon p = null;
    if (polygons.length > 0)
      p = singleton.runAlgorithmAux(polygons, 0, polygons.length);
    return p;
  }

  private EdPolygon runAlgorithmAux(EdPolygon[] regions, int rStart, int rEnd) {

    if (regions.length == 0)
      throw new IllegalArgumentException();

    EdPolygon pHull = null;

    do {

      int size = rEnd - rStart;
      if (db && size > 1 && T.update()) {
        DArray polys = new DArray();
        for (int i = rStart; i < rEnd; i++)
          polys.add(regions[i]);
        T.msg("build possible hull from polygons" //
            + T.show(polys, MyColor.cRED));
      }

      if (size == 1)
        pHull = regions[rStart];
      else {
        int rMid = (rStart + rEnd) / 2;
        EdPolygon polyA = runAlgorithmAux(regions, rStart, rMid);
        EdPolygon polyB = runAlgorithmAux(regions, rMid, rEnd);
        renderPolyA = polyA;
        renderPolyB = polyB;
        if (db && T.update())
          T.msg("finding kernel point");
        starCenter = HullUtil.findKernelPoint(polyA, polyB, C.vb(DB_STARCENT));
        if (db && T.update())
          T.msg("kernel point" + T.show(starCenter));

        // build potential type II edges
        DArray pv = new DArray();
        {
          boolean db = this.db && C.vb(DB_POTEDGES);

          for (int polyPass = 0; polyPass < 2; polyPass++) {
            EdPolygon p = (polyPass == 0) ? polyA : polyB;
            EdPolygon other = (polyPass == 1) ? polyA : polyB;
            for (int j = 0; j < p.nPoints(); j++) {
              FPoint2 v = p.getPoint(j);
              if (db && T.update())
                T.msg("adding edges for vertex" + T.show(v));

              // add type I edge
              addSegment(pv, v, p.getPointMod(j + 1), "type I edge");

              if (isCenter(v))
                continue;

              FPoint2 ltan = HullUtil.findTangentVertex(other, v, false);
              if (ltan != null)
                addSegment(pv, v, ltan, "left tangent line");

              if (!C.vb(LEFTONLY)) {
                FPoint2 rtan = HullUtil.findTangentVertex(other, v, true);
                if (rtan != null)
                  addSegment(pv, rtan, v, "right tangent line");
              }
            }
          }
        }
        if (db && T.update())
          T.msg("potential edges" + T.show(pv, null, STRK_THIN, MARK_NONE));

        DArray env = EnvTools.calcUpper(pv, C.vb(DB_WALKENV) ? EnvTools.F_TRACE
            : 0);
        if (db && T.update())
          T.msg("envelope" + T.show(env, null, STRK_THICK, MARK_NONE));

        {
          boolean db = this.db && C.vb(DB_POLYFROMENV);
          DArray vt = new DArray();
          FPoint2 expNext = null;
          for (int i = 0; i <= env.size(); i++) {
            WalkSeg seg = (WalkSeg) env.getMod(i);

            FPoint2 pt = seg.leftPt();
            FPoint2 pt2 = seg.rightPt();

            if (db && T.update())
              T.msg("next envelope segment" + EdSegment.showDirected(pt, pt2)
                  + " and expNext" + T.show(expNext) + "\n pt=" + pt + "\npt2="
                  + pt2 + "\nexp=" + expNext
                  + (expNext != null ? "" + expNext.distance(pt) : ""));

            if (expNext != null && expNext.distance(pt) > 1e-2) {
              // if expected next and actual next are on same ray,
              // only add expected next; else, add expected next and
              // star center
              if (Math.abs(angleOf(expNext) - angleOf(pt)) > EPS) {
                if (db && T.update())
                  T.msg("adding expected next..starCenter"
                      + EdSegment.showDirected(expNext, starCenter));
                vt.add(expNext);
                vt.add(starCenter);
              } else {
                if (db && T.update())
                  T.msg("adding expected next" + T.show(expNext));
                vt.add(expNext);
              }
            }
            if (i == env.size())
              continue;
            if (db && T.update())
              T.msg("adding segment left" + T.show(pt));
            vt.add(pt);
            expNext = pt2;
          }

          pHull = new EdPolygon(vt);
        }
        renderPolyA = null;
        renderPolyB = null;
      }
    } while (false);
    return pHull;
  }

  public void runAlgorithm() {
    db = true;

    possHull = null;
    starCenter = null;
    renderPolyA = null;
    renderPolyB = null;

    EdPolygon[] rgn = UHullMain.getPolygons();
    if (C.vb(SAMPLE)) {
      String hash = UHullMain.getHash(rgn);
      if (!hash.equals(sampleHash))
        sampledHull = null;
      if (sampledHull == null) {
        sampledHull = SampPossHull.construct2(rgn);
        sampleHash = hash;
      }
    }
    if (rgn.length > 0)
      possHull = runAlgorithmAux(rgn, 0, rgn.length);
  }

  /**
   * Determine if three points make a left turn
   * @param p1
   * @param p2
   * @param p3
   * @return true if p1,p2,p3 is a left turn
   */
  private static boolean left(FPoint2 p1, FPoint2 p2, FPoint2 p3) {
    return MyMath.sideOfLine(p1, p2, p3) > 0;
  }

  private int walkSegId;
  private boolean isCenter(FPoint2 pt) {
    return same(starCenter, pt);
  }

  /**
   * Generalized segments for calculating upper envelope of outward-facing
   * polygon edges.
   * The x coordinates are angles.
   */
  private class WalkSeg implements GenSeg {

    /**
     * Construct segment, whose endpoints may lie on ray from star center
     * @param p0
     * @param p1
     */
    public WalkSeg(FPoint2 p0, FPoint2 p1) {
      final boolean db = false;
      if (db && T.update())
        T.msg("constructing WalkSeg\np0=" + p0 + "\np1=" + p1 + "\nsc="
            + starCenter);
      double th0, th1;
      if (!isCenter(p0)) {
        th0 = th1 = angleOf(p0);
        if (!isCenter(p1))
          th1 = angleOf(p1);
      } else {
        th0 = th1 = angleOf(p1);
      }

      // if angles are essentially the same, make them equal
      if (Math.abs(th0 - th1) < EPS)
        th1 = th0;

      if (th1 < EPS)
        th1 = Math.PI * 2;
      construct(p0, p1, th0, th1);
    }

    /**
     * Construct segment
     * @param p0
     * @param p1 endpoints 
     * @param th0
     * @param th1 angles corresponding to endpoints
     */
    public WalkSeg(FPoint2 p0, FPoint2 p1, double th0, double th1) {
      if (Math.abs(MyMath.normalizeAngle(th1 - th0)) < EPS)
        T.err("illegal arguments: th0=" + th0 + " th1=" + th1);
      construct(p0, p1, th0, th1);
    }

    private void construct(FPoint2 p0, FPoint2 p1, double th0, double th1) {
      final boolean db = false;
      if (db && T.update())
        T.msg("WalkSeg, construct;\n p0=" + p0 + "\n p1=" + p1 + "\n th0="
            + th0 + "\n th1=" + th1);

      this.id = walkSegId++;
      this.ap0 = p0;
      this.ap1 = p1;

      if (WITH_SNAP) {
        th0 = MyMath.snapToGrid(th0, EPS);
        th1 = MyMath.snapToGrid(th1, EPS);
      }

      this.theta0 = th0;
      this.theta1 = th1;
      if (th0 > th1) {
        T.err("illegal arg:\n p0=" + p0 + " p1=" + p1 + "\n left=" + theta0
            + " r=" + theta1);
      }
    }

    public GenSeg clipTo(double left, double right) {
      final boolean db = false;
      if (db && T.update())
        T.msg("clipTo left=" + MyMath.degrees(left) + " right="
            + MyMath.degrees(right) + "\n" + this);
      WalkSeg w = null;
      if (Math.abs(left - right) > EPS) {
        w = new WalkSeg(ptAt(left), ptAt(right));
      }
      return w;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("WalkSeg");
      sb.append("[");
      sb.append(Tools.fa2(theta0));
      sb.append(" ...");
      sb.append(Tools.fa2(theta1));
      sb.append("]");
      return sb.toString();
    }
    private FPoint2 ptAt(double theta) {
      final boolean db = false;
      if (db && T.update())
        T.msg("ptAt theta=" + Tools.fa(theta)
            + T.show(this, MyColor.cRED, -1, MARK_DISC));
      FPoint2 ray = MyMath.ptOnCircle(starCenter, theta, 20.0);
      FPoint2 isect = MyMath.linesIntersection(starCenter, ray, leftPt(),
          rightPt(), null);
      if (db && T.update())
        T.msg("intersection" + EdSegment.show(starCenter, ray)
            + EdSegment.show(leftPt(), rightPt()) + T.show(isect));
      if (isect == null)
        isect = starCenter;
      return isect;
    }

    private double yAt(double theta) {
      return FPoint2.distance(ptAt(theta), starCenter);
    }

    public GenSeg higherOf(GenSeg seg1, GenSeg seg2, double x) {
      WalkSeg w1 = (WalkSeg) seg1;
      WalkSeg w2 = (WalkSeg) seg2;
      double diff = w1.yAt(x) - w2.yAt(x);
      return (diff < 0) ? w2 : w1;
    }

    public int id() {
      return id;
    }

    public double left() {
      return theta0;
    }

    public double right() {
      return theta1;
    }

    public String showSweepLine(double x) {
      double xo = x;
      x = MyMath.clamp(x, 0, Math.PI * 2);
      return EdSegment.showDirected(starCenter, MyMath.ptOnCircle(starCenter,
          x, 50), x == xo ? MyColor.cPURPLE : MyColor.cRED, STRK_RUBBERBAND);
    }

    public void render(Color c, int stroke, int markType) {
      FPoint2 p1 = ptAt(theta0);
      FPoint2 p2 = ptAt(theta1);

      if (theta1 - theta0 < EPS) {
        p1 = this.ap0;
        p2 = this.ap1;
      }

      V.pushColor(c, MyColor.cRED);
      V.pushStroke(stroke);
      V.drawLine(p1, p2);
      if (markType < 0)
        markType = MARK_DISC;
      if (markType >= 0) {
        V.mark(p1, markType);
        V.mark(p2, markType);
      }
      V.pop(2);
    }

    public Double nextIntersection(GenSeg seg1, GenSeg seg2, double pastX) {
      WalkSeg w1 = (WalkSeg) seg1;
      WalkSeg w2 = (WalkSeg) seg2;
      FPoint2 l1 = w1.leftPt();
      FPoint2 r1 = w1.rightPt();
      FPoint2 l2 = w2.leftPt();
      FPoint2 r2 = w2.rightPt();

      FPoint2 isect = MyMath.lineSegmentIntersection(l1, r1, l2, r2, null);
      Double ret = null;
      if (isect != null) {
        double th = angleOf(isect);
        if (th > pastX && th < w1.theta1 && th >= w1.theta0 && th < w2.theta1
            && th >= w2.theta0)
          ret = new Double(th);
      }
      return ret;
    }

    public FPoint2 rightPt() {
      return ap1;
    }
    public FPoint2 leftPt() {
      return ap0;
    }

    // endpoints
    private FPoint2 ap0, ap1;
    // angles of endpoints
    private double theta0, theta1;
    // id of segment, for consistent sorting
    private int id;
  }

  private double angleOf(FPoint2 v) {
    if (isCenter(v))
      T.err("illegal argument");
    return MyMath.normalizeAnglePositive(MyMath.polarAngle(starCenter, v));
  }

  private boolean same(FPoint2 p1, FPoint2 p2) {
    return p1.distance(p2) < EPS;
  }

  private void addSegment(DArray list, FPoint2 p1, FPoint2 p2, String desc) {
    final boolean db = C.vb(DB_POTEDGES);
    do {

      // if star center is on or to right of line containing segment, 
      // don't add segment

      if (MyMath.ptDistanceToLine(starCenter, p1, p2, null) < EPS
          || !left(p1, p2, starCenter)) {
        if (db && T.update())
          T.msg("addSegment (" + desc
              + "),\n star center is not to left, skipping"
              + EdSegment.showDirected(p1, p2));
        break;
      }
      if (db && T.update())
        T.msg("addSegment (" + desc + ")" + EdSegment.showDirected(p1, p2));

      double t1 = angleOf(p1);
      double t2 = angleOf(p2);

      if (t1 > t2) {
        // line crosses the theta=0 ray;
        // determine intersection point of ray with line
        FPoint2 ipt = MyMath.linesIntersection(starCenter, new FPoint2(
            starCenter.x + 100, starCenter.y), p1, p2, null);
        list.add(new WalkSeg(p1, ipt, t1, Math.PI * 2));
        list.add(new WalkSeg(ipt, p2, 0, t2));
        if (db && T.update())
          T.msg("added split segment crossing theta=0" + T.show(list.last())
              + T.show(list.getMod(-2)));
      } else {
        list.add(new WalkSeg(p1, p2));
        if (db && T.update())
          T.msg("added segment" + T.show(list.last()));
      }
    } while (false);
  }

  private EdPolygon possHull;
  private BitMap sampledHull;
  private String sampleHash;
  private FPoint2 starCenter;
  private Renderable renderPolyA, renderPolyB;
}
