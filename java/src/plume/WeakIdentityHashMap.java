/*
 * @(#)WeakHashMap.java	1.30 04/02/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package plume;

import java.util.Iterator;
import java.util.Map;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Set;
import java.util.AbstractSet;
import java.util.NoSuchElementException;
import java.util.Collection;
import java.util.AbstractCollection;
import java.util.ConcurrentModificationException;
import java.util.ArrayList;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

/*>>>
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
*/


/**
 * This class combines the features of {@link java.util.WeakHashMap} and
 * {@link java.util.IdentityHashMap}.
 * The implementation is a modified version of {@link java.util.WeakHashMap}
 * from JDK 1.5, which differs from the original in two ways:
 * <ul>
 *  <li> uses of hashCode() are replaced by System.identityHashCode()</li>
 *  <li> uses of object equality (.equals) are replaced by identity checks (==)</li>
 * </ul>
 * See {@link java.util.IdentityHashMap}
 * for more information on the changes made in an identity hash map..
 *
 * <hr>
 *
 * A hashtable-based <tt>Map</tt> implementation with <em>weak
 * keys</em>.  An entry in a <tt>WeakIdentityHashMap</tt> will
 * automatically be removed when its key is no longer in ordinary use.
 * More precisely, the presence of a mapping for a given key will not
 * prevent the key from being discarded by the garbage collector, that
 * is, made finalizable, finalized, and then reclaimed.  When a key
 * has been discarded its entry is effectively removed from the map,
 * so this class behaves somewhat differently than other <tt>Map</tt>
 * implementations.
 *
 * <p> Both null values and the null key are supported. This class has
 * performance characteristics similar to those of the <tt>HashMap</tt>
 * class, and has the same efficiency parameters of <em>initial capacity</em>
 * and <em>load factor</em>.
 *
 * <p> Like most collection classes, this class is not synchronized.  A
 * synchronized <tt>WeakIdentityHashMap</tt> may be constructed using the
 * <tt>Collections.synchronizedMap</tt> method.
 *
 * <p> The behavior of the <tt>WeakIdentityHashMap</tt> class depends
 * in part upon the actions of the garbage collector, so several
 * familiar (though not required) <tt>Map</tt> invariants do not hold
 * for this class.  Because the garbage collector may discard keys at
 * any time, a <tt>WeakIdentityHashMap</tt> may behave as though an
 * unknown thread is silently removing entries.  In particular, even
 * if you synchronize on a <tt>WeakIdentityHashMap</tt> instance and
 * invoke none of its mutator methods, it is possible for the
 * <tt>size</tt> method to return smaller values over time, for the
 * <tt>isEmpty</tt> method to return <tt>false</tt> and then
 * <tt>true</tt>, for the <tt>containsKey</tt> method to return
 * <tt>true</tt> and later <tt>false</tt> for a given key, for the
 * <tt>get</tt> method to return a value for a given key but later
 * return <tt>null</tt>, for the <tt>put</tt> method to return
 * <tt>null</tt> and the <tt>remove</tt> method to return
 * <tt>false</tt> for a key that previously appeared to be in the map,
 * and for successive examinations of the key set, the value set, and
 * the entry set to yield successively smaller numbers of elements.
 *
 * <p> Each key object in a <tt>WeakIdentityHashMap</tt> is stored
 * indirectly as the referent of a weak reference.  Therefore a key
 * will automatically be removed only after the weak references to it,
 * both inside and outside of the map, have been cleared by the
 * garbage collector.
 *
 * <p> <strong>Implementation note:</strong> The value objects in a
 * <tt>WeakIdentityHashMap</tt> are held by ordinary strong
 * references.  Thus care should be taken to ensure that value objects
 * do not strongly refer to their own keys, either directly or
 * indirectly, since that will prevent the keys from being discarded.
 * Note that a value object may refer indirectly to its key via the
 * <tt>WeakIdentityHashMap</tt> itself; that is, a value object may
 * strongly refer to some other key object whose associated value
 * object, in turn, strongly refers to the key of the first value
 * object.  One way to deal with this is to wrap values themselves
 * within <tt>WeakReferences</tt> before inserting, as in:
 * <tt>m.put(key, new WeakReference(value))</tt>, and then unwrapping
 * upon each <tt>get</tt>.
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> or <tt>add</tt> methods, the iterator will throw a
 * <tt>ConcurrentModificationException</tt>.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:  <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @version	1.30, 02/19/04
 * @author      Doug Lea
 * @author      Josh Bloch
 * @author	Mark Reinhold
 * @since	1.2
 * @see		java.util.HashMap
 * @see		java.lang.ref.WeakReference
 */
@SuppressWarnings({"unchecked", "rawtypes", "nullness", "keyfor", "interning", "regex", "purity"}) // old, non-typesafe Sun code, not worth annotating or checking
// Annotated for lock checking as an experiment in annotating non-typesafe code.
public class WeakIdentityHashMap<K,V>
    extends AbstractMap<K,V>
    implements Map<K,V> {

    /**
     * The default initial capacity -- MUST be a power of two.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load fast used when none specified in constructor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    private /*@Nullable*/ Entry<K,V>[] table;

    /**
     * The number of key-value mappings contained in this weak hash map.
     */
    private int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int threshold;

    /**
     * The load factor for the hash table.
     */
    private final float loadFactor;

    /**
     * Reference queue for cleared WeakEntries
     */
    private final ReferenceQueue<K> queue = new ReferenceQueue<K>();

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    private volatile int modCount;

    /**
     * Constructs a new, empty <tt>WeakIdentityHashMap</tt> with the
     * given initial capacity and the given load factor.
     *
     * @param  initialCapacity The initial capacity of the
     *      <tt>WeakIdentityHashMap</tt>
     * @param  loadFactor      The load factor of the
     *      <tt>WeakIdentityHashMap</tt>
     * @throws IllegalArgumentException  If the initial capacity is negative,
     *      or if the load factor is nonpositive.
     */
    public WeakIdentityHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;
        @SuppressWarnings("unchecked")
        Entry<K,V>[] tmpTable = (Entry<K,V>[]) new Entry[capacity];
        table = tmpTable;
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
    }

    /**
     * Constructs a new, empty <tt>WeakIdentityHashMap</tt> with the
     * given initial capacity and the default load factor, which is
     * <tt>0.75</tt>.
     *
     * @param  initialCapacity The initial capacity of the
     *      <tt>WeakIdentityHashMap</tt>
     * @throws IllegalArgumentException  If the initial capacity is negative.
     */
    public WeakIdentityHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new, empty <tt>WeakIdentityHashMap</tt> with the
     * default initial capacity (16) and the default load factor
     * (0.75).
     */
    public WeakIdentityHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = DEFAULT_INITIAL_CAPACITY;
        @SuppressWarnings("unchecked")
        Entry<K,V>[] tmpTable = (Entry<K,V>[]) new Entry[DEFAULT_INITIAL_CAPACITY];
        table = tmpTable;
    }

    /**
     * Constructs a new <tt>WeakIdentityHashMap</tt> with the same
     * mappings as the specified <tt>Map</tt>.  The
     * <tt>WeakIdentityHashMap</tt> is created with default load
     * factor, which is <tt>0.75</tt> and an initial capacity
     * sufficient to hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   t the map whose mappings are to be placed in this map.
     * @throws  NullPointerException if the specified map is null.
     * @since	1.3
     */
    public WeakIdentityHashMap(Map<? extends K, ? extends V> t) {
        this(Math.max((int) (t.size() / DEFAULT_LOAD_FACTOR) + 1, 16),
             DEFAULT_LOAD_FACTOR);
        putAll(t);
    }

    // internal utilities

    /**
     * Value representing null keys inside tables.
     */
    // This is problematic because it isn't of the right type.
    // We can't lie here to the type system by claiming it is of type K,
    // because NULL_KEY is a static field but K is a per-instance type parameter.
    private static final Object NULL_KEY = new Object();

    /**
     * Use NULL_KEY for key if it is null.
     */
    // not: "private static <K> K maskNull(K key)" because NULL_KEY isn't of type K.
    @SuppressWarnings("cast.unsafe")
    /*@Pure*/ private static /*>>>@GuardSatisfied(1) @NonNull*/ Object maskNull(/*>>>@GuardSatisfied(1) @Nullable*/ Object key) {
        // OK to cast NULL_KEY.
        // This is needed so that the type of the return expression is @GuardSatisfied(1).
        // Otherwise it would be the LUB of @GuardedBy({}) and @GuardSatisfied(1), which is @GuardedByInaccessible.
        return (key == null ? /*>>>(@GuardSatisfied(1) Object)*/ NULL_KEY : key);
    }

    /**
     * Return internal representation of null key back to caller as null
     */
    // Argument is actually either of type K, or is NULL_KEY.
    @SuppressWarnings("unchecked")
    /*@Pure*/ private static <K> /*@Nullable*/ K unmaskNull(K key) {
        return (key == NULL_KEY ? null : key);
    }

    /**
     * Check for equality of non-null reference x and possibly-null y.  Uses
     * identity equality.
     */
    /*@Pure*/ static boolean eq(/*@GuardedByInaccessible*/ Object x, /*>>>@GuardedByInaccessible @Nullable*/ Object y) {
        return x == y;
    }

    /** Return the hash code for x **/
    /*@Pure*/ static int hasher (/*@GuardSatisfied*/ Object x) {
        return System.identityHashCode (x);
    }

    /**
     * Return index for hash code h.
     */
    /*@Pure*/ static int indexFor(int h, int length) {
        return h & (length-1);
    }

    /**
     * Expunge stale entries from the table.
     */
    @SuppressWarnings("purity") // actually has side effects due to weak pointers
    /*@SideEffectFree*/ private void expungeStaleEntries(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this*/) {
	Entry<K,V> e;
        // These types look wrong to me.
        while ( (e = (Entry<K,V>) queue.poll()) != null) { // unchecked cast
            int h = e.hash;
            int i = indexFor(h, table.length);

            Entry<K,V> prev = table[i];
            Entry<K,V> p = prev;
            while (p != null) {
                Entry<K,V> next = p.next;
                if (p == e) {
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                    e.next = null;  // Help GC
                    e.value = null; //  "   "
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
        }
    }

    /**
     * Return the table after first expunging stale entries
     */
    /*@Pure*/ private /*@Nullable*/ Entry<K,V>[] getTable(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this*/) {
        expungeStaleEntries();
        return table;
    }

    /**
     * Returns the number of key-value mappings in this map.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    /*@Pure*/ public int size(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this*/) {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    /*@Pure*/ public boolean isEmpty(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this*/) {
        return size() == 0;
    }

    /**
     * Returns the value to which the specified key is mapped in this weak
     * hash map, or <tt>null</tt> if the map contains no mapping for
     * this key.  A return value of <tt>null</tt> does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it is also
     * possible that the map explicitly maps the key to <tt>null</tt>. The
     * <tt>containsKey</tt> method may be used to distinguish these two
     * cases.
     *
     * @param   key the key whose associated value is to be returned.
     * @return  the value to which this map maps the specified key, or
     *          <tt>null</tt> if the map contains no mapping for this key.
     * @see #put(Object, Object)
     */
    /*@Pure*/ public /*@Nullable*/ V get(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this,*/ /*>>>@GuardSatisfied @Nullable*/ Object key) {
        Object k = maskNull(key);
        int h = hasher (k);
        /*@Nullable*/ Entry<K,V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K,V> e = tab[index];
        while (e != null) {
            if (e.hash == h && eq(k, e.get()))
                return e.value;
            e = e.next;
        }
        return null;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return  <tt>true</tt> if there is a mapping for <tt>key</tt>;
     *          <tt>false</tt> otherwise
     */
    /*@Pure*/ public boolean containsKey(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this,*/ /*>>>@GuardSatisfied @Nullable*/ Object key) {
        return getEntry(key) != null;
    }

    /**
     * Returns the entry associated with the specified key in the HashMap.
     * Returns null if the HashMap contains no mapping for this key.
     */
    /*@SideEffectFree*/ /*@Nullable*/ Entry<K,V> getEntry(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this,*/ /*>>>@GuardSatisfied @Nullable*/ Object key) {
        Object k = maskNull(key);
        int h = hasher (k);
        /*@Nullable*/ Entry<K,V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K,V> e = tab[index];
        while (e != null && !(e.hash == h && eq(k, e.get())))
            e = e.next;
        return e;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the HashMap previously associated
     *	       <tt>null</tt> with the specified key.
     */
    public /*@Nullable*/ V put(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this,*/ K key, V value) {
        @SuppressWarnings("unchecked")
        K k = (K) maskNull(key);
        int h = System.identityHashCode (k);
        /*@Nullable*/ Entry<K,V>[] tab = getTable();
        int i = indexFor(h, tab.length);

        for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
            if (h == e.hash && eq(k, e.get())) {
                V oldValue = e.value;
                if (value != oldValue)
                    e.value = value;
                return oldValue;
            }
        }

        modCount++;
	Entry<K,V> e = tab[i];
        tab[i] = new Entry<K,V>(k, value, queue, h, e);
        if (++size >= threshold)
            resize(tab.length * 2);
        return null;
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    void resize(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this,*/ int newCapacity) {
        /*@Nullable*/ Entry<K,V>[] oldTable = getTable();
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        @SuppressWarnings("unchecked")
        Entry<K,V>[] newTable = (Entry<K,V>[]) new Entry[newCapacity];
        transfer(oldTable, newTable);
        table = newTable;

        /*
         * If ignoring null elements and processing ref queue caused massive
         * shrinkage, then restore old table.  This should be rare, but avoids
         * unbounded expansion of garbage-filled tables.
         */
        if (size >= threshold / 2) {
            threshold = (int)(newCapacity * loadFactor);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    /** Transfer all entries from src to dest tables */
    private void transfer(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this,*/ /*@Nullable*/ Entry<K,V>[] src, /*@Nullable*/ Entry<K,V>[] dest) {
        for (int j = 0; j < src.length; ++j) {
            Entry<K,V> e = src[j];
            src[j] = null;          // Help GC (?)
            while (e != null) {
                Entry<K,V> next = e.next;
                Object key = e.get();
                if (key == null) {
                    e.next = null;  // Help GC
                    e.value = null; //  "   "
                    size--;
                } else {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map These
     * mappings will replace any mappings that this map had for any of the
     * keys currently in the specified map.<p>
     *
     * @param m mappings to be stored in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0)
            return;

        /*
         * Expand the map if the map if the number of mappings to be added
         * is greater than or equal to threshold.  This is conservative; the
         * obvious condition is (m.size() + size) >= threshold, but this
         * condition could result in a map with twice the appropriate capacity,
         * if the keys to be added overlap with the keys already in this map.
         * By using the conservative calculation, we subject ourself
         * to at most one extra resize.
         */
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        for (Iterator<? extends Map.Entry<? extends K, ? extends V>> i = m.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<? extends K, ? extends V> e = i.next();
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the map previously associated <tt>null</tt>
     *	       with the specified key.
     */
    public /*@Nullable*/ V remove(Object key) {
        Object k = maskNull(key);
        int h = hasher (k);
        /*@Nullable*/ Entry<K,V>[] tab = getTable();
        int i = indexFor(h, tab.length);
        Entry<K,V> prev = tab[i];
        Entry<K,V> e = prev;

        while (e != null) {
            Entry<K,V> next = e.next;
            if (h == e.hash && eq(k, e.get())) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return e.value;
            }
            prev = e;
            e = next;
        }

        return null;
    }



    /** Special version of remove needed by Entry set */
    /*@Nullable*/ Entry<K,V> removeMapping(/*@Nullable*/ Object o) {
        if (!(o instanceof Map.Entry))
            return null;
        /*@Nullable*/ Entry<K,V>[] tab = getTable();
        Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
        Object k = maskNull(entry.getKey());
        int h = hasher (k);
        int i = indexFor(h, tab.length);
        Entry<K,V> prev = tab[i];
        Entry<K,V> e = prev;

        while (e != null) {
            Entry<K,V> next = e.next;
            if (h == e.hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return e;
            }
            prev = e;
            e = next;
        }

        return null;
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        // clear out ref queue. We don't need to expunge entries
        // since table is getting cleared.
        while (queue.poll() != null)
            ;

        modCount++;
        /*@Nullable*/ Entry<K,V>[] tab = table;
        for (int i = 0; i < tab.length; ++i)
            tab[i] = null;                   // Help GC (?)
        size = 0;

        // Allocation of array may have caused GC, which may have caused
        // additional entries to go stale.  Removing these entries from the
        // reference queue will make them eligible for reclamation.
        while (queue.poll() != null)
            ;
   }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    /*@Pure*/ public boolean containsValue(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this,*/ /*>>>@GuardSatisfied @Nullable*/ Object value) {
	if (value==null)
            return containsNullValue();

	/*@Nullable*/ Entry<K,V>[] tab = getTable();
        for (int i = tab.length ; i-- > 0 ;)
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (value.equals(e.value))
                    return true;
	return false;
    }

    /**
     * Special-case code for containsValue with null argument
     */
    private boolean containsNullValue(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this*/) {
	/*@Nullable*/ Entry<K,V>[] tab = getTable();
        for (int i = tab.length ; i-- > 0 ;)
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (e.value==null)
                    return true;
	return false;
    }

    /**
     * The entries in this hash table extend WeakReference, using its main ref
     * field as the key.
     */
    private static class Entry<K,V> extends WeakReference<K> implements Map.Entry<K,V> {
        private V value;
        private final int hash;
        private /*@Nullable*/ Entry<K,V> next;

        /**
         * Create new entry.
         */
        Entry(K key, V value,
	      ReferenceQueue<K> queue,
              int hash, Entry<K,V> next) {
            super(key, queue);
            this.value = value;
            this.hash  = hash;
            this.next  = next;
        }

        /*@Pure*/ public K getKey(/*>>>@GuardSatisfied Entry<K,V> this*/) {
            return WeakIdentityHashMap.<K>unmaskNull(get());
        }

        /*@Pure*/ public V getValue(/*>>>@GuardSatisfied Entry<K,V> this*/) {
            return value;
        }

        public V setValue(V newValue) {
	    V oldValue = value;
            value = newValue;
            return oldValue;
        }

        @SuppressWarnings("purity") // side effects on local state
        /*@Pure*/ public boolean equals(/*>>>@GuardSatisfied Entry<K,V> this,*/ /*>>>@GuardSatisfied @Nullable*/ Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<K,V> e = (Map.Entry<K,V>)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (eq (k1, k2)) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        @SuppressWarnings("purity") // side effects on local state
        /*@Pure*/ public int hashCode(/*>>>@GuardSatisfied Entry<K,V> this*/) {
            Object k = getKey();
            Object v = getValue();
            return  ((k==null ? 0 : hasher (k)) ^
                     (v==null ? 0 : v.hashCode()));
        }

        /*@SideEffectFree*/ public String toString(/*>>>@GuardSatisfied Entry<K,V> this*/) {
            return getKey() + "=" + getValue();
        }
    }

    private abstract class HashIterator<T> implements Iterator<T> {
        int index;
        /*@Nullable*/ Entry<K,V> entry = null;
        /*@Nullable*/ Entry<K,V> lastReturned = null;
        int expectedModCount = modCount;

        /**
         * Strong reference needed to avoid disappearance of key
         * between hasNext and next
         */
        /*@Nullable*/ Object nextKey = null;

        /**
         * Strong reference needed to avoid disappearance of key
         * between nextEntry() and any use of the entry
         */
	/*@Nullable*/ Object currentKey = null;

        HashIterator() {
            index = (size() != 0 ? table.length : 0);
        }

        public boolean hasNext() {
            /*@Nullable*/ Entry<K,V>[] t = table;

            while (nextKey == null) {
                Entry<K,V> e = entry;
                int i = index;
                while (e == null && i > 0)
                    e = t[--i];
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    return false;
                }
                nextKey = e.get(); // hold on to key in strong ref
                if (nextKey == null)
                    entry = entry.next;
            }
            return true;
        }

        /** The common parts of next() across different types of iterators */
        protected Entry<K,V> nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextKey == null && !hasNext())
                throw new NoSuchElementException();

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            WeakIdentityHashMap.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

    }

    private class ValueIterator extends HashIterator<V> {
        public V next() {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends HashIterator<K> {
        public K next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends HashIterator<Map.Entry<K,V>> {
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    // Views

    private transient /*@Nullable*/ Set<Map.Entry<K,V>> entrySet = null;
    private transient volatile /*@Nullable*/ Set<K>   our_keySet = null;

    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    /*@SideEffectFree*/ public Set<K> keySet(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this*/) {
        Set<K> ks = our_keySet;
        return (ks != null ? ks : (our_keySet = new KeySet()));
    }

    private class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        /*@Pure*/ public int size(/*>>>@GuardSatisfied KeySet this*/) {
            return WeakIdentityHashMap.this.size();
        }

        /*@Pure*/ public boolean contains(/*>>>@GuardSatisfied KeySet this,*/ /*>>>@GuardSatisfied @Nullable*/ Object o) {
            return containsKey(o);
        }

        public boolean remove(/*@Nullable*/ Object o) {
            if (containsKey(o)) {
                WeakIdentityHashMap.this.remove(o);
                return true;
            }
            else
                return false;
        }

        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        public Object[] toArray() {
            Collection<K> c = new ArrayList<K>(size());
            for (Iterator<K> i = iterator(); i.hasNext(); )
                c.add(i.next());
            return c.toArray();
        }

        public <T> T[] toArray(T[] a) {
            Collection<K> c = new ArrayList<K>(size());
            for (Iterator<K> i = iterator(); i.hasNext(); )
                c.add(i.next());
            return c.toArray(a);
        }
    }

    transient volatile /*@Nullable*/ Collection<V> our_values = null;

    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    /*@SideEffectFree*/ public Collection<V> values(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this*/) {
        Collection<V> vs = our_values;
        return (vs != null ?  vs : (our_values = new Values()));
    }

    private class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        /*@Pure*/ public int size(/*>>>@GuardSatisfied Values this*/) {
            return WeakIdentityHashMap.this.size();
        }

        /*@Pure*/ public boolean contains(/*>>>@GuardSatisfied Values this,*/ /*>>>@GuardSatisfied @Nullable*/ Object o) {
            return containsValue(o);
        }

        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        public Object[] toArray() {
            Collection<V> c = new ArrayList<V>(size());
            for (Iterator<V> i = iterator(); i.hasNext(); )
                c.add(i.next());
            return c.toArray();
        }

        public <T> T[] toArray(T[] a) {
            Collection<V> c = new ArrayList<V>(size());
            for (Iterator<V> i = iterator(); i.hasNext(); )
                c.add(i.next());
            return c.toArray(a);
        }
    }

    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     * @see java.util.Map.Entry
     */
    /*@SideEffectFree*/ public Set<Map.Entry<K,V>> entrySet(/*>>>@GuardSatisfied WeakIdentityHashMap<K,V> this*/) {
        Set<Map.Entry<K,V>> es = entrySet;
        return (es != null ? es : (entrySet = new EntrySet()));
    }

    private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }

        /*@Pure*/ public boolean contains(/*>>>@GuardSatisfied EntrySet this,*/ /*>>>@GuardSatisfied @Nullable*/ Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<K,V> e = (Map.Entry<K,V>)o;
            Object k = e.getKey();
            Entry candidate = getEntry(k);
            return candidate != null && candidate.equals(e);
        }

        public boolean remove(/*@Nullable*/ Object o) {
            return removeMapping(o) != null;
        }

        /*@Pure*/ public int size(/*>>>@GuardSatisfied EntrySet this*/) {
            return WeakIdentityHashMap.this.size();
        }

        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        public Object[] toArray() {
            Collection<Map.Entry<K,V>> c = new ArrayList<Map.Entry<K,V>>(size());
            for (Iterator<Map.Entry<K,V>> i = iterator(); i.hasNext(); )
                c.add(new OurSimpleEntry<K,V>(i.next()));
            return c.toArray();
        }

        public <T> T[] toArray(T[] a) {
            Collection<Map.Entry<K,V>> c = new ArrayList<Map.Entry<K,V>>(size());
            for (Iterator<Map.Entry<K,V>> i = iterator(); i.hasNext(); )
                c.add(new OurSimpleEntry<K,V>(i.next()));
            return c.toArray(a);
        }
    }

    /** Version copied from Abstract Map because it is not public **/
    static class OurSimpleEntry<K,V> implements Map.Entry<K,V> {
        K key;
        V value;

        public OurSimpleEntry(K key, V value) {
            this.key   = key;
                this.value = value;
        }

        public OurSimpleEntry(Map.Entry<K,V> e) {
            this.key   = e.getKey();
                this.value = e.getValue();
        }

        /*@Pure*/ public K getKey(/*>>>@GuardSatisfied OurSimpleEntry<K,V> this*/) {
            return key;
        }

        /*@Pure*/ public V getValue(/*>>>@GuardSatisfied OurSimpleEntry<K,V> this*/) {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

  /*@Pure*/ public boolean equals(/*>>>@GuardSatisfied OurSimpleEntry<K,V> this,*/ /*>>>@GuardSatisfied @Nullable*/ Object o) {
            if (!(o instanceof Map.Entry))
            return false;
            Map.Entry<K,V> e = (Map.Entry<K,V>)o;
            return WeakIdentityHashMap.eq(key, e.getKey())
                && eq(value, e.getValue());
        }

        /*@Pure*/ public int hashCode(/*>>>@GuardSatisfied OurSimpleEntry<K,V> this*/) {
            return ((key   == null)   ? 0 :   key.hashCode()) ^
               ((value == null)   ? 0 : value.hashCode());
        }

        /*@SideEffectFree*/ public String toString(/*>>>@GuardSatisfied OurSimpleEntry<K,V> this*/) {
            return key + "=" + value;
        }

        private static boolean eq(/*>>>@GuardSatisfied @Nullable*/ Object o1, /*>>>@GuardSatisfied @Nullable*/ Object o2) {
            return (o1 == null ? o2 == null : o1.equals(o2));
        }
    }

}
