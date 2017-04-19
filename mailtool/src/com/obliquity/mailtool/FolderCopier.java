package com.obliquity.mailtool;

import java.net.URISyntaxException;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

public class FolderCopier extends AbstractMailClient {
	public FolderCopier(String fromURI) throws URISyntaxException, MessagingException {
		super(fromURI);
	}

	public static void main(String[] args) throws URISyntaxException, MessagingException {
		String fromURI = null;
		String toURI = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-from":
					fromURI = args[++i];
					break;

				case "-to":
					toURI = args[++i];
					break;

				default:
					System.err.println("Unrecognised option: " + args[i]);
					System.exit(1);
					;
			}
		}

		if (fromURI == null || toURI == null) {
			System.err.println("You must supply both -from and -to URIs");
			System.exit(2);
		}

		FolderCopier copier = new FolderCopier(fromURI);

		try {
			copier.copy(toURI);
		} catch (URISyntaxException | MessagingException e) {
			e.printStackTrace();
		}
	}

	public void copy(String toURI)
			throws URISyntaxException, MessagingException {
		Folder fromFolder = getMainFolder();
		
		Folder toFolder = connectToFolder(toURI);

		System.out.println("Source folder has " + fromFolder.getMessageCount()
				+ " messages");

		System.out.println("Destination folder has "
				+ toFolder.getMessageCount() + " messages");

		if (Boolean.getBoolean("reallycopy")) {
			fromFolder.open(Folder.READ_ONLY);
			toFolder.open(Folder.READ_WRITE);
			
			Message[] messages = fromFolder.getMessages();

			fromFolder.copyMessages(messages, toFolder);

			System.out.println("Source folder now has "
					+ fromFolder.getMessageCount() + " messages");

			System.out.println("Destination folder now has "
					+ toFolder.getMessageCount() + " messages");
		}
	}
}
