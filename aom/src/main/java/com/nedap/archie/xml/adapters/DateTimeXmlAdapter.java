package com.nedap.archie.xml.adapters;

import com.nedap.archie.datetime.DateTimeFormatters;
import com.nedap.archie.datetime.DateTimeParsers;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

/**
 * Created by pieter.bos on 24/06/16.
 */
public class DateTimeXmlAdapter extends XmlAdapter<String, TemporalAccessor> {

    @Override
    public TemporalAccessor unmarshal(String stringValue) {
        return stringValue != null? DateTimeParsers.parseDateTimeValue(stringValue):null;
    }

    @Override
    public String marshal(TemporalAccessor value) {
        if(value instanceof LocalDateTime || value instanceof ZonedDateTime || value instanceof OffsetDateTime) {
            return value.toString();
        }
        if(value.isSupported(ChronoField.MICRO_OF_SECOND) && value.get(ChronoField.MICRO_OF_SECOND) != 0l) {
            return DateTimeFormatters.ISO_8601_DATE_TIME.format(value);
        } else {
            return DateTimeFormatters.ISO_8601_DATE_TIME_WITHOUT_MICROS.format(value);
        }
    }



}
