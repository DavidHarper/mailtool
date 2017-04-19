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

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import java.io.*;
import java.net.URISyntaxException;

public class SimpleClient extends AbstractMailClient {
	public static void main(String[] args) {
		String folderURI = null;
		boolean recursive = false;
		boolean verbose = false;
		boolean fetchParts = false;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-uri"))
				folderURI = args[++i];
			else if (args[i].equalsIgnoreCase("-recursive"))
				recursive = true;
			else if (args[i].equalsIgnoreCase("-verbose"))
				verbose = true;
			else if (args[i].equalsIgnoreCase("-fetchparts"))
				fetchParts = true;
		}

		if (folderURI == null) {
			System.err.println("You must specify -uri");
			System.exit(1);
		}

		SimpleClient client = null;

		try {
			client = new SimpleClient(folderURI);
		} catch (MessagingException | URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}

		client.run(recursive, verbose, fetchParts);
	}

	public SimpleClient(String folderURI) throws MessagingException, URISyntaxException {
		super(folderURI);
	}

	public void run(boolean recursive, boolean verbose, boolean fetchParts) {
		try {
			processFolder(getMainFolder(), recursive, verbose, fetchParts);

			getStore().close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void processFolder(Folder folder, boolean recursive, boolean verbose, boolean fetchParts)
			throws MessagingException, IOException {
		int type = folder.getType();

		System.out.println("PROCESSING FOLDER " + folder.getFullName());

		if ((type & Folder.HOLDS_FOLDERS) != 0 && recursive)
			processSubFolders(folder, verbose, fetchParts);

		if ((type & Folder.HOLDS_MESSAGES) != 0)
			processMessages(folder, verbose, fetchParts);
	}

	private void processSubFolders(Folder folder, boolean verbose, boolean fetchParts)
			throws MessagingException, IOException {
		Folder[] subfolders = folder.list();

		for (int i = 0; i < subfolders.length; i++)
			processFolder(subfolders[i], true, verbose, fetchParts);
	}

	private String flagsToString(Flags flags) {
		String str = "";

		for (Flag flag : flags.getSystemFlags()) {
			str += " " + flagToString(flag);
		}

		return str;
	}

	private String flagToString(Flag flag) {
		if (flag == Flag.ANSWERED)
			return "ANSWERED";
		else if (flag == Flag.DELETED)
			return "DELETED";
		else if (flag == Flag.DRAFT)
			return "DRAFT";
		else if (flag == Flag.FLAGGED)
			return "FLAGGED";
		else if (flag == Flag.RECENT)
			return "RECENT";
		else if (flag == Flag.SEEN)
			return "SEEN";
		else if (flag == Flag.USER)
			return "USER";
		else
			return null;
	}

	private void processMessages(Folder folder, boolean verbose, boolean fetchParts)
			throws MessagingException, IOException {
		folder.open(Folder.READ_ONLY);

		Message message[] = folder.getMessages();

		for (int i = 0, n = message.length; i < n; i++) {
			Address[] to = message[i].getRecipients(Message.RecipientType.TO);

			System.out.println("Message : " + i + ":\n" + "From:    " + message[i].getFrom()[0]);

			if (to != null) {
				System.out.print("To:      ");

				for (int j = 0; j < to.length; j++) {
					if (j > 0)
						System.out.print(", ");
					System.out.print(to[j]);
				}

				System.out.println();
			}

			System.out.println("Date:    " + message[i].getSentDate() + "\n" + "Subject: " + message[i].getSubject()
					+ "\n" + "Flags:   " + flagsToString(message[i].getFlags()));

			if (verbose) {
				int messageSize = -1;

				if (message[i] instanceof MimeMessage) {
					MimeMessage msg = (MimeMessage) message[i];

					messageSize = msg.getSize();
				}

				if (messageSize > 0)
					System.out.println("Size:    " + messageSize);
				
				Object content = message[i].getContent();

				if (content instanceof String)
					System.out.println("Content is text, " + ((String) content).length() + " bytes");
				else if (content instanceof Multipart) {
					Multipart mp = (Multipart) content;

					int parts = mp.getCount();

					System.out.println("Content is multi-part with " + parts + " parts");

					for (int j = 0; j < parts; j++) {
						System.out.println("Part #" + j);

						Part part = mp.getBodyPart(j);

						System.out.println("Content-Type: " + part.getContentType());
						System.out.println("Length: " + part.getSize());

						String disp = part.getDisposition();
						if (disp != null)
							System.out.println("Disposition: " + disp);

						String desc = part.getDescription();
						if (desc != null)
							System.out.println("Description: " + desc);

						String filename = part.getFileName();
						if (filename != null)
							System.out.println("FileName: " + filename);

						if (part instanceof MimeBodyPart) {
							MimeBodyPart mbp = (MimeBodyPart) part;

							String encoding = mbp.getEncoding();

							if (encoding != null)
								System.out.println("Encoding: " + encoding);
						}

						if (fetchParts) {
							InputStream is = part.getInputStream();

							byte buffer[] = new byte[1024];

							int size = 0;
							int nbytes;

							while ((nbytes = is.read(buffer)) > 0)
								size += nbytes;

							is.close();

							System.out.println("Decoded size: " + size);
						}

						System.out.println();
					}
				} else
					System.out.println("Content is of type " + content.getClass().getName());
			}

			System.out.println();
		}

		folder.close(false);
	}
}
