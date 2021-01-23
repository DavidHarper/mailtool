/*
 * mailtool - a package for processing IMAP mail folders
 *
 * Copyright (C) 2017 David Harper at obliquity.com
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 * See the COPYING file located in the top-level-directory of
 * the archive of this library for complete text of license.
 */

package com.obliquity.mailtool.routerlogs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouterLogParser {
	private static final String MAIN_PATTERN = "^\\[(.*)\\]\\s+(.*), [A-Z][a-z]+day, ([A-Z][a-z]+ \\d{2}, \\d{4} \\d{2}:\\d{2}:\\d{2})$";
	
	private final Pattern mainPattern = Pattern.compile(MAIN_PATTERN);
	
	private static final String LAN_ACCESS_PATTERN = "from (\\d+\\.\\d+\\.\\d+\\.\\d+):\\d+ to \\d+\\.\\d+\\.\\d+\\.\\d+:(\\d+)";
	
	private final Pattern lanAccessPattern = Pattern.compile(LAN_ACCESS_PATTERN);
	
	private final Pattern[] lanAccessPatterns = { lanAccessPattern };
	
	private static final String DOS_ATTACK_PATTERN_1 = "^from source: (\\d+\\.\\d+\\.\\d+\\.\\d+), port (\\d+)$";
	
	private final Pattern dosAttackPattern1 = Pattern.compile(DOS_ATTACK_PATTERN_1);
	
	private static final String DOS_ATTACK_PATTERN_2 = "^from source: (\\d+\\.\\d+\\.\\d+\\.\\d+)$";
	
	private final Pattern dosAttackPattern2 = Pattern.compile(DOS_ATTACK_PATTERN_2);
	
	private final Pattern[] dosAttackPatterns = { dosAttackPattern1, dosAttackPattern2 };
	
	private static final String INTERNET_CONNECTED_PATTERN = "^IP address: (\\d+\\.\\d+\\.\\d+\\.\\d+)$";
	
	private final Pattern internetConnectedPattern = Pattern.compile(INTERNET_CONNECTED_PATTERN);
	
	private final Pattern[] internetConnectedPatterns = { internetConnectedPattern };
	
	private static final String WLAN_ACCESS_REJECTED_PATTERN = "^from MAC address ([0-9A-Fa-f:]+)$";
	
	private final Pattern wlanAccessRejectedPattern = Pattern.compile(WLAN_ACCESS_REJECTED_PATTERN);
	
	private final Pattern[] wlanAccessRejectedPatterns = { wlanAccessRejectedPattern };
		
	private final Pattern[] accessControlEntryPatterns = null;
	
	private final Pattern[] upnpSetEventPatterns = null;
	
	private static final String DATE_TIME_INPUT_FORMAT= "MMM d, yyyy HH:mm:ss";
	
	private final SimpleDateFormat dateInputFormat = new SimpleDateFormat(DATE_TIME_INPUT_FORMAT);
	
	private static final String DATE_TIME_OUTPUT_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	private final SimpleDateFormat dateOutputFormat = new SimpleDateFormat(DATE_TIME_OUTPUT_FORMAT);
	
	public static void main(String[] args) {
		RouterLogParser parser = new RouterLogParser();
		
		try {
			parser.parseContent(System.in);
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}
	
	public void parseContent(InputStream is) throws IOException, ParseException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		while (true) {
			String line = br.readLine();

			if (line == null)
				break;

			line = line.trim();

			if (line.length() == 0)
				continue;

			Matcher matcher = mainPattern.matcher(line);

			boolean matches = matcher.matches();

			if (matches) {
				String type = matcher.group(1);

				String subtype = null;

				int colonOffset = type.indexOf(':');

				if (colonOffset >= 0) {
					subtype = type.substring(colonOffset + 1).trim();
					type = type.substring(0, colonOffset);
				}

				String where = matcher.group(2);

				String when = matcher.group(3);

				Date whenDate = when == null ? null : dateInputFormat.parse(when);

				analyseLogEntry(type, subtype, where, whenDate);
			}
		}
	}
	
	private void analyseLogEntry(String type, String subtype, String where, Date when) throws ParseException {
		switch (type) {
		case "LAN access from remote":
			analyseLogEntry("LAN_ACCESS", subtype, where, when, lanAccessPatterns);
			break;

		case "DHCP IP":
			// Do nothing
			break;

		case "admin login":
			// Do nothing
			break;

		case "DoS Attack":
			analyseLogEntry("DOS_ATTACK", subtype, where, when, dosAttackPatterns);
			break;

		case "Internet connected":
			analyseLogEntry("INTERNET_CONNECTED", subtype, where, when, internetConnectedPatterns);
			break;

		case "WLAN access rejected":
			analyseLogEntry("WLAN_ACCESS_REJECTED", subtype, where, when, wlanAccessRejectedPatterns);
			break;

		case "Access Control":
			analyseLogEntry("ACCESS_CONTROL", subtype, where, when, accessControlEntryPatterns);
			break;
			
		case "UPnP set event":
			analyseLogEntry("UPNP_SET_EVENT", subtype, where, when, upnpSetEventPatterns);
			break;

		default:
			System.err.println("UNKNOWN TYPE: " + type + (subtype != null ? " (" + subtype + ")" : "") + " at " + when);
		}
	}
	
	private void analyseLogEntry(String type, String subtype, String where, Date when, Pattern[] patterns) throws ParseException {
		System.out.print(dateOutputFormat.format(when) + "," + type);
		
		System.out.print("," + (subtype == null ? "" : subtype));
		
		if (patterns != null) {
			for (Pattern pattern : patterns) {
				Matcher matcher = pattern.matcher(where);
				
				if (matcher.matches()) {
					for (int i = 1; i <= matcher.groupCount(); i++)
						System.out.print("," + matcher.group(i));
					
					break;
				}
			}
			
			System.out.println();
		} else {
			System.out.println("," + where);
		}
	}	
}
