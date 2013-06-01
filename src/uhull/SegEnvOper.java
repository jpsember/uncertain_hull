package uhull;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;
import uhull.EnvTools.*;

public class SegEnvOper implements TestBedOperation, Globals {
  /*! .enum  .private 3400
      simpenv
   */

    private static final int SIMPENV          = 3400;//!
/*!*/

  public void addControls() {
    C.sOpenTab("SegEnv");
    C.sStaticText("Upper envelope of line segments");
    C.sCheckBox(SIMPENV, "simp env",
        "construct envelope using simple O(nh) method", false);
    C.sCloseTab();
  }

  public static SegEnvOper singleton = new SegEnvOper();

  private SegEnvOper() {
  }

  public void processAction(TBAction a) {
  }

  private DArray buildSegs() {
    origSegs = Editor.readObjects(EdSegment.FACTORY, false, true);
    DArray ret = buildGenSegs(origSegs); //new DArray();

    //    for (int i = 0; i < origSegs.size(); i++) {
    //      EdSegment seg = (EdSegment) origSegs.get(i);
    //      FPoint2 p1 = seg.getPoint(0);
    //      FPoint2 p2 = seg.getPoint(1);
    //      if (p1.x == p2.x)
    //        continue;
    //      ret.add(new MySeg(i, seg));
    //    }
    return ret;
  }

  public void runAlgorithm() {
    env = null;
    DArray segs = buildSegs();
    env = EnvTools.calcUpper(segs, EnvTools.F_RNDORDER
        | (C.vb(SIMPENV) ? EnvTools.F_SIMP : 0));
  }

  public void paintView() {
    Editor.render();
    T.render(env, null, -1, MARK_DISC);
  }
  private static class MySeg implements GenSeg {
    private int id;
    public MySeg(int id, EdSegment seg) {
      this.id = id;
      this.seg = seg;
      FPoint2 p1 = seg.getPoint(0);
      FPoint2 p2 = seg.getPoint(1);
      if (p1.x > p2.x) {
        FPoint2 tmp = p1;
        p1 = p2;
        p2 = tmp;
      }
      line = new LineEqn(p1, p2);
      left = p1.x;
      right = p2.x;
    }
    public int id() {
      return id;
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("MySeg[");
      sb.append(Tools.f(left));
      sb.append('/');
      sb.append(Tools.f(right));
      sb.append(']');
      return sb.toString();
    }
    private MySeg(int id) {
      this.id = id;
    }
    public GenSeg clipTo(double left, double right) {
      if (left >= right || left < this.left || right > this.right)
        throw new IllegalArgumentException();

      MySeg s = new MySeg(id());
      s.seg = this.seg;
      s.line = this.line;
      s.left = left;
      s.right = right;
      return s;
    }

    public FPoint2 pt(double val) {
      return new FPoint2(val, line.yAt(val));
    }

    public GenSeg higherOf(GenSeg seg1, GenSeg seg2, double x) {
      MySeg s1 = (MySeg) seg1;
      MySeg s2 = (MySeg) seg2;
      double yDiff = s1.line.yAt(x) - s2.line.yAt(x);
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

    public Double nextIntersection(GenSeg seg1, GenSeg seg2, double pastX) {
      Double ret = null;
      do {
        MySeg s1 = (MySeg) seg1;
        MySeg s2 = (MySeg) seg2;

        FPoint2 pt = LineEqn.intersection(s1.line, s2.line);
        if (pt == null)
          break;
        if (pt.x <= pastX)
          break;
        if (pt.x > seg1.right())
          break;
        if (pt.x > seg2.right())
          break;
        if (pt.x < seg1.left())
          break;
        if (pt.x < seg2.left())
          break;
        ret = new Double(pt.x);
      } while (false);

      return ret;
    }

    public double right() {
      return right;
    }

    public void render(Color c, int stroke, int markType) {
      V.pushStroke(stroke);
      V.pushColor(c, MyColor.cRED);
      FPoint2 p1 = pt(left); //new FPoint2(left, line.yAt(left));
      FPoint2 p2 = pt(right); //new FPoint2(right, line.yAt(right));
      if (markType >= 0) {
        V.mark(p1, markType, .7);
        V.mark(p2, markType, .7);
      }
      V.drawLine(p1, p2);
      V.pop(2);
    }
    public String showSweepLine(double theta) {
      double PAD = 10;
      theta = MyMath.clamp(theta, -PAD, PAD + 100);

      {
        Object ret = new EdSegment(theta, 0, theta, 100);
        T.show(ret, MyColor.cDARKGREEN, Globals.STRK_NORMAL, -1);
      }
      return "";
    }

    private EdSegment seg;
    private double left, right;
    private LineEqn line;
  }
  /**
   * Calculate the upper envelope of a set of EdSegments
   * @param segments set of EdSegments
   * @return upper envelope EdSegments
   */
  public static DArray calcUpperEnvelope(Collection segments) {
    DArray segs = buildGenSegs(segments);
    DArray es = EnvTools.calcUpper(segs, EnvTools.F_RNDORDER);
    DArray ret = new DArray();
    for (int i = 0; i < es.size(); i++) {
      MySeg s = (MySeg) es.get(i);
      ret.add(new EdSegment(s.pt(s.left), s.pt(s.right)));
    }
    return ret;
  }

  public static DArray buildGenSegs(Collection segments) {
    DArray segs = new DArray();
    int i = 0;
    for (Iterator it = segments.iterator(); it.hasNext(); i++) {
      EdSegment s = (EdSegment) it.next();
      FPoint2 p1 = s.getPoint(0);
      FPoint2 p2 = s.getPoint(1);
      if (p1.x == p2.x)
        continue;
      segs.add(new MySeg(i, s));
    }
    return segs;
  }

  private DArray origSegs;
  private DArray env;
}
