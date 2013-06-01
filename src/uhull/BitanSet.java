package uhull;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class BitanSet implements Renderable, Globals {

  public void render(Color c, int stroke, int markType) {
    if (c == null) {
      c = color;
      if (c == null)
        c = MyColor.cDARKGREEN;
    }

    if (set != null)
      for (Iterator it = set.iterator(); it.hasNext();) {
        BSEnt ent = next(it);

        T.render(ent, c, stroke, markType);
      }
    T.render(discBeingAdded, MyColor.cDARKGREEN, STRK_THICK, -1);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("BitanSet[");
    Iterator it = iterator();
    while (it.hasNext()) {
      sb.append("\n ");
      sb.append(next(it));
    }
    sb.append(" ]");
    return sb.toString();
  }

  public BSEnt last() {
    return (BSEnt) set.last();
  }

  private Iterator iterator() {
    return new OurIterator(firstEnt);
  }
  private BSEnt next(Iterator it) {
    return (BSEnt) it.next();
  }

  /**
   * Extract all discs from set, in ccw order
   * @return array of discs
   */
  public DArray getDiscs() {
    DArray ret = new DArray();
    for (BSEnt b = firstEnt; b != null; b = b.next())
      ret.add(b.disc());
    return ret;
  }

  public BitanSet() {
  }

  public void setDebug(boolean debug) {
    this.db = debug;
  }

  public void construct(EdDisc[] discs) {

    discs = (EdDisc[]) discs.clone();
    Arrays.sort(discs, HullUtil.sortByRadii);

    do {
      EdObject[] extr = HullUtil.calcExtremeDiscs(discs, true);
      westDisc = (EdDisc) extr[HullUtil.WEST];
      eastDisc = (EdDisc) extr[HullUtil.EAST];
      if (westDisc == null)
        break;

      if (db && T.update())
        T.msg("west disc=" + westDisc + " east disc=" + eastDisc
            + T.show(westDisc) + T.show(eastDisc));

      final double OFFSET = 30;
      // add discs 'at infinity' for east, west discs
      FPoint2 or;
      //EdDisc wInf = new EdDisc(or.x, or.y - OFFSET, westDisc.getRadius());
      or = eastDisc.getOrigin();
      EdDisc eInf = new EdDisc(or.x, or.y - OFFSET, eastDisc.getRadius());
      eInf.setLabel("~");

      BSEnt ent0 = new BSEnt(eInf, BSEnt.THETA_MIN, eastDisc, OFFSET);
      BSEnt ent1 = null;
      if (westDisc != eastDisc) {
        BiTangent b = new BiTangent(eastDisc, westDisc);
        double length = FPoint2.distance(b.tangentPt(0), b.tangentPt(1));
        ent1 = new BSEnt(eastDisc, b.thetaP(), westDisc, length);
      }
      //      or = westDisc.getOrigin();
      //       EdDisc wInf = new EdDisc(or.x, or.y - OFFSET, westDisc.getRadius());
      //      wInf.setLabel("~");
      //      BSEnt ent2 = new BSEnt(westDisc, BSEnt.THETA_MAX, wInf, OFFSET);
      BSEnt ent2 = new BSEnt(westDisc, BSEnt.THETA_MAX, null, OFFSET);

      set = new TreeSet(BSEnt.comparator);
      set.add(ent0);
      set.add(ent2);
      if (ent1 != null) {
        set.add(ent1);
        BSEnt.join(ent0, ent1);
        BSEnt.join(ent1, ent2);
      } else {
        BSEnt.join(ent0, ent2);
      }

      firstEnt = ent0;

      for (int di = 0; di < discs.length; di++) {
        if (di == 0) {
          if (db && T.update())
            T.msg("pausing before first");
        }
        add(discs[di]);
      }

    } while (false);
  }

  /**
   * Add disc to set
   * @param query disc to add
   * @return true if disc modified the hull
   */
  private boolean add(EdDisc query) {

    discBeingAdded = query;

    if (db && T.update())
      T.msg("BitanSet.add " + query);

    boolean wasAdded = false;
    do {
      if (query == eastDisc || query == westDisc) {
        if (db && T.update())
          T.msg("east or west disc, skipping");
        break;
      }

      BSEnt uEnt;
      {
        SortedSet tailSet = set.tailSet(BSEnt.searchEntry(query,
            BSEnt.THETA_MIN));
        if (tailSet.isEmpty()) {
          if (db && T.update())
            T.msg("tailset empty, skipping");
          break;
        }
        uEnt = (BSEnt) tailSet.first();
      }

      BiTangent qBwd = null;
      BiTangent qFwd = new BiTangent(query, uEnt.disc());
      if (db && T.update())
        T.msg("found u:" + uEnt + T.show(uEnt) + T.show(qFwd));
      if (!qFwd.defined()) {
        if (db && T.update())
          T.msg("undefined, query disc must contain another");
        break;
      }

      // make sure this is a hull disc by examining angle from previous
      {
        double prevTheta = uEnt.prev().theta();
        if (db && T.update())
          T.msg("verifying hull tangent by comparing\n" + "qFwd angle="
              + Tools.fa2(qFwd.thetaP()) + " with\n" + "prev angle="
              + Tools.fa2(prevTheta) + T.show(uEnt.prev())
              + T.show(qFwd, MyColor.cDARKGREEN));

        if (qFwd.thetaP() <= prevTheta) {
          if (db && T.update())
            T.msg("qFwd angle < prev, not a hull tangent" + T.show(uEnt.prev())
                + T.show(qFwd, MyColor.cDARKGREEN));
          break;
        }
      }

      // scan backward until we find bitangent whose flipped predicate
      // is true

      BSEnt ent = uEnt;
      {
        for (ent = uEnt;; ent = ent.prev()) {
          qBwd = new BiTangent(ent.disc(), query);
          if (qBwd.thetaP() > ent.prev().theta()
              && qBwd.thetaP() < BSEnt.THETA_MAX) {
            if (db && T.update())
              T.msg("stopping");
            break;
          }
        }
      }

      // delete existing bitangents whose angles fall between the new ones
      for (BSEnt d = ent; d.theta() < qFwd.thetaP(); d = d.next())
        set.remove(d);

      BSEnt qBwdEnt = new BSEnt(qBwd.disc(0), qBwd.theta(), query, FPoint2
          .distance(qBwd.tangentPt(0), qBwd.tangentPt(1)));
      BSEnt qFwdEnt = new BSEnt(query, qFwd.theta(), qFwd.disc(1), FPoint2
          .distance(qFwd.tangentPt(0), qFwd.tangentPt(1)));

      if (db && T.update())
        T.msg("qBwd=" + qBwdEnt + T.show(qBwdEnt) + "\n" + "qFwd=" + qFwdEnt
            + T.show(qFwdEnt) + "\nent1Prev=" + ent.prev() + "\nuEnt=" + uEnt);
      BSEnt.join(ent.prev(), qBwdEnt);
      BSEnt.join(qBwdEnt, qFwdEnt);
      BSEnt.join(qFwdEnt, uEnt);

      if (db && T.update())
        T.msg("adding qBwd, qFwd entries" + T.show(qBwdEnt) + T.show(qFwdEnt));
      set.add(qBwdEnt);
      set.add(qFwdEnt);
      wasAdded = true;

      if (db && T.update())
        T.msg("after additions:\n" + this);
    } while (false);

    discBeingAdded = null;

    return wasAdded;
  }

  private static class OurIterator implements Iterator {

    private BSEnt next;
    public OurIterator(BSEnt firstEnt) {
      next = firstEnt;
    }
    public boolean hasNext() {
      return next != null;
    }

    public Object next() {
      BSEnt ret = next;
      if (ret == null)
        throw new IllegalStateException();
      next = next.next();
      return ret;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public void setColor(Color color) {
    this.color = color;
  }
  public Color color() {
    return color;
  }
  public boolean isEmpty() {
    return firstEnt == null;
  }

  private EdDisc discBeingAdded;
  private EdDisc westDisc, eastDisc;
  private boolean db;
  private TreeSet set;
  private BSEnt firstEnt;
  private Color color;
}
