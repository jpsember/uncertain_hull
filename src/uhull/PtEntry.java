package uhull;

import java.awt.*;
import testbed.*;
import uhull.COper3.*;
import base.*;
import static base.Tools.*;

public class PtEntry extends FPoint2 implements Globals {
  private static final double EPS = 1e-3;

  public static boolean samePoint(FPoint2 p1, FPoint2 p2) {
    return FPoint2.distanceSquared(p1, p2) < EPS * EPS;
  }

  //  private static String tp(FPoint2 pt) {
  //    if (pt == null)
  //      return "<null>";
  //    return T.show(pt, null, -1, MARK_DISC);
  //  }

  /**
   * Copy constructor
   * 
   * If source is also a PtEntry, copies source, index fields;
   * else sets both to -1
   */
  public PtEntry(FPoint2 pt) {
    super(pt);
    this.id = uniqueId++;
    orig = this;

    if (pt instanceof PtEntry) {
      PtEntry p2 = (PtEntry) pt;
      orig = p2.orig;
    }
    if (false && id == 132)
      T.err("constructed 132");
  }

  //  /**
  //   * Insert point after this point, if it is not equal to this point or its follower
  //   * @param pt new point
  //   * @return new point's entry, or existing point if it was the same
  //   * @deprecated
  //   */
  //  public PtEntry addPoint(FPoint2 pt) {
  //
  //    PtEntry ret;
  //
  //    final boolean db = false;
  //    boolean same = PtEntry.samePoint(pt, this);
  //
  //    if (db && T.update())
  //      T.msg("addPoint " + tp(pt) + pt + "\nexisting=" + this + tp(this)
  //          + " same=" + same);
  //    if (same) {
  //      T.err("illegal");
  //      ret = this;
  //    } else if (PtEntry.samePoint(pt, this.next())) {
  //      T.err("illegal");
  //      ret = this.next();
  //    } else {
  //
  //      PtEntry ent = new PtEntry(pt);
  //
  //      PtEntry follow = this.next();
  //
  //      join(this, ent);
  //      join(ent, follow);
  //      ret = ent;
  //    }
  //    return ret;
  //  }

  //  /**
  //   * @deprecated
  //   */
  //  public void delete() {
  //    next2.prev2 = prev2;
  //    prev2.next2 = next2;
  //    next2 = null;
  //    prev2 = null;
  //  }

  /**
   * Delete entry, return its next entry, or null if none remain
   * @return ent
   */
  public PtEntry delete(boolean ccw) {
    PtEntry ret = next(ccw);

    if (ret != null)
      join(prev2, next2);
    this.prev2 = null;
    this.next2 = null;
    return ret;
  }

  public Object source() {
    return orig.source;
  }

  //  /**
  //   * @deprecated
  //   * @return
  //   */
  //  public int index() {
  //    return this.index;
  //  }

  public void render() {
    V.pushScale(.6);

    V.draw(
        //this.toString()  
        "src<" + source() + ">", MyMath.ptOnCircle(this, Math.PI / 4, 2),
        Globals.TX_FRAME | Globals.TX_BGND);
    V.pop();
  }

  //  /**
  //   * @deprecated
  //   * @return
  //   */
  //  public PtEntry next() {
  //    return next2;
  //  }
  //  /**
  //   * @deprecated
  //   * @return
  //   */
  //  public PtEntry prev() {
  //    return prev2;
  //  }

  public void setSource(Object src) {
    //    if (this.orig != null)
    //      T.err("illegal state");
    this.source = src;
  }

  //  /**
  //   * @deprecated
  //   * @param index
  //   */
  //  public void setIndex(int index) {
  ////    if (this.orig != null)
  ////      T.err("illegal state");
  //    this.index = index;
  //
  //  }
  public void setPrev(PtEntry e) {
    this.prev2 = e;
  }
  public void setNext(PtEntry e) {
    this.next2 = e;
  }
  public String toString() {
    if (true) {
      StringBuilder sb = new StringBuilder();
      sb.append("s<" + source() + ">");
      sb.append("#" + id);
      sb.append(" ");
      sb.append("(");
      sb.append(Tools.f(x));
      sb.append(Tools.f(y));
      sb.append(")");
      sb.append(" n:" + (next2 == null ? "-" : "y") + " p:"
          + (prev2 == null ? "-" : "y"));
      return sb.toString();
    } else
      return super.toString();
  }
  public static void join(PtEntry a, PtEntry b) {
    if (a != null)
      a.next2 = b;
    if (b != null)
      b.prev2 = a;
  }

  //  private static EdPolygon buildPolygonFrom(PtEntry p) {
  //    EdPolygon poly = new EdPolygon();
  //    PtEntry p0 = p;
  //    while (true) {
  //      poly.addPoint(p);
  //      p = p.next(true);
  //      if (p == null) {
  //        poly.setFlags(EdPolygon.FLG_OPEN);
  //        break;
  //      }
  //      if (p == p0)
  //        break;
  //    }
  //    return poly;
  //  }

  public PtEntry orig() {
    return orig;
  }

  public void render(Color c, int stroke, int markType) {
    V.pushColor(c, MyColor.cRED);
    V.mark(this, MARK_DISC, 1.0);
    V.pop();
  }
  //  /**
  //   * Build polygon from loop of entries
  //   * @param c
  //   * @return polygon
  //   */
  //  private static MyPolygon buildPolygon(PtEntry c) {
  //    final boolean db = false; //C.vb(COper.DB_FILTER);
  //    if (db && T.update())
  //      T.msg("buildPolygon from entries");
  //    DArray pts = new DArray();
  //    PtEntry c0 = c;
  //    PtEntry cPrev = c.prev2;
  //    do {
  //      if (samePoint(c, cPrev)) {
  //        T.msg("duplicate point" + T.show(c));
  //      } else {
  //        PtEntry ent = new PtEntry(c);
  //        pts.add(ent);
  //        if (db && T.update())
  //          T.msg("adding distinct point " + ent + T.show(ent));
  //        cPrev = c;
  //      }
  //      c = c.next(true);
  //    } while (c != c0);
  //
  //    return new MyPolygon((PtEntry[]) pts.toArray(PtEntry.class));
  //  }
  /**
   * Determine if node has been deleted (i.e., if 
   * its next/prev are null)
   * @return true if not connected to neighboring nodes
   */
  public boolean isDeleted() {
    return next2 == null;
  }

  /**
   * Render a linked list of nodes
   */
  public static void renderSet(FPoint2 kernelPt, PtEntry e0) {
    PtEntry ent = e0;
    Inf inf = Inf.create();

    final boolean db = false;
    if (db) 
      pr("renderSet, e0="+e0);
          
    do {
      inf.update();

      Color c = MyColor.cDARKGREEN;

      ent.render(c, -1, -1);
      V.pushColor(c);
      PtEntry entNext = ent.next(true);
            
      if (entNext != null) {
        V.pushStroke(STRK_THICK);
        V.drawLine(ent, entNext);
        V.pop();
      }
      V.pop();
      ent = ent.next(true);
      if (db) 
        pr(" moved to next "+ent);
     } while (ent != null && ent != e0);

  }

  public PtEntry next(boolean ccw) {
    return ccw ? next2 : prev2;
  }
  public PtEntry prev(boolean ccw) {
    return ccw ? prev2 : next2;
  }

  /**
   * Insert an entry after this one
   * @param ent
   * @return ent
   */
  public PtEntry insert(PtEntry ent, boolean ccw) {
    if (ccw) {
      PtEntry oldNext = next2;
      join(this, ent);
      join(ent, oldNext);
    } else {
      PtEntry oldPrev = prev2;
      join(ent, this);
      join(oldPrev, ent);
    }

    return ent;
  }

  public static void resetIds() {
    uniqueId = 100;
  }

  /**
   * Determine convex hull of a polygon
   * @param handle handle to polygon
   * @return handle to convex hull
   */
  public static PtEntry convexHull(PtEntry handle) {

    DArray p = new DArray();
    p.add(handle);
    for (PtEntry ent = handle.next(true); ent != handle; ent = ent.next(true))
      p.add(ent);

    DArray h = MyMath.convexHull(p);
    PtEntry ret = null;

    for (int i = 0; i < h.size(); i++) {
      int j = h.getInt(i);
      PtEntry origPt = (PtEntry) p.get(j);
      PtEntry hullPt = new PtEntry(origPt);
      if (ret == null) {
        ret = hullPt;
        PtEntry.join(ret, ret);
      } else {
        ret = ret.insert(hullPt, true);
      }
    }
    return ret;
  }

  /**
   * Construct loop of pts from EdPolygon.
   * Sets source for each point to the polygon.
   */
  public static PtEntry buildFrom(EdPolygon p) {
    PtEntry ret = new PtEntry(p.getPoint(0));
    ret.setSource(p);
    PtEntry.join(ret, ret);
    for (int i = 1; i < p.nPoints(); i++) {
      ret = ret.insert(new PtEntry(p.getPoint(i)), true);
      ret.setSource(p);
    }
    return ret;
  }

  /**
   * Construct EdPolygon from handle
   * @return EdPolygon
   */
  public EdPolygon toPolygon() {
    final boolean db = false;

    if (db)
      pr("PtEntry.toPolygon");

    EdPolygon p = new EdPolygon();
    PtEntry ent = this;

    do {
      if (db)
        pr("ent=" + ent + ", next=" + ent.next(true));

      p.addPoint(ent);
      ent = ent.next(true);
    } while (ent != null && ent != this);
    return p;
  }

  // links to neighbors
  private PtEntry prev2;
  private PtEntry next2;

  private PtEntry orig;
  private Object source;
  private int id;

  /**
   * For debug purposes only, a unique id for this instance
   * @return
   */
  public int id() {
    return id;
  }
  private static int uniqueId = 100;
}
