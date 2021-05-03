/*
 * mailtool - a package for processing IMAP mail folders
 *
 * Copyright (C) 2021 David Harper at obliquity.com
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.obliquity.mailtool.MessageHandler;

public class DumpPlainTextAttachmentHandler implements MessageHandler {
	private static final String TEXT_PLAIN_MIME_TYPE = "text/plain";
			
	private final SimpleDateFormat datefmt = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	
	private File basedir = null;
	private int fileNumber = 0;
	
	public DumpPlainTextAttachmentHandler() {
		String basedirname = System.clearProperty("com.obliquity.mailtool.mailhandler.dumpplaintextattachmenthandler.basedir");
		
		if (basedirname != null) {
			basedir = new File(basedirname);
			
			if (!basedir.exists())
				basedir.mkdirs();
		}
	}

	public void handleMessage(Message message) throws MessagingException, IOException {
		Part body = getPlainTextAttachment(message);
		
		if (body == null)
			return;

		PrintStream ps = System.out;
		
		if (basedir != null) {
			fileNumber++;
			
			String filename = String.format("message%06d.txt", fileNumber);
			
			File file = new File(basedir, filename);
			
			ps = new PrintStream(file);
			
			System.out.println("Creating file " + filename);
		}
		
		displayMessageHeaders(message, ps);
		
		ps.println();
				
		ps.println(body.getContent().toString());
		
		if (basedir != null)
			ps.close();
	}

	private Part getPlainTextAttachment(Message message) {
		if (!(message instanceof MimeMessage))
			return null;
		
		MimeMessage mimeMessage = (MimeMessage)message;
		
		try {
			Object content = mimeMessage.getContent();
			
			if (content instanceof Multipart) {
				Multipart mp = (Multipart)content;
				
				int parts = mp.getCount();
				
				for (int j = 0; j < parts; j++) {
					Part part = mp.getBodyPart(j);
					
					if (part.isMimeType(TEXT_PLAIN_MIME_TYPE))
						return part;
				}
			}
		}
		catch (MessagingException me) {
			me.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return null;
	}
	
	private void displayMessageHeaders(Message message, PrintStream ps) throws MessagingException {
		Date sentDate = message.getSentDate();
		String subject = message.getSubject();
		InternetAddress from = (InternetAddress)message.getFrom()[0];
		InternetAddress to = (InternetAddress)message.getAllRecipients()[0];

		ps.println("From: " + from.getAddress());
		if (to != null)
			ps.println("To: " + to.getAddress());
		ps.println("Date: " + datefmt.format(sentDate));
		ps.println("Subject: " + subject);	
	}
}
