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

package com.obliquity.mailtool;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

public abstract class AbstractMailClient {
	private Folder mainFolder = null;
	
	public AbstractMailClient(String folderURI) throws URISyntaxException, MessagingException {
		mainFolder = connectToFolder(folderURI);
	}
		
	protected Folder getMainFolder() {
		return mainFolder;
	}
	
	protected Store getStore() {
		return mainFolder.getStore();
	}
	
	protected Folder connectToFolder(String folderURI)
			throws URISyntaxException, MessagingException {
		URI uri = new URI(folderURI);

		String scheme = uri.getScheme();
		String user = uri.getUserInfo();
		String host = uri.getHost();
		int port = uri.getPort();
		String path = uri.getPath();

		if (path.startsWith("/"))
			path = path.substring(1);

		Properties props = new Properties();

		props.put("mail." + scheme + ".host", host);
		props.put("mail." + scheme + ".user", user);

		if (port > 0)
			props.put("mail." + scheme + ".port", port);

		Authenticator auth = new SimpleAuthenticator(user);

		Session session = Session.getInstance(props, auth);

		boolean debug = Boolean.getBoolean("debug");

		session.setDebug(debug);

		Store store = session.getStore(scheme);

		store.connect();
		
		return path.length() > 0 ? store.getFolder(path) : store.getDefaultFolder();
	}
	
	protected void displayMessageHeaders(Message message, PrintStream ps) throws MessagingException {
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
		
		ps.println("Date:    " + message.getSentDate());
		
		ps.println("Subject: " + message.getSubject());		
	}

	protected void displayMessage(Message message, PrintStream ps) throws MessagingException, IOException {
		displayMessageHeaders(message, ps);
		
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
		}
		
		ps.println();
	}
	
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
