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

public class PromoteAllDirectSubfolders extends AbstractMailClient {
	public PromoteAllDirectSubfolders(String folderURI) throws URISyntaxException, MessagingException {
		super(folderURI);
	}

	public static void main(String[] args) {
		String folderURI = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-uri"))
				folderURI = args[++i];
		}

		if (folderURI == null) {
			System.err.println("You must specify -uri");
			System.exit(1);
		}

		PromoteAllDirectSubfolders client = null;

		try {
			client = new PromoteAllDirectSubfolders(folderURI);
		} catch (MessagingException | URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}

		client.run();
	}

	private void run() {
		try {
			Folder mainFolder = getMainFolder();

			int type = mainFolder.getType();

			if ((type & Folder.HOLDS_FOLDERS) == 0)
				return;

			Folder[] subfolders = mainFolder.list();

			char delimiter = mainFolder.getSeparator();

			int converted = 0;
			int notConverted = 0;

			for (Folder folder : subfolders) {
				String folderName = folder.getFullName();

				int offset = folderName.indexOf(delimiter);

				String newFolderName = folderName.substring(offset + 1);

				Folder newFolder = folder.getStore().getFolder(newFolderName);

				if (newFolder.exists()) {
					System.err.println("Cannot rename folder " + folderName + " to " + newFolderName
							+ " because target folder already exists.");

					notConverted++;
				} else {
					System.out.print("Preparing to rename folder " + folderName + " to " + newFolderName);

					boolean success = folder.renameTo(newFolder);

					System.out.println(success ? " OK" : " FAILED");

					if (success)
						converted++;
					else
						notConverted++;
				}
			}
			
			System.out.println("\nSUMMARY\n\nFolders examined: " + subfolders.length + "\nFolders renamed: " + converted +
					"\nFolders NOT renamed: " + notConverted);

			getStore().close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
