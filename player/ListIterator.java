/* RunIterator.java */

/**
 *  The RunIterator class iterates over a RunLengthEncoding and allows other
 *  classes to inspect the runs in a run-length encoding, one run at a time.
 *  A newly constructed RunIterator "points" to the first run in the encoding
 *  used to construct it.  Each time next() is invoked, it returns a run
 *  (represented as an array of four ints); a sequence of calls to next()
 *  returns run in consecutive order until every run has been returned.
 *
 *  Client classes should never call the RunIterator constructor directly;
 *  instead they should invoke the iterator() method on a RunLengthEncoding
 *  object, which will construct a properly initialized RunIterator for the
 *  client.
 *
 *  Calls to hasNext() determine whether another run is available, or whether
 *  the iterator has reached the end of the run-length encoding.  When
 *  a RunIterator reaches the end of an encoding, it is no longer useful, and
 *  the next() method may throw an exception; thus it is recommended to check
 *  hasNext() before each call to next().  To iterate through the encoding
 *  again, construct a new RunIterator by invoking iterator() on the
 *  RunLengthEncoding and throw the old RunIterator away.
 *
 *  A RunIterator is not guaranteed to work if the underlying RunLengthEncoding
 *  is modified after the RunIterator is constructed.  (Especially if it is
 *  modified by setPixel().)
 */

//package player.list;
package player;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ListIterator<T> implements Iterator<T> {

    private List<T> list;
    private ListNode<T> next;


    /**
     *  RunIterator() constructs a new iterator starting with a specified run.
     *
     *  @param node the run where this iterator starts.
     */
    public ListIterator(List<T> lst){
        next = lst.front();
        list = lst;
    }

    /**
     *  hasNext() returns true if this iterator has more runs.  If it returns
     *  false, then the next call to next() may throw an exception.
     *
     *  @return true if the iterator has more elements.
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     *  next() return the next item in the list.
     *
     *  @return the next item from the list
     *  @throws NoSuchElementException if the iteration has no more elements.
     */
    public T next(){
        if (next == null){
            throw new NoSuchElementException();
        }
        T val = next.item;
        next = list.next(next);
        return val;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
