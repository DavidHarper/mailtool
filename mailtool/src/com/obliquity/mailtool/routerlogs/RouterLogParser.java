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
	
	private static final String LAN_ACCESS_PATTERN = "from (\\d+\\.\\d+\\.\\d+\\.\\d+):\\d+ to (\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)";
	
	private final Pattern lanAccessPattern = Pattern.compile(LAN_ACCESS_PATTERN);
	
	private static final String DOS_TYPE_PATTERN = "^DoS Attack: (.*)$";
	
	private final Pattern dosTypePattern = Pattern.compile(DOS_TYPE_PATTERN);
	
	private static final String DOS_ATTACK_PATTERN_1 = "^from source: (\\d+\\.\\d+\\.\\d+\\.\\d+), port (\\d+)$";
	
	private final Pattern dosAttackPattern1 = Pattern.compile(DOS_ATTACK_PATTERN_1);
	
	private static final String DOS_ATTACK_PATTERN_2 = "^from source: (\\d+\\.\\d+\\.\\d+\\.\\d+)$";
	
	private final Pattern dosAttackPattern2 = Pattern.compile(DOS_ATTACK_PATTERN_2);
	
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
				String what = matcher.group(1);
				
				String where = matcher.group(2);
				
				String when = matcher.group(3);

				analyseLogEntry(what, where, when);
			}
		}
	}
	
	private void analyseLogEntry(String what, String where, String when) throws ParseException {
		if (what.startsWith("LAN access from remote")) {
			analyseLanAccessEntry(where, when);
		} else if (what.startsWith("DHCP IP:")) {
			// Do nothing
		} else if (what.startsWith("DoS Attack:")) {
			analyseDosAttackEntry(what, where, when);
		} else if (what.startsWith("admin login")) {
			// Do nothing
		} else {
			System.err.println("UNKNOWN TYPE: " + what);
		}
	}
	
	private void analyseLanAccessEntry(String where, String when) throws ParseException {		
		Matcher matcher = lanAccessPattern.matcher(where);
		
		Date whenDate = dateInputFormat.parse(when);
		
		System.out.print(dateOutputFormat.format(whenDate) + "\tLAN_ACCESS");
		
		if (matcher.matches())
			for (int i = 1; i <= matcher.groupCount(); i++)
				System.out.print("\t" + matcher.group(i));
		
		System.out.println();
	}
	
	private void analyseDosAttackEntry(String what, String where, String when) throws ParseException {
		Matcher matcher = dosTypePattern.matcher(what);
		
		String type = matcher.matches() ? matcher.group(1) : "UNKNOWN";
		
		matcher = dosAttackPattern1.matcher(where);
		
		if (!matcher.matches())
			matcher = dosAttackPattern2.matcher(where);
		
		Date whenDate = dateInputFormat.parse(when);
		
		System.out.print(dateOutputFormat.format(whenDate) + "\tDOS_ATTACK\t" + type);
		
		if (matcher.matches())
			for (int i = 1; i <= matcher.groupCount(); i++)
				System.out.print("\t" + matcher.group(i));
		
		System.out.println();
	}

}
