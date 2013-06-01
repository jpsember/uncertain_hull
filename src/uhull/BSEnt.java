package uhull;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class BSEnt implements Renderable {

  public static final int SEARCH_NONE = 0;
  public static final int SEARCH_NORMAL = 1;

  public static final double THETA_MIN = Math.PI * 0.5;
  public static final double THETA_MAX = Math.PI * 1.5;

  private BSEnt(boolean searching, EdDisc disc, double theta, EdDisc d2,
      double length) {
    this.searching = searching;
    this.disc = disc;
    this.disc2 = d2;
    this.length = length;
    this.theta = MyMath.normalizeAnglePositive(theta);
  }
  public BSEnt(EdDisc d, double theta, EdDisc d2, double length) {
    this(false, d, theta, d2, length);
  }

  public static BSEnt searchEntry(EdDisc d, double theta) {
    return new BSEnt(true, d, theta, null, 0);
  }

  public double theta() {
    return theta;
  }

  public EdDisc disc() {
    return disc(0);
  }
  public EdDisc disc(int index) {
    return index == 0 ? disc : disc2;
  }

  public void render(Color c, int stroke, int markType) {
    if (c == null)
      c = MyColor.cRED;
    if (!removed) {
      FPoint2 tangentPt = disc.polarPoint(theta + Math.PI / 2);
      V.pushColor(c);
      V.pushStroke(stroke, Globals.STRK_THIN);
      if (length > 0) {
        FPoint2 pt1 = MyMath.ptOnCircle(tangentPt, theta, length);
        V.drawLine(tangentPt, pt1);
      } else if (!searching) {
        FPoint2 pt2 = MyMath.ptOnCircle(tangentPt, theta, 5);
        EdSegment.plotDirectedLine(tangentPt, pt2);
      }
      V.pop(2);
    }

    if (disc.getLabel().equals("~")) {
      Tools.warn("skipping for presentation purposes");
      return;
    }

    disc.render(c, stroke, markType);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (searching) {
      sb.append("normal:");
    }
    sb.append(disc.getLabel());
    if (!searching) {
      sb.append(' ');
      sb.append(Tools.fa(theta));
    }

    sb.append("}");
    return sb.toString();
  }

  public static boolean isUpperEnv(double theta) {
    double t = MyMath.normalizeAnglePositive(theta);
    return (t >= THETA_MIN && t < THETA_MAX);
  }

  public static final Comparator comparator = new Comparator() {
    public int compare(Object arg0, Object arg1) {
      boolean db = false;
      if (arg0 == arg1) {
        return 0;
      }

      double ret;
      BSEnt ent0 = (BSEnt) arg0;
      BSEnt ent1 = (BSEnt) arg1;

      StringBuilder sb = null;
      BiTangent b = null;

      if (!ent0.searching) {
        ret = ent0.theta() - ent1.theta();
      } else {
        db = false;
        if (db)
          sb = new StringBuilder();

        EdDisc a = ent1.disc();
        EdDisc query = ent0.disc();

        b = new BiTangent(query, a);
        double minTheta = ent0.theta;
        ret = 1;
        if (b.thetaP() > minTheta && b.thetaP() < ent1.theta())
          ret = -1;

        if (db)
          sb.append("compare from q=" + query + " to a=" + ent1
              + "\n minTheta=" + Tools.fa2(minTheta) + "\n\n");

      }

      int iret = MyMath.sign(ret);
      if (db && T.update()) {
        sb.append("compared\n " + arg0 + "\n " + arg1 + "\n  ===> returning "
            + iret);
        T.msg(sb.toString() + T.show(arg0) + T.show(arg1)
            + T.show(b, MyColor.cDARKGREEN));
      }
      return iret;
    }
  };

  public static void join(BSEnt e1, BSEnt e2) {
    if (e1 != null)
      e1.next = e2;
    if (e2 != null)
      e2.prev = e1;
  }

  public BSEnt prev() {
    return prev;
  }
  public BSEnt next() {
    return next;
  }
  public void setRemoved() {
    this.removed = true;
  }

  private BSEnt prev, next;
  private double theta;
  private EdDisc disc;
  private EdDisc disc2;
  private boolean searching;
  // for display purposes only
  private double length;
  private boolean removed;
}
