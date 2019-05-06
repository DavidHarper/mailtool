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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import com.obliquity.mailtool.MessageHandler;

public class SimpleMessageHandler implements MessageHandler {
	private final PrintStream ps = System.out;
	
	private final char TAB = '\t';
	
	private final SimpleDateFormat datefmt = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	
	public void handleMessage(Message message) throws MessagingException {
		InternetAddress from = (InternetAddress)message.getFrom()[0];
		InternetAddress to = (InternetAddress)message.getAllRecipients()[0];
		Date sentDate = message.getSentDate();
		String subject = message.getSubject();
		String folderName = message.getFolder().getFullName();
		
		ps.print(folderName);
		ps.print(TAB);
		ps.print(from.getAddress());
		ps.print(TAB);
		ps.print(to.getAddress());
		ps.print(TAB);
		ps.print(datefmt.format(sentDate));
		ps.print(TAB);
		ps.print(subject);
		ps.println();	
	}

}
