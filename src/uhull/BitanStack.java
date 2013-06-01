package uhull;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class BitanStack implements Renderable, Globals {

  private boolean db;

  public void setTrace(boolean t) {
    this.db = t;
  }

  public void render(Color c, int stroke, int markType) {
    if (c == null) {
      c = color;
      if (c == null)
        c = MyColor.cDARKGREEN;
    }

    T.render(removed, c, STRK_RUBBERBAND, -1);
    for (int i = 0; i < stack.size(); i++)
      T.render(displayEnt(i), c, stroke, markType);
  }

  /**
   * Construct a BSEnt for a particular stack disc.
   * For display purposes only.
   * @param distFromTop distance from top of stack
   * @return BSEnt
   */
  private BSEnt displayEnt(int distFromTop) {
    EdDisc aDisc = (EdDisc) stack.peek(distFromTop);
    EdDisc bDisc = null;
    double candidateTheta = BSEnt.THETA_MAX;
    double len = 5;
    if (distFromTop + 1 < stack.size()) {
      bDisc = (EdDisc) stack.peek(distFromTop + 1);
      BiTangent bit2 = new BiTangent(aDisc, bDisc);
      len = FPoint2.distance(bit2.tangentPt(0), bit2.tangentPt(1));
      candidateTheta = bit2.thetaP();
    }
    BSEnt ent = new BSEnt(aDisc, candidateTheta, null, len);
    return ent;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("BitanStack[");
    Iterator it = stack.iterator(true);
    while (it.hasNext()) {
      sb.append("\n ");
      sb.append(it.next());
    }
    sb.append(" ]");
    return sb.toString();
  }

  public BitanStack(BitanSet bitanSet) {
    BSEnt ent = bitanSet.last();
    while (ent != null) {
      stack.push(ent.disc());
      ent = ent.prev();
    }

    // Pop the ~east/east entry, since it is unnecessary.
    if (!stack.isEmpty())
      stack.pop();

    setColor(bitanSet.color());
  }

  /**
   * Pop discs from stack until we find one that makes a possible
   * hull bitangent with a query disc, or until the stack is empty.
   * @param query disc for source of bitangent
   * @param minTheta angle of hull bitangent must be larger than this value
   * @return entry, if found
   */
  public EdDisc find(EdDisc query, double minTheta) {

    final boolean db2 = false;

    EdDisc ret = null;
    minTheta = MyMath.normalizeAnglePositive(minTheta);

    if (db2 && T.update())
      T.msg("BitanStack.find query=" + query + " minTheta="
          + Tools.fa2(minTheta));

    while (!stack.isEmpty()) {

      // stack contains only discs, so
      // get angle xy as angle of bitangent
      // constructed from top two discs;
      // if only one disc exists, use THETA_MAX

      double xyTheta = BSEnt.THETA_MAX;

      EdDisc xDisc = (EdDisc) stack.peek(0);
      EdDisc yDisc = null;

      if (stack.size() >= 2) {
        yDisc = (EdDisc) stack.peek(1);
        BiTangent b = new BiTangent(xDisc, yDisc);
        xyTheta = b.thetaP();
      }

      String msg = null;

      if (query != xDisc) {

        BiTangent b = new BiTangent(query, xDisc);

        // if bitangent undefined, 
        // x must contain q; pop x

        if (!b.defined()) {
          msg = "redundant disc";
        } else {
          // pop if qx not in range; otherwise, return x
          double qxTheta = b.thetaP();
          if (qxTheta <= minTheta || qxTheta >= xyTheta)
            msg = "qx not in range";
          else
            ret = xDisc;
        }
      } else {
        // q is equal to x; return y
        ret = yDisc;
      }

      if (msg == null)
        break;

      BSEnt poppedEntry = displayEnt(0);
      if (db && T.update())
        T.msg(msg + T.show(poppedEntry, null, STRK_THICK, -1));
      removed.add(poppedEntry);

      stack.pop();
    }
    return ret;
  }

  public void setColor(Color color) {
    this.color = color;
  }
  public Color color() {
    return color;
  }
  public boolean isEmpty() {
    return stack.isEmpty();
  }
  public int size() {
    return stack.size();
  }
  public EdDisc peek(int distFromTop) {
    return (EdDisc) stack.peek(distFromTop);
  }

  private DArray stack = new DArray();
  private DArray removed = new DArray();
  private Color color;
}
