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
import java.net.URISyntaxException;
import java.util.Date;

import javax.mail.*;
import javax.mail.event.*;

public class FolderMonitor extends AbstractMailClient {
	private static final long SLEEP_TIME = 5000;

	public FolderMonitor(String folderURI) throws MessagingException, URISyntaxException {
		super(folderURI);
	}
	
	public static void main(String[] args) {
		String folderURI = args[0];
		
		FolderMonitor monitor = null;
		
		try {
			monitor = new FolderMonitor(folderURI);
		} catch (MessagingException | URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		monitor.run();
	}

	private void run() {
		try {
			monitorFolder(getMainFolder());

			getStore().close();
		} catch (MessagingException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}
	
	private void monitorFolder(Folder folder) throws MessagingException {
		if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
			folder.open(Folder.READ_ONLY);

			folder.addMessageCountListener(new MessageCountAdapter() {
				public void messagesAdded(MessageCountEvent event) {
					Message[] messages = event.getMessages();

					showDate();
					
					System.out.println("Got " + messages.length
							+ " new messages");

					for (int i = 0; i < messages.length; i++) {
						System.out.println("Message " + i + ":");
						try {
							displayMessage(messages[i], System.out);
						} catch (MessagingException | IOException e) {
							e.printStackTrace();
						}
					}
				}

				public void messagesRemoved(MessageCountEvent event) {
					Message[] messages = event.getMessages();

					showDate();
					
					System.out.println("Removed " + messages.length
							+ " messages, isRemoved returns "
							+ event.isRemoved());

					for (int i = 0; i < messages.length; i++) {
						System.out.println("Message " + i + ":");
						try {
							if (messages[i].isExpunged())
								System.out.println("*** EXPUNGED ***");
							else
								displayMessage(messages[i], System.out);
						} catch (MessagingException | IOException e) {
							e.printStackTrace();
						}
					}
				}

			});

			folder.addMessageChangedListener(new MessageChangedListener() {
				public void messageChanged(MessageChangedEvent event) {
					Message message = event.getMessage();

					showDate();
					
					System.out
							.println("Flags changed on this message (change type = "
									+ event.getMessageChangeType() + "):");

					try {
						displayMessage(message, System.out);
					} catch (MessagingException | IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
		
		folder.addFolderListener(new FolderAdapter() {
			public void folderCreated(FolderEvent event) {
				showDate();
				System.out.println("Folder created: " + event.getFolder().getName());
			}
			
			public void folderRenamed(FolderEvent event) {
				showDate();
				System.out.println("Folder renamed: " + event.getFolder().getName() + " became " + event.getNewFolder().getName());
			}
			
			public void folderDeleted(FolderEvent event) {
				showDate();
				System.out.println("Folder deleted: " + event.getFolder().getName());
			}
		});
		
		System.out.println("Watching folder " + folder.getFullName());

		while (true) {
			int type = folder.getType();
			
			boolean containsMessages = (type & Folder.HOLDS_MESSAGES) != 0;
			
			boolean containsFolders = (type & Folder.HOLDS_FOLDERS) != 0;
			
		    try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		    if (containsMessages)
		    	folder.getMessageCount();
		    
		    if (containsFolders)
		    	folder.list();
		}
	}
	
	private void showDate() {
		Date now = new Date();
		System.out.println("\n\n***** TIMESTAMP: " + now + " *****\n");
	}
}
