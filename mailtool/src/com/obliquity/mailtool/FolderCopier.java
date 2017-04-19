package com.obliquity.mailtool;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

public class FolderCopier {
	public static void main(String[] args) {
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

		FolderCopier copier = new FolderCopier();

		try {
			copier.copy(fromURI, toURI);
		} catch (URISyntaxException | MessagingException e) {
			e.printStackTrace();
		}
	}

	public void copy(String fromURI, String toURI)
			throws URISyntaxException, MessagingException {
		Folder fromFolder = getFolder(fromURI);

		Folder toFolder = getFolder(toURI);

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

	private Folder getFolder(String folderURI)
			throws URISyntaxException, MessagingException {
		URI uri = new URI(folderURI);

		String scheme = uri.getScheme();
		String user = uri.getUserInfo();
		String host = uri.getHost();
		int port = uri.getPort();
		String path = uri.getPath();

		if (path.startsWith("/"))
			path = path.substring(1);

		Properties props = new Properties();

		props.put("mail." + scheme + ".host", host);
		props.put("mail." + scheme + ".user", user);

		if (port > 0)
			props.put("mail." + scheme + ".port", port);

		Authenticator auth = new SimpleAuthenticator(user);

		Session session = Session.getInstance(props, auth);

		boolean debug = Boolean.getBoolean("debug");

		session.setDebug(debug);

		Store store = session.getStore(scheme);

		store.connect();

		return store.getFolder(path);
	}
}
