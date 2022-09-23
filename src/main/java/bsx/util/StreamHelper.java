package bsx.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StreamHelper {
    
    public static int compare(IntStream first, IntStream second) {
        return compare(first.iterator(), second.iterator());
    }
    
    public static int compare(PrimitiveIterator.OfInt first, PrimitiveIterator.OfInt second) {
        while (true) {
            boolean next1 = first.hasNext();
            boolean next2 = second.hasNext();
            if (!next1 && !next2) {
                return 0;
            } else if (!next1) {
                return 1;
            } else if (!next2) {
                return -1;
            } else {
                int result = Integer.compare(first.nextInt(), second.nextInt());
                if (result != 0) return result;
            }
        }
    }
    
    public static <T> int compare(Stream<T> first, Stream<T> second, Comparator<T> cmp) {
        return compare(first.iterator(), second.iterator(), cmp);
    }
    
    public static <T> int compare(Iterator<T> first, Iterator<T> second, Comparator<T> cmp) {
        while (true) {
            boolean next1 = first.hasNext();
            boolean next2 = second.hasNext();
            if (!next1 && !next2) {
                return 0;
            } else if (!next1) {
                return 1;
            } else if (!next2) {
                return -1;
            } else {
                T elem1 = first.next();
                T elem2 = second.next();
                int result = cmp.compare(elem1, elem2);
                if (result != 0) return result;
            }
        }
    }
}
