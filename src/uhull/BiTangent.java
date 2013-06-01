package uhull;

import java.awt.*;
import testbed.*;
import base.*;

public class BiTangent implements Renderable, Globals {

  //  public static void setOldMethod(boolean f) {
  //    oldMethod = f;
  //  }
  //  private static boolean oldMethod = false;

  public BiTangent(EdDisc a, EdDisc b) {
    final boolean db = false;

    if (db)
      Streams.out.println("BiTangent for " + a + ", " + b);

    construct(a, b);
  }

  private void construct(EdDisc a, EdDisc b) {
    this.discA = a;
    this.discB = b;

    if (EdDisc.partiallyDisjoint(a, b)) {

      //if (!UHullMain.oldBitanMethod())
      {

        final boolean db = false;

        if (a.getRadius() == b.getRadius()) {
          FPoint2 oa = a.getOrigin(), ob = b.getOrigin();
          
          FPoint2 n = new FPoint2(-(ob.y - oa.y),ob.x - oa.x);
          n.normalize();
          n.x *= a.getRadius();
          n.y *= a.getRadius();
          seg = new DirSeg(FPoint2.add(oa,n,null),FPoint2.add(ob,n,null));
          return;
        }
        
        boolean swap = a.getRadius() > b.getRadius();

        if (swap) {
          b = (EdDisc) discA;
          a = (EdDisc) discB;
        }

        if (db && T.update())
          T.msg("BiTangent construct, arad=" + Tools.f(a.getRadius())
              + " brad=" + Tools.f(b.getRadius()) + " swap=" + swap
              + " origin.a=" + T.show(a.getOrigin()));

        FPoint2 oa = a.getOrigin();
        FPoint2 ob = b.getOrigin();

        double U = ob.x, V = ob.y;
        double A = oa.x - U, B = oa.y - V;
        double R1 = a.getRadius();
        double R2 = b.getRadius();
        double S = R2 - R1;

        double x1, y1, x2, y2;
        x1 = A;
        y1 = B;

        boolean secondRoot;
        boolean altSlope = Math.abs(B) < Math.abs(A);
        if (!altSlope) {

          double C1 = S * S / B, C2 = -A / B;
          double qA = 1 + C2 * C2, qB = 2 * C1 * C2, qC = C1 * C1 - S * S;
          double root = Math.sqrt(qB * qB - 4 * qA * qC);
          x2 = (-qB - root) / (2 * qA);
          y2 = C1 + C2 * x2;

          secondRoot = MyMath.sideOfLine(x2, y2, A, B, 0, 0) < 0;

          if (swap ^ secondRoot) {
            x2 = (-qB + root) / (2 * qA);
            y2 = C1 + C2 * x2;
          }
        } else {

          double C1 = S * S / A, C2 = -B / A;
          double qA = 1 + C2 * C2, qB = 2 * C1 * C2, qC = C1 * C1 - S * S;
          double root = Math.sqrt(qB * qB - 4 * qA * qC);
          y2 = (-qB - root) / (2 * qA);
          x2 = C1 + C2 * y2;

          secondRoot = MyMath.sideOfLine(x2, y2, A, B, 0, 0) < 0;

          if (swap ^ secondRoot) {
            y2 = (-qB + root) / (2 * qA);
            x2 = C1 + C2 * y2;
          }
        }
        // now grow both discs back to r1, r2

        double tx = U;
        double ty = V;

//        if (S == 0) {
//          FPoint2 unit = new FPoint2(-A, -B);
//          if (swap) {
//            unit.x = -unit.x;
//            unit.y = -unit.y;
//          }
//          unit.normalize();
//          tx += -unit.y * R1;
//          ty += unit.x * R1;
//        } else 
        {
          double F = R1 / S;
          tx += x2 * F;
          ty += y2 * F;
        }

        if (db && T.update())
          T.msg("adding offset to both points: " + tx + ", " + ty
              + T.show(new FPoint2(tx, ty)));
        x1 += tx;
        y1 += ty;
        x2 += tx;
        y2 += ty;
        FPoint2 p1 = new FPoint2(x1, y1);
        FPoint2 p2 = new FPoint2(x2, y2);
        if (swap) {
          FPoint2 tmp = p1;
          p1 = p2;
          p2 = tmp;
        }

        seg = new DirSeg(p1, p2);

        if (db && T.update())
          T.msg("swap=" + swap + " altSlope=" + altSlope + " secondRoot="
              + secondRoot + " dirseg=" + EdSegment.showDirected(p1, p2));
      } 
//      else {
//
//        double th = calcTheta(a, b);
//        LineEqn eqn = new LineEqn(a.polarPoint(th + Math.PI / 2), th);
//        double ta = eqn.parameterFor(a.getOrigin());
//        double tb = eqn.parameterFor(b.getOrigin());
//        seg = new DirSeg(eqn.pt(ta), eqn.pt(tb));
//
//      }
    }
  }
  public BiTangent(FPoint2 pt1, String lbl1, FPoint2 pt2, String lbl2) {
    EdDisc d1 = new EdDisc(pt1, 0);
    d1.setLabel(lbl1);
    EdDisc d2 = new EdDisc(pt2, 0);
    d2.setLabel(lbl2);
    construct(d1, d2);
  }

  public boolean defined() {
    return seg != null;
  }

  public FPoint2 tangentPt(int index) {
    return seg.endPoint(index);
  }
  private String getLabel(int pt) {
    return disc(pt).getLabel();
  }

  public LineEqn lineEqn() {
    return seg.lineEqn();
  }

  public static final Color BITAN_COLOR = MyColor.get(MyColor.GREEN, .3); //)cPURPLE;
  public void render(Color c, int stroke, int markType) {
    if (c == null)
      c = Color.RED;
    V.pushColor(c);
    if (defined()) {
      seg.render(c, stroke, markType);
    } else {

      for (int i = 0; i < 2; i++) {
        if (i == 1 && discA == discB)
          continue;

        EdObject obj = object(i);
        if (obj instanceof EdDisc) {
          EdDisc d = (EdDisc) obj;

          stroke = STRK_RUBBERBAND;
          V.pushStroke(stroke);
          double r = Math.max(d.getRadius() - 4, 2.0);
          V.drawCircle(d.getOrigin(), r);
          V.popStroke();
        } else {
          Tools.unimp("rendering undefined bitangents of polygons");
        }
      }

    }
    V.popColor();

  }

  public double theta() {
    if (!defined())
      return 0;
    return seg.lineEqn().polarAngle();
  }
  public double thetaP() {
    return MyMath.normalizeAnglePositive(theta());
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("b<");
    sb.append(getLabel(0));
    sb.append(getLabel(1));
    sb.append('>');
    return sb.toString();
  }

  //  private static final double BADTHETA = -999;
  // private static final double TOLERANCE = 1e-12;

  //  /**
  //   * Calculate angle of bitangent to two discs.
  //   * Assumes discs are partially disjoint.
  //   * Uses Newton's method.
  //   * @param a, b : discs
  //   * @return polar angle of bitangent, or BADTHETA if problem
  //   */
  //  public static double OLDcalcTheta(EdDisc a, EdDisc b) {
  //    final boolean db = TEST;
  //
  //    double ret = BADTHETA;
  //
  //    double deltaR = b.getRadius() - a.getRadius();
  //    double fa = b.getOrigin().x - a.getOrigin().x;
  //    double fb = b.getOrigin().y - a.getOrigin().y;
  //    if (db)
  //      Streams.out.println("calcTheta\n a=" + a + "\n b=" + b + "\n fa="
  //          + Tools.f(fa) + " fb=" + Tools.f(fb));
  //
  //    double p0 = 0;
  //
  //    // use angle of segment connecting origins as initial approximation
  //    p0 = MyMath.polarAngle(a.getOrigin(), b.getOrigin());
  //
  //    if (db)
  //      Streams.out.println("initial approximation is " + Tools.fa(p0));
  //
  //    for (int step = 0; step < 15; step++) {
  //      double c = Math.cos(p0), s = Math.sin(p0);
  //      double fval = deltaR + fb * c - fa * s;
  //      double fprime = -(fb * s + fa * c);
  //      double p = p0;
  //      if (Math.abs(fprime) >= TOLERANCE) {
  //        p = p0 - fval / fprime;
  //      }
  //
  //      if (db)
  //        Streams.out.println(" " + Tools.f(step) + "  f=" + Tools.f(fval, 3, 12)
  //            + " f'=" + Tools.f(fprime, 3, 12) + " p0(deg)=" + Tools.fa(p0)
  //            + " chg=" + Tools.f(Math.abs(p - p0), 3, 12));
  //
  //      if (Math.abs(p - p0) < TOLERANCE) {
  //        if (Math.abs(fval) < 1e-5)
  //          ret = p;
  //        break;
  //      }
  //      p0 = p;
  //    }
  //
  //    if (ret == BADTHETA)
  //      Tools.warn("problem calculating theta for:\n" + a + "\n" + b);
  //
  //    //    if (true) {
  //    //      ret = MyMath.normalizeAnglePositive(ret);
  //    //
  //    //      double ret2 = newCalcTheta(a, b);
  //    //      Streams.out.println("calcTheta=" + Tools.fa(ret) + " ,  "
  //    //          + Tools.fa(ret2) + Tools.f(Math.abs(ret - ret2)));
  //    //
  //    //    }
  //    //
  //    return ret;
  //  }

//  /**
//   * Calculate angle of bitangent to two discs.
//   * Assumes discs are partially disjoint.
//   * @param a, b : discs
//   * @return polar angle of bitangent, or BADTHETA if problem
//   */
//  private static double calcTheta(EdDisc a, EdDisc b) {
//
//    final boolean db = false;
//
//    double aRadius = a.getRadius();
//    double bRadius = b.getRadius();
//
//    FPoint2 aOrigin = a.getOrigin();
//    FPoint2 bOrigin = b.getOrigin();
//
//    double bth = MyMath.polarAngle(aOrigin, bOrigin);
//    double c = FPoint2.distance(aOrigin, bOrigin);
//    double theta;
//    //    if (false) {
//    //
//    //      double phi = Math.asin(Math.abs(bRadius - aRadius) / c);
//    //
//    //      if (aRadius < bRadius)
//    //        theta = bth + phi;
//    //      else
//    //        theta = bth - phi;
//    //    } else {
//    double phi = Math.asin((bRadius - aRadius) / c);
//
//    //if (aRadius < bRadius)
//    theta = bth + phi;
//    //else
//    //  theta = bth - phi;
//
//    //    }
//
//    theta = MyMath.normalizeAnglePositive(theta);
//
//    if (db && T.update())
//      T.msg("calcTheta " + T.show(a, MyColor.cPURPLE) + T.show(b) + " = "
//          + Tools.fa(theta));
//
//    return theta;
//  }

  public static void plotDirectedHalfPlane(FPoint2 p0, FPoint2 p1, int markType) {
    double SEP = .4 * V.getScale();
    double ang = MyMath.polarAngle(p0, p1);
    FPoint2 d0 = MyMath.ptOnCircle(p0, ang + Math.PI / 2, SEP);
    FPoint2 d1 = MyMath.ptOnCircle(p1, ang + Math.PI / 2, SEP);

    EdSegment.plotDirectedLine(p0, p1);
    V.pushStroke(STRK_RUBBERBAND);
    V.drawLine(d0, d1);
    V.popStroke();

    if (markType >= 0) {
      V.mark(d0, markType);
      V.mark(d1, markType);
    }
  }

  public EdDisc disc(int i) {
    return (EdDisc) object(i); //(i == 0) ? discA : discB;
  }
  public EdObject object(int i) {
    return (i == 0) ? discA : discB;
  }

  private EdObject discA, discB;
  private DirSeg seg;

}
