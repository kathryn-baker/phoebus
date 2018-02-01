/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper class to parse user defined time strings to absolute or relative
 * time durations.
 *
 * @author shroffk
 */
@SuppressWarnings("nls")
public class TimeParser {

    static final Pattern durationTimeQunatityUnitsPattern = Pattern
            .compile("\\s*(\\d*)\\s*(ms|milli|sec|secs|min|mins|hour|hours|day|days)\\s*", Pattern.CASE_INSENSITIVE);

    // Patterns need to be listed longest-first.
    // Otherwise "days" would match just the "d"
    static final Pattern timeQuantityUnitsPattern = Pattern.compile(
            "\\s*(\\d*)\\s*(millis|ms|seconds|second|secs|sec|s|minutes|minute|mins|min|hours|hour|h|days|day|d|weeks|week|w|months|month|years|year|y)\\s*",
            Pattern.CASE_INSENSITIVE);

    /**
     * A Helper function to help you convert various string represented time
     * definitions to an absolute Instant.
     *
     * @param time a string that represents an instant in time
     * @return the parsed Instant or null
     */
    public static Instant getInstant(String time) {
        if (time.equalsIgnoreCase("now")) {
            return Instant.now();
        } else {
            Matcher nUnitsAgoMatcher = timeQuantityUnitsPattern.matcher(time);
            while (nUnitsAgoMatcher.find()) {
                return Instant.now().minus(parseDuration(nUnitsAgoMatcher.group(1)));
            }
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            return LocalDateTime.parse(time, formatter).atZone(TimeZone.getDefault().toZoneId()).toInstant();
        }
    }

    /**
     * Return a {@link TimeInterval} between this instant represented by the string and "now"
     * @param time
     * @return TimeInterval
     */
    public static TimeInterval getTimeInterval(String time) {
        Instant now = Instant.now();
        if (time.equalsIgnoreCase("now")) {
            return TimeInterval.between(now, now);
        } else {
            Matcher nUnitsAgoMatcher = timeQuantityUnitsPattern.matcher(time);
            while (nUnitsAgoMatcher.find()) {
                return TimeInterval.between(now.minus(parseTemporalAmount(nUnitsAgoMatcher.group(1))), now);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            return TimeInterval.between(LocalDateTime.parse(time, formatter).atZone(TimeZone.getDefault().toZoneId()).toInstant(), now);
        }
    }


    private final static List<ChronoUnit> durationUnits = Arrays.asList(MILLIS, SECONDS, MINUTES, HOURS);

    /**
     * parses the given string into a {@link Duration}. The method only supports
     * {@link ChronoUnit#MILLIS}, {@link ChronoUnit#SECONDS},
     * {@link ChronoUnit#MINUTES}, and {@link ChronoUnit#HOURS}. Days {@link ChronoUnit#DAYS} are treated as 24 HOURS.
     *
     * e.g. parseDuraiton("5h 3min 34s");
     *
     * @param string
     * @return
     * @deprecated use {@link #parseTemporalAmount(String)}
     */
    @Deprecated
    public static Duration parseDuration(String string) {
        int quantity = 0;
        String unit = "";
        Matcher timeQunatityUnitsMatcher = durationTimeQunatityUnitsPattern.matcher(string);
        Map<ChronoUnit, Integer> timeQuantities = new HashMap<ChronoUnit, Integer>();
        while (timeQunatityUnitsMatcher.find()) {
            quantity = "".equals(timeQunatityUnitsMatcher.group(1)) ? 1
                    : Integer.valueOf(timeQunatityUnitsMatcher.group(1));
            unit = timeQunatityUnitsMatcher.group(2).toLowerCase();
            switch (unit) {
            case "ms":
            case "milli":
                timeQuantities.put(MILLIS, quantity);
                break;
            case "s":
            case "sec":
            case "secs":
                timeQuantities.put(SECONDS, quantity);
                break;
            case "m":
            case "min":
            case "mins":
                timeQuantities.put(MINUTES, quantity);
                break;
            case "h":
            case "hour":
            case "hours":
                timeQuantities.put(HOURS, quantity);
                break;
            case "d":
            case "day":
            case "days":
                timeQuantities.put(DAYS, quantity);
                break;
            default:
                break;
            }
        }
        Duration duration = Duration.ofSeconds(0);
        for (Entry<ChronoUnit, Integer> entry : timeQuantities.entrySet()) {
            duration = duration.plus(entry.getValue(), entry.getKey());
        }
        return duration;
    }

    /** Parse a temporal amount like "1 day 20 seconds"
     *
     *  <p>Uses a {@link Duration} if the time span includes
     *  hours, minutes or seconds.
     *  Otherwise a {@link Period} is used because
     *  "1 month" can then be used for calendar-based
     *  computations.
     *
     *  @param string Text
     *  @return {@link Duration} or {@link Period}
     */
    public static TemporalAmount parseTemporalAmount(final String string) {
        int quantity = 0;
        String unit = "";
        Matcher timeQuantityUnitsMatcher = timeQuantityUnitsPattern.matcher(string);
        Map<ChronoUnit, Integer> timeQuantities = new HashMap<ChronoUnit, Integer>();
        while (timeQuantityUnitsMatcher.find()) {
            quantity = "".equals(timeQuantityUnitsMatcher.group(1)) ? 1
                    : Integer.valueOf(timeQuantityUnitsMatcher.group(1));
            unit = timeQuantityUnitsMatcher.group(2).toLowerCase();
            if (unit.startsWith("y"))
                timeQuantities.put(YEARS, quantity);
            else if (unit.startsWith("mi"))
                timeQuantities.put(MINUTES, quantity);
            else if (unit.startsWith("h"))
                timeQuantities.put(HOURS, quantity);
            else if (unit.startsWith("d"))
                timeQuantities.put(DAYS, quantity);
            else if (unit.startsWith("w"))
                timeQuantities.put(WEEKS, quantity);
            else if (unit.startsWith("s"))
                timeQuantities.put(SECONDS, quantity);
            else if (unit.startsWith("mo"))
                timeQuantities.put(MONTHS, quantity);
            else if (unit.startsWith("mi")  ||
                     unit.equals("ms"))
                timeQuantities.put(MILLIS, quantity);
        }
        if (Collections.disjoint(timeQuantities.keySet(), durationUnits)) {
            Period result = Period.ZERO;
            result = result.plusYears(timeQuantities.containsKey(YEARS) ? timeQuantities.get(YEARS) : 0);
            result = result.plusMonths(timeQuantities.containsKey(MONTHS) ? timeQuantities.get(MONTHS) : 0);
            result = result.plusDays(timeQuantities.containsKey(DAYS) ? timeQuantities.get(DAYS) : 0);
            return result;
        } else {
            Duration result = Duration.ofSeconds(0);
            for (Entry<ChronoUnit, Integer> entry : timeQuantities.entrySet()) {
                result = result.plus(entry.getValue(), entry.getKey());
            }
            return result;
        }
    }

    /** Format a temporal amount
     *
     *  <p>Creates a text like "2 days" that
     *  {@link #parseTemporalAmount(String)}
     *  can parse
     *  @param amount
     *  @return Text
     */
    public static String format(final TemporalAmount amount)
    {
        final StringBuilder buf = new StringBuilder();
        if (amount instanceof Period)
        {
            final Period period = (Period) amount;
            if (period.isZero())
                return "now";
            if (period.getYears() == 1)
                buf.append("1 year ");
            else if (period.getYears() > 1)
                buf.append(period.getYears()).append(" years ");


            if (period.getMonths() == 1)
                buf.append("1 month ");
            else if (period.getMonths() > 0)
                buf.append(period.getMonths()).append(" months ");

            if (period.getDays() == 1)
                buf.append("1 day");
            else if (period.getDays() > 0)
                buf.append(period.getDays()).append(" days");
        }
        else
        {
            long secs = ((Duration) amount).getSeconds();
            if (secs == 0)
                return "now";

            int p = (int) (secs / (365*24*60*60));
            if (p > 0)
            {
                if (p == 1)
                    buf.append("1 year ");
                else
                    buf.append(p).append(" years ");
                secs -= p * (365*24*60*60);
            }

            p = (int) (secs / (12*24*60*60));
            if (p > 0)
            {
                if (p == 1)
                    buf.append("1 month ");
                else
                    buf.append(p).append(" months ");
                secs -= p * (12*24*60*60);
            }

            p = (int) (secs / (24*60*60));
            if (p > 0)
            {
                if (p == 1)
                    buf.append("1 day ");
                else
                    buf.append(p).append(" days ");
                secs -= p * (24*60*60);
            }

            p = (int) (secs / (60*60));
            if (p > 0)
            {
                if (p == 1)
                    buf.append("1 hour ");
                else
                    buf.append(p).append(" hours ");
                secs -= p * (60*60);
            }

            p = (int) (secs / (60));
            if (p > 0)
            {
                if (p == 1)
                    buf.append("1 minute ");
                else
                    buf.append(p).append(" minutes ");
                secs -= p * (60);
            }

            if (secs > 0)
                if (secs == 1)
                    buf.append("1 second ");
                else
                    buf.append(secs).append(" seconds ");

            final int ms = ((Duration)amount).getNano() / 1000000;
            if (ms > 0)
                buf.append(ms).append(" ms");
        }
        return buf.toString().trim();
    }
}
