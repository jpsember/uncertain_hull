package uhull;

import java.awt.*;
import java.util.*;
import java.util.List;
import testbed.*;
import base.*;

public class HullUtil {
  public static final int EAST = 0, NORTH = 1, WEST = 2, SOUTH = 3;
  public static String compareHulls(EdPolygon a, EdPolygon b) {
    String sa = toString(canonical(a));
    String sb = toString(canonical(b));
    return Tools.compare(sa, sb);
  }

  //  public static String descriptionOf(Object[] d) {
  //    StringBuilder sb = new StringBuilder("[");
  //    for (int i = 0; i < d.length; i++) {
  //      if (i > 0)
  //        sb.append('/');
  //      Object di = d[i];
  //      if (di instanceof EdObject) {
  //        EdObject e = (EdObject) di;
  //        sb.append(e.getFactory().getTag());
  //        for (int j = 0; j < e.nPoints(); j++)
  //          sb.append(e.getPoint(j));
  //      } else
  //        sb.append(di);
  //    }
  //    sb.append("]");
  //    return sb.toString();
  //  }

  /**
   * Construct a canonical representation of a polygon.
   * Snaps vertices to a uniform grid, and starts with bottom left point
   * @param poly
   * @return canonical polygon
   */
  public static EdPolygon canonical(EdPolygon poly) {
    FPoint2[] pts = new FPoint2[poly.nPoints()];
    int start = -1;

    for (int i = 0; i < pts.length; i++) {
      FPoint2 pt = poly.getPoint(i);
      pt = MyMath.snapToGrid(pt, 1e-3);
      pts[i] = pt;

      if (start < 0 || FPoint2.compareLex(pt, pts[start]) < 0)
        start = i;
    }
    EdPolygon ret = new EdPolygon();
    for (int i = 0; i < pts.length; i++)
      ret.addPoint(pts[MyMath.mod(start + i, pts.length)]);
    return ret;
  }

  public static String toString(EdPolygon c) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < c.nPoints(); i++) {
      FPoint2 pt = c.getPoint(i);
      sb.append("(");
      sb.append(Tools.f(pt.x, 3, 1));
      sb.append(Tools.f(pt.y, 3, 1));
      sb.append(") ");
      if ((i + 1) % 4 == 0)
        sb.append("\n");
    }
    Tools.addCr(sb);
    return sb.toString();
  }

  public static EdObject[] calcExtremeDiscs(EdObject[] discs, boolean innerHull) {
    double[] ev = new double[4];
    EdObject[] ed = new EdObject[4];
    for (int i = 0; i < discs.length; i++)
      procExtremeDisc(ev, ed, discs[i], innerHull);
    return ed;
  }

  public static EdObject[] calcExtremeDiscs(Collection discs, boolean innerHull) {
    double[] ev = new double[4];
    EdDisc[] ed = new EdDisc[4];
    for (Iterator it = discs.iterator(); it.hasNext();)
      procExtremeDisc(ev, ed, (EdDisc) it.next(), innerHull);
    return ed;
  }

  /**
   * Test object to see if it's an extreme object.  Modifies array of current
   * values and objects appropriately.
   * @param extVals array of current extreme values, derived from previous objects
   * @param extDiscs array of current extreme objects; if entry is null, none exists
   * @param d object to test
   */
  private static void procExtremeDisc(double[] extVals, EdObject[] extDiscs,
      EdObject obj, boolean innerHull) {
    FRect r = obj.getBounds();
    for (int dir = 0; dir < 4; dir++) {
      double x;
      switch (dir) {
      default: // EAST
        x = innerHull ? r.x : r.endX();
        break;
      case NORTH:
        x = innerHull ? r.y : r.endY();
        break;
      case WEST:
        x = innerHull ? r.endX() : r.x;
        break;
      case SOUTH:
        x = innerHull ? r.endY() : r.y;
        break;
      }

      boolean update = (extDiscs[dir] == null);
      if (!update) {
        update = (dir >= 2) ^ (extVals[dir] < x);
      }

      if (update) {
        extDiscs[dir] = obj;
        extVals[dir] = x;
      }
    }
  }

  public static FRect calcBoxTangents(Collection discs, boolean innerHull) {
    final boolean db = false;
    if (db && T.update())
      T.msg("calcBoxTangents for discs" + T.show(discs));

    FRect ret = null;

    double[] ev = new double[4];
    EdDisc[] ed = new EdDisc[4];
    for (Iterator it = discs.iterator(); it.hasNext();)
      procExtremeDisc(ev, ed, (EdDisc) it.next(), innerHull);
    if (db && T.update())
      T.msg("extreme discs" + T.show(ed[0]) + T.show(ed[1]) + T.show(ed[2])
          + T.show(ed[3]) + " ev=" + DArray.toString(ev));
    if (ed[0] != null) {
      double minx, maxx, miny, maxy;
      if (innerHull) {
        minx = ev[WEST];
        maxx = ev[EAST];
        miny = ev[SOUTH];
        maxy = ev[NORTH];
      } else {
        maxx = ev[WEST];
        minx = ev[EAST];
        maxy = ev[SOUTH];
        miny = ev[NORTH];
      }

      if (minx <= maxx && miny <= maxy)
        ret = new FRect(minx, miny, maxx - minx, maxy - miny);
    }
    return ret;
  }
  /**
   * Construct polygon from hull bitangents
   * @param bitangents
   * @return EdPolygon of hull, or null if it is empty
   */
  public static EdPolygon hullFromBiTangents(DArray bitangents, EdObject[] polys) {
    return hullFromBiTangentsAux(bitangents, polys, true);
  }

  /**
   * Construct polygon from hull bitangents
   * @param bitangents
   * @return EdPolygon of hull, or null if it is empty
   */
  private static EdPolygon hullFromBiTangentsAux(DArray bitangents,
      EdObject[] polys, boolean innerHull) {
    final boolean db = false;

    if (db && T.update())
      T.msg("hullFromBiTangents" + T.showAll(bitangents, MyColor.cRED)
          + " polys=" + polys);
    EdPolygon hp = null;

    do {
      FRect r;
      if (polys == null) {
        // determine N,S,E,W hull tangents
        Set discs = new HashSet();
        for (Iterator it = bitangents.iterator(); it.hasNext();) {
          BiTangent b = (BiTangent) it.next();
          discs.add(b.disc(0));
          discs.add(b.disc(1));
        }
        r = calcBoxTangents(discs, innerHull);
      } else {
        if (polys.length == 0)
          break;

        EdObject[] ex = calcExtremeDiscs(polys, innerHull);
        double x0, y0, x1, y1;
        if (innerHull) {
          x0 = ex[WEST].getBounds().endX();
          y0 = ex[SOUTH].getBounds().endY();
          x1 = ex[EAST].getBounds().x;
          y1 = ex[NORTH].getBounds().y;
        } else {
          x0 = ex[WEST].getBounds().x;
          y0 = ex[SOUTH].getBounds().y;
          x1 = ex[EAST].getBounds().endX();
          y1 = ex[NORTH].getBounds().endY();

        }
        if (x1 < x0 || y1 < y0)
          break;

        r = new FRect(x0, y0, x1 - x0, y1 - y0);
      }
      if (r == null)
        break;
      if (db && T.update())
        T.msg("boxTangents=" + T.show(r));
      hp = new EdPolygon();
      {
        hp.addPoint(r.bottomLeft(), false);
        hp.addPoint(r.bottomRight(), false);
        hp.addPoint(r.topRight(), false);
        hp.addPoint(r.topLeft(), false);
      }
      if (db && T.update())
        T.msg("initial hp" + T.show(hp));
      for (Iterator it = bitangents.iterator(); it.hasNext();) {
        BiTangent b = (BiTangent) it.next();
        LineEqn ln = b.lineEqn();
        if (db && T.update())
          T.msg("clipping to bitangent" + T.show(b)
              + T.show(hp, MyColor.cDARKGREEN));
        hp = hp.clipTo(ln.pt(0), ln.pt(1));
        if (hp == null)
          break;
      }
    } while (false);
    return hp;
  }

  public static EdPolygon calcHullOfCenters(EdDisc[] d) {
    DArray cp = new DArray();
    for (int i = 0; i < d.length; i++)
      cp.add(d[i].getOrigin());

    return calcHullOf(cp);
  }

  public static EdPolygon calcHullOf(DArray pts) {
    EdPolygon hull = new EdPolygon();
    DArray hp = MyMath.convexHull(pts);

    //    if (false) {
    //      Tools.warn("taking hull of hull");
    //      DArray pts2 = new DArray();
    //      for (int i = 0; i < hp.length; i++)
    //        pts2.add(pts.getFPoint2(hp[i]));
    //      pts = pts2;
    //      hp = MyMath.convexHull(pts);
    //    }
    //
    for (int i = 0; i < hp.size(); i++)
      hull.addPoint(pts.getFPoint2(hp.getInt(i)), false);

    return hull;
  }
  public static EdDisc[] transformDiscs(EdDisc[] origDiscs, Map discMap) {

    EdDisc[] discs = new EdDisc[origDiscs.length];
    for (int i = 0; i < origDiscs.length; i++) {
      EdDisc dOriginal = origDiscs[i];
      EdDisc dTransformed = transformDisc(dOriginal);
      discs[i] = dTransformed;
      discMap.put(dTransformed, dOriginal);
    }
    return discs;
  }

  private static EdDisc transformDisc(EdDisc dOriginal) {
    EdDisc dTransformed = new EdDisc(dOriginal);
    FPoint2 orig = dOriginal.getOrigin();
    dTransformed.setPoint(0, new FPoint2(100 - orig.x, 100 - orig.y), false,
        null);
    return dTransformed;
  }

  private static EdPolygon transform(EdPolygon p) {
    EdPolygon p2 = new EdPolygon( );
    FRect r = p.getBounds();
    double cx = r.midPoint().x;
    for (int i = 0; i < p.nPoints(); i++) {
      FPoint2 pt = p.getPoint(i);
      p2.addPoint(cx * 2 - pt.x, pt.y);
    }
    return p2;
  }

  public static void flipSelectedDiscs() {
    for (Iterator it = Editor.editObjects(EdDisc.FACTORY).iterator(); it
        .hasNext();) {
      EdDisc d = (EdDisc) it.next();
      EdDisc d2 = transformDisc(d);
      d.setPoint(0, d2.getOrigin(), false, null);
    }
  }

  public static EdPolygon build(EdObject[] discs) {
    return JarvisHull.build(discs);
  }

  public static void render(EdPolygon hullPolygon) {
    V.pushScale(.6);
    T.render(hullPolygon, MyColor.cDARKGREEN, -1, Globals.MARK_DISC);
    V.pop();
  }

  public static final Comparator sortByRadii = new Comparator() {
    public int compare(Object arg0, Object arg1) {
      EdDisc d0 = (EdDisc) arg0, d1 = (EdDisc) arg1;
      return MyMath.sign(d0.getRadius() - d1.getRadius());
    }
  };

  public static String bitangentsString(DArray hullBitan) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    int j = 0;
    EdObject prev = null;
    for (int i = 0; i < hullBitan.size(); i++) {
      BiTangent b = (BiTangent) hullBitan.get(i);
      String lbl = b.object(0).getLabel();
      prev = b.object(1);
      if (lbl.equals("~"))
        continue;
      if (j++ > 0)
        sb.append(' ');
      sb.append(lbl);
    }
    if (prev != null) {
      if (j++ > 0)
        sb.append(' ');
      sb.append(prev.getLabel());
    }
    sb.append(']');
    return sb.toString();
  }

  private static double theta(EdObject p, int n, double currTheta) {
    FPoint2 p1 = p.getPointMod(n);
    FPoint2 p2 = p.getPointMod(n + 1);
    double th = MyMath.normalizeAnglePositive(MyMath.polarAngle(p1, p2));
    if (th < currTheta)
      th += Math.PI * 2;
    return th;
  }

  private static int rightmostVert(EdObject p) {
    int ret = -1;
    FPoint2 prevPt = null;
    for (int i = 0; i < p.nPoints(); i++) {
      FPoint2 pt = p.getPoint(i);
      if (prevPt == null || pt.x > prevPt.x) {
        prevPt = pt;
        ret = i;
      }
    }
    return ret;
  }

  public static DArray calcBitangents(EdObject a, EdObject b) {
    final boolean db = false;

    final Color ca = MyColor.cDARKGREEN, cb = MyColor.cPURPLE;

    DArray ret = new DArray();

    if (db && T.update())
      T.msg("calcBitangents " + T.show(a, ca) + T.show(b, cb));
    do {
      if (a.nPoints() < 1 || b.nPoints() < 1)
        break;

      int ia = rightmostVert(a);
      int ib = rightmostVert(b);

      FPoint2 ptA = a.getPointMod(ia);
      FPoint2 ptB = b.getPointMod(ib);

      double theta = Math.PI / 2;

      LineEqn lb = new LineEqn(ptB, theta);

      // side is true if a.vertex is to the left of b.tangent
      boolean side = lb.sideOfLine(ptA) > 0;
      //Inf inf = new Inf("calcBitangents",100);

      //Streams.out.println("\ncalcBitangents");
      while (true) {
        //        Streams.out.println(" ia="+ia+" ib="+ib+" theta="+theta);
        //        Inf.update(inf);

        // in case polygon is degenerate, keep track of whether progress made
        int olda = ia;
        int oldb = ib;
        int newa = ia;
        int newb = ib;
        double a2 = theta(a, ia, theta);
        double b2 = theta(b, ib, theta);
        if (a2 < b2) {
          newa = (ia + 1) % a.nPoints();
          theta = a2;
        } else {
          newb = (ib + 1) % b.nPoints();
          theta = b2;
        }

        if (db && T.update())
          T.msg("ia="
              + ia
              + " ib="
              + ib
              + " side="
              + Tools.f(side)
              + " theta="
              + Tools.f(MyMath.degrees(theta))
              + T.show(ptA)
              + T.show(ptB)
              + EdSegment.show(ptA, MyMath.ptOnCircle(ptA, theta, 5), ca,
                  Globals.STRK_THICK)
              + EdSegment.show(ptB, MyMath.ptOnCircle(ptB, theta, 5), cb,
                  Globals.STRK_THICK)

          );

        lb = new LineEqn(ptB, theta);
        boolean prevSide = side;
        side = lb.sideOfLine(ptA) > 0;

        if (side != prevSide) {
          if (!side) {
            BiTangent bt;

            bt = new BiTangent(ptA, a.getLabel(), ptB, b.getLabel());

            if (db && T.update())
              T.msg("adding bitangent" + T.show(bt));
            ret.add(bt);
          }
        }

        if (ia != newa) {
          ia = newa;
          ptA = a.getPointMod(ia);
        }
        if (ib != newb) {
          ib = newb;
          ptB = b.getPointMod(ib);
        }
        if (theta >= Math.PI * 2.5 || (olda == ia && oldb == ib))
          break;
      }
    } while (false);
    return ret;
  }

  public static EdPolygon buildPossibleHull(EdPolygon[] regions) {
    return PossHullOper.buildPossibleHull(regions);
  }

  public static DArray buildCombEdges() {
    DArray combEdges = new DArray();
    EdPolygon[] d = UHullMain.getPolygons();
    for (int i = 0; i < d.length; i++) {
      EdPolygon a = d[i];
      for (int j = i + 1; j < d.length; j++) {
        EdPolygon b = d[j];
        for (int k = 0; k < a.nPoints(); k++) {
          FPoint2 pk = a.getPoint(k);
          FPoint2 pk0 = a.getPointMod(k - 1);
          FPoint2 pk1 = a.getPointMod(k + 1);

          // if pk is a reflex vertex, skip.

          if (MyMath.sideOfLine(pk0, pk, pk1) < 0)
            continue;
          for (int m = 0; m < b.nPoints(); m++) {
            FPoint2 pm = b.getPoint(m);
            FPoint2 pm0 = b.getPointMod(m - 1);
            FPoint2 pm1 = b.getPointMod(m + 1);
            // if pm is a reflex vertex, skip.

            if (MyMath.sideOfLine(pm0, pm, pm1) < 0)
              continue;

            double side = Math.signum(MyMath.sideOfLine(pk, pm, pk0));
            if (side != Math.signum(MyMath.sideOfLine(pk, pm, pk1)))
              continue;
            if (side != Math.signum(MyMath.sideOfLine(pk, pm, pm0)))
              continue;
            if (side != Math.signum(MyMath.sideOfLine(pk, pm, pm1)))
              continue;

            combEdges.add(new EdSegment(pk, pm));
          }
        }
      }
    }
    return combEdges;
  }

  private static final Color C1 = MyColor.cPURPLE, C2 = MyColor.get(
      MyColor.CYAN, .3);

  private static final boolean db = false;

  /**
   * Find vertex of a convex polygon that is tangent to the directed line from a
   * point outside the polygon to the vertex
   * @param poly array of FPoint2s forming a convex polygon
   * @param src point outside the polygon
   * @param rightTangent true if polygon is to be to the right of the tangent line,
   *  false if left
   * @return vertex, or null if failed (polygon isn't convex, or point is inside)
   */
  public static FPoint2 findTangentVertex(EdPolygon poly, FPoint2 src,
      boolean rightTangent) {

    final boolean db = false;

    poly = poly.getConvexHull();

    if (db && T.update())
      T.msg("findTangentVertex, rightTangent=" + Tools.f(rightTangent)
          + T.show(src) + T.show(poly, MyColor.cRED));

    int tv = -1;

    int seek = (rightTangent ? TCODE_RR : TCODE_LL);
    // int seek2 = rightTangent ? TCODE_RL : TCODE_LR;

    do {
      // start search with min and max vertices
      int vert1 = 0;
      int vert3 = poly.nPoints() - 1;

      // calc tangent codes for these vertices
      int tCode1 = tCode(src, poly, vert1, rightTangent);
      int tCode3 = tCode(src, poly, vert3, rightTangent);

      // if one of them is target, stop
      if (tCode1 == seek) {
        tv = vert1;
        break;
      }
      if (tCode3 == seek) {
        tv = vert3;
        break;
      }

      Inf inf = new Inf("target", poly.nPoints() * 3);

      // repeat while more vertices to search
      while (vert1 != vert3) {
        Inf.update(inf);

        // find vertex that is halfway between them
        int vert2 = MyMath.mod(vert1
            + MyMath.mod(vert3 - vert1, poly.nPoints()) / 2, poly.nPoints());
        int tCode2 = tCode(src, poly, vert2, rightTangent);

        if (db && T.update())
          T.msg("v1=" + vert1 + ":" + tCode(tCode1) + " v2=" + vert2 + ":"
              + tCode(tCode2) + " v3=" + vert3 + ":" + tCode(tCode3));

        // if it's the target vertex, stop
        if (tCode2 == seek) {
          tv = vert2;
          break;
        }

        // determine which half has the target
        boolean side2;

        // if all three points' codes are equal, we must
        // disambiguate further
        if (tCode1 == tCode2 && tCode2 == tCode3) {
          FPoint2 loc1 = poly.getPoint(vert1);
          FPoint2 loc2 = poly.getPoint(vert2);
          boolean left = MyMath.sideOfLine(src, loc1, loc2) > 0;
          side2 = (tCode1 == TCODE_LR) ^ !left;
          if (db && T.update())
            T.msg("tCode1:" + tCode(tCode1) + " left=" + left
                + EdSegment.show(src, loc1) + T.show(loc2) + " side2=" + side2);
        } else
          side2 = (tCode2 != (seek ^ 1));

        if (!side2) {
          tCode3 = tCode2;
          vert3 = vert2;
        } else {
          tCode1 = tCode2;
          vert1 = vert2;
        }
      }
    } while (false);

    FPoint2 target = null;
    if (tv >= 0) {
      target = poly.getPoint(tv);
      if (db && T.update())
        T.msg("target" + EdSegment.show(src, target, MyColor.cRED, -1));
    }
    return target;
  }

  public static FPoint2 findKernelPoint(EdPolygon polyA, EdPolygon polyB,
      boolean db) {

    FPoint2 kernelPt = null;
    EdSegment s1 = null, s2 = null;
    EdPolygon isect = calcIntersection(polyA, polyB);
    if (db && T.update())
      T.msg("findKernelPoint intersection of polygons"
          + T.show(isect, MyColor.cRED, Globals.STRK_THICK, -1));
    if (isect != null) {
      kernelPt = isect.getPoint(0);
    } else {
      Object obj1 = findKernelObject(polyA, polyB, false, db);
      if (obj1 instanceof FPoint2) {
        kernelPt = (FPoint2) obj1;
      } else {
        s1 = (EdSegment) obj1;
        Object obj2 = findKernelObject(polyA, polyB, true, db);
        if (obj2 instanceof FPoint2)
          T.err("unexpected");
        s2 = (EdSegment) obj2;
        kernelPt = MyMath.linesIntersection(s1.getPoint(0), s1.getPoint(1), s2
            .getPoint(0), s2.getPoint(1), null);
      }
    }
    if (db && T.update())
      T.msg("findKernelPoint" + T.show(s1, MyColor.cDARKGRAY)
          + T.show(s2, MyColor.cDARKGRAY) + T.show(kernelPt));
    return kernelPt;

  }

  /**
  * Calculate the intersection of two convex polygons
  * @param ha vertices of first polygon
  * @param hb vertices of second
  * @return intersection, or null if it is empty
  */
  private static EdPolygon calcIntersection(EdPolygon pa, EdPolygon pb) {
    DArray ha = pa.getConvexHull().getPts();
    DArray hb = pb.getConvexHull().getPts();
    EdPolygon p = new EdPolygon(ha);
    for (int i = 0; i < hb.size(); i++) {
      FPoint2 p0 = hb.getFPoint2(i);
      FPoint2 p1 = hb.getFPoint2((i + 1) % hb.size());
      p = p.clipTo(p0, p1);
      if (p == null)
        break;
    }
    return p;
  }
  /**
   * Find an object associated with kernel, by performing bitangent search
   * @param polyA first polygon
   * @param polyB second polygon
   * @param orientation false for first bitangent, true for second
   * @return FPoint2, if detected a point inside convex hull of other;
   *  else EdSegment, representing bitangent
   */
  private static Object findKernelObject(EdPolygon polyA, EdPolygon polyB,
      boolean orientation, boolean db) {

    FPoint2 alpha = polyA.getPoint(0);
    FPoint2 beta = null;
    Object result = null;
    String msg = null;
    boolean parity = false;

    if (db && T.update())
      T.msg("starting with arbitrary point on hull of A" + T.show(alpha));

    Inf inf = new Inf();

    while (true) {
      inf.update();
      FPoint2 a, b;
      EdPolygon poly;
      if (!parity) {
        a = alpha;
        b = beta;
        poly = polyB;
      } else {
        a = beta;
        b = alpha;
        poly = polyA;
      }

      FPoint2 bp = HullUtil.findTangentVertex(poly, a, orientation);
      if (db && T.update())
        T.msg("parity=" + parity + T.show(a)
            + T.show(poly, MyColor.cDARKGREEN, Globals.STRK_THICK, -1)
            + EdSegment.showDirected(a, bp));

      if (bp == null) {
        result = a;
        msg = "no tangent found; must be common to both hulls";
        break;
      }
      if (bp == b) {
        result = new EdSegment(a, b);
        msg = "tangent point hasn't changed; found bitangent";
        break;
      }
      if (!parity)
        beta = bp;
      else
        alpha = bp;
      parity ^= true;
    }
    if (db && T.update())
      T.msg(msg + T.show(result));
    return result;
  }

  private static final int TCODE_LL = 0, TCODE_LR = 1, TCODE_RL = 2,
      TCODE_RR = 3;

  private static String tCode(int c) {
    return "LLLRRLRR".substring(c * 2, c * 2 + 2);
  }
  private static int tCode(FPoint2 srcVert, EdPolygon hull, int index,
      boolean rightTangent) {
    FPoint2 p1 = hull.getPointMod(index);
    FPoint2 p0 = hull.getPointMod(index - 1);
    FPoint2 p2 = hull.getPointMod(index + 1);
    int ret = 0;
    boolean nextLeft = MyMath.sideOfLine(srcVert, p1, p2) > 0;
    boolean prevLeft = MyMath.sideOfLine(srcVert, p1, p0) > 0;

    if (!prevLeft)
      ret |= 1;
    if (!nextLeft)
      ret |= 2;
    if (db && T.update())
      T.msg("tCode v#" + index + EdSegment.show(srcVert, p1)
          + EdSegment.show(srcVert, p0) + EdSegment.show(srcVert, p2)
          + T.show("p1:" + index, null, p1.x + 3, p1.y, 0)
          + T.show("p0:" + (index - 1), null, p0.x + 3, p0.y, 0)
          + T.show("p2:" + (index + 1), null, p2.x + 3, p2.y, 0)
          + "\nnextLeft=" + Tools.f(nextLeft) + " prevLeft="
          + Tools.f(prevLeft) + "\nret=" + tCode(ret)
          + T.show(tCode(ret), null, p1.x + 15, p1.y, 0));

    if (rightTangent) {
      if (ret == TCODE_LL)
        ret = TCODE_RL;
    } else {
      if (ret == TCODE_RR)
        ret = TCODE_RL;
    }
    return ret;
  }

  private static int rightMostVertex(List a) {
    int ex = -1;
    for (int i = 0; i < a.size(); i++) {
      if (ex < 0 || FPoint2.compareLex(pt(a, i), pt(a, ex), false) > 0)
        ex = i;
    }
    return ex;
  }

  private static FPoint2 pt(List a, int index) {
    return (FPoint2) a.get(MyMath.mod(index, a.size()));
  }

  private static final boolean DBCOMBINE = false;

  /**
   * Add point to hull list.
   * If point is same as last, does nothing.
   * If previous point lies between new and its previous, removes last.
   * @param pts list of hull points
   * @param pt new point
   * @param debugMsg for debugging only
   * @return true if point was not same as last but was same as first,
   *   i.e. hull is complete
   */
  private static boolean addPoint(DArray pts, FPoint2 pt, String debugMsg) {
    final boolean db = DBCOMBINE;

    if (db && T.update())
      T.msg("adding point " + pt + T.show(pt) + ": " + debugMsg);
    boolean done = false;
    do {
      if (pts.size() > 1 && pt == pts.get(0)) {
        done = true;
        break;
      }

      if (!pts.isEmpty() && pt == pts.last())
        break;
      if (pts.size() >= 2) {
        FPoint2 p0 = (pts.size() >= 2) ? (FPoint2) pts.peek(1) : pt;
        FPoint2 p1 = (pts.size() >= 1) ? (FPoint2) pts.peek(0) : pt;
        double s = MyMath.sideOfLine(p0, p1, pt);
        if (db && T.update())
          T.msg("sideOfLine " + p0 + " " + p1 + " is " + s);
        if (s == 0) {
          pts.pop();
        }
      }
      pts.add(pt);
    } while (false);
    return done;
  }

  /**
   * Determine convex hull of two convex polygons in linear time,
   * using rotating calipers
   * @param a first polygon
   * @param b second polygon
   * @return convex hull of the two
   */
  public static DArray hullOfConvex(List a, List b) {
    final boolean db = DBCOMBINE;
    if (db && T.update())
      T.msg("hullOfConvex" + T.show(a, C1) + T.show(b, C2));
    DArray ret = new DArray();

    int aIndex = rightMostVertex(a);
    int bIndex = rightMostVertex(b);

    double theta = Math.PI / 2;
    FPoint2 aVert = pt(a, aIndex);
    FPoint2 bVert = pt(b, bIndex);
    LineEqn aLine = new LineEqn(aVert, theta);
    LineEqn bLine = new LineEqn(bVert, theta);

    boolean bActive = false;
    boolean bActiveDefined = false;

    if (a.size() < 3 || b.size() < 3)
      throw new IllegalArgumentException();

    Inf inf = null;
    if (DBCOMBINE)
      inf = new Inf("hull", 100);

    while (true) {
      if (DBCOMBINE)
        Inf.update(inf);

      // determine which is the limit pivot
      double anga = MyMath.polarAngle(aVert, pt(a, aIndex + 1));
      double angb = MyMath.polarAngle(bVert, pt(b, bIndex + 1));
      double aDiff = MyMath.normalizeAngle(anga - angb);
      boolean bIsPivot = aDiff > 0;

      if (bIsPivot) {
        theta = angb;
        bIndex++;
        bVert = pt(b, bIndex);
      } else {
        theta = anga;
        aIndex++;
        aVert = pt(a, aIndex);
      }

      aLine = new LineEqn(aVert, theta);
      bLine = new LineEqn(bVert, theta);

      if (db && T.update())
        T.msg("combining hulls" + T.show(aLine, C1) + T.show(bLine, C2)
            + T.show(aVert, C1) + T.show(bVert, C2) + "\naLine=" + aLine
            + "\nbLine=" + bLine);

      int bSide = aLine.sideOfLine(bVert);
      boolean bActiveNew = bSide < 0;
      if (bSide == 0)
        bActiveNew = bVert.y > aVert.y;

      if (!bActiveDefined) {
        bActive = bActiveNew;
        bActiveDefined = true;
      }

      if (bActiveNew != bActive) {
        bActive = bActiveNew;
        if (bIsPivot == bActive)
          if (addPoint(ret, bIsPivot ? pt(b, bIndex - 1) : pt(a, aIndex - 1),
              "crossover aux"))
            break;
        if (addPoint(ret, bActive ? pt(b, bIndex) : pt(a, aIndex), "crossover"))
          break;
      }

      if (bIsPivot == bActive)
        if (addPoint(ret, bIsPivot ? bVert : aVert, "right pivot"))
          break;

    }
    if (db && T.update())
      T.msg(" hullOfConvex" + T.show(a, C1) + T.show(b, C2) + " returning"
          + T.show(ret, MyColor.cRED));
    return ret;
  }

  public static void flipSelectedPolygons() {
    for (Iterator it = Editor.editObjects(EdPolygon.FACTORY).iterator(); it
        .hasNext();) {
      EdPolygon p = (EdPolygon) it.next();
      EdPolygon p2 = transform(p);
      for (int i = 0; i < p.nPoints(); i++)
        p.setPoint(i, p2.getPoint(i));
    }

  }
  
  
}
