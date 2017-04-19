package com.obliquity.mailtool;

import java.net.URISyntaxException;

import javax.mail.Folder;
import javax.mail.MessagingException;

public class ListFolders extends AbstractMailClient {
	public static void main(String[] args) {
		String folderURI = null;
		boolean counters = false;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-uri"))
				folderURI = args[++i];
			else if (args[i].equalsIgnoreCase("-counters"))
				counters = true;
		}
		
		if (folderURI ==  null) {
			System.err.println("You must specify -uri");
			System.exit(1);
		}
		
		ListFolders client = null;
		
		try {
			client = new ListFolders(folderURI);
		} catch (MessagingException | URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}

		client.run(counters);
	}

	private void run(boolean counters) {
		try {
			processFolder(getMainFolder(), counters);

			getStore().close();
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

	public ListFolders(String folderURI)
			throws MessagingException, URISyntaxException {
		super(folderURI);
	}

}
