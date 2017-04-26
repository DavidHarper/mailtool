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

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

public class MigrationClient extends AbstractMailClient {
	private final FlagTerm NOT_DELETED = new FlagTerm(new Flags(Flags.Flag.DELETED), false);

	public MigrationClient(String folderURI) throws URISyntaxException, MessagingException {
		super(folderURI);
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

		MigrationClient migrator = new MigrationClient(fromURI);

		try {
			migrator.migrateTo(toURI);
		} catch (URISyntaxException | MessagingException e) {
			e.printStackTrace();
		}
	}
	
	public void migrateTo(String toURI) throws URISyntaxException, MessagingException {
		Folder fromFolder = getMainFolder();

		Folder toFolder = connectToFolder(toURI);
		
		System.out.println("STAGE 1: COUNT ALL SOURCE FOLDERS AND MESSAGES");
		
		int[] counts = enumerateFoldersAndMessages(fromFolder);
		
		System.out.println("\nTOTAL :  " + counts[1] + " messages in " + counts[0] + " folders.\n");
		
		System.out.println("STAGE 2 : COPY ALL SOURCE FOLDERS AND MESSAGES");
		
		migrateFoldersAndMessages(fromFolder, toFolder);
	}

	private int[] enumerateFoldersAndMessages(Folder folder) throws MessagingException {
		int folderCount = 0;
		int messageCount = 0;
		
		int type = folder.getType();
		
		boolean holdsMessages = (type & Folder.HOLDS_MESSAGES) != 0;
		boolean holdsFolders = (type & Folder.HOLDS_FOLDERS) != 0;
				
		if (holdsMessages) {
			folder.open(Folder.READ_ONLY);
			Message[] messages = folder.search(NOT_DELETED);
			messageCount += messages.length;
			folder.close(false);
			System.out.println("\t" + folder.getFullName() + " : " + messages.length + " messages [" + folder.getName() + "]");
		}
		
		if (holdsFolders) {
			Folder[] subfolders = folder.list();
			
			for (Folder subfolder : subfolders) {
				folderCount++;
				
				int[] counters = enumerateFoldersAndMessages(subfolder);
				
				folderCount += counters[0];
				messageCount += counters[1];
			}
		}
		
		return new int[] {folderCount, messageCount};
	}
	
	private void migrateFoldersAndMessages(Folder fromFolder, Folder toFolder) throws MessagingException {
		int type = fromFolder.getType();
		
		boolean holdsMessages = (type & Folder.HOLDS_MESSAGES) != 0;
		boolean holdsFolders = (type & Folder.HOLDS_FOLDERS) != 0;
		
		if (holdsMessages)
			copyMessages(fromFolder, toFolder);
		
		if (holdsFolders) {
			Folder[] subfolders = fromFolder.list();
			
			for (Folder fromSubfolder : subfolders) {
				Folder toSubfolder = makeDestinationSubfolder(fromSubfolder, toFolder);
				
				toSubfolder.create(fromSubfolder.getType());
				
				migrateFoldersAndMessages(fromSubfolder, toSubfolder);
			}
		}
	}
	
	private Folder makeDestinationSubfolder(Folder fromSubfolder, Folder toFolder) throws MessagingException {
		Store store = toFolder.getStore();
		
		String toFolderFullName = toFolder.getFullName();
		
		String newFolderName = toFolderFullName.length() > 0 ? 
				toFolder.getFullName() + toFolder.getSeparator() + fromSubfolder.getName() : fromSubfolder.getName();
		
		return store.getFolder(newFolderName);
	}
	
	private void copyMessages(Folder fromFolder, Folder toFolder) throws MessagingException {
		fromFolder.open(Folder.READ_ONLY);
		toFolder.open(Folder.READ_WRITE);
		
		Message[] messages = fromFolder.search(NOT_DELETED);
		
		System.out.println("\tCopying " + messages.length + " messages from " + fromFolder.getFullName() + " to " + toFolder.getFullName());

		fromFolder.copyMessages(messages, toFolder);
		
		fromFolder.close(false);
		toFolder.close(false);
	}
}
