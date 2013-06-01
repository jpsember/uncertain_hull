package uhull;

import base.*;
import testbed.*;
import java.awt.*;
import java.util.*;

public class UHullMain extends TestBed {
  /*! .enum  .private .prefix G_ 4000
       togglediscs     
       random rndtest flip togdashed flippoly _
  */

  private static final int G_TOGGLEDISCS = 4000;//!
  private static final int G_RANDOM = 4001;//!
  private static final int G_RNDTEST = 4002;//!
  private static final int G_FLIP = 4003;//!
  private static final int G_TOGDASHED = 4004;//!
  private static final int G_FLIPPOLY = 4005;//!
  /* !*/
  public static final boolean FULL = false;

  public static void main(String[] args) {
    new UHullMain().doMainGUI(args);
  }

//  public static boolean oldBitanMethod() {
//    return C.vb(G_OLDBITAN);
//  }
  // -------------------------------------------------------
  // TestBed overrides
  // -------------------------------------------------------

  //@SuppressWarnings(value = "deprecation")
  public void addOperations() {
    addOper(GuarDiscHullOper.singleton);
    addOper(OSHullOper.singleton);
    // if (false)
    addOper(EnvOper2.singleton);

    addOper(COper3.singleton);
    addOper(ValleyOper2.singleton);
    addOper(new GeneratorOper());

    addOper(DiscSampleOper.singleton);

    addOper(JarvisHull.singleton);
    addOper(PossHullOper.singleton);
    addOper(SampPossHull.singleton);

    if (FULL) {
      addOper(TestsOper.singleton);
      addOper(HullFromBitanOper.singleton);
      addOper(SegEnvOper.singleton);
      if (false)
        addOper(EnvOper.singleton);
    }

  }
  public void addControls() {
    C.sOpen();
    C.sButton(G_RANDOM, "Random", "Generate random discs");
//    C.sCheckBox(G_OLDBITAN, "Old bitangents",
//        "Use old bitangent calculation method", false);

    if (FULL)
      C.sCheckBox(G_RNDTEST, "Test", "Repeatedly generate random discs", false);
    C.sClose();
  }
  public void initEditor() {
    Editor.addObjectType(EdPolygon.FACTORY);
    Editor.addObjectType(EdDisc.FACTORY);
    Editor.addObjectType(EdSegment.FACTORY);
    Editor.addObjectType(EdPoint.FACTORY);

    Editor.openMenu();
    C.sMenuItem(G_TOGGLEDISCS, "Toggle discs/points", "!^t");
    C.sMenuItem(G_FLIP, "Rotate discs 180 degrees", null);
    C.sMenuItem(G_FLIPPOLY, "Flip polygon horz", null);
    C.sMenuItem(G_TOGDASHED, "Toggle dashed/normal", "!^D");
    Editor.closeMenu();
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      case G_RANDOM:
        GeneratorOper.generateRandom();
        break;
      case G_RNDTEST:
        if (C.vb(G_RNDTEST))
          GeneratorOper.generateRandom();
        break;

      case G_TOGGLEDISCS:
        {
          EdDisc[] discs2 = getDiscs();
          for (int i = 0; i < discs2.length; i++) {
            EdDisc c = discs2[i];
            if (!c.isSelected())
              continue;
            c.togglePointMode();
          }
        }
        break;
      case G_TOGDASHED:
        {
          for (Iterator it = Editor.editObjects(null).iterator(); it.hasNext();) {
            EdObject obj = (EdObject) it.next();
            obj.toggleFlags(EdObject.FLAG_PLOTDASHED);
          }
        }
        break;

      case G_FLIP:
        HullUtil.flipSelectedDiscs();
        break;
      case G_FLIPPOLY:
        HullUtil.flipSelectedPolygons();
        break;
      //      case G_ROTATE:
      //      case G_ROTBWD:
      //        {
      //          FPoint2 origin = V.viewRect.midPoint();
      //          Matrix trans = Matrix.getTranslate(origin, false);
      //          Matrix.mult(trans, Matrix.getRotate(MyMath.radians(C.vd(G_ANGLE))
      //              * (a.ctrlId == G_ROTBWD ? -1 : 1)), trans);
      //          Matrix.mult(trans, Matrix.getTranslate(origin, true), trans);
      //
      //          DArray oa = Editor.editObjects(null, true, false);
      //          for (Iterator it = oa.iterator(); it.hasNext();) {
      //            EdObject obj = (EdObject) it.next();
      //            for (int i = 0; i < obj.nPoints(); i++) {
      //              obj.setTransformedPoint(i, trans.apply(obj.getPoint(i)));
      //            }
      //          }
      //        }
      //        break;
      }
    }
  }
  public void setParameters() {
    parms.appTitle = "Guaranteed and Possible Hulls";
    parms.menuTitle = "UHull";
    parms.fileExt = "dat";
    //  parms.traceSteps = 2000;
  }

  public void paintView() {
    discs = null;
    polygons = null;
    segments = null;
    super.paintView();

    if (C.vb(G_RNDTEST)) {
      if (T.lastEvent() == null) {
        GeneratorOper.generateRandom();
        V.repaint();
      } else
        C.setb(G_RNDTEST, false);
    }
  }

  public static EdSegment[] getSegments() {
    if (segments == null) {
      DArray a = Editor.readObjects(EdSegment.FACTORY, false, true);
      segments = (EdSegment[]) a.toArray(EdSegment.class);
    }
    return segments;
  }

  public static EdPolygon[] getPolygons() {
    if (polygons == null) {

      DArray a = Editor.readObjects(EdPolygon.FACTORY, false, true);
      polygons = (EdPolygon[]) a.toArray(EdPolygon.class);
      for (int i = 0; i < polygons.length; i++) {
        polygons[i] = EdPolygon.normalize(polygons[i]);
      }
    }
    return polygons;
  }

  /**
   * Construct a string that uniquely describes a set of EdObjects
   * @param obj
   * @return
   */
  public static String getHash(EdObject[] obj) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < obj.length; i++) {
      sb.append(obj[i].getHash());
    }
    return sb.toString();
  }

  public static void perturbDiscs() {
    Matrix m = new Matrix(3);
    m.setIdentity();

    m.translate(-50, -50);
    m.rotate(MyMath.radians(1));
    m.translate(50, 50);

    for (Iterator it = Editor.editObjects(EdDisc.FACTORY, false, false)
        .iterator(); it.hasNext();) {
      EdDisc ed = (EdDisc) it.next();
      FPoint2 loc = ed.getOrigin();
      loc = m.apply(loc, null);
      ed.setPoint(0, loc);
    }
    Editor.unselectAll();
  }

  public static EdDisc[] getDiscs() {
    if (discs == null) {
      DArray a = Editor.readObjects(EdDisc.FACTORY, false, true);
      filterUnique(a);
      discs = (EdDisc[]) a.toArray(EdDisc.class);
    }
    return discs;
  }

  private static void filterUnique(DArray a) {
    Map map = new HashMap();
    for (int i = 0; i < a.size(); i++) {
      EdDisc d = (EdDisc) a.get(i);
      String key = "" + d.getOrigin() + " " + Tools.f(d.getRadius());
      if (map.containsKey(key)) {
        a.remove(i);
        i--;
      } else
        map.put(key, d);
    }
  }

  private static EdDisc[] discs;
  private static EdPolygon[] polygons;
  private static EdSegment[] segments;
}
