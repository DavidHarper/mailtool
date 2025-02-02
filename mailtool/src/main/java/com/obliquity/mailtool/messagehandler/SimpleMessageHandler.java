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

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Flags.Flag;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.obliquity.mailtool.MessageHandler;

public class SimpleMessageHandler implements MessageHandler {
	private PrintStream ps = System.out;
	
	private final char TAB = '\t';
	
	private final SimpleDateFormat datefmt = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	
	private boolean tabular = false;
	
	private boolean showContent = Boolean.getBoolean("com.obliquity.mailtool.messagehandler.simplemessagehandler.showcontent");
	
	public void setTabular(boolean tabular) {
		this.tabular = tabular;
	}
	
	public boolean isTabular() {
		return tabular;
	}
	
	public void setPrintStream(PrintStream ps) {
		this.ps = ps;
	}
	
	public PrintStream getPrintStream() {
		return ps;
	}
	
	public void handleMessage(Message message) throws MessagingException, IOException {
		if (tabular)
			displayMessageInTabularFormat(message);
		else
			displayMessage(message);
	}
	
	private void displayMessageInTabularFormat(Message message) throws MessagingException, IOException {
		InternetAddress from = (InternetAddress)message.getFrom()[0];
		Address[] to_list = message.getAllRecipients();
		InternetAddress to = to_list == null ? null : (InternetAddress)to_list[0];
		Date sentDate = message.getSentDate();
		String subject = message.getSubject();
		String folderName = message.getFolder().getFullName();
		int size = (message instanceof MimeMessage) ? ((MimeMessage)message).getSize() : -1;
		String msgid = (message instanceof MimeMessage) ? ((MimeMessage)message).getMessageID() : null;
		
		ps.print(folderName);
		ps.print(TAB);
		ps.print(from.getAddress());
		ps.print(TAB);
		ps.print(to == null ? "NULL" : to.getAddress());
		ps.print(TAB);
		ps.print(sentDate != null ? datefmt.format(sentDate) : "NULL");
		ps.print(TAB);
		ps.print(size);
		ps.print(TAB);
		ps.print(subject);
		ps.print(TAB);
		ps.print(msgid == null ? "NULL" : msgid);
		
		if (message instanceof MimeMessage)
			displayAttachmentsInTabularFormat((MimeMessage)message);
		
		ps.println();
	}
	
	private void displayAttachmentsInTabularFormat(MimeMessage message) throws MessagingException, IOException {
		Object content = message.getContent();
		
		if (content instanceof Multipart) {
			Multipart mp = (Multipart)content;
			
			int parts = mp.getCount();
			
			for (int j = 0; j < parts; j++) {
				Part part = mp.getBodyPart(j);
				
				String filename = part.getFileName();
				
				ps.print(TAB);

				ps.print(j + ":" + part.getContentType() + ":" + part.getSize() + ":" + filename);
			}
		}
	}
	
	protected void displayMessageHeaders(Message message) throws MessagingException {
		Address[] to = message.getRecipients(Message.RecipientType.TO);
		
		ps.println("From:    " + message.getFrom()[0]);
		
		if (to != null) {
			ps.print("To:      ");
			for (int i = 0; i < to.length; i++) {
				if (i > 0)
					ps.print(", ");
				ps.print(to[i]);
			}
			ps.println();
		}
		
		Date sentDate = message.getSentDate();
		Date receivedDate = message.getReceivedDate();
		
		ps.println("Date:    " + (sentDate != null ? sentDate : 
			(receivedDate != null ? receivedDate + " [Received]" : "[NO DATES]")));
		
		ps.println("Subject: " + message.getSubject());

		if (message instanceof MimeMessage) {
			MimeMessage msg = (MimeMessage)message;
			String msgid = msg.getMessageID();
			if (msgid != null)
				ps.println("MsgID:   " + msgid);
		}
	}

	protected void displayMessage(Message message) throws MessagingException, IOException {
		displayMessageHeaders(message);
		
		String flags = flagsToString(message);

		if (flags != null)
			ps.println("Flags:   " + flags);
		
		if (message instanceof MimeMessage) {
			MimeMessage msg = (MimeMessage)message;
			
			int messageSize = msg.getSize();
			ps.println("Size:    " + messageSize);

			Object content = message.getContent();
			
			if (content instanceof Multipart) {
				Multipart mp = (Multipart)content;
				
				int parts = mp.getCount();
					ps.println("Parts:   " + parts);
				
				for (int j = 0; j < parts; j++) {
					Part part = mp.getBodyPart(j);
					ps.println("\tPart " + j + ": Content-Type=" + part.getContentType() + "; Length=" + part.getSize());
				}
			}
			
			if (showContent && content instanceof String) {
				ps.println(CONTENT_HEADER);
				ps.print(content);
				ps.println(CONTENT_TRAILER);
			}

			ps.println();
		}
	}
	
	private static final String CONTENT_HEADER  = "============================== CONTENT BEGINS ==============================";
	private static final String CONTENT_TRAILER = "==============================  CONTENT ENDS  ==============================";
	
	protected String flagsToString(Message message) throws MessagingException {
		String str = null;
		
		str = setOrAppend(str, message, Flags.Flag.ANSWERED, "ANSWERED");
		
		str = setOrAppend(str, message, Flags.Flag.DELETED, "DELETED");
		
		str = setOrAppend(str, message, Flags.Flag.DRAFT, "DRAFT");
		
		str = setOrAppend(str, message, Flags.Flag.RECENT, "RECENT");
		
		str = setOrAppend(str, message, Flags.Flag.SEEN, "SEEN");
		
		return str;
	}
	
	private String setOrAppend(String str, Message message, Flag flag, String text) throws MessagingException {
		if (message.isSet(flag)) {
			if (str == null)
				str = text;
			else
				str += " | " + text;;			
		}
		
		return str;
	}

}
