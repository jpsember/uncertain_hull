package uhull;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import base.*;
import testbed.*;
import uhull.SegEnvOper.*;

public class EnvOper implements TestBedOperation, Globals {
  /*! .enum  .private 4250
      plotbitan plotsine plotsimp compare rndorder tracep1 testord tracep2
  */

    private static final int PLOTBITAN        = 4250;//!
    private static final int PLOTSINE         = 4251;//!
    private static final int PLOTSIMP         = 4252;//!
    private static final int COMPARE          = 4253;//!
    private static final int RNDORDER         = 4254;//!
    private static final int TRACEP1          = 4255;//!
    private static final int TESTORD          = 4256;//!
    private static final int TRACEP2          = 4257;//!
/*!*/

  private static final boolean db = true;

  private EnvOper() {
  }
  public static EnvOper singleton = new EnvOper();

  public void addControls() {
    C.sOpenTab("Env0");
    {
      C.sStaticText(//
          "Generate guaranteed convex hull of uncertain discs or polygons, "
              + "using algorithm of Nagai, "
              + "Yasutome, and Tokura; function domain is angle of tangents ");
      C.sCheckBox(PLOTSINE, "plot waves",
          "plot all sin/cos functions, not just those forming upper envelope",
          false);
      C.sCheckBox(PLOTBITAN, "plot bitangents", null, false);
      C.sCheckBox(RNDORDER, "random order",
          "shuffle segments before calculating envelope", false);
      C.sCheckBox(TRACEP1, "trace pass 1", null, true);
      C.sCheckBox(TRACEP2, "trace pass 2", null, true);
      C.sCheckBox(TESTORD, "test order", null, false);

      {
        C.sOpen();
        C.sCheckBox(PLOTSIMP, "simple",
            "Plot guaranteed hull, for testing purposes, "
                + "using simple, robust algorithm", false);
        C.sNewColumn();
        C.sCheckBox(COMPARE, "verify", "Verify that generated hull "
            + "matches the simple, slow algorithm's output", false);
        C.sClose();
      }
    }

    C.sCloseTab();
  }

  public void processAction(TBAction a) {
  }

  public static EdObject[] getRegions() {
    EdObject[] regions = null;
    do {
      regions = UHullMain.getPolygons();
      if (regions.length > 0)
        break;
      regions = UHullMain.getSegments();
      if (regions.length > 0)
        break;
      regions = UHullMain.getDiscs();

      if (regions.length > 0)
        break;
      regions = null;
    } while (false);
    return regions;
  }

  public void runAlgorithm() {
    bitan = null;
    hull = null;
    simpleHull = null;
    polyOrSeg = false;
    lowerEnvList = new DArray();
    int flags = 0;
    if (C.vb(RNDORDER))
      flags |= EnvTools.F_RNDORDER;
    DArray env = null;
    regions = getRegions();
    if (regions == null)
      return;
    polyOrSeg = !(regions[0] instanceof EdDisc);

    if (polyOrSeg) {
      DArray lowerEnvSegs = new DArray();
      int segId = 0;
      for (int i = 0; i < regions.length; i++) {
        Color c = EnvTools.multicolor(i);
        EdObject p = regions[i];

        DArray segs = new DArray();
        for (int j = 0; j < p.nPoints(); j++) {
          FPoint2 pt = p.getPoint(j);
          segs.add(new MySeg(segId++, c, pt, p, j));
        }
        if (db && T.update())
          T.msg("calculating lower hull for polygon"
              + T.show(p, c, STRK_THICK, -1));
        DArray pEnv = EnvTools.calcLower(segs, flags
            | (C.vb(TRACEP1) ?  EnvTools.F_TRACE : 0));
        lowerEnvList.add(pEnv);

        if (C.vb(PLOTSINE)) {
          T.show(segs, c, STRK_THIN, -1);
          T.show(pEnv, c, STRK_NORMAL, MARK_DISC);
        }

        lowerEnvSegs.addAll(pEnv);
      }
      if (db && T.update())
        T.msg("calculating upper envelope of lower envelopes");
      env = EnvTools.calcUpper(lowerEnvSegs, flags
          | (C.vb(TRACEP2) ?  EnvTools.F_TRACE : 0));
    } else {
      DArray segs = new DArray();
      for (int i = 0; i < regions.length; i++) {
        EdDisc seg = (EdDisc) regions[i];
        segs.add(new MySeg(i, seg));
      }
      if (C.vb(PLOTSINE)) {
        T.show(segs, MyColor.cLIGHTGRAY, -1, -1);
      }
      env = EnvTools.calcUpper(segs, flags);
    }

//    // turn off auxilliary tracing now that envelope completed
//    if (!C.vb(TESTORD)) {
//      if (env != null) {
//        for (int i = 0; i < env.size(); i++) {
//          MySeg s = (MySeg) env.get(i);
//          s.auxTrace = null;
//        }
//      }
//    }
    T.show(env, MyColor.cRED, STRK_NORMAL, MARK_DISC);

    constructHull(env, polyOrSeg ? regions : null);
    lowerEnvList = null;

    if (C.vb(COMPARE) && hull != null) {
      EdPolygon simp = simpleHull();
      if (simp != null) {
        String diff = HullUtil.compareHulls(hull, simp);
        if (diff != null)
          T.err("Generated hull and simple version don't agree:\n" + diff);
      }
    }

  }

  //  /**
  //   * Determine bottom right vertex of polygon
  //   * (vertex that is rightmost; settle ties by choosing lower)
  //   * @param p
  //   * @return index of bottom right vertex
  //   */
  //  private static int findStartVertex(EdObject p) {
  //    FPoint2 best = null;
  //    int bestInd = -1;
  //    for (int i = 0; i < p.nPoints(); i++) {
  //      FPoint2 pt = p.getPoint(i);
  //      if (best == null || (best.x > pt.x || (best.x == pt.x && best.y < pt.y))) {
  //        bestInd = i;
  //        best = pt;
  //      }
  //    }
  //    return bestInd;
  //  }

  private void constructHull(DArray env, EdObject[] polys) {

    if (db && T.update())
      T.msg("constructing hull bitangents from envelope"
          + T.show(env, MyColor.cRED));

    boolean t = ( //polyOrSeg && 
    C.vb(TESTORD));
    DArray te = new DArray();

    if (env.size() >= 2) {
      MySeg prev = (MySeg) env.last();
      bitan = new DArray();
      for (int i = 0; i < env.size(); i++) {
        MySeg seg = (MySeg) env.get(i);
        BiTangent b = null;
        //        if (!polyOrSeg) {
        //          if (prev.disc() != seg.disc()) {
        //            b = new BiTangent(prev.disc(), seg.disc());
        //          }
        //        } else 
        if (prev.source != seg.source) {
          EdObject p1 = (EdObject) prev.source;
          EdObject p2 = (EdObject) seg.source;

          if (!polyOrSeg) {
            b = new BiTangent(prev.disc(), seg.disc());
            if (t) {
              te.add(new OrdEntry(b, prev.disc, -1, seg.disc, -1));
            }
          } else {
            b = new BiTangent(prev.loc, p1.getLabel(), seg.loc, p2.getLabel());
            if (t) {
              te.add(new OrdEntry(b, (EdObject) prev.source, prev.vertIndex, //prev.(Z_PolyVert) prev.auxTrace,
                  (EdObject) seg.source, seg.vertIndex));
              //    (Z_PolyVert) seg.auxTrace));
            }
          }
          if (!b.defined())
            T.err("not defined");
        }
        if (b != null) {
          if (db && T.update())
            T.msg("bitangent" + T.show(bitan, BiTangent.BITAN_COLOR)
                + T.show(prev.source, MyColor.cDARKGREEN, STRK_THICK, -1)
                + T.show(seg.source, MyColor.cBLUE, STRK_THICK, -1)
                + T.show(b, MyColor.cRED)
                + T.show(prev, MyColor.cDARKGREEN, STRK_THICK, -1)
                + T.show(seg, MyColor.cBLUE, STRK_THICK, -1));

          bitan.add(b);
        }
        prev = seg;
      }
      if (C.vb(PLOTBITAN) && bitan != null)
        T.show(HullUtil.bitangentsString(bitan), null, 0, 100, -1);

      if (t) {
        analyze(te);
      }

      hull = HullUtil.hullFromBiTangents(bitan, polys);
    }
  }

  /**
   * Verify that vertex ordering property holds for upper hull bitangents
   * @param te array of OrdEntries
   */
  private void analyze(DArray a) {

    //    boolean db = true;

    // a.sort(OrdEntry.comparator);
    for (int side = 0; side < 2; side++) {

      DArray te = new DArray();
      for (int i = 0; i < a.size(); i++) {
        OrdEntry e = (OrdEntry) a.get(i);
        double th = e.thetaP();
        boolean sf = (th >= BSEnt.THETA_MIN && th <= BSEnt.THETA_MAX);
        if (sf != (side == 0))
          continue;
        te.add(e);
      }

      //      if (db && T.update())
      //        T.msg("verifying order property:\n" + te.toString(true));
      //      DArray s = new DArray();
      //      int errState = 0;
      //
      //      for (int i = 0; i < te.size(); i++) {
      //        OrdEntry ent = (OrdEntry) te.get(i);
      //        boolean change = false;
      //
      //        for (int vi = 0; vi < 2; vi++) {
      //          Z_PolyVert v = ent.vert(vi);
      //          int pi = v.polyIndex();
      //          if (!s.exists(pi)) {
      //            s.growSet(pi, new Integer(-1));
      //          }
      //          int iv = s.getInt(pi);
      //          if (db && T.update())
      //            T.msg("checking entry #" + i + ": " + v + " (prev=" + iv + ")");
      //
      //          int vn = v.calcVertIndex(side == 0);
      //
      //          if (db && T.update())
      //            T.msg("checking entry #" + i + ": " + v + " (prev=" + iv + ")");
      //
      //          if (iv > vn) {
      //            errState = 2;
      //            ent.setProblem();
      //          }
      //          if (iv < vn) {
      //            change = true;
      //            s.setInt(pi, vn);
      //          }
      //        }
      //        if (!change) {
      //          ent.setProblem();
      //          errState = 2;
      //        }
      //      }
      //      if (false && errState == 2)
      //        T.err("vertex order problem, side=" + side + ":\n"
      //            + T.show(te.toString(true), null, 85, 100, 15 | TX_CLAMP));
      //
      if (side == 0)
        T.show(te.toString(true), null, 85, 100, 15 | TX_CLAMP);
    }

  }
  public void paintView() {
    Editor.render();
    T.render(lowerEnvList, null, STRK_THIN, MARK_X);
    if (C.vb(PLOTSIMP)) {
      T.render(simpleHull(), MyColor.cLIGHTGRAY, STRK_THICK, -1);
    }
    if (C.vb(PLOTBITAN))
      T.render(bitan, BiTangent.BITAN_COLOR);
    HullUtil.render(hull);
  }

  /**
   * Generate hull polygon using simple, slow algorithm (if not already
   * generated)
   * @return hull polygon
   */
  private EdPolygon simpleHull() {
    if (simpleHull == null) {
      if (regions != null)
        simpleHull = HullUtil.build(regions);
    }
    return simpleHull;
  }
  private EdPolygon simpleHull;
  private DArray bitan;
  private EdPolygon hull;
  private EdObject[] regions;
  private DArray lowerEnvList;
  private boolean polyOrSeg;

  private static class MySeg implements GenSeg {
    public MySeg(int id, Color color, FPoint2 pt, Object source, int vertIndex) {
      this.id = id;
      this.color = color;
      this.loc = pt;
      this.radius = 0;
      this.source = source;
      this.vertIndex = vertIndex;
//      this.auxTrace = auxTrace;
      left = 0;
      right = Math.PI * 2;
    }

    public MySeg(int id, EdDisc disc) {
      this.id = id;
      this.source = disc;
//      this.auxTrace = disc;
      this.disc = disc;
      this.vertIndex = -1;
      this.loc = disc.getOrigin();
      this.radius = disc.getRadius();
      left = 0;
      right = Math.PI * 2;
    }

    private EdDisc disc() {
      return disc;
    }

    private boolean isDisc() {
      return disc != null;
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("MySeg[");
      sb.append(loc);
      if (isDisc())
        sb.append("r:" + Tools.f(radius));
//      else {
//        sb.append("aux:" + auxTrace);
//      }
      sb.append(Tools.fa2(left));
      sb.append('/');
      sb.append(Tools.fa2(right));
      sb.append(']');
      return sb.toString();
    }

    public GenSeg clipTo(double left, double right) {
      if (left >= right || left < this.left || right > this.right)
        throw new IllegalArgumentException();

      MySeg s;
      if (isDisc())
        s = new MySeg(this.id, this.disc);
      else {
        s = new MySeg(this.id, color, loc, source, vertIndex); 
        //        Streams.out.println("clipped: "+this+"\n to: "+s);
      }
      s.left = left;
      s.right = right;
      return s;
    }

    private double yAt(double theta) {
      return loc.x * Math.sin(theta) - loc.y * Math.cos(theta) - radius;
    }

    public GenSeg higherOf(GenSeg seg1, GenSeg seg2, double x) {
      MySeg s1 = (MySeg) seg1;
      MySeg s2 = (MySeg) seg2;
      double yDiff = s1.yAt(x) - s2.yAt(x);
      GenSeg ret = null;
      if (yDiff < 0)
        ret = seg2;
      if (yDiff > 0)
        ret = seg1;
      return ret;
    }

    public double left() {
      return left;
    }
    private int id;
    public int id() {
      return id;
    }
    private int vertIndex;
    private static int[] iterCounts = new int[25];
    private static int iterSamples;

    private static DArray calcCommonTheta(MySeg wa, MySeg wb) {
      final boolean db = false;
      final double TOL = 1e-10;

      DArray ret = new DArray();
      do {
        if (wa.isDisc()) {
          if (EdDisc.encloses(wa.loc, wa.radius, wb.loc, wb.radius)
              || EdDisc.encloses(wb.loc, wb.radius, wa.loc, wa.radius))
            break;
        }
        double u = wa.loc.x - wb.loc.x;
        double v = wb.loc.y - wa.loc.y;

        double s = wb.radius - wa.radius;

        double t0 = 0;
        double sin = Math.sin(t0);
        double cos = Math.cos(t0);
        double der = u * cos - v * sin;
        if (Math.abs(der) < TOL)
          t0 = Math.PI * .234;

        double tStart = t0;
        for (int pass = 0; pass < 3; pass++) {
          StringBuilder sb = null;
          if (db)
            sb = new StringBuilder();
          if (db && T.update())
            T.msg("pass=" + pass);
          double f;
          int i;
          for (i = 0;; i++) {
            if (i >= iterCounts.length)
              throw new FPError("too many iterations: " + wa + ", " + wb);

            sin = Math.sin(t0);
            cos = Math.cos(t0);
            der = u * cos - v * sin;
            f = sin * u + cos * v + s;
            if (db)
              sb.append(Tools.f(i) + " t0=" + Tools.fa(t0) + " f=" + Tools.f(f)
                  + " der=" + Tools.f(der) + "\n");
            if (Math.abs(f) < TOL)
              break;
            double dx = -f / der;
            t0 = MyMath.normalizeAnglePositive(t0 + dx);
          }
          iterCounts[i]++;
          iterSamples++;
          double t1 = MyMath.normalizeAnglePositive(t0);

          if (false) {
            Tools.warn("making sure isect pt not clipped out of either");

            if (!wa.contains(t1) || !wb.contains(t1))
              continue;
          }
          t0 = tStart + (1 + pass) * Math.PI * 2.0 / 3.0;
          boolean add = true;

          if (db && T.update())
            T.msg(sb + "testing if value exists: " + Tools.fa(t1));
          for (int k = 0; k < ret.size(); k++) {
            double t2 = ret.getDouble(k);
            if (Math.abs(t1 - t2) < TOL * 5) {
              if (db && T.update())
                T.msg("value matches old, skipping: " + Tools.fa(t2));
              add = false;
            }
          }
          if (add)
            ret.addDouble(t1);
        }
      } while (false);

      if (false) {
        Tools.warn("printing samples data");
        if (iterSamples % 1000 == 1) {
          Streams.out.println("Iterations:");
          for (int i = 0; i < iterCounts.length; i++) {
            double k = iterCounts[i];
            if (k == 0)
              continue;
            Streams.out.println(Tools.f(i) + ": "
                + Tools.f((k * 100) / iterSamples));
          }
        }
      }
      return ret;
    }

    private boolean contains(double theta) {
      return left <= theta && theta <= right;
    }

    public Double nextIntersection(GenSeg seg1, GenSeg seg2, double pastX) {
      MySeg s1 = (MySeg) seg1;
      MySeg s2 = (MySeg) seg2;

      DArray d = calcCommonTheta(s1, s2);
      double tBest = Math.PI * 2;
      for (int k = 0; k < d.size(); k++) {
        double t = d.getDouble(k);

        if (!s1.contains(t) || !s2.contains(t))
          continue;
        if (t < tBest && t > pastX)
          tBest = t;
      }
      Double ret = null;
      if (tBest < Math.PI * 2)
        ret = new Double(tBest);
      return ret;
    }

    public double right() {
      return right;
    }
    private FPoint2 pt(double theta) {
      return new FPoint2(theta, yAt(theta));
    }

    private void plotInterval(double thMin, double thMax) {
      final double RESOLUTION = 80;
      FPoint2 prev = null;

      for (double t = thMin;; t += Math.PI * 2 / RESOLUTION) {
        t = Math.min(t, thMax);
        FPoint2 curr = EnvTools.transform(pt(t));
        if (prev != null)
          V.drawLine(prev, curr);
        prev = curr;
        if (t == thMax)
          break;
      }
    }

    public void render(Color c, int stroke, int markType) {
      V.pushColor(c, color);
      V.pushStroke(stroke); //, Globals.STRK_THIN);
      plotInterval(left, right);
      if (markType >= 0) {
        if (left != 0)
          V.mark(EnvTools.transform(pt(left)), markType, .5);
        if (right != Math.PI * 2)
          V.mark(EnvTools.transform(pt(right)), markType, .5);
      }

//      T.render(auxTrace, V.get2DGraphics().getColor(), -1, -1);

      V.pop(2);
      if (markType >= 0 && Editor.withLabels(false) && isDisc()) {
        V.pushColor(MyColor.cDARKGRAY);
        V.pushScale(.55);
        double t = (left + right) * .5;
        String s;
        if (isDisc())
          s = disc.getLabel();
        else
          s = source.toString();

        FPoint2 loc = EnvTools.transform(pt(t));
        V.draw(s, loc.x, loc.y + 2, Globals.TX_BGND | Globals.TX_FRAME);
        V.pop(2);
      }
    }

    public String showSweepLine(double theta) {
      double PAD = MyMath.radians(5);
      theta = MyMath.clamp(theta, -PAD, PAD + 2 * Math.PI);
      {
        FPoint2 p1 = EnvTools.transform(new FPoint2(theta, -130));
        FPoint2 p2 = EnvTools.transform(new FPoint2(theta, 130));
        Object ret = new EdSegment(p1, p2);
        T.show(ret, MyColor.cDARKGREEN, Globals.STRK_NORMAL, -1);
      }
      return "";
    }

    private Color color;
    private EdDisc disc;
    private FPoint2 loc;
    private double radius;
    private double left, right;
    private Object source;
//    // for tracing purposes only
//    private Object auxTrace;
  }
  
  
  private static class OrdEntry {
    //  public static final Comparator comparator2 = new Comparator() {
    //    public int compare(Object arg0, Object arg1) {
    //      OrdEntry e1 = (OrdEntry) arg0;
    //      OrdEntry e2 = (OrdEntry) arg1;
    //
    //      double th1 = e1.thetaP() - Math.PI / 2;
    //      double th2 = e2.thetaP() - Math.PI / 2;
    //      if (th1 < 0)
    //        th1 += Math.PI * 2;
    //      if (th2 < 0)
    //        th2 += Math.PI * 2;
    //      double diff = th1 - th2;
    //
    //      return MyMath.sign(diff);
    //    }
    //  };
    //  public void setProblem() {
    //    problem = true;
    //  }
    //
    public double thetaP() {
      return b2.thetaP();
    }

    public OrdEntry(BiTangent b, EdObject obj1, int vertIndex1, EdObject obj2,
        int vertIndex2) {
      this.b2 = b;
      this.v1 = obj1;
      this.v2 = obj2;
      this.vi1 = vertIndex1;
      this.vi2 = vertIndex2;
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(v1);
      if (vi1 >= 0)
        sb.append(vi1);
      sb.append("-");
      sb.append(v2);
      if (vi2 >= 0)
        sb.append(vi2);
      return sb.toString();
    }
    
//    public EdObject vert(int vi) {
//      return vi == 0 ? v1 : v2;
//    }
    
    private BiTangent b2;
    private EdObject v1, v2;
    private int vi1, vi2;
  }

}
