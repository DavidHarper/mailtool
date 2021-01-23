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

public class ListFolderParents extends AbstractMailClient {
	public ListFolderParents(String folderURI) throws URISyntaxException, MessagingException {
		super(folderURI);
	}
	
	public static void main(String[] args) {
		String folderURI = args[0];
		
		try {
			ListFolderParents worker = new ListFolderParents(folderURI);
			
			worker.run();
		} catch (MessagingException | URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void run() throws MessagingException {
		for (Folder folder = getMainFolder(); folder != null; folder = folder.getParent()) {
			int type = folder.getType();
			
			System.out.print("'" + folder.getFullName() + "'");
			
			if ((type & Folder.HOLDS_FOLDERS) != 0)
				System.out.print(" [HOLDS_FOLDERS]");
			
			if ((type & Folder.HOLDS_MESSAGES) != 0)
				System.out.print(" [HOLDS_MESSAGES]");
			
			if (folder.getParent() == null)
				System.out.print(" [IS ROOT]");
			
			System.out.println();
		}
	}
}
