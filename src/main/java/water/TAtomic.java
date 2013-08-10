package water;

/**
 * A typed atomic update.
 */
public abstract class TAtomic<T extends Iced> extends Atomic<TAtomic<T>> {
  /** Atomically update an old value to a new one.
   * @param old  The old value, it may be null.  It is a defensive copy.
   * @return The new value; if null if this atomic update no longer needs to be run
   */
  public abstract T atomic(T old);

  @Override public Value atomic(Value val) {
    T old = val == null ? null : (T)(val.get().clone());
    T nnn = atomic(old);
    return  nnn == null ? null : new Value(_key,nnn);
  }
}
