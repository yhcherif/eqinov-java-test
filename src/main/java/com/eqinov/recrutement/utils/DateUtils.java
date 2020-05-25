package com.eqinov.recrutement.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe utilitaire pour la manipulation de date
 *
 * @author Guillaume SIMON - EQINOV
 * @since 24 janv. 2020
 *
 */
public class DateUtils {
	public static final ZoneId EUROPE_PARIS = ZoneId.of("Europe/Paris");

	private DateUtils() {
	}

	public static Integer secondsFromEpoch(LocalDate date) {
		return Long.valueOf(date.atStartOfDay().atZone(EUROPE_PARIS).toInstant().toEpochMilli() / 1000).intValue();
	}

	public static Integer secondsFromEpoch(LocalDateTime date) {
		return Long.valueOf(date.atZone(EUROPE_PARIS).toInstant().toEpochMilli() / 1000).intValue();
	}

	public static Integer secondsFromEpoch(ZonedDateTime date) {
		return Long.valueOf(date.toEpochSecond()).intValue();
	}

	public static LocalDateTime convertToDateTime(String str){
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

		return LocalDateTime.parse(str, formatter);
	}
}
