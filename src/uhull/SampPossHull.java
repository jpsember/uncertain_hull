package uhull;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class SampPossHull implements TestBedOperation, Globals {
  /*! .enum  .private 2400
     resetsamp ntrials plotlast run resetrun step resolution plotcombedges plotguar simple
  */

    private static final int RESETSAMP        = 2400;//!
    private static final int NTRIALS          = 2401;//!
    private static final int PLOTLAST         = 2402;//!
    private static final int RUN              = 2403;//!
    private static final int RESETRUN         = 2404;//!
    private static final int STEP             = 2405;//!
    private static final int RESOLUTION       = 2406;//!
    private static final int PLOTCOMBEDGES    = 2407;//!
    private static final int PLOTGUAR         = 2408;//!
    private static final int SIMPLE           = 2409;//!
/*!*/

  public void addControls() {
    C.sOpenTab("PHullSamp");
    C.sStaticText("Generates possible convex hull"
        + " of uncertain polygons by sampling points");

    C.sButton(STEP, "Step", "Process single random hull");
    {
      C.sOpen();
      C.sButton(RESETSAMP, "Reset", "Clears sampled hull");
      C.sButton(RUN, "Run", "Generate a number of random hulls");
      C.sButton(RESETRUN, "Reset & Run", null);

      C.sNewColumn();
      C.sIntSpinner(NTRIALS, "# trials",
          "Number of samples to clip guaranteed hull to", 1, 10000, 100, 10);
      C.sClose();
    }

    C.sIntSpinner(RESOLUTION, "Res", "Sampling resolution", 1, 10, 3, 1);
    C.sCheckBox(PLOTLAST, "Plot last sample", null, true);
    C.sCheckBox(PLOTCOMBEDGES, "Plot derived edges",
        "plots edges between each pair of bicolored vertices", false);
    C.sCheckBox(PLOTGUAR, "Plot guaranteed", null, false);
    C.sCheckBox(SIMPLE, "Plot simple",
        "plot possible hull generated using simple algorithm", false);
    C.sCloseTab();
  }

  public static SampPossHull singleton = new SampPossHull();

  private SampPossHull() {
  }

  public static BitMap construct2(EdPolygon[] polygons) {
    singleton.resetSamp();
    singleton.run(polygons, 5000, 2);
    BitMap ps = singleton.pts2;
    singleton.resetSamp();
    return ps;
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      case RESETSAMP:
        resetSamp();
        break;
      case STEP:
        run(null, 1, -1);
        break;
      case RESETRUN:
        resetSamp();
        run(null, C.vi(NTRIALS), -1);
        break;
      case RUN:
        run(null, C.vi(NTRIALS), -1);
        break;
      }
    }
  }
  private void resetSamp() {
    hullPoly = null;
    lastHull = null;
    pts2 = null;
  }

  private void run(EdPolygon[] d, int trials, int res) {

    final boolean db = false;

    if (d == null)
      d = UHullMain.getPolygons();
    if (res < 0)
      res = C.vi(RESOLUTION);

    double gs = res * res * .05;
    if (res > 0) {
      {
        if (pts2 == null) {
          pts2 = new BitMap();
        }
        pts2.prepare(
            UHullMain.getHash(d),
            gs);
        pts2.setColor(MyColor.get(MyColor.LIGHTGRAY, .5));
      }
    }

    DArray pts = new DArray();

    Random rnd = new Random();

    PolyBoundary[] pb = new PolyBoundary[d.length];
    for (int i = 0; i < d.length; i++)
      pb[i] = new PolyBoundary(d[i]);

    for (int i = 0; i < trials; i++) {
      totalFrames++;
      boolean vert = (totalFrames & 0x1) == 0;
      if (db)
        Streams.out.println("running trial #" + i + " on " + d.length
            + " discs");
      pts.clear();
      for (int j = 0; j < d.length; j++) {
        EdPolygon ds = d[j];

        FPoint2 pt;
        if (trials == 1) {
          FRect r = pb[j].bounds();
          while (true) {
            pt = new FPoint2(MyMath.rnd(r.width) + r.x, MyMath.rnd(r.height)
                + r.y);
            if (d[j].isPointInside(pt) != 0)
              break;
          }
        } else {
          if (vert)
            pt = ds.getPoint(rnd.nextInt(ds.nPoints()));
          else
            pt = pb[j].randomBoundaryPt(rnd);
        }
        pts.add(pt);
      }

      if (pts.size() == 2) {
        EdSegment s = new EdSegment(pts.getFPoint2(0), pts.getFPoint2(1));
        lastHull = s;
        s.renderTo(pts2.getGraphics());

      } else {

        EdPolygon p;
        {
          DArray ch = MyMath.convexHull(pts);
          p = new EdPolygon();
          for (int k = 0; k < ch.size(); k++)
            p.addPoint(pts.getFPoint2(ch.getInt(k)));
        }
        lastHull = p;
        p.fill(pts2.getGraphics());
      }
    }
  }

//  /**
//   * Paint polygon into sample buffer
//   * @param ptSamples
//   * @param p
//   * @param color
//   * @deprecated
//   */
//  public static void plotConvexPolygon(Z_PointSamples ptSamples, EdPolygon p,
//      int color) {
//
//    do {
//      int nVert = p.nPoints();
//      if (nVert < 2)
//        break;
//
//      // find lowest left, lowest right vertices
//      int i0 = -1, i1 = -1;
//      FPoint2 lPt0 = null;
//      FPoint2 lPt1 = null;
//      for (int i = 0; i < p.nPoints(); i++) {
//        FPoint2 pt = p.getPoint(i);
//        if (lPt0 == null
//            || (pt.y < lPt0.y || (pt.y == lPt0.y && pt.x < lPt0.x))) {
//          lPt0 = pt;
//          i0 = i;
//        }
//        if (lPt1 == null
//            || (pt.y < lPt1.y || (pt.y == lPt1.y && pt.x > lPt1.x))) {
//          lPt1 = pt;
//          i1 = i;
//        }
//      }
//      int j0 = (i0 + nVert - 1) % nVert;
//      int j1 = (i1 + 1) % nVert;
//      FPoint2 p0 = p.getPoint(i0);
//      FPoint2 p1 = p0;
//      FPoint2 q0 = p.getPoint(j0);
//      FPoint2 q1 = p.getPoint(j1);
//
//      double scanY = p0.y;
//      double scanX0 = p0.x;
//      double scanX1 = p1.x;
//
//      while (true) {
//        // choose new left, right destination vertices if necessary
//        if (scanY == q0.y) {
//          p0 = q0;
//          j0 = (j0 + nVert - 1) % nVert;
//          q0 = p.getPoint(j0);
//          if (q0.y < p0.y)
//            break;
//          continue;
//        }
//        if (scanY == q1.y) {
//          p1 = q1;
//          j1 = (j1 + 1) % nVert;
//          q1 = p.getPoint(j1);
//          if (q1.y < p1.y)
//            break;
//          continue;
//        }
//
//        double xa0 = (q0.x - p0.x) / (q0.y - p0.y);
//        double xa1 = (q1.x - p1.x) / (q1.y - p1.y);
//
//        while (true) {
//          double maxStep = Math.min(q0.y - scanY, q1.y - scanY);
//          if (maxStep <= 0)
//            break;
//
//          double step = Math.min(maxStep, ptSamples.pixelSize());
//          if (scanX0 > scanX1)
//            T.err("problem");
//
//          if (true) {
//            ptSamples.horzLine(scanX0, scanX1 - scanX0, scanY, color);
//          } else {
//
//            double rx = scanX0;
//
//            do {
//              ptSamples.add(rx, scanY, color);
//              rx = Math.min(rx + ptSamples.pixelSize(), scanX1);
//            } while (rx < scanX1);
//          }
//
//          scanY += step;
//          scanX0 += xa0 * step;
//          scanX1 += xa1 * step;
//        }
//
//      }
//    } while (false);
//
//  }

  public void runAlgorithm() {
    calcdBitangents = null;
    calcdHull = null;
    combEdges = null;
    guarHull = null;
    simpleHull = null;
    if (C.vb(PLOTCOMBEDGES))
      combEdges = HullUtil.buildCombEdges();
    if (C.vb(PLOTGUAR))
      guarHull = EnvOper2.guaranteedHull(UHullMain.getPolygons());
    if (C.vb(SIMPLE))
      simpleHull = HullUtil.buildPossibleHull(UHullMain.getPolygons());
  }

  public void paintView() {
    //  T.render(ptSamples);
    T.render(pts2);
    T.renderAll(combEdges, MyColor.cDARKGREEN, STRK_THIN, -1);

    Editor.render();

    {
      if (C.vb(PLOTLAST) && lastHull != null) {
        lastHull.render(null, STRK_RUBBERBAND, -1);
      }
      T.render(hullPoly, Color.BLACK, -1, -1);
      T.render(simpleHull, MyColor.cPURPLE, STRK_THICK, -1);
    }

    T.renderAll(calcdBitangents, MyColor.cBLUE, STRK_THIN, -1);
    T.render(calcdHull, MyColor.cDARKGREEN, -1, MARK_DISC);
    T.render(guarHull, MyColor.cDARKGREEN, -1, -1);
  }
  private static class PolyBoundary {
    public PolyBoundary(EdPolygon p) {
      edges = new PolyEdge[p.nPoints()];
      for (int i = 0; i < edges.length; i++) {
        edges[i] = new PolyEdge(p, i);
        totalLength += edges[i].length;
      }
      bounds = p.getBounds();
    }
    private FRect bounds;
    public FPoint2 randomBoundaryPt(Random rnd) {
      double pos = rnd.nextDouble() * totalLength;
      int j = 0;
      PolyEdge pe = edges[j];
      while (pos >= pe.length) {
        pos -= pe.length;
        j++;
        pe = edges[j];
      }
      if (pe.degenPt != null)
        return pe.degenPt;

      double t = rnd.nextDouble() * (pe.t1 - pe.t0) + pe.t0;
      return pe.lineEqn.pt(t);
    }
    private static class PolyEdge {
      public LineEqn lineEqn;
      public double t0, t1;
      public double length;
      public FPoint2 degenPt;
      public PolyEdge(EdPolygon p, int en) {
        FPoint2 p0 = p.getPointMod(en);
        FPoint2 p1 = p.getPointMod(en + 1);
        length = FPoint2.distance(p0, p1);
        lineEqn = new LineEqn(p0, p1);
        if (lineEqn.defined()) {
          t0 = lineEqn.parameterFor(p0);
          t1 = lineEqn.parameterFor(p1);
        } else
          degenPt = p0;
      }
    }
    private PolyEdge[] edges;
    private double totalLength;
    public FRect bounds() {
      return bounds;
    }
  }

  private EdPolygon hullPoly;
  private EdPolygon calcdHull;
  private Renderable lastHull;
  private DArray calcdBitangents;
  private BitMap pts2;
  private int totalFrames;
  private DArray combEdges;
  private EdPolygon guarHull;
  private EdPolygon simpleHull;
}
