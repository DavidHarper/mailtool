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

public class FolderDeleter extends AbstractMailClient {
	private static final String DANGER_MODE = "dangerMode";
	
	public FolderDeleter(String folderURI) throws URISyntaxException, MessagingException {
		super(folderURI);
	}
	
	public static void main(String[] args) throws URISyntaxException, MessagingException {
		String folderURI = null;
		boolean recursive = false;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-uri":
				folderURI = args[++i];
				break;
				
			case "-recurse":
			case "-recursive":
				recursive = true;
				break;

			default:
				System.err.println("Unrecognised option: " + args[i]);
				System.exit(1);
				;
			}
		}

		if (folderURI == null) {
			System.err.println("You must supply -uri argument");
			System.exit(2);
		}

		FolderDeleter deleter = new FolderDeleter(folderURI);

		try {
			deleter.delete(recursive);
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	public void delete(boolean recursive) throws MessagingException {
		boolean dangerMode = Boolean.getBoolean(DANGER_MODE);
		
		if (recursive && !dangerMode) {
			System.err.println("You must enable danger mode to use the recursive option");
			return;
		}
		
		Folder folder = getMainFolder();
		
		Folder rootFolder = folder.getStore().getDefaultFolder();
		
		if (folder.equals(rootFolder) || folder.getFullName().length() == 0) {
			System.err.println("Cannot delete this store's default folder!");
			return;
		}
		
		folder.delete(recursive);
	}
}
