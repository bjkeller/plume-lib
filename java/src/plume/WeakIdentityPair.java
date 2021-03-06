package plume;

import java.lang.ref.WeakReference;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
*/


/**
 * Immutable pair class:
 * type-safely holds two objects of possibly-different types.
 * <p>
 * Differs from Pair in the following ways:  is immutable, cannot hold
 * null, holds its elements with weak pointers, and its equals() method
 * uses object equality to compare its elements.
 */
public class WeakIdentityPair<T1 extends Object,T2 extends Object> {

  final private WeakReference<T1> a;
  final private WeakReference<T2> b;

  // Must cache the hashCode to prevent it from changing.
  final private int hashCode;

  public WeakIdentityPair(T1 a, T2 b) {
    if (a == null || b == null) {
      throw new IllegalArgumentException(String.format(
              "WeakIdentityPair cannot hold null: %s %s", a, b));
    }
    this.a = new WeakReference<T1>(a);
    this.b = new WeakReference<T2>(b);
    int localHashCode = 0;
    try {
      localHashCode = a.hashCode() + b.hashCode();
    } catch (StackOverflowError e) {
    }
    hashCode = localHashCode;
  }

  /** Factory method with short name and no need to name type parameters.
   * @param <A> type of first argument
   * @param <B> type of second argument
   * @param a first argument
   * @param b second argument
   * @return a WeakIdentityPair of (a, b)
   */
  public static <A extends Object, B extends Object> WeakIdentityPair<A, B> of(A a, B b) {
    return new WeakIdentityPair<A, B>(a, b);
  }

  /** Return the first element of the pair, or null if it has been garbage-collected.
   * @return the first element of the pail, or null if it has been garbage-collected
   */
  /*@SideEffectFree*/ public /*@Nullable*/ T1 getA() {
    return a.get();
  }

  /** Return the second element of the pair, or null if it has been garbage-collected.
   * @return the second element of the pair, or null if it has been garbage-collected
   */
  /*@SideEffectFree*/ public /*@Nullable*/ T2 getB() {
    return b.get();
  }

  @Override
  /*@SideEffectFree*/ public String toString() {
    return "<" + String.valueOf(a) + "," + String.valueOf(b) + ">";
  }

  @Override
  @SuppressWarnings({"interning", "not.deterministic.call"}) // getA is not @deterministic, but its value is checked, so this method is deterministic
  /*@Pure*/ public boolean equals(/*@Nullable*/ Object obj) {
    if (! (obj instanceof WeakIdentityPair<?, ?>)) {
      return false;
    }
    // generics are not checked at run time!
    @SuppressWarnings("unchecked")
    WeakIdentityPair<T1, T2> other = (WeakIdentityPair<T1, T2>) obj;

    if (hashCode != other.hashCode) {
      return false;
    }

    /*@Nullable*/ T1 a = getA();
    /*@Nullable*/ T2 b = getB();
    /*@Nullable*/ T1 oa = other.getA();
    /*@Nullable*/ T2 ob = other.getB();
    if (a == null || b == null || oa == null || ob == null) {
      // false if any of the components has been garbage-collected
      return false;
    }
    return a == oa && b == ob;
  }


  @Override
  /*@Pure*/
  public int hashCode() {
    return hashCode;
  }

}
