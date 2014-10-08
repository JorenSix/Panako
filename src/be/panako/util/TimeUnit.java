/***************************************************************************
*                                                                          *                     
* Panako - acoustic fingerprinting                                         *   
* Copyright (C) 2014 - Joren Six / IPEM                                    *   
*                                                                          *
* This program is free software: you can redistribute it and/or modify     *
* it under the terms of the GNU Affero General Public License as           *
* published by the Free Software Foundation, either version 3 of the       *
* License, or (at your option) any later version.                          *
*                                                                          *
* This program is distributed in the hope that it will be useful,          *
* but WITHOUT ANY WARRANTY; without even the implied warranty of           *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
* GNU Affero General Public License for more details.                      *
*                                                                          *
* You should have received a copy of the GNU Affero General Public License *
* along with this program.  If not, see <http://www.gnu.org/licenses/>     *
*                                                                          *
****************************************************************************
*    ______   ________   ___   __    ________   ___   ___   ______         *
*   /_____/\ /_______/\ /__/\ /__/\ /_______/\ /___/\/__/\ /_____/\        *      
*   \:::_ \ \\::: _  \ \\::\_\\  \ \\::: _  \ \\::.\ \\ \ \\:::_ \ \       *   
*    \:(_) \ \\::(_)  \ \\:. `-\  \ \\::(_)  \ \\:: \/_) \ \\:\ \ \ \      * 
*     \: ___\/ \:: __  \ \\:. _    \ \\:: __  \ \\:. __  ( ( \:\ \ \ \     * 
*      \ \ \    \:.\ \  \ \\. \`-\  \ \\:.\ \  \ \\: \ )  \ \ \:\_\ \ \    * 
*       \_\/     \__\/\__\/ \__\/ \__\/ \__\/\__\/ \__\/\__\/  \_____\/    *
*                                                                          *
****************************************************************************
*                                                                          *
*                              Panako                                      * 
*                       Acoustic Fingerprinting                            *
*                                                                          *
****************************************************************************/



package be.panako.util;

/**
 * A <tt>TimeUnit</tt> represents time durations at a given unit of
 * granularity and provides utility methods to convert across units,
 * and to perform timing and delay operations in these units.  A
 * <tt>TimeUnit</tt> does not maintain time information, but only
 * helps organize and use time representations that may be maintained
 * separately across various contexts.  A nanosecond is defined as one
 * thousandth of a microsecond, a microsecond as one thousandth of a
 * millisecond, a millisecond as one thousandth of a second, a minute
 * as sixty seconds, an hour as sixty minutes, and a day as twenty four
 * hours.
 *
 * <p>A <tt>TimeUnit</tt> is mainly used to inform time-based methods
 * how a given timing parameter should be interpreted. For example,
 * the following code will timeout in 50 milliseconds if the 
 * java.util.concurrent.locks.Lock  is not available:
 *
 * <pre>  Lock lock = ...;
 *  if ( lock.tryLock(50L, TimeUnit.MILLISECONDS) ) ...
 * </pre>
 * while this code will timeout in 50 seconds:
 * <pre>
 *  Lock lock = ...;
 *  if ( lock.tryLock(50L, TimeUnit.SECONDS) ) ...
 * </pre>
 *
 * Note however, that there is no guarantee that a particular timeout
 * implementation will be able to notice the passage of time at the
 * same granularity as the given <tt>TimeUnit</tt>.
 *
 * @since 1.5
 * @author Doug Lea
 */
public enum TimeUnit {
    NANOSECONDS {
        public double toNanos(double d)   { return d; }
        public double toMicros(double d)  { return d/(C1/C0); }
        public double toMillis(double d)  { return d/(C2/C0); }
        public double toSeconds(double d) { return d/(C3/C0); }
        public double toMinutes(double d) { return d/(C4/C0); }
        public double toHours(double d)   { return d/(C5/C0); }
        public double toDays(double d)    { return d/(C6/C0); }
        public double convert(double d, TimeUnit u) { return u.toNanos(d); }
    },
    MICROSECONDS {
        public double toNanos(double d)   { return d * C1/C0; }
        public double toMicros(double d)  { return d; }
        public double toMillis(double d)  { return d/(C2/C1); }
        public double toSeconds(double d) { return d/(C3/C1); }
        public double toMinutes(double d) { return d/(C4/C1); }
        public double toHours(double d)   { return d/(C5/C1); }
        public double toDays(double d)    { return d/(C6/C1); }
        public double convert(double d, TimeUnit u) { return u.toMicros(d); }
    },
    MILLISECONDS {
        public double toNanos(double d)   { return d * C2/C0; }
        public double toMicros(double d)  { return d * C2/C1; }
        public double toMillis(double d)  { return d; }
        public double toSeconds(double d) { return d/(C3/C2); }
        public double toMinutes(double d) { return d/(C4/C2); }
        public double toHours(double d)   { return d/(C5/C2); }
        public double toDays(double d)    { return d/(C6/C2); }
        public double convert(double d, TimeUnit u) { return u.toMillis(d); }
    },
    SECONDS {
        public double toNanos(double d)   { return d *  C3/C0; }
        public double toMicros(double d)  { return d *  C3/C1; }
        public double toMillis(double d)  { return d *  C3/C2; }
        public double toSeconds(double d) { return d; }
        public double toMinutes(double d) { return d/(C4/C3); }
        public double toHours(double d)   { return d/(C5/C3); }
        public double toDays(double d)    { return d/(C6/C3); }
        public double convert(double d, TimeUnit u) { return u.toSeconds(d); }
    },
    MINUTES {
        public double toNanos(double d)   { return d * C4/C0; }
        public double toMicros(double d)  { return d * C4/C1; }
        public double toMillis(double d)  { return d * C4/C2; }
        public double toSeconds(double d) { return d * C4/C3; }
        public double toMinutes(double d) { return d; }
        public double toHours(double d)   { return d/(C5/C4); }
        public double toDays(double d)    { return d/(C6/C4); }
        public double convert(double d, TimeUnit u) { return u.toMinutes(d); }
    },
    HOURS {
        public double toNanos(double d)   { return d * C5/C0; }
        public double toMicros(double d)  { return d * C5/C1; }
        public double toMillis(double d)  { return d * C5/C2; }
        public double toSeconds(double d) { return d * C5/C3; }
        public double toMinutes(double d) { return d * C5/C4; }
        public double toHours(double d)   { return d; }
        public double toDays(double d)    { return d/(C6/C5); }
        public double convert(double d, TimeUnit u) { return u.toHours(d); }
    },
    DAYS {
        public double toNanos(double d)   { return d * C6/C0; }
        public double toMicros(double d)  { return d * C6/C1; }
        public double toMillis(double d)  { return d * C6/C2; }
        public double toSeconds(double d) { return d * C6/C3; }
        public double toMinutes(double d) { return d * C6/C4; }
        public double toHours(double d)   { return d * C6/C5; }
        public double toDays(double d)    { return d; }
        public double convert(double d, TimeUnit u) { return u.toDays(d); }
    };

    // Handy constants for conversion methods
    static final double C0 = 1.0;
    static final double C1 = C0 * 1000.0;
    static final double C2 = C1 * 1000.0;
    static final double C3 = C2 * 1000.0;
    static final double C4 = C3 * 60.0;
    static final double C5 = C4 * 60.0;
    static final double C6 = C5 * 24.0;

    // To maintain full signature compatibility with 1.5, and to improve the
    // clarity of the generated javadoc (see 6287639: Abstract methods in
    // enum classes should not be listed as abstract), method convert
    // etc. are not declared abstract but otherwise act as abstract methods.

    /**
     * Convert the given time duration in the given unit to this
     * unit.  Conversions from finer to coarser granularities
     * truncate, so lose precision. For example converting
     * <tt>999</tt> milliseconds to seconds results in
     * <tt>0</tt>. Conversions from coarser to finer granularities
     * with arguments that would numerically overflow saturate to
     * <tt>double.MIN_VALUE</tt> if negative or <tt>double.MAX_VALUE</tt>
     * if positive.
     *
     * <p>For example, to convert 10 minutes to milliseconds, use:
     * <tt>TimeUnit.MILLISECONDS.convert(10L, TimeUnit.MINUTES)</tt>
     *
     * @param sourceDuration the time duration in the given <tt>sourceUnit</tt>
     * @param sourceUnit the unit of the <tt>sourceDuration</tt> argument
     * @return the converted duration in this unit
     */
    public double convert(double sourceDuration, TimeUnit sourceUnit) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>NANOSECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>double.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>double.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    public double toNanos(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>MICROSECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>double.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>double.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    public double toMicros(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>MILLISECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>double.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>double.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    public double toMillis(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>SECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>double.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>double.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    public double toSeconds(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>MINUTES.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>double.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>double.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     * @since 1.6
     */
    public double toMinutes(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>HOURS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>double.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>double.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     * @since 1.6
     */
    public double toHours(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>DAYS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     * @since 1.6
     */
    public double toDays(double duration) {
        throw new AbstractMethodError();
    } 

}

