package uhull;

import java.util.*;
import base.*;
import testbed.*;

public class GeneratorOper implements TestBedOperation, Globals {
  /*! .enum  .private 1600
  seed count minrad maxrad _ _ position _  
   type rndsqr circle spiral  rnddisc shadows rndpolys rndpolysc rndseg
   polyvert hullseg manypoints
  */

    private static final int SEED             = 1600;//!
    private static final int COUNT            = 1601;//!
    private static final int MINRAD           = 1602;//!
    private static final int MAXRAD           = 1603;//!
    private static final int POSITION         = 1606;//!
    private static final int TYPE             = 1608;//!
    private static final int RNDSQR           = 1609;//!
    private static final int CIRCLE           = 1610;//!
    private static final int SPIRAL           = 1611;//!
    private static final int RNDDISC          = 1612;//!
    private static final int SHADOWS          = 1613;//!
    private static final int RNDPOLYS         = 1614;//!
    private static final int RNDPOLYSC        = 1615;//!
    private static final int RNDSEG           = 1616;//!
    private static final int POLYVERT         = 1617;//!
    private static final int HULLSEG          = 1618;//!
    private static final int MANYPOINTS       = 1619;//!
/*!*/

  public void addControls() {
    C.sOpenTab("Gen");
    C.sStaticText("Generates random sites");
    {
      C.sOpenComboBox(TYPE, "Pattern", "Select pattern of random sites", false);
      C.sChoice(RNDDISC, "random (disc)");
      C.sChoice(RNDSQR, "random (square)");
      C.sChoice(CIRCLE, "circle");
      C.sChoice(SPIRAL, "spiral");
      C.sChoice(SHADOWS, "bitangents");
      C.sChoice(RNDPOLYS, "rndpolys");
      C.sChoice(RNDPOLYSC, "rndpolys (convex)");
      C.sChoice(RNDSEG, "rnd segments");
      C.sChoice(POLYVERT, "O(n) poly vert bitan");
      C.sChoice(HULLSEG, "hull segments");
      C.sChoice(MANYPOINTS, "largepoly)");

      C.sCloseComboBox();
    }
    {
      C.sOpen();
      {
        C.sOpen();
        C.sIntSlider(SEED, "Seed",
            "Random number generator seed (zero for unseeded)", 0, 100, 0, 1);
        //C.sNewColumn();
        C.sIntSlider(COUNT, "Count", "Number to generate", 0, 100, 12, 1);
        C.sClose();
      }
      {
        C.sOpen();
        C.sIntSlider(MINRAD, "Min Radius", "Minimum radius", 0, 30, 0, 1);
        //C.sNewColumn();
        C.sIntSlider(MAXRAD, "Max Radius", "Maximum radius", 0, 250, 50, 1);
        C.sClose();
      }
      {
        C.sOpen();
        C.sIntSlider(POSITION, "Position", "Distance from center (circular)",
            0, 200, 30, 1);
        C.sNewColumn();
        C.sClose();
      }

      C.sClose();
    }
    C.sCloseTab();
  }

  public static void generateRandom() {
    genRand(C.vi(TYPE));
  }
  private static void genRand(int type) {
    {
      DArray items = new DArray();

      int seed = C.vi(SEED);

      Random r = MyMath.seed(seed); //) Random r = seed == 0 ? new Random() : new Random(seed);
      int c = C.vi(COUNT);

      int radMin = Math.min(C.vi(MINRAD), C.vi(MAXRAD));
      int radMax = Math.max(C.vi(MINRAD), C.vi(MAXRAD));
      double range = (radMax - radMin) * .1;
      final int PADDING = 3;
      FPoint2 size = V.logicalSize();
      double sx = size.x - 2 * PADDING;
      double sy = size.y - 2 * PADDING;

      switch (type) {
      default:
        {
          for (int i = 0; i < c; i++) {
            double rv = r.nextDouble();
            double rad = rv * rv * range + radMin;
            EdDisc e = new EdDisc(//
                new FPoint2(//
                    r.nextDouble() * sx + PADDING, //
                    r.nextDouble() * sy + PADDING //
                ), rad);
            items.add(e);
          }
        }
        break;
      case SHADOWS:
        {
          double fx = 50;
          double fy = 35;

          double szMax = C.vi(POSITION);

          double xl = fx - szMax;
          double xr = fx + szMax;
          double basey = fy - szMax;

          // add left, right base discs
          double rad = 5;
          items.add(new EdDisc(MyMath.ptOnCircle(new FPoint2(xl, basey),
              Math.PI * .75, rad), rad));
          items.add(new EdDisc(MyMath.ptOnCircle(new FPoint2(xr, basey),
              Math.PI * .25, rad), rad));

          double xMax = fx + szMax * 1.2;
          double xMin = fx + szMax * .1;

          double p = .95;

          for (int i = 0; i < c; i++) {
            double x = xMin + i * ((xMax - xMin) / c);
            double fnx = Math.pow((x - fx), p) + fy;

            // calculate gradient along x
            double grad = p * Math.pow((x - fx), p - 1);

            double t = (fx - x) / -grad;
            double y = fnx + t;
            FPoint2 onCurve = new FPoint2(x, fnx);

            FPoint2 origin = new FPoint2(fx, y);
            items.add(new EdDisc(origin, onCurve.distance(origin)));
          }
        }
        break;

      case RNDDISC:
        {
          double szMax = C.vi(POSITION) * (80.0 / 30.0) / 100.0;

          for (int i = 0; i < c; i++) {
            double rv = r.nextDouble();
            double rad = rv * rv * range + radMin;
            double theta = r.nextDouble() * Math.PI * 2;
            double rd = Math.sqrt(r.nextDouble()) * szMax * .5;

            EdDisc e = new EdDisc(//
                new FPoint2(size.x / 2 + Math.cos(theta) * sx * rd, size.y / 2
                    + Math.sin(theta) * sy * rd), rad);
            items.add(e);
          }
        }
        break;
      case CIRCLE:
        {
          for (int i = 0; i < c; i++) {
            FPoint2 cn = MyMath.ptOnCircle(new FPoint2(50, 50), MyMath
                .radians(i * 360.0 / c), C.vi(POSITION));
            EdDisc e = new EdDisc(cn, C.vi(MINRAD));
            items.add(e);
          }
        }
        break;
      case SPIRAL:
        {
          double s2 = c * .4;

          int r0 = C.vi(MINRAD);
          int r1 = Math.max(r0, C.vi(MAXRAD));
          if (r1 == r0)
            r1++;

          for (int i = 0; i < c; i++) {
            double scl = (i + c * .3) / (double) (c - 1);

            FPoint2 cn = MyMath.ptOnCircle(new FPoint2(50, 50), MyMath
                .radians(i * 360.0 / s2), C.vi(POSITION) * scl);
            EdDisc e = new EdDisc(cn, .05 * r.nextInt(r1 - r0) + r0);
            items.add(e);
          }
        }
        break;
      case MANYPOINTS:
        {
          double r0 = C.vi(MINRAD) * (250.0 / 30.0);
          double r1 = Math.max(r0, C.vi(MAXRAD));
          FPoint2 origin = new FPoint2(50, 50);
          int np = C.vi(POSITION) * 3;
          EdPolygon p = new EdPolygon();

          for (int i = 0; i < np; i++) {
            double th = (i * Math.PI * 2) / np;
            double rad = MyMath.rnd(1.0) * (r1 - r0) + r0;
            rad = rad * (50.0 / 250);
            p.addPoint(MyMath.ptOnCircle(origin, th, rad));
          }
          items.add(p);
        }
        break;
      case RNDPOLYS:
      case RNDPOLYSC:
        {
          int r0 = C.vi(MINRAD);
          int r1 = Math.max(r0, C.vi(MAXRAD));
          if (r1 == r0)
            r1++;
          for (int i = 0; i < c; i++) {
            double sz = MyMath.rnd((r1 - r0)) + r0;
            sz = MyMath.clamp(sz, 2.0, 99);

            double x = MyMath.rnd(100 - sz);
            double y = MyMath.rnd(100 - sz);
            FRect bnds = new FRect(x, y, sz, sz);
            EdPolygon poly = EdPolygon.randomPoly(new Random(), Math.min(
                C.vi(POSITION) / 4 + 3, 30), bnds);
            if (poly == null) continue;
            if (type == RNDPOLYSC)
              poly = poly.getConvexHull(); //poly.calcHull();

            items.add(poly);
          }
        }
        break;
      case RNDSEG:
        {
          double pad = MyMath.clamp(C.vi(POSITION) * 40 / 200.0, 0, 40);
          sx = size.x - 2 * pad;
          sy = size.y - 2 * pad;
          c = c * 8;
          for (int i = 0; i < c; i++) {
            double rv = r.nextDouble();
            double rad = rv * rv * range + radMin;
            FPoint2 pt = new FPoint2(r.nextDouble() * sx + pad, //
                r.nextDouble() * sy + pad);
            double theta = (r.nextDouble() - .5) * Math.PI * .99;
            FPoint2 p1 = MyMath.ptOnCircle(pt, theta, rad);
            FPoint2 p0 = MyMath.ptOnCircle(pt, theta + Math.PI, rad);
            EdSegment e = new EdSegment(p0, p1);
            items.add(e);
          }
        }
        break;
      case POLYVERT:
        {
          FPoint2 o = new FPoint2(50, 50);
          FPoint2[] p = new FPoint2[3];

          p[0] = o;
          p[1] = new FPoint2(50 - 1, 50);
          p[2] = new FPoint2(50 - 1, 50 + 1);
          items.add(new EdPolygon(p));
          double rad = C.vi(POSITION) * 90.0 / 200.0;

          for (int i = 0; i < c; i++) {
            double th = -(i * Math.PI / 2.01) / c;
            p[0] = MyMath.ptOnCircle(o, th, rad);
            p[1] = MyMath.ptOnCircle(o, th - Math.PI * .98, rad);
            p[2] = MyMath.ptOnCircle(o, th - Math.PI * .97, rad);
            items.add(new EdPolygon(p));
          }
        }
        break;
      case HULLSEG:
        {
          double A = 25;
          double B = 45;
          double C = 45;
          double D = 4.2;
          double E = 15;
          double cx = 40;
          double cy = 20;

          Matrix m = Matrix.getRotate(MyMath.radians(220));
          m = Matrix.mult(Matrix.getTranslate(new FPoint2(cx, cy), false), m,
              null);
          //          m = Matrix.mult(m, Matrix.getTranslate(new FPoint2(cx, cy), true),
          //              null);
          double ym = 0;
          for (int i = 0; i < c; i++) {
            FPoint2 p0 = new FPoint2(-B / 2 - A, C / 2 - (E * i) / c);
            FPoint2 p1 = new FPoint2(-B / 2 + (B * i) / c, +C / 2 - C - ym * ym);

            items.add(new EdSegment(m.apply(p1), m.apply(p0)));
            ym += D / c;
          }

        }
        break;
      }
      Editor.replaceAllObjects(items);
    }

  }
  //  private int genType = RANDOM;
  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      default:
        if (a.ctrlId / 100 == SEED / 100)
          genRand(C.vi(TYPE));
        break;
      }
    }
  }
  public void runAlgorithm() {
  }
  public void paintView() {
    Editor.render();
  }

}
