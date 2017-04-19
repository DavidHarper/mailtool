package com.obliquity.mailtool;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

public class ListFolders extends AbstractMailClient {
	public static void main(String[] args) {
		String protocol = "imap";
		String host = null;
		int port = 0;
		String user = null;
		String folderName = null;
		boolean counters = false;
		
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
				folderName = args[++i];
			else if (args[i].equalsIgnoreCase("-counters"))
				counters = true;
		}
		
		if (user == null || host == null) {
			System.err.println("You must specify -user, -host and -folder");
			System.exit(1);
		}
		
		ListFolders client = null;
		
		try {
			client = new ListFolders(user, host, port, protocol);
		} catch (MessagingException e) {
			e.printStackTrace();
			System.exit(1);
		}

		client.run(folderName, counters);
	}

	private void run(String folderName, boolean counters) {
		try {
			Store store = getStore();

			Folder folder = folderName == null ? store.getDefaultFolder() : store.getFolder(folderName);

			processFolder(folder, counters);

			store.close();
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

	public ListFolders(String user, String host, int port, String protocol)
			throws MessagingException {
		super(user, host, port, protocol);
	}

}
