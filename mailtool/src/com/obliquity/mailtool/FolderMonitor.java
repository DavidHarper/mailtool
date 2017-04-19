package com.obliquity.mailtool;

import java.io.IOException;

import javax.mail.*;
import javax.mail.event.*;

public class FolderMonitor extends AbstractMailClient {
	private static final long SLEEP_TIME = 5000;

	public FolderMonitor(String user, String host, int port, String protocol) throws MessagingException {
		super(user, host, port, protocol);
	}
	
	public static void main(String[] args) {
		String protocol = "imap";
		String host = null;
		String user = null;
		int port = 0;
		String folder = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-imap"))
				protocol = "imap";
			else if (args[i].equalsIgnoreCase("-imaps"))
				protocol = "imaps";
			else if (args[i].equalsIgnoreCase("-host"))
				host = args[++i];
			else if (args[i].equalsIgnoreCase("-user"))
				user = args[++i];
			else if (args[i].equalsIgnoreCase("-port"))
				port = Integer.parseInt(args[++i]);
			else if (args[i].equalsIgnoreCase("-folder"))
				folder = args[++i];
		}
		
		if (user == null || host == null || folder == null) {
			System.err.println("You must specify -user, -host and -folder");
			System.exit(1);
		}
		
		FolderMonitor monitor = null;
		
		try {
			monitor = new FolderMonitor(user, host, port, protocol);
		} catch (MessagingException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		monitor.run(folder);
	}

	private void run(String folderName) {
		try {
			Store store = getStore();

			Folder folder = store.getFolder(folderName);

			monitorFolder(folder);

			store.close();
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
							displayMessage(messages[i]);
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
								displayMessage(messages[i]);
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
						displayMessage(message);
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
