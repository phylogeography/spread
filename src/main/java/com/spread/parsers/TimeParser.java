package com.spread.parsers;

import com.spread.data.TimeLine;
import com.spread.exceptions.SpreadException;
import com.spread.utils.ParsersUtils;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimeParser {

    public static final String separator = "/";
    private static final String dateRegex = "\\d{4}" + separator + "\\d{2}" + separator + "\\d{2}";

    private String mrsd;
    private LocalDate endDate;
    private DateTimeFormatter dateFormatter;

    public TimeParser(String mrsd) throws SpreadException {
        this.mrsd = mrsd;
        this.dateFormatter = DateTimeFormat.forPattern("yyyy" + separator + "MM" + separator + "dd");

        parseTime();
    }

    private void parseTime() throws SpreadException {

        Integer year = 0;
        Integer month = 0;
        Integer day = 0;

        String[] endDateFields;
        if (mrsd.contains(".")) {

            // negative dates
            int decimalDateSign = 1;
            if (mrsd.contains(ParsersUtils.NEGATIVE_SIGN)) {
                decimalDateSign = -1;
                mrsd = mrsd.split(separator)[1];
            }

            endDateFields = convertToYearMonthDay(Double.valueOf(mrsd));

            year = decimalDateSign * Integer.valueOf(endDateFields[ParsersUtils.YEAR_INDEX]);
            month = Integer.valueOf(endDateFields[ParsersUtils.MONTH_INDEX]);
            day = Integer.valueOf(endDateFields[ParsersUtils.DAY_INDEX]);

        } else if (mrsd.contains(separator)) {

            endDateFields = mrsd.split(separator);
            if (endDateFields.length == 3) {

                year = Integer.valueOf(endDateFields[ParsersUtils.YEAR_INDEX]);
                month = Integer.valueOf(endDateFields[ParsersUtils.MONTH_INDEX]);
                day = Integer.valueOf(endDateFields[ParsersUtils.DAY_INDEX]);

            } else if (endDateFields.length == 2) {

                year = Integer.valueOf(endDateFields[ParsersUtils.YEAR_INDEX]);
                month = Integer.valueOf(endDateFields[ParsersUtils.MONTH_INDEX]);

            } else if (endDateFields.length == 1) {

                year = Integer.valueOf(endDateFields[ParsersUtils.YEAR_INDEX]);

            } else {
                throw new SpreadException("Unrecognised date format " + this.mrsd);
            }

        } else {
            throw new SpreadException("Unrecognised MRSD format " + this.mrsd);
        }

        // joda monthOfYear must be [1,12]
        if (month == 0) {
            month = 1;
        }

        // joda dayOfMonth must be [1,31]
        if (day == 0) {
            day = 1;
        }

        this.endDate = new LocalDate(year, month, day);
    }

    public TimeLine getTimeLine(double rootNodeHeight) {

        String startDate = this.getNodeDate(rootNodeHeight);
        String endDate = dateFormatter.print(this.endDate);

        TimeLine timeLine = new TimeLine(startDate, endDate);

        return timeLine;
    }

    public String getNodeDate(double nodeHeight) {

        String[] fields = convertToYearMonthDay(nodeHeight);
        Integer years = Integer.valueOf(fields[ParsersUtils.YEAR_INDEX]);
        Integer months = Integer.valueOf(fields[ParsersUtils.MONTH_INDEX]);
        Integer days = Integer.valueOf(fields[ParsersUtils.DAY_INDEX]);
        LocalDate date = endDate.minusYears(years).minusMonths(months).minusDays(days);
        String stringDate = dateFormatter.print(date);

        return stringDate;
    }

    public static Boolean isParseableDate(String dateString) {
        return dateString.matches(dateRegex);
    }

    public String[] convertToYearMonthDay(double fractionalDate) {

        String[] yearMonthDay = new String[3];

        int year = (int) fractionalDate;
        String yearString;

        if (year < 10) {
            yearString = "000" + year;
        } else if (year < 100) {
            yearString = "00" + year;
        } else if (year < 1000) {
            yearString = "0" + year;
        } else {
            yearString = "" + year;
        }

        yearMonthDay[0] = yearString;

        double fractionalMonth = fractionalDate - year;

        int month = (int) (12.0 * fractionalMonth);
        String monthString;

        if (month < 10) {
            monthString = "0" + month;
        } else {
            monthString = "" + month;
        }

        yearMonthDay[1] = monthString;

        int day = (int) Math.round(30 * (12 * fractionalMonth - month));
        String dayString;

        if (day < 10) {
            dayString = "0" + day;
        } else {
            dayString = "" + day;
        }

        yearMonthDay[2] = dayString;

        return yearMonthDay;
    }

}
