package uhull;

import java.util.*;
import base.*;
import testbed.*;

public class JarvisHull implements TestBedOperation, Globals {
  /*! .enum  .private 2800
        jdispangles   _ _ plotbitan showbitan
  */

    private static final int JDISPANGLES      = 2800;//!
    private static final int PLOTBITAN        = 2803;//!
    private static final int SHOWBITAN        = 2804;//!
/*!*/

  public void addControls() {
    C.sOpenTab("Jarvis");
    C.sStaticText("Uses Jarvis' March to construct "
        + "guaranteed convex hull of uncertain " + "discs or polygons");
    C.sCheckBox(PLOTBITAN, "Plot bitangents",
        "Plot hull bitangents that are clipped "
            + "to produce the hull polygon", false);
    C.sCheckBox(JDISPANGLES, "Trace: angles", null, false);
    C.sCheckBox(SHOWBITAN, "Show bitangents", "Display sequence of bitangents",
        false);
    C.sCloseTab();
  }

  public static JarvisHull singleton = new JarvisHull();

  private JarvisHull() {
  }

  public void processAction(TBAction a) {
  }

  private static DArray buildBitangents(EdObject[] discs) {
    JarvisHull hull = new JarvisHull();
    boolean deb = false;
    if (deb)
      Tools.warn("turning on debug for jarvis");
    return hull.jarvisMarch(discs, deb);
  }

  /**
   * Build guaranteed hull of discs or polygons using Jarvis march
   * @param obj  discs or polygons to build from
   * @param bitan if not null, bitangents are stored here
   * @return polygon, or null if it is empty
   */
  private static EdPolygon build(EdObject[] obj, DArray bitan) {
    DArray bt = buildBitangents(obj);

    if (bitan != null) {
      bitan.clear();
      bitan.addAll(bt);
    }
    EdPolygon hullPolygon = HullUtil.hullFromBiTangents(bt, obj.length > 0
        && obj[0] instanceof EdPolygon ? obj : null);
    return hullPolygon;
  }

  /**
   * Build guaranteed hull of discs or polygons using Jarvis march
   * @param obj  discs or polygons to build from
   * @return polygon, or null if it is empty
   */
  public static EdPolygon build(EdObject[] obj) {
    return build(obj, null);
  }

  public void runAlgorithm() {
    hullPolygon = null;
    bitangents = null;
    boolean db = false;
    DArray bitan = null;
    EdPolygon[] polys = UHullMain.getPolygons();
    if (polys.length == 0) {
      polys = null;
      EdDisc[] objects = UHullMain.getDiscs();
      if (objects.length != 0)
        bitan = jarvisMarch(objects, true);
    } else {
      bitan = jarvisMarch(polys, true);
    }
    if (bitan != null) {
      hullPolygon = HullUtil.hullFromBiTangents(bitan, polys);
      if (db && T.update())
        T.msg("bitan=\n" + bitan.toString(true));
      if (db && T.update())
        T.msg("hullPolygon=\n" + HullUtil.toString(hullPolygon));
      if (C.vb(SHOWBITAN) && bitangents != null)
        T.show(HullUtil.bitangentsString(bitangents), null, 0, 100, -1);
      // now that polygon completely built, get rid of other trace elements
      if (!C.vb(PLOTBITAN))
        bitangents = null;
    }
    objSet = null;
  }

  public void paintView() {
    Editor.render();
    T.renderAll(objSet, MyColor.cDARKGREEN, -1, -1);
    T.renderAll(bitangents, MyColor.cPURPLE, -1, -1);
    HullUtil.render(hullPolygon);

  }

  /**
   * Perform Jarvis march.
   */
  private DArray jarvisMarch(EdObject[] objects, boolean db) {

    boolean isPoly = objects.length > 0
        && (objects[0] instanceof EdPolygon || objects[0] instanceof EdSegment);
    EdObject startDisc;
    if (!isPoly) {
      EdObject[] extr = HullUtil.calcExtremeDiscs((EdDisc[]) objects, true);
      startDisc = extr[HullUtil.SOUTH];
    } else {
      EdObject[] conv = new EdObject[objects.length];
      startDisc = null;
      double startY = 0;
      for (int i = 0; i < objects.length; i++) {
        EdObject p = (EdObject) objects[i];
        if (p instanceof EdPolygon) {
          p = ((EdPolygon) p).getConvexHull(); //calcHull();
          //        p2.setLabel(p.getLabel());
        }

        conv[i] = p;
        FRect r = p.getBounds();
        double y = r.endY();
        if (startDisc == null || y < startY) {
          startY = y;
          startDisc = p;
        }
      }
      objects = conv;
    }

    objSet = objects;
    bitangents = new DArray();

    if (startDisc != null) {

      if (db && T.update())
        T.msg("jarvisMarch, start object " + T.show(startDisc));

      EdObject prevDisc = startDisc;

      double totalAngle = 0;
      double prevAng = 0;
Inf inf = new Inf("jarvisHull", 1000);

      while (true) {
Inf.update(inf);
        EdObject nextHullDisc = null;

        double smallestAngleChange = 0;
        BiTangent nextb = null;
        for (int i = 0; i < objSet.length; i++) {
          EdObject di = objSet[i];
          if (prevDisc == di)
            continue;

          BiTangent testEdge;
          if (!isPoly) {
 testEdge = new BiTangent((EdDisc) prevDisc, (EdDisc) di);
          } else {
            testEdge = bitan(prevDisc, di, prevAng);
          }

          if (testEdge == null || !testEdge.defined())
            continue;

          double ang = MyMath.normalizeAnglePositive(testEdge.lineEqn()
              .polarAngle()
              - prevAng);

          if (db && C.vb(JDISPANGLES) && T.update())
            T.msg("examining" + T.show(di) + " angle=" + Tools.fa(ang)
                + T.show(testEdge) + " smallest="
                + Tools.fa(smallestAngleChange));

          if (nextHullDisc == null || ang < smallestAngleChange) {
            smallestAngleChange = ang;
            nextHullDisc = di;
            nextb = testEdge;
          }
        }
        if (nextb == null) {
          if (db && T.update())
            T.msg("no next object found, we must be done");
          break;
        }

        //BiTangent bn = new BiTangent(prevDisc, nextHullDisc);
        if (!bitangents.isEmpty())
          totalAngle += smallestAngleChange;
        if (db && T.update())
          T.msg("next edge" + T.show(nextb) + " ["
              + Tools.fa(smallestAngleChange) + Tools.fa(totalAngle) + "]");

        if (prevDisc == startDisc && totalAngle >= Math.PI * 2 - 1e-2) {
          if (db && T.update())
            T.msg("we're done");
          break;
        }
        bitangents.add(nextb);
        prevDisc = nextHullDisc;
        prevAng = nextb.thetaP();
      }
    }
   return bitangents;
  }

  /**
   * Calculate smallest bitangent of polygons that is greater than
   * some minimum angle
   * @param a
   * @param b
   * @param minTheta
   * @return smallest bitangent, or null
   */
  private static BiTangent bitan(EdObject a, EdObject b, double minTheta) {
    BiTangent ret = null;
    double prevMin = 0;
    DArray bl = HullUtil.calcBitangents(a, b);

    for (int i = 0; i < bl.size(); i++) {
      BiTangent bt = (BiTangent) bl.get(i);
      double tdiff = MyMath.normalizeAnglePositive(bt.theta() - minTheta);
      if (tdiff == 0)
        tdiff = Math.PI * 2;
      if (ret == null || tdiff < prevMin) {
        prevMin = tdiff;
        ret = bt;
      }
    }
    return ret;
  }

  // polygon representing guaranteed hull, or null if empty
  private EdPolygon hullPolygon;
  // list of bitangents generated by Jarvis march
  private DArray bitangents;
  // list of uncertain objects remaining to be examined by Jarvis march
  private EdObject[] objSet;
}
