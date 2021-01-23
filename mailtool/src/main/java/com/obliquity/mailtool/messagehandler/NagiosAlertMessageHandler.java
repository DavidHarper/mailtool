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

package com.obliquity.mailtool.messagehandler;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.obliquity.mailtool.MessageHandler;

public class NagiosAlertMessageHandler implements MessageHandler {
	private final PrintStream ps = System.out;
	
	private final String FS = ",";
	
	private final SimpleDateFormat datefmt = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	
	private final Pattern pattern = Pattern.compile("([A-Z]+)\\s+([A-Za-z]+)\\sAlert\\s+-\\s+([\\w\\-/ :\\.]+)\\sis\\s([A-Z]+)$");
	
	public void handleMessage(Message message) throws MessagingException {
		Date sentDate = message.getSentDate();
		String subject = message.getSubject();
		
		Matcher matcher = pattern.matcher(subject);
		
		if (!matcher.matches())
			return;
		
		String alertType = matcher.group(1);
		
		String alertTargetType = matcher.group(2);
		
		String alertTarget = matcher.group(3);
		
		String[] words = alertTarget.split("/");
		
		String alertHost = words[0];
		
		String alertService = words.length > 1 ? words[1] : "NULL";
		
		String alertLevel = matcher.group(4);
		
		ps.print(datefmt.format(sentDate));
		ps.print(FS);
		ps.print(alertType);
		ps.print(FS);
		ps.print(alertTargetType);
		ps.print(FS);
		ps.print(alertHost);
		ps.print(FS);
		ps.print(alertService);
		ps.print(FS);
		ps.print(alertLevel);
		ps.println();	
	}


}
