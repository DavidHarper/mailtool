package com.obliquity.mailtool;

import java.io.IOException;
import java.net.URISyntaxException;

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
				System.out.println("Folder created: " + event.getFolder().getName());
			}
			
			public void folderRenamed(FolderEvent event) {
				System.out.println("Folder renamed: " + event.getFolder().getName() + " became " + event.getNewFolder().getName());
			}
			
			public void folderDeleted(FolderEvent event) {
				System.out.println("Folder deleted: " + event.getFolder().getName());
			}
		});

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
}
