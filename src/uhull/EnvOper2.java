package uhull;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import base.*;
import testbed.*;
import uhull.SegEnvOper.*;

public class EnvOper2 implements TestBedOperation, Globals {
  /*! .enum  .private 4275
      plotbitan plotsine plotsimp compare rndorder tracep1  tracep2 
      _ three simpenv plotenv scale scalefactor
  */

  private static final int PLOTBITAN = 4275;//!
  private static final int PLOTSINE = 4276;//!
  private static final int PLOTSIMP = 4277;//!
  private static final int COMPARE = 4278;//!
  private static final int RNDORDER = 4279;//!
  private static final int TRACEP1 = 4280;//!
  private static final int TRACEP2 = 4281;//!
  private static final int THREE = 4283;//!
  private static final int SIMPENV = 4284;//!
  private static final int PLOTENV = 4285;//!
  private static final int SCALE = 4286;//!
  private static final int SCALEFACTOR = 4287;//!
  /*!*/
  private static final boolean FULL = UHullMain.FULL;

  private static boolean db;

  private EnvOper2() {
  }
  public static EnvOper2 singleton = new EnvOper2();

  public void addControls() {
    C.sOpenTab("Env");
    {
      C.sStaticText(//
      "Generate guaranteed convex hull of uncertain polygons, "
          + "using algorithm of Nagai, "
          + "Yasutome, and Tokura; function domain is slope of tangents");
      C.sCheckBox(PLOTENV, "plot env", "plot upper envelope", true);
      C.sCheckBox(PLOTSINE, "plot waves",
          "plot all functions, not just those forming upper envelope", false);
      C.sCheckBox(PLOTBITAN, "plot bitangents", null, false);
      if (FULL)
        C.sCheckBox(THREE, "three sections",
            "divide circle into three sections instead of four", true);

      if (FULL) {
        C.sCheckBox(SCALE, "scale polygons", null, false);
        C.sIntSlider(SCALEFACTOR, "factor:", null, 0, 1000, 500, 10);
        C.sCheckBox(RNDORDER, "random order",
            "shuffle segments before calculating envelope", false);
      }
      C.sCheckBox(TRACEP1, "trace pass 1", null, true);
      C.sCheckBox(TRACEP2, "trace pass 2", null, true);
      //C.sButton(ROT, "rotate", "rotate selected objects into next quadrant");
      C.sCheckBox(SIMPENV, "simp env",
          "construct envelope using simple O(nh) method", false);
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
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      }
    }
  }

  private static FPoint2 transform(FPoint2 pt, int quad) {
    final double CENTER_X = 50;
    final double CENTER_Y = 35;
    FPoint2 r = new FPoint2();
    double x = pt.x - CENTER_X;
    double y = pt.y - CENTER_Y;

    if (!three) {
      switch (quad) {
      default:
        r.setLocation(x, y);
        break;
      case 1:
        r.setLocation(y, -x);
        break;
      case 2:
        r.setLocation(-x, -y);
        break;
      case 3:
        r.setLocation(-y, x);
        break;
      }
    } else {
      final double C = .5, S = Math.sqrt(3) / 2;
      switch (quad) {
      default:
        r.setLocation(x, y);
        break;
      case 1:
        r.setLocation(-C * x + S * y, -S * x - C * y);
        break;
      case 2:
        r.setLocation(-C * x - S * y, S * x - C * y);
        break;
      }
    }
    r.x += CENTER_X;
    r.y += CENTER_Y;
    return r;
  }

  private EdObject transform(EdObject obj, int quad, Map origMap) {
    EdObject ret;

    if (obj instanceof EdDisc) {
      EdDisc d = (EdDisc) obj;
      d = new EdDisc(d);
      d.setPoint(0, transform(d.getOrigin(), quad));
      ret = d;
    } else {
      EdObject o2 = null;
      try {
        o2 = (EdObject) obj.getClass().newInstance();
      } catch (Exception e) {
        T.err(e);
      }

      //      FPoint2 orig = null;
      //   double t = C.vi(SCALEFACTOR) / 1000.0;

      //      if (trgn != null) {
      //        FRect r = obj.getBounds();
      //        orig = r.midPoint();
      //      }

      for (int i = 0; i < obj.nPoints(); i++) {
        FPoint2 p0 = obj.getPoint(i);
        //        if (orig != null) {
        //          p0 = FPoint2.interpolate(orig, p0, t);
        //         // System.out.println("interp to "+p0);
        //        }

        o2.setPoint(i, transform(p0, quad));
      }
      o2.setLabel(obj.getLabel());

      ret = o2;
    }
    if (origMap != null)
      origMap.put(ret, obj);
    //    if (quad == 0 && trgn != null)
    //      trgn.add(ret);
    return ret;
  }

//  private void showSequence(DArray env) {
//    if (TestBed.plotTraceMessages()) {
//      // display bitangent sequence in column on right
//      DArray te = new DArray();
//      MySeg prev = null;
//      for (int i = 0; i < env.size(); i++) {
//        MySeg seg = (MySeg) env.get(i);
//        if (prev != null && prev.source != seg.source) {
//          StringBuilder sb = new StringBuilder();
//          sb.append(prev.source);
//          if (prev.vertIndex >= 0)
//            sb.append(prev.vertIndex);
//          sb.append("-");
//          sb.append(seg.source);
//          if (seg.vertIndex >= 0)
//            sb.append(seg.vertIndex);
//          te.add(sb.toString());
//          //        te.add(new OrdEntry(null, (EdObject) prev.source, prev.vertIndex,
//          //            (EdObject) seg.source, seg.vertIndex));
//        }
//        prev = seg;
//      }
//      T.show(te.toString(true), null, 92, 100, 15 | TX_CLAMP);
//    }
//  }
  public static EdPolygon guaranteedHull(EdObject[] r) {
    singleton.silent = true;
    singleton.runAlg0(r);
    return singleton.hull;
  }

  public void runAlgorithm() {
    silent = false;
    EdObject[] obj = EnvOper.getRegions();
    if (obj != null && obj[0] instanceof EdPolygon)
      runAlg0(obj);
//    runAlg0(EnvOper.getRegions());
  }
  private EdObject scaleObj(EdObject obj) {

    double t = C.vi(SCALEFACTOR) / 1000.0;
    FRect r = obj.getBounds();
    FPoint2 orig = r.midPoint();

    EdObject o2 = null;
    try {
      o2 = (EdObject) obj.getClass().newInstance();
    } catch (Exception e) {
      T.err(e);
    }

    for (int i = 0; i < obj.nPoints(); i++) {
      FPoint2 p0 = obj.getPoint(i);
      p0 = FPoint2.interpolate(orig, p0, t);
      o2.setPoint(i, p0);
    }
    o2.setLabel(obj.getLabel());
    return o2;
  }

  private void runAlg0(EdObject[] reg) {
    regions = reg;
    three = true;
    bitan = null;
    hull = null;

    simpleHull = null;
    lowerEnvList = new DArray();
    int flags = 0;
    if (!silent) {
      three = !FULL || C.vb(THREE);

      if (!FULL || C.vb(RNDORDER))
        flags |= EnvTools.F_RNDORDER;
      if (C.vb(SIMPENV))
        flags |= EnvTools.F_SIMP;
    }

    if (regions == null || regions.length == 0)
      return;
    polyOrSeg = !(regions[0] instanceof EdDisc);

    if (polyOrSeg && (FULL && C.vb(SCALE))) {
      T.show(regions, MyColor.cLIGHTGRAY, STRK_THIN, -1);

      for (int i = 0; i < regions.length; i++) {
        regions[i] = scaleObj(regions[i]);
      }
      T.show(regions, MyColor.cBLUE, -1, -1);
    }

    DArray genv = new DArray();

    int dbQuad = 0;
    for (int quad = 0; quad < (three ? 3 : 4); quad++) {

      db = !silent &&  (quad == dbQuad);
      if (true || !db)
        flags &= ~EnvTools.F_TRACE;
      else
        flags |= EnvTools.F_TRACE;

      if (db && T.update())
        T.msg("calculating hull for quadrant " + quad);

      // construct transformed objects, based on quadrant
      Map origObj = new HashMap();
      EdObject[] tr = new EdObject[regions.length];
      for (int i = 0; i < regions.length; i++) {
        tr[i] = transform(regions[i], quad, origObj);
      }

      DArray env;
      if (polyOrSeg) {
        DArray firstEnvSegs = new DArray();
        int segId = 0;
        for (int i = 0; i < tr.length; i++) {
          Color c = EnvTools.multicolor(i);
          EdObject p = (EdObject) tr[i];

          DArray segs = new DArray();
          for (int j = 0; j < p.nPoints(); j++) {
            FPoint2 pt = p.getPoint(j);
            segs.add(new MySeg(segId++, c, pt, p, j));
          }
          if (db && T.update())
            T.msg("calculating upper envelope for polygon"
                + T.show(p, c, STRK_THICK, -1) + T.show(segs));

          DArray pEnv = EnvTools.calcUpper(segs, flags
              | (C.vb(TRACEP1) ? EnvTools.F_TRACE : 0));
          if (db && C.vb(PLOTENV) && C.vb(PLOTSINE)) {
            T.show(segs, c, STRK_THIN, -1);
            T.show(pEnv, c, STRK_NORMAL, MARK_DISC);
          }
          if (db && T.update())
            T.msg("upper envelope for polygon"
                + T.show(segs, null, STRK_RUBBERBAND, -1)
                + T.show(p, c, STRK_THICK, -1) + T.show(pEnv));
          firstEnvSegs.addAll(pEnv);
        }
        env = EnvTools.calcLower(firstEnvSegs, flags
            | (C.vb(TRACEP2) ? EnvTools.F_TRACE : 0));

        if (db && T.update())
          T.msg("lower envelope for upper envelopes"
              + T.show(firstEnvSegs, MyColor.cRED, -1, -1)
              + T.show(env, null, STRK_THICK, -1));
      } else {

        DArray segs = new DArray();

        for (int i = 0; i < tr.length; i++) {
          EdDisc seg = (EdDisc) tr[i];
          segs.add(new MySeg(i, seg));
        }
        if (!silent && C.vb(PLOTENV) && C.vb(PLOTSINE))
          T.show(segs, MyColor.cLIGHTGRAY, -1, -1);

        env = EnvTools.calcUpper(segs, flags
            | (!silent && C.vb(TRACEP2) ? EnvTools.F_TRACE : 0));
      }

      if (db) {
        if (C.vb(PLOTENV))
          T.show(env, MyColor.cRED, STRK_NORMAL, MARK_DISC);
        //showSequence(env);
      }

      // transform each edge in envelope back to original quadrant,
      // add to main list
      for (int i = 0; i < env.size(); i++) {
        MySeg s = (MySeg) env.get(i);
        if (!polyOrSeg) {
          s.source = origObj.get(s.source);
          //          s.disc = (EdDisc) origObj.get(s.disc);
        } else {
          MySeg s2 = new MySeg(s.id(), s.color, s.loc, origObj.get(s.source),
              s.vertIndex);
          EdObject poly = (EdObject) s2.source;
          s2.loc = poly.getPoint(s2.vertIndex);
          s = s2;
        }
        genv.add(s);
      }
    }

    db = !silent;

    constructHull(genv, polyOrSeg ? regions : null);
    lowerEnvList = null;

    if (!silent && C.vb(COMPARE) && hull != null) {
      EdPolygon simp = simpleHull();
      if (simp != null) {
        String diff = HullUtil.compareHulls(hull, simp);
        if (diff != null)
          T.err("Generated hull and simple version don't agree:\n" + diff);
      }
    }
    silent = false;
  }

  private void constructHull(DArray env, EdObject[] polys) {
    if (db && T.update())
      T.msg("constructing hull bitangents from quadrant envelopes"
          + T.show(env, MyColor.cRED));

    if (env.size() >= 2) {
      MySeg prev = (MySeg) env.last();
      bitan = new DArray();
      for (int i = 0; i < env.size(); i++) {
        MySeg seg = (MySeg) env.get(i);
        BiTangent b = null;
        if (!polyOrSeg) {
          if (prev.source != seg.source) {
            b = new BiTangent(prev.disc(), seg.disc());
          }
        } else {
          if (prev.source != seg.source) {
            EdObject p1 = (EdObject) prev.source;
            EdObject p2 = (EdObject) seg.source;

            b = new BiTangent(prev.loc, p1.getLabel(), seg.loc, p2.getLabel());
            if (!b.defined())
              T.err("not defined");
          }
        }
        if (b != null) {
          bitan.add(b);
          if (db && T.update())
            T.msg("adding bitangent:" + T.show(b));
        }
        prev = seg;
      }
      if (false && C.vb(PLOTBITAN) && bitan != null)
        T.show(HullUtil.bitangentsString(bitan), null, 0, 100, -1);

      hull = HullUtil.hullFromBiTangents(bitan, polys);
    }
  }

  public void paintView() {
    if (!(FULL && C.vb(SCALE)))
      Editor.render();

    if (C.vb(PLOTENV)) {
      T.render(lowerEnvList, null, STRK_THIN, MARK_X);
    }

    if (C.vb(PLOTSIMP)) {
      T.render(simpleHull(), MyColor.cLIGHTGRAY, STRK_THICK, -1);
    }
    if (C.vb(PLOTBITAN)) {
      T.render(bitan, BiTangent.BITAN_COLOR, STRK_THIN, -1);
    }
    HullUtil.render(hull);
  }

  /**
   * Generate hull polygon using simple, slow algorithm (if not already
   * generated)
   * @return hull polygon
   */
  private EdPolygon simpleHull() {
    if (simpleHull == null) {
      if (regions != null) {
        simpleHull = HullUtil.build(regions);
      }
    }
    return simpleHull;
  }
  private EdPolygon simpleHull;
  private DArray bitan;
  private EdPolygon hull;
  private EdObject[] regions;
  private DArray lowerEnvList;
  private boolean polyOrSeg;
  private static boolean three;
  private boolean silent;

  /*
   * Generalized segment class
   */
  private static class MySeg implements GenSeg {
    private static final boolean db = false;

    public int id() {
      return id;
    }
    public MySeg(int id, Color color, FPoint2 pt, Object source, int vertIndex) {
      this.id = id;
      this.color = color;
      this.loc = pt;
      this.radius = 0;
      this.source = source;
      this.vertIndex = vertIndex;

      setLeftRight();
      if (db)
        Streams.out.println("MySeg constructed, pt=" + pt + " id=" + id
            + " left=" + left + " right=" + right);
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(source);
      if (vertIndex >= 0)
        sb.append(vertIndex);
      return sb.toString();
    }
    private void setLeftRight() {
      if (three) {
        left = -RANGE3;
        right = RANGE3;
      } else {
        left = -1;
        right = 1;
      }
    }

    public MySeg(int id, EdDisc disc) {
      this.id = id;
      this.source = disc;
      this.vertIndex = -1;
      this.loc = disc.getOrigin();
      this.radius = disc.getRadius();
      setLeftRight();
      if (db)
        Streams.out.println("MySeg constructed, loc=" + loc + " id=" + id
            + " left=" + left + " right=" + right);
    }

    private boolean isDisc() {
      return source instanceof EdDisc;
    }
    public GenSeg clipTo(double left, double right) {
      if (left >= right || left < this.left || right > this.right)
        throw new IllegalArgumentException();

      MySeg s;
      if (isDisc())
        s = new MySeg(id, disc());
      else
        s = new MySeg(id, color, loc, source, vertIndex);

      s.left = left;
      s.right = right;
      return s;
    }

    private double yAt(double s) {
      double w = Math.sqrt(1 + s * s);
     // return 1.3 * (loc.y * s - loc.x) / w - radius + 30;
      return 1.3 * (s*loc.x - loc.y) / w - radius + 30;
    }

    public GenSeg higherOf(GenSeg seg1, GenSeg seg2, double s) {
      MySeg s1 = (MySeg) seg1;
      MySeg s2 = (MySeg) seg2;
      double yDiff;

      if (s1.radius > 0 || s2.radius > 0) {
        yDiff = s1.yAt(s) - s2.yAt(s);
      } else {
        yDiff = s * (s1.loc.x - s2.loc.x) - (s1.loc.y - s2.loc.y);
      }

      GenSeg ret = null;
      if (yDiff < 0)
        ret = seg2;
      if (yDiff > 0)
        ret = seg1;

      if (db && T.update())
        T.msg("higherOf" + T.show(seg1, MyColor.cPURPLE)
            + T.show(seg2, MyColor.cDARKGREEN) + " equals "
            + T.show(ret, MyColor.cRED));
      return ret;
    }

    public double left() {
      return left;
    }

    public Double nextIntersection(GenSeg seg1, GenSeg seg2, double pastX) {
      boolean db = false;

      MySeg s1 = (MySeg) seg1;
      MySeg s2 = (MySeg) seg2;
      Double ret = null;

      if (s1.radius > 0 || s2.radius > 0) {

        double p = s2.radius - s1.radius;
        double q = s2.loc.x - s1.loc.x;
        double t = s1.loc.y - s2.loc.y;
        double A, B, C;

        A = q * q - p * p;
        B = 2 * q * t;
        C = t * t - p * p;

        double discrim = B * B - 4 * A * C;
        if (db && T.update())
          T.msg("nextIntersection"
              + T.show(seg1, MyColor.cPURPLE, STRK_THICK, -1)
              + T.show(seg2, MyColor.cDARKGREEN, STRK_THICK, -1) + " pastX "
              + Tools.f(pastX) + "\n s1=" + s1.loc + " r1="
              + Tools.f(s1.radius) + "\n s2=" + s2.loc + " r2="
              + Tools.f(s2.radius) + "\n p=" + Tools.f(p) + " q=" + Tools.f(q)
              + " t=" + Tools.f(t) + "\n A=" + Tools.f(A) + " B=" + Tools.f(B)
              + " C=" + Tools.f(C) + "\n discrim=" + discrim); //Tools.f(discrim));

        if (discrim >= 0) {
          double R = Math.sqrt(discrim);
          if (db && T.update())
            T.msg("sqrt(discrim)=" + R);
          boolean found = false;
          double tBest = -1;
          for (int pass = 0; pass < 2; pass++) {
            double r = (pass == 0) ? (-B + R) / (2 * A) : (-B - R) / (2 * A);
            if (db && T.update())
              T.msg("pass=" + pass + " r=" + r + " pastX=" + pastX);
            if (r <= pastX)
              continue;
            if (found && r >= tBest)
              continue;
            if (r > Math.min(seg1.right(), seg2.right()))
              continue;

            double y1 = s1.yAt(r);
            double y2 = s2.yAt(r);

            if (db && T.update())
              T.msg("y1=" + y1 + " y2=" + y2);

            {
              final double EPSILON = 1e-2;

              if (Math.abs(y1 - y2) > EPSILON)
                continue;
            }

            found = true;
            tBest = r;
            if (db && T.update())
              T.msg("found=true, tBest=" + r);
          }
          if (found)
            ret = new Double(tBest);
        }
      } else {

        double xd = s1.loc.x - s2.loc.x;
        double yd = s1.loc.y - s2.loc.y;
        
        if (xd != 0) {
          double s = yd/xd;
          if (s > pastX && s <= seg1.right() && s <= seg2.right())
            ret = new Double(s);
        }
      }

      if (db && T.update())
        T.msg("nextIntersection" + T.show(seg1, MyColor.cPURPLE)
            + T.show(seg2, MyColor.cDARKGREEN) + " pastX " + Tools.f(pastX)
            + " equals " + ret);

      return ret;
    }
    public double right() {
      return right;
    }
    private FPoint2 pt(double s) {
      return new FPoint2( tfmX(s),  yAt(s)  );
    }

    private static final double RANGE4 = 1;
    private static final double RANGE3 = 1.732050808;

    private static double tfmX(double s) {
      double ret;

      if (three) {
        ret = (s + RANGE3) * (Math.PI / RANGE3);
      } else {
        ret = (s + RANGE4) * (Math.PI / RANGE4);
      }
      return ret;
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
      V.pushStroke(stroke);
      plotInterval(left, right);
      if (markType >= 0) {
        if (left != 0)
          V.mark(EnvTools.transform(pt(left)), markType, .5);
        if (right != Math.PI * 2)
          V.mark(EnvTools.transform(pt(right)), markType, .5);
      }

      V.pop(2);
      if (markType >= 0 && Editor.withLabels(false) && isDisc()) {
        V.pushColor(MyColor.cDARKGRAY);
        V.pushScale(.55);
        double t = (left + right) * .5;

        FPoint2 loc = EnvTools.transform(pt(t));
        V.draw(source.toString(), loc.x, loc.y + 2, Globals.TX_BGND
            | Globals.TX_FRAME);
        V.pop(2);
      }
    }

    public String showSweepLine(double x) {

      double PAD = .05;
      double RNG = three ? RANGE3 : RANGE4;

      x = MyMath.clamp(x, -RNG - PAD, RNG + PAD);
      {
        FPoint2 p1 = EnvTools.transform(new FPoint2(tfmX(x), -130));
        FPoint2 p2 = EnvTools.transform(new FPoint2(tfmX(x), 130));
        Object ret = new EdSegment(p1, p2);
        T.show(ret, MyColor.cDARKGREEN, Globals.STRK_NORMAL, -1);
      }
      return "";
    }

    public EdDisc disc() {
      if (!isDisc())
        throw new IllegalStateException();
      return (EdDisc) source;
    }

    private Color color;
    private FPoint2 loc;
    private double radius;
    private double left, right;
    private Object source;
    private int vertIndex;
    // unique id to enforce consistent ordering
    private int id;
  }
}
