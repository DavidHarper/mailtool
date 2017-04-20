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
import javax.mail.MessagingException;

public class ListFolders extends AbstractMailClient {
	public static void main(String[] args) {
		String folderURI = null;
		boolean counters = false;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-uri"))
				folderURI = args[++i];
			else if (args[i].equalsIgnoreCase("-counters"))
				counters = true;
		}
		
		if (folderURI ==  null) {
			System.err.println("You must specify -uri");
			System.exit(1);
		}
		
		ListFolders client = null;
		
		try {
			client = new ListFolders(folderURI);
		} catch (MessagingException | URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}

		client.run(counters);
	}

	private void run(boolean counters) {
		try {
			char delimiter = getMainFolder().getSeparator();
			
			String defaultFolderName = getMainFolder().getStore().getDefaultFolder().getFullName();
			
			if (defaultFolderName != null)
				System.out.println("# Default folder is \"" + defaultFolderName + "\"");
			
			System.out.println("# Folder delimiter is " + delimiter);
			
			processFolder(getMainFolder(), counters);

			getStore().close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
	}

	private void processFolder(Folder folder, boolean counters) throws MessagingException {
		int type = folder.getType();
		
		if ((type & Folder.HOLDS_MESSAGES) != 0) {
			System.out.print(folder.getFullName());

			if (counters) {
				folder.open(Folder.READ_ONLY);
			
				int messages = folder.getMessageCount();
				int deletedMessages = folder.getDeletedMessageCount();
				int newMessages = folder.getNewMessageCount();
				int unreadMessages = folder.getUnreadMessageCount();
			
				folder.close(false);
			
				System.out.print("\t" + messages + "\t" + newMessages + "\t" + unreadMessages + "\t" + deletedMessages);
			}
			
			System.out.println();
		}

		if ((type & Folder.HOLDS_FOLDERS) != 0) {
			Folder[] subfolders = folder.list();
			
			for (int i = 0; i < subfolders.length; i++) {
				try {
					processFolder(subfolders[i], counters);
				}
				catch (MessagingException e) {
					System.err.println("ERROR whilst processing " + subfolders[i].getFullName() + " : " + e.getMessage());
				}
			}
		}
	}

	public ListFolders(String folderURI)
			throws MessagingException, URISyntaxException {
		super(folderURI);
	}

}
