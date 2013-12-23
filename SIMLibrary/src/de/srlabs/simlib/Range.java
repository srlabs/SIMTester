package de.srlabs.simlib;

import java.util.Iterator;

public class Range {

    public static Iterable<Integer> range(final int start, final int stop, final boolean asciiOnly) {

        return new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    private int counter = start;

                    @Override
                    public boolean hasNext() {
                        return counter <= stop;
                    }

                    @Override
                    public Integer next() {
                        try {
                            return counter;
                        } finally {
                            counter += 1;
                        }
                    }

                    @Override
                    public void remove() {
                        throw new IllegalStateException("remove() not implemented");
                    }
                };
            }
        };
    }

    public static Iterable<Integer> range(final int start, final int stop) {
        return range(start, stop, false);
    }

    public static Iterable<Integer> range(final int stop) {
        return range(0, stop, false);
    }
}
