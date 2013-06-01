package uhull;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;
import uhull.EnvTools.*;

public class HullFromBitanOper implements TestBedOperation, Globals {
  /*! .enum  .private 1950  radius test sortoffset 
   */

  private static final int RADIUS = 1950;//!
  private static final int TEST = 1951;//!
  private static final int SORTOFFSET = 1952;//!
  /*!*/

  public void addControls() {
    C.sOpenTab("Halfplanes");
    C
        .sStaticText("Calculates intersection of halfplanes that are sorted by polar angle.\n"
            + "Place a number of segments representing (directed) halfplane lines.");
    C.sCheckBox(TEST, "pass 2 first", "perform pass 2 first (fails; pass 1 must be performed first)", false);
    C.sIntSlider(RADIUS, "radius", "radius for filling gaps", 10, 1000, 100, 1);
    C.sIntSlider(SORTOFFSET, "sort offset",
        "constant to add to angles for sorting", 0, 360, 0, 10);
    C.sCloseTab();
  }

  public static HullFromBitanOper singleton = new HullFromBitanOper();

  private HullFromBitanOper() {
  }

  public void processAction(TBAction a) {
  }

  /**
   * Construct intersection of sorted half planes
   * @param segs array of at least two MySegs, 
   *    representing half planes; sorted by polar angle
   * @return polygon, or null if intersection is empty
   */
  public static EdPolygon clipSortedHalfPlanes(DArray segs) {
    if (segs.size() < 2)
      throw new IllegalArgumentException();
    Inf inf = Inf.create();

    final boolean db = true;
    hullPoly = null;
    hullSegs = new DQueue();
    currSeg = null;

    int sortSegIndex = 0;

    MySeg s1 = (MySeg) segs.get(sortSegIndex++);
    hullSegs.push(s1, false);
    MySeg s2 = (MySeg) segs.get(sortSegIndex++);

    // determine if we need to fill a gap > pi 
    // in initial segs
    if (!MySeg.ccw(s1, s2)) {
      s2 = buildFillGapLine(s1, s2);
      sortSegIndex--;
      if (db && T.update())
        T.msg("filling gap in initial lines" + T.show(getSeg(0, false)));
    }
    hullSegs.push(s2, false);

    if (db && T.update())
      T.msg("pushed initial two lines");

    while (true) {
      Inf.update(inf);
      MySeg seg = null;
      if (sortSegIndex < segs.size()) {
        seg = (MySeg) segs.get(sortSegIndex);
      } else {
        // no segs remain; if we need to close a gap,
        // add special gap seg
        seg = getSeg(0, true);
      }
      sortSegIndex++;

      // determine if we need to close a gap of > pi
      {
        MySeg sPrev = getSeg(0, false);
        if (!MySeg.ccw(sPrev, seg)) {
          seg = buildFillGapLine(sPrev, seg);
          if (db && T.update())
            T.msg("filling gap" + T.show(seg));
          sortSegIndex--;
        }
      }

      // if we have processed all segments, and we're not
      // closing a gap, exit
      if (sortSegIndex > segs.size())
        break;

      currSeg = seg;

      // if current polygon is bounded, and its
      // bounding vertex lies to the right of the new line, skip this line.
      {
        MySeg sFirst = getSeg(0, true);
        MySeg sLast = getSeg(0, false);
        if (MySeg.ccw(sLast, sFirst)) {
          FPoint2 vert = MySeg.isect(sFirst, sLast);
          if (!seg.toRight(vert)) {
            if (db && T.update())
              T.msg("closing vertex is left of new line; skipping"
                  + T.show(vert));
            continue;
          }
        }
      }

      for (int clipPass = 0; clipPass < 2; clipPass++) {
        boolean clipFront = (clipPass == 1)  ^ (C.vb(TEST));

        while (true) {
          if (hullSegs.size() < 2) {
            if (db && T.update())
              T.msg("no vertices in queue," + " done clipping from "
                  + fb(clipFront));
            break;
          }
          MySeg a = getSeg(1, clipFront);
          MySeg b = getSeg(0, clipFront);
          FPoint2 ab = MySeg.isect(a, b);
          if (!seg.toRight(ab)) {
            if (db && T.update())
              T
                  .msg(fb(clipFront)
                      + " vertex not right of new line, stop clipping"
                      + T.show(ab));
            break;
          }
          MySeg popped = (MySeg) hullSegs.pop(clipFront);

          if (db && T.update())
            T.msg("popping line to clip " + fb(clipFront) + " vert"
                + T.show(popped) + T.show(ab));

          if (b.toRight(MySeg.isect(seg, a)) && a.toRight(MySeg.isect(seg, b))) {
            if (db && T.update())
              T.msg("detected empty intersection");
            hullSegs = null;
            currSeg = null;
            return null;
          }
        }
      }
      hullSegs.push(seg, false);
    }
    currSeg = null;
    hullPoly = buildPolygon(hullSegs);
    hullSegs = null;
    return hullPoly;
  }

  /**
   * Construct a line near infinity that fills a gap of > pi
   * @param s1 first seg
   * @param s2 next seg
   * @return segment that has angle between the two, so neither new gap is > pi;
   *    segment is far away, so resulting polygon looks unbounded
   */
  private static MySeg buildFillGapLine(MySeg s1, MySeg s2) {
    final boolean db = false;
    double sep = MyMath.normalizeAnglePositive(s2.angle - Math.PI - s1.angle);

    if (db && T.update())
      T.msg("buildCloseGapSeg for " + T.show(s1) + T.show(s2, MyColor.cBLUE)
          + " a1=" + Tools.fa(s1.angle) + " a2=" + Tools.fa(s2.angle) + " sep="
          + Tools.fa2(sep));
    FPoint2 mid = FPoint2.midPoint(s1.mid, s2.mid);
    double th = s1.angle + sep / 2;

    if (db && T.update())
      T.msg("sep=" + Tools.fa(sep) + " close theta=" + Tools.fa(th));
    final double RAD = C.vi(RADIUS);
    FPoint2 pt0 = MyMath.ptOnCircle(mid, th, RAD);
    EdSegment seg = new EdSegment(pt0, MyMath.ptOnCircle(pt0, th + Math.PI / 2,
        10));
    return new MySeg(seg);
  }

  /**
   * Get segment from queue
   * @param dist distance from end of queue
   * @param fromFront true for front, false for back
   * @return segment
   */
  private static MySeg getSeg(int dist, boolean fromFront) {
    return (MySeg) hullSegs.peek(dist, fromFront);
  }
  private static String fb(boolean front) {
    return front ? "front" : "back";
  }
  public void runAlgorithm() {
    hullPoly = null;
    hullSegs = null;
    currSeg = null;

    DArray origSegs = Editor.readObjects(EdSegment.FACTORY, false, true);
    DArray segs = buildHalfplanes(origSegs);
    if (segs.size() < 2)
      return;
    clipSortedHalfPlanes(segs);
  }

  /**
   * Build polygon from queued lines.  
   * Fills in gaps with segs near infinity.
   * @return polygon 
   */
  private static EdPolygon buildPolygon(DQueue segs) {
    EdPolygon hullPoly = null;

    if (segs.size() < 2)
      T.err("illegal argument");
    hullPoly = new EdPolygon();
    MySeg sPrev = (MySeg) segs.peek(0, false);
    for (int i = 0; i < segs.size(); i++) {
      MySeg sCurr = (MySeg) segs.peek(i, true);

      // determine if we need to fill a gap > pi 
      // in initial segs
      if (!MySeg.ccw(sPrev, sCurr)) {
        sCurr = buildFillGapLine(sPrev, sCurr);
        i--;
      }
      FPoint2 newVert = null;
      newVert = MySeg.isect(sPrev, sCurr);
      hullPoly.addPoint(newVert);
      sPrev = sCurr;
    }
    return hullPoly;
  }

  public void paintView() {
    EdPolygon hp = hullPoly;
    Color c = Color.green;
    if (hp == null)
      c = Color.LIGHT_GRAY;

    if (hp == null && hullSegs != null && hullSegs.size() >= 2)
      hp = buildPolygon(hullSegs);

    if (hp != null)
      hp.fill(c);

    Editor.render();
    if (hullSegs != null) {
      for (int i = 0; i < hullSegs.size(); i++) {
        MySeg seg = getSeg(i, true);
        T.render(seg, MyColor.cDARKGREEN, STRK_NORMAL, -1);
        V.draw("" + i, seg.pt(0), TX_FRAME | TX_BGND);
      }

    }
    T.render(currSeg, MyColor.cRED, STRK_THICK, -1);
  }

  public static DArray buildHalfplanes(Collection segments) {
    DArray segs = new DArray();
    for (Iterator it = segments.iterator(); it.hasNext();) {
      EdSegment s = (EdSegment) it.next();
      FPoint2 p1 = s.getPoint(0);
      FPoint2 p2 = s.getPoint(1);
      if (p1.x == p2.x)
        continue;
      segs.add(new MySeg(s));
    }

    // sort lines by polar angle
    segs.sort(new Comparator() {
      public int compare(Object arg0, Object arg1) {
        MySeg s0 = (MySeg) arg0;
        MySeg s1 = (MySeg) arg1;
        double OFFSET = MyMath.radians(C.vi(SORTOFFSET));
        return Double.compare(MyMath.normalizeAngle(s0.angle + OFFSET), MyMath
            .normalizeAngle(s1.angle + OFFSET));
      }
    });

    return segs;
  }

  private static class MySeg implements Renderable {

    /**
     * Determine if seg b is ccw from seg a
     * @param a
     * @param b
     * @return true if angle(b) in angle(a)...angle(a)+pi
     */
    public static boolean ccw(MySeg a, MySeg b) {
      return MyMath.normalizeAngle(b.angle - a.angle) >= 0;
    }
    public static FPoint2 isect(MySeg a, MySeg b) {
      FPoint2 isect = MyMath.linesIntersection(a.p0, a.p1, b.p0, b.p1, null);
      if (isect == null)
        T.msg("no intersection" + T.show(a) + T.show(b));
      return isect;
    }

    public void render(Color c, int stroke, int markType) {
      V.pushColor(c, MyColor.cRED);
      V.pushStroke(STRK_THIN);
      double rad = 50;
      V.drawLine(pt(-rad), pt(rad));
      V.pop();
      V.pushStroke(stroke, STRK_NORMAL); //STRK_THICK);
      EdSegment.plotDirectedLine(p0, p1);
      V.pop(2);
    }
    public MySeg(EdSegment seg) {
      p0 = seg.getPoint(0);
      p1 = seg.getPoint(1);
      angle = MyMath.polarAngle(p0, p1);
      mid = FPoint2.midPoint(p0, p1);
    }
    public FPoint2 pt(double t) {
      return MyMath.ptOnCircle(mid, angle, t);
    }
    public boolean toRight(FPoint2 pt) {
      return MyMath.sideOfLine(p0, p1, pt) < 0;
    }

    public double angle;
    public FPoint2 p0, p1;
    public FPoint2 mid;
  }

  private static DQueue hullSegs;
  private static EdPolygon hullPoly;
  private static MySeg currSeg;
}
