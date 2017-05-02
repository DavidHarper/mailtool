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
import java.text.DecimalFormat;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

public class MigrationClient extends AbstractMailClient {
	private final FlagTerm NOT_DELETED = new FlagTerm(new Flags(Flags.Flag.DELETED), false);
	
	private final DecimalFormat dfmt = new DecimalFormat("0.0");

	public MigrationClient(String folderURI) throws URISyntaxException, MessagingException {
		super(folderURI);
	}

	public static void main(String[] args) throws URISyntaxException, MessagingException {
		String fromURI = null;
		String toURI = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-from":
				fromURI = args[++i];
				break;

			case "-to":
				toURI = args[++i];
				break;
				
			case "-help":
				showHelp(null);
				System.exit(0);

			default:
				showHelp("Unrecognised option: " + args[i]);
				System.exit(1);
			}
		}

		if (fromURI == null || toURI == null) {
			showHelp("You must supply both -from and -to URIs");
			System.exit(2);
		}

		MigrationClient migrator = new MigrationClient(fromURI);

		try {
			migrator.migrateTo(toURI);
		} catch (URISyntaxException | MessagingException e) {
			e.printStackTrace();
		}
	}
	
	private static void showHelp(String message) {
		if (message != null)
			System.err.println("ERROR: " + message + "\n");
		
		String[] lines = {
				"WHAT THIS PROGRAM DOES",
				"",
				"This Java program copies folders and messages from a source IMAP server",
				"to a destination IMAP server.  Source and destination are both specified as",
				"URIs in the form imaps://<USERNAME>@<HOST>/<FOLDER>",
				"",
				"If no folder is explicitly specified in one or other URI, then the program",
				"uses the folder that is the root of the IMAP folder hierarchy for that user",
				"account.",
				"",
				"The program will recursively copy all subfolders and messages contained in the",
				"source folder into the destination folder.  For example,",
				"",
				"<COPY_COMMAND> -from imaps://bob@imap.example.com/MyStuff -to imaps://bob@imap.google.com/Personal",
				"",
				"copies all messages and subfolders in MyStuff on the source IMAP server into",
				"the folder Personal on the destination IMAP server.  For example, if MyStuff",
				"contains a folder named XYZ, this subfolder will be created as Personal/XYZ",
				"on the destination IMAP server, and all messages and subfolders in MyStuff/XYZ",
				"will be copied into Personal/XYZ.",
				"",
				"If any destination folders or subfolders do not exist, they will be created.",
				"",
				"If the username for either account contains an @ symbol, it must be replaced by",
				"the URL-encoded version i.e. %40.  For example, username bob@microsoft.com on",
				"IMAP server imap.example.com should be specified as the URI",
				"",
				"\timaps://bob%40microsoft.com@imap.example.com/",
				"",
				"If the folder names contain non-alphanumeric characters, they must be replaced by",
				"the URL-encoded version of the character.  In particular, spaces must be replaced",
				"by %20.",
				"",
				"WARNING: Error checking is basic.  The program will exit immediately if an error",
				"is encountered.  This can include dropped network connections, exceeding disk",
				"quota on the destination server, illegal IMAP operations, etc.",
				"",
				"The program does not check for duplicate emails, so if it is run more than once",
				"with the same source and destination arguments, you WILL end up with duplicates",
				"of all of the emails copied from the source.",
				"",
				"SOURCE CODE",
				"",
				"The source code for this program is available at GitHub:",
				"",
				"\thttps://github.com/DavidHarper/mailtool",
				"",
				"DISCLAIMER OF LIABILITY",
				"",
				"The GNU General Public Licence (https://www.gnu.org/licenses/gpl-3.0.en.html)",
				"applies to this program.  Please note in particular:",
				"",
				" * This program is distributed in the hope that it will be useful,",
				" * but WITHOUT ANY WARRANTY; without even the implied warranty of",
				" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU",
				" * General Public License for more details.",
				""
		};
		
		for (String line : lines)
			System.err.println(line);
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
			System.out.println("\t" + folder.getFullName() + " : " + messages.length + " messages");
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
		
		if (!toFolder.exists())
			toFolder.create(type);
		
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
		
		System.out.print("\tCopying " + messages.length + " messages from " + fromFolder.getFullName() + " to " + toFolder.getFullName());
		System.out.flush();

		long startTime = System.currentTimeMillis();
		
		fromFolder.copyMessages(messages, toFolder);
		
		long endTime = System.currentTimeMillis();
		
		double seconds = (double)(endTime - startTime)/1000.0;
		
		System.out.println(" [" + dfmt.format(seconds) + " seconds]");
		System.out.flush();
		
		fromFolder.close(false);
		toFolder.close(false);
	}
}
