package water;

import water.H2O.H2OCountedCompleter;

/** Objects which are passed & remotely executed.<p>
 * <p>
 * Efficient serialization methods for subclasses will be automatically
 * generated, but explicit ones can be provided.  Transient fields will
 * <em>not</em> be mirrored between the VMs.
 * <ol>
 * <li>On the local vm, this task will be serialized and sent to a remote.</li>
 * <li>On the remote, the task will be deserialized.</li>
 * <li>On the remote, the {@link #invoke(H2ONode)} method will be executed.</li>
 * <li>On the remote, the task will be serialized and sent to the local vm</li>
 * <li>On the local vm, the task will be deserialized
 * <em>into the original instance</em></li>
 * <li>On the local vm, the {@link #onAck()} method will be executed.</li>
 * <li>On the remote, the {@link #onAckAck()} method will be executed.</li>
 * </ol>
 */
public abstract class DTask<T extends DTask> extends H2OCountedCompleter implements Freezable {
  // Track if the reply came via TCP - which means a timeout on ACKing the TCP
  // result does NOT need to get the entire result again, just that the client
  // needs more time to process the TCP result.
  transient boolean _repliedTcp; // Any return/reply/result was sent via TCP

  /** Top-level remote execution hook.  Called on the <em>remote</em>. */
  public void dinvoke( H2ONode sender ) { compute2(); }

  /** 2nd top-level execution hook.  After the primary task has received a
   * result (ACK) and before we have sent an ACKACK, this method is executed
   * on the <em>local vm</em>.  Transients from the local vm are available here.
   */
  public void onAck() {}

  /** 3rd top-level execution hook.  After the original vm sent an ACKACK,
   * this method is executed on the <em>remote</em>.  Transients from the remote
   * vm are available here.
   */
  public void onAckAck() {}

  /** Override to remove 2 lines of logging per RPC. */
  public boolean logVerbose() { return true; }

  // The abstract methods to be filled in by subclasses.  These are automatically
  // filled in by any subclass of DTask during class-load-time, unless one
  // is already defined.  These methods are NOT DECLARED ABSTRACT, because javac
  // thinks they will be called by subclasses relying on the auto-gen.
  private RuntimeException barf() {
    return new RuntimeException(getClass().toString()+" should be automatically overridden in the subclass by the auto-serialization code");
  }
  @Override public AutoBuffer write(AutoBuffer bb) { throw barf(); }
  @Override public <F extends Freezable> F read(AutoBuffer bb) { throw barf(); }
  @Override public <F extends Freezable> F newInstance() { throw barf(); }
  @Override public int frozenType() { throw barf(); }
  @Override public AutoBuffer writeJSONFields(AutoBuffer bb) { throw barf(); }
  @Override public water.api.DocGen.FieldDoc[] toDocField() { return null; }
  public void copyOver(T that) { throw barf(); }
  @Override public DTask clone() {
    try { return (DTask)super.clone(); }
    catch( CloneNotSupportedException e ) { throw water.util.Log.errRTExcept(e); }
  }
}
