package uhull;

import java.awt.*;
import base.*;
import testbed.*;

public class DiscSampleOper implements TestBedOperation, Globals {
  /*! .enum  .private 3900
     resetsamp ntrials plotlast run step   plotsimp 
  */

    private static final int RESETSAMP        = 3900;//!
    private static final int NTRIALS          = 3901;//!
    private static final int PLOTLAST         = 3902;//!
    private static final int RUN              = 3903;//!
    private static final int STEP             = 3904;//!
    private static final int PLOTSIMP         = 3905;//!
/*!*/

  public void addControls() {
    C.sOpenTab("GSamp");
    C.sStaticText("Generates guaranteed hull"
        + " as intersection of convex hulls of randomly"
        + " sampled points from discs");
    C.sCheckBox(PLOTSIMP, "Plot simp",
        "Plot guaranteed hull, for testing purposes, "
            + "using simple, robust algorithm", false);

    C.sButton(STEP, "Step", "Process single random hull");
    {
      C.sOpen();
      C.sButton(RUN, "Run", "Generate a number of random hulls");
      C.sNewColumn();
      C.sIntSpinner(NTRIALS, "# trials",
          "Number of samples to clip guaranteed hull to", 1, 10000, 10, 10);
      C.sClose();
    }
    C.sButton(RESETSAMP, "Reset", "Clears sampled hull");
    C.sCheckBox(PLOTLAST, "Plot last sample", null, true);
    C.sCloseTab();
  }

  public static DiscSampleOper singleton = new DiscSampleOper();
  private DiscSampleOper() {
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      case RESETSAMP:
        hullPoly = null;
        lastHull = null;
        lastSamples = null;
        break;
      case STEP:
        run(1);
        break;
      case RUN:
        run(C.vi(NTRIALS));
        break;
      }
    }
  }

  private void run(int trials) {

    final boolean db = false;

    EdDisc[] d = UHullMain.getDiscs();
    String newHash = UHullMain.getHash(d);
    if (!newHash.equals(hash)) {
      hash = newHash;
      hullPoly = null;
    }
    if (hullPoly == null) {
      hullPoly = INFINITE;
    }

    if (d.length < 3)
      return;

    for (int i = 0; i < trials; i++) {
      if (db)
        Streams.out.println("running trial #" + i + " on " + d.length
            + " discs");

      DArray pts = new DArray();
      for (int j = 0; j < d.length; j++) {
        EdDisc ds = d[j];
        FPoint2 pt = MyMath.rndPtInDisc(ds.getOrigin(), ds.getRadius(), null);
        pts.add(pt);
      }
      lastSamples = pts;
      EdPolygon p = HullUtil.calcHullOf(pts);
      lastHull = p;
      if (db)
        Streams.out.println(" sampled convex hull=\n" + p);

      if (hullPoly == INFINITE) {
        if (db)
          Streams.out
              .println(" hullPoly was infinite, replacing with this hull");

        hullPoly = new EdPolygon();
        hullPoly.copyPointsFrom(p);
      } else if (hullPoly == EMPTY) {
        if (db)
          Streams.out.println(" hullPoly already empty");

      } else {
        // clip guar. convex hull to each side of this polygon
        for (int s = 0; s < p.nPoints(); s++) {
          if (hullPoly == EMPTY)
            break;

          if (db)
            Streams.out.println("getting segment " + s + " of " + p.nPoints());

          FPoint2 s0 = p.getPointMod(s);
          FPoint2 s1 = p.getPointMod(s + 1);
          if (db)
            Streams.out.println(" s0=" + s0 + " s1=" + s1);

          hullPoly = hullPoly.clipTo(s0, s1);
          hullPoly.filterCollinear(1e-2);
          if (db)
            Streams.out.println(" clipped to side " + s0 + " ... " + s1 + ":\n"
                + hullPoly);
        }
      }
    }
  }

  public void runAlgorithm() {
    simpleHull = null;
    if (C.vb(PLOTSIMP))
      simpleHull = HullUtil.build(UHullMain.getDiscs());
  }

  public void paintView() {
    Editor.render();
//    Tools
//        .unimp("find out why dashed lines occur in discs when resizing window");
    T.render(simpleHull, MyColor.cPURPLE, STRK_THICK, -1);
    if (C.vb(PLOTLAST)) {
      T.renderAll(lastSamples, MyColor.cPURPLE, -1, MARK_DISC);
      T.render(lastHull, MyColor.cPURPLE, STRK_RUBBERBAND, -1);
    }
    T.render(hullPoly, MyColor.cDARKGREEN, -1, -1); //MARK_DISC);

  }

  private static final EdPolygon INFINITE = new EdPolygon(),
      EMPTY = new EdPolygon();

  private String hash;
  private EdPolygon hullPoly;
  private EdPolygon lastHull;
  private DArray lastSamples;
  private EdPolygon simpleHull;
}
