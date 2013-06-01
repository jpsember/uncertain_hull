package uhull;

import base.*;
import testbed.*;

/**
 * Generalized segment interface
 * 
 * See Hershberger paper, p.172, section 4
 */
public interface GenSeg extends Renderable {

  /**
   * Get xcoord of left extent of segment
   * @return
   */
  public double left();
  /**
   * Get xcoord of right extent of segment
   * @return
   */
  public double right();
  /**
   * Determine which segment is highest at an xcoord
   * @param seg1 first seg
   * @param seg2 second seg; its id will always be greater than seg1's
   * @param x xcoord
   * @return higher of the segs, or null if equal
   */
  public GenSeg higherOf(GenSeg seg1, GenSeg seg2, double x);
  /**
   * Determine next xcoord of intersection of two segments
   * @param seg1 first seg
   * @param seg2 second seg; its id will always be greater than seg1's
   * @param pastX xcoord
   * @return xcoord of first intersection higher than pastX, or null if none
   */
  public Double nextIntersection(GenSeg seg1, GenSeg seg2, double pastX);
  
  /**
   * Construct clipped copy of this segment
   * @param left new left extent
   * @param right new right extent
   * @return clipped segment
   */
  public GenSeg clipTo(double left, double right);
  
  public String showSweepLine(double x);
  
  /**
   * Get id of segment.  This ensures consistent ordering for
   * comparisons.  If two segment's ids are the same, they are assumed to
   * be subsets of the same segment.
   * @return
   */
  public int id();
  
}
