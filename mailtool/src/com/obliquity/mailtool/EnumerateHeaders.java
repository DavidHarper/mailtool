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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.FlagTerm;

public class EnumerateHeaders extends AbstractMailClient {
	private final FlagTerm notDeleted = new FlagTerm(new Flags(Flags.Flag.DELETED), false);

	public EnumerateHeaders(String folderURI) throws URISyntaxException, MessagingException {
		super(folderURI);
	}

	public static void main(String[] args) {
		String folderURI = null;
		boolean recursive = false;
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-uri":
				folderURI = args[++i];
				break;
				
			case "-recurse":
			case "-recursive":
				recursive = true;
				break;
				
			default:	
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}		
		}
		
		EnumerateHeaders worker;
		try {
			worker = new EnumerateHeaders(folderURI);
			
			worker.run(recursive);
		} catch (URISyntaxException | MessagingException e) {
			e.printStackTrace();
		}
	}
	
	public void run(boolean recursive) throws MessagingException {
		Folder folder = getMainFolder();
		
		Map<String, Integer> headerMap = new HashMap<String, Integer>();
		
		int messageCount = processFolder(folder, headerMap, recursive);
		
		Set<String> keys = headerMap.keySet();
		
		SortedSet<String> sortedKeys = new TreeSet<String>(keys);
		
		for (String key : sortedKeys)
			System.out.println(key + "\t" + headerMap.get(key));
		
		System.err.println("# Examined " + messageCount + " messages.");
	}
	
	private int processFolder(Folder folder, Map<String, Integer> headerMap, boolean recursive) throws MessagingException {
		int messageCount = 0;
		
		int type = folder.getType();
		
		if ((type & Folder.HOLDS_MESSAGES) != 0) {
			folder.open(Folder.READ_ONLY);
			
			Message[] messages = folder.search(notDeleted);

			for (Message message : messages)
				processMessage(message, headerMap);
			
			messageCount = messages.length;
			
			folder.close(false);
		}
		
		if (((type & Folder.HOLDS_FOLDERS) != 0) && recursive) {
			Folder[] subfolders = folder.list();

			for (Folder subfolder : subfolders)
				messageCount += processFolder(subfolder, headerMap, recursive);
		}		
		
		return messageCount;
	}
	
	private void processMessage(Message message, Map<String, Integer> headerMap) throws MessagingException {
		@SuppressWarnings("unchecked")
		Enumeration<Header> allHeaders = message.getAllHeaders();
		
		while (allHeaders.hasMoreElements()) {
			Header header = allHeaders.nextElement();
			
			String name = header.getName();
			
			if (headerMap.containsKey(name)) {
				headerMap.put(name, 1 + headerMap.get(name));
			} else {
				headerMap.put(name, 1);
			}
		}
	}
}
