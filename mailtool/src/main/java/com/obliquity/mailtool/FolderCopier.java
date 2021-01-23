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

import java.net.URISyntaxException;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

public class FolderCopier extends AbstractMailClient {
	public FolderCopier(String fromURI) throws URISyntaxException, MessagingException {
		super(fromURI);
	}

	public static void main(String[] args) throws URISyntaxException, MessagingException {
		String fromURI = null;
		String toURI = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-from":
				fromURI = args[++i];
				break;

			case "-to":
				toURI = args[++i];
				break;

			default:
				System.err.println("Unrecognised option: " + args[i]);
				System.exit(1);
				;
			}
		}

		if (fromURI == null || toURI == null) {
			System.err.println("You must supply both -from and -to URIs");
			System.exit(2);
		}

		FolderCopier copier = new FolderCopier(fromURI);

		try {
			copier.copy(toURI);
		} catch (URISyntaxException | MessagingException e) {
			e.printStackTrace();
		}
	}

	public void copy(String toURI) throws URISyntaxException, MessagingException {
		Folder fromFolder = getMainFolder();

		Folder toFolder = connectToFolder(toURI);

		System.out.println("Source folder has " + fromFolder.getMessageCount() + " messages");

		System.out.println("Destination folder has " + toFolder.getMessageCount() + " messages");

		fromFolder.open(Folder.READ_ONLY);
		
		toFolder.open(Folder.READ_WRITE);

		Message[] messages = fromFolder.getMessages();

		fromFolder.copyMessages(messages, toFolder);

		System.out.println("Source folder now has " + fromFolder.getMessageCount() + " messages");

		System.out.println("Destination folder now has " + toFolder.getMessageCount() + " messages");
	}
}
