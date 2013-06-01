package uhull;

public class BSEntException extends RuntimeException {

  public static final int CODE_DISCCONTAINSANOTHER = 1;
  public static final int CODE_ENTRYREDUNDANT = 2;
  public BSEntException(int code, BSEnt ent, String msg) {
    super(msg);
    this.ent = ent;
    this.code = code;
  }
  private int code;
  public int code() {
    return code;
  }
  private BSEnt ent;
  public BSEnt entry() {
    return ent;
  }
}
