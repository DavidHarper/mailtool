package com.obliquity.mailtool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

public abstract class AbstractMailClient {
	private static final String DEFAULT_PROTOCOL = "imaps";

	private Store store;
	
	public AbstractMailClient(String user, String host, int port, String protocol) throws MessagingException {
		if (protocol == null)
			protocol = DEFAULT_PROTOCOL;
		
		Properties props = new Properties();

		InputStream is = getClass().getResourceAsStream("imap.props");

		if (is != null) {
			try {
				props.load(is);
				is.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		String trustedHosts = System.getProperty("trustedhosts");
		
		if (trustedHosts != null) {
			props.put("mail." + protocol + ".ssl.trust", trustedHosts);
			System.err.println("Trusting hosts: " + trustedHosts);
		}
		
		props.put("mail." + protocol + ".host", host);		
		props.put("mail." + protocol + ".user", user);
		
		if (port > 0)
			props.put("mail." + protocol + ".port", port);
		String username = props.getProperty("mail." + protocol + ".user");

		Authenticator auth = new SimpleAuthenticator(username);

		Session session = Session.getInstance(props, auth);
		
		boolean debug = Boolean.getBoolean("debug");
		
		session.setDebug(debug);

		store = session.getStore(protocol);

		store.connect();
	}
	
	protected Store getStore() {
		return store;
	}

	protected void displayMessage(Message message) throws MessagingException, IOException {
		Address[] to = message.getRecipients(Message.RecipientType.TO);
		
		System.out.println("From:    " + message.getFrom()[0]);
		
		if (to != null) {
			System.out.print("To:      ");
			for (int i = 0; i < to.length; i++) {
				if (i > 0)
					System.out.print(", ");
				System.out.print(to[i]);
			}
			System.out.println();
		}
		
		System.out.println("Date:    " + message.getSentDate());
		
		System.out.println("Subject: " + message.getSubject());
		
		String flags = flagsToString(message);

		if (flags != null)
			System.out.println("Flags:   " + flags);
		
		if (message instanceof MimeMessage) {
			MimeMessage msg = (MimeMessage)message;
			
			int messageSize = msg.getSize();
			System.out.println("Size:    " + messageSize);

			Object content = message.getContent();
			
			if (content instanceof Multipart) {
				Multipart mp = (Multipart)content;
				
				int parts = mp.getCount();
					System.out.println("Parts:   " + parts);
				
				for (int j = 0; j < parts; j++) {
					Part part = mp.getBodyPart(j);
					System.out.println("\tPart " + j + ": Content-Type=" + part.getContentType() + "; Length=" + part.getSize());
				}

			}
		}
		
		System.out.println();
	}
	
	protected String flagsToString(Message message) throws MessagingException {
		String str = null;
		
		str = setOrAppend(str, message, Flags.Flag.ANSWERED, "ANSWERED");
		
		str = setOrAppend(str, message, Flags.Flag.DELETED, "DELETED");
		
		str = setOrAppend(str, message, Flags.Flag.DRAFT, "DRAFT");
		
		str = setOrAppend(str, message, Flags.Flag.RECENT, "RECENT");
		
		str = setOrAppend(str, message, Flags.Flag.SEEN, "SEEN");
		
		return str;
	}
	
	private String setOrAppend(String str, Message message, Flag flag, String text) throws MessagingException {
		if (message.isSet(flag)) {
			if (str == null)
				str = text;
			else
				str += " | " + text;;			
		}
		
		return str;
	}
}
