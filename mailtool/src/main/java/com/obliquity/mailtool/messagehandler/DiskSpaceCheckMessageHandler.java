/*
 * mailtool - a package for processing IMAP mail folders
 *
 * Copyright (C) 2024 David Harper at obliquity.com
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

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.obliquity.mailtool.MessageHandler;

public class DiskSpaceCheckMessageHandler implements MessageHandler {
	private final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final Pattern pattern = Pattern.compile("^([\\w\\-]+)\\s+/(\\w+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+).*");
	private final PrintStream ps = System.out;
	private final String FS = ",";

	public void handleMessage(Message message) throws MessagingException, IOException {
		Date sentDate = message.getSentDate();
		
		Object content = message.getContent();
				
		if (!(content instanceof String))
			return;
		
		String datestr = datefmt.format(sentDate);
		
		String[] lines = ((String)content).split("[\n\r]+");
				
		for (String line : lines) {		
			Matcher matcher = pattern.matcher(line);
			
			if (matcher.matches()) {
				ps.print(datestr);
				ps.print(FS);
				ps.print(matcher.group(1));
				ps.print(FS);
				ps.print(matcher.group(2));
				ps.print(FS);
				ps.print(matcher.group(3));
				ps.print(FS);
				ps.print(matcher.group(4));
				ps.print(FS);
				ps.print(matcher.group(5));
				ps.println();
			}
		}
	}
}
