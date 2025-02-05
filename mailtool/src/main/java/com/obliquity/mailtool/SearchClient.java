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
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AndTerm;
import javax.mail.search.DateTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.MessageIDTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.RecipientTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.SubjectTerm;

import com.obliquity.mailtool.messagehandler.SimpleMessageHandler;

import javax.mail.search.SizeTerm;


public class SearchClient extends AbstractMailClient {
	private static final String DANGER_MODE= "dangerMode";
	
	private final MessageDateComparator comparator = new MessageDateComparator();
	
	private boolean recursive = false;
	private boolean purge = false;
	private boolean quiet = false;
	private boolean sort = false;
	private Folder copyToFolder = null;

	private MessageHandler messageHandler = null;
	
	public static void main(String[] args) {
		String folderURI = null;
		String folderList = null;
		String sender = null;
		String senderLike = null;
		String recipient = null;
		String mimeType = null;
		String messageid = null;
		Date after = null;
		Date before = null;
		String subject = null;
		int largerThan = 0;
		boolean unread = false;
		boolean recursive = false;
		boolean deleted = false;
		boolean purge = false;
		boolean tabular = false;
		boolean quiet = false;
		boolean sort = false;
		String copyToFolderName = null;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String handlerName = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-uri"))
				folderURI = args[++i];
			else if (args[i].equalsIgnoreCase("-folders"))
				folderList = args[++i];
			else if (args[i].equalsIgnoreCase("-sender"))
				sender = args[++i];
			else if (args[i].equalsIgnoreCase("-senderlike"))
				senderLike = args[++i];
			else if (args[i].equalsIgnoreCase("-recipient"))
				recipient = args[++i];
			else if (args[i].equalsIgnoreCase("-mimetype"))
				mimeType = args[++i];
			else if (args[i].equalsIgnoreCase("-messageid"))
				messageid = args[++i];
			else if (args[i].equalsIgnoreCase("-after"))
				try {
					after = format.parse(args[++i]);
				} catch (ParseException e) {
					System.err.println("Failed to parse -after argument: " + e.getMessage());
				}
			else if (args[i].equalsIgnoreCase("-before"))
				try {
					before = format.parse(args[++i]);
				} catch (ParseException e) {
					System.err.println("Failed to parse -before argument: " + e.getMessage());
				}
			else if (args[i].equalsIgnoreCase("-older"))
				try {
					int days = Integer.parseInt(args[++i]);
						
					long milliseconds = (long)days * 86400000L;
						
					Date now  = new Date();
						
					before = new Date(now.getTime() - milliseconds);
				} catch (NumberFormatException e) {
					System.err.println("Failed to parse -older argument: " + e.getMessage());
				}
			else if (args[i].equalsIgnoreCase("-newer"))
				try {
					int days = Integer.parseInt(args[++i]);
					
					long milliseconds = (long)days * 86400000L;
					
					Date now  = new Date();
					
					after = new Date(now.getTime() - milliseconds);
				} catch (NumberFormatException e) {
					System.err.println("Failed to parse -newer argument: " + e.getMessage());
				}
			else if (args[i].equalsIgnoreCase("-subject"))
				subject = args[++i];
			else if (args[i].equalsIgnoreCase("-largerthan"))
				largerThan = Integer.parseInt(args[++i]);
			else if (args[i].equalsIgnoreCase("-unread"))
				unread = true;
			else if (args[i].equalsIgnoreCase("-deleted"))
				deleted = true;
			else if (args[i].equalsIgnoreCase("-recursive"))
				recursive = true;
			else if (args[i].equalsIgnoreCase("-quiet"))
				quiet = true;
			else if (args[i].equalsIgnoreCase("-sort"))
				sort = true;
			else if (args[i].equalsIgnoreCase("-purge"))
				purge = true;
			else if (args[i].equalsIgnoreCase("-tabular"))
				tabular = true;
			else if (args[i].equalsIgnoreCase("-copyto"))
				copyToFolderName = args[++i];
			else if (args[i].equalsIgnoreCase("-handler"))
				handlerName = args[++i];
			else if (args[i].equalsIgnoreCase("-help")) {
				printUsage(System.err, null);
				System.exit(0);
			} else {
				printUsage(System.err, "Unknown option: " + args[i]);
				System.exit(1);
			}
		}
		
		if (folderURI == null) {
			printUsage(System.err, "A mandatory argument is missing");
			System.exit(1);
		}
		
		if (recursive && purge && !Boolean.getBoolean(DANGER_MODE)) {
			printUsage(System.err, "For safety, you can't use the -purge option with the -recursive option.");
			System.exit(1);
		}
		
		SearchTerm term = null;
		
		if (sender != null)
			term = addSenderTerm(term, sender);
		
		if (senderLike != null)
			term = addSenderLikeTerm(term, senderLike);
		
		if (recipient != null)
			term = addRecipientTerm(term, recipient);
		
		if (subject != null)
			term = addSubjectTerm(term, subject);
		
		if (mimeType != null)
			term = addMimeTypeTerm(term, mimeType);

		if (after != null)
			term = addDateTerm(term, after, DateTerm.GE);
				
		if (before != null) 
			term = addDateTerm(term, before, DateTerm.LE);
		
		if (messageid != null)
			term = addMessageIDTerm(term, messageid);
		
		if (unread)
			term = addUnreadTerm(term);
		
		if (deleted)
			term = addDeletedTerm(term);
		else
			term = addNotDeletedTerm(term);
		
		if (largerThan > 0)
			term = addLargerThanTerm(term, largerThan);
		
		if (term == null) {
			printUsage(System.err, "No search terms were specified");
			System.exit(1);
		}
		
		MessageHandler handler = null;
		
		if (handlerName != null) {
			try {
				Class<?> handlerClass = Class.forName(handlerName);

				Object o = handlerClass.getDeclaredConstructor().newInstance();

				if (o instanceof MessageHandler)
					handler = (MessageHandler) o;
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else if (!quiet)
			handler = new SimpleMessageHandler();

		try {		
			SearchClient client = new SearchClient(folderURI);
			
			client.setRecursive(recursive);
			client.setQuiet(quiet);
			client.setPurge(purge);
			client.setSort(sort);
			
			if (tabular && handler instanceof SimpleMessageHandler)
				((SimpleMessageHandler)handler).setTabular(true);
							
			if (handler != null)
				client.setHandler(handler);
			
			if (copyToFolderName != null)
				client.setMoveToFolder(copyToFolderName);
			
			client.run(folderList, term);
		} catch (MessagingException | URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static String HELP_TEXT[] = {
		"MANDATORY ARGUMENTS",
		"\t-uri\t\tThe URI of the IMAP server",
		"",
		"OPTIONAL ARGUMENTS",
		"\t-folders\tComma-separated list of the folders to be searched",
		"",
		"\t-recursive\t[BOOLEAN] Search all sub-folders recursively",
		"",
		"\t-quiet\t\t[BOOLEAN] Display only a summary of the messages",
		"",
		"\t-purge\t\t[BOOLEAN] Mark all matching messages for deletion",
		"",
		"\t-tabular\t[BOOLEAN] Display message headers in tab-separated format",
		"",
		"\t-copyto\t\tCopy all matching messages to the named folder",
		"",
		"\t-handler\tName of the class which will process matching messages [default: com.obliquity.mailtool.messagehandler.SimpleMessageHandler]",
		"",
		"NOTE THAT -purge CANNOT BE USED WITH -recursive",
		"",
		"SEARCH CRITERIA",
		"\t-sender\t\tShow only messages from this sender",
		"\t-recipient\tShow only messages with this user in the To or CC fields",
		"",
		"\t-senderlike\tShow only messages where the sender string contains this substring",
		"",
		"\t-subject\tShow only messages with this subject",
		"",
		"\t-after\t\tShow only messages sent on or after this date and time",
		"\t-before\t\tShow only messages sent on or before this date and time",
		"",
		"\t-older\t\tOnly show messages which are older than this number of days",
		"\t-newer\t\tonly show messages which are newer than this number of days",
		"",
		"\t-messageid\tShow only messages with this Message-ID",
		"",
		"\t-mimetype\tShow only messages which contain a MIME attachment of this type",
		"\t-largerthan\tShow only messageslarger than this (in bytes)",
		"",
		"\t-unread\t\t[BOOLEAN] Show only unread messages",
		"\t-deleted\t[BOOLEAN] Show only deleted messages"
	};

	private static void printUsage(PrintStream ps, String message) {
		if (message != null)
			ps.println("WARNING: " + message + "\n\n");

		for (int i = 0; i < HELP_TEXT.length; i++)
			ps.println(HELP_TEXT[i]);
	}

	public SearchClient(String folderURI) throws MessagingException, URISyntaxException {
		super(folderURI);
	}
	
	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}
	
	public boolean isQuiet() {
		return quiet;
	}
	
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}
	
	public boolean isRecursive() {
		return recursive;
	}
	
	public void setPurge(boolean purge) {
		this.purge = purge;
	}
	
	public boolean isPurge() {
		return purge;
	}
	
	public void setSort(boolean sort) {
		this.sort = sort;
	}
	
	public boolean isSort() {
		return sort;
	}

	public void setHandler(MessageHandler handler) {
		this.messageHandler = handler;
	}
	
	public void setMoveToFolder(String copyToFolderName) throws MessagingException {
		Store store = getStore();
		
		Folder folder = store.getFolder(copyToFolderName);
		
		if (!folder.exists())
			folder.create(Folder.HOLDS_MESSAGES);
		
		this.copyToFolder = folder;
	}
	
	private static SearchTerm addSenderTerm(SearchTerm term, String sender) {
		SearchTerm senderTerm = null;
		Address senderAddress;

		try {
			senderAddress = new InternetAddress(sender);

			senderTerm = new FromTerm(senderAddress);
		} catch (AddressException e) {
			e.printStackTrace();
		}

		return (term == null) ? senderTerm : new AndTerm(term, senderTerm);
	}
	
	private static SearchTerm addSenderLikeTerm(SearchTerm term, String senderLike) {
		SearchTerm senderLikeTerm = new FromStringTerm(senderLike);

		return (term == null) ? senderLikeTerm : new AndTerm(term, senderLikeTerm);
	}

	private static SearchTerm addRecipientTerm(SearchTerm term, String recipient) {
		Address recipientAddress;
		SearchTerm recipientTerm = null;

		try {
			recipientAddress = new InternetAddress(recipient);

			SearchTerm toTerm = new RecipientTerm(RecipientType.TO,
					recipientAddress);
			SearchTerm ccTerm = new RecipientTerm(RecipientType.CC,
					recipientAddress);

			recipientTerm = new OrTerm(toTerm, ccTerm);
		} catch (AddressException e) {
			e.printStackTrace();
		}

		return (term == null) ? recipientTerm
				: new AndTerm(term, recipientTerm);
	}

	private static SearchTerm addSubjectTerm(SearchTerm term, String subject) {
		SearchTerm subjectTerm = new SubjectTerm(subject);

		return (term == null) ? subjectTerm : new AndTerm(term, subjectTerm);
	}

	private static SearchTerm addMimeTypeTerm(SearchTerm term, String mimeType) {
		SearchTerm mimeTypeTerm = new AttachmentTerm(mimeType);

		return (term == null) ? mimeTypeTerm : new AndTerm(term, mimeTypeTerm);
	}

	private static SearchTerm addDateTerm(SearchTerm term, Date date, int comparison) {
		DateTerm dateTerm = new SentDateTerm(comparison, date);
		
		return (term == null) ? dateTerm : new AndTerm(term, dateTerm);
	}

	private static SearchTerm addUnreadTerm(SearchTerm term) {
		FlagTerm flagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

		return (term == null) ? flagTerm : new AndTerm(term, flagTerm);
	}

	private static SearchTerm addDeletedTerm(SearchTerm term) {
		FlagTerm flagTerm = new FlagTerm(new Flags(Flags.Flag.DELETED), true);

		return (term == null) ? flagTerm : new AndTerm(term, flagTerm);
	}

	private static SearchTerm addNotDeletedTerm(SearchTerm term) {
		FlagTerm flagTerm = new FlagTerm(new Flags(Flags.Flag.DELETED), false);

		return (term == null) ? flagTerm : new AndTerm(term, flagTerm);
	}

	private static SearchTerm addLargerThanTerm(SearchTerm term, int largerThan) {
		SizeTerm sizeTerm = new SizeTerm(SizeTerm.GT, largerThan);

		return (term == null) ? sizeTerm : new AndTerm(term, sizeTerm);
	}
	
	private static SearchTerm addMessageIDTerm(SearchTerm term, String messageid) {
		MessageIDTerm messageIDTerm = new MessageIDTerm(messageid);
		
		return (term == null) ? messageIDTerm : new AndTerm(term, messageIDTerm);
	}

	public void run(String folderName, SearchTerm term) {
		try {
			Store store = getStore();
			
			String[] folders = (folderName == null) ? null : folderName.split(",");
			
			if (folders == null) {
				if (purge)
					throw new Exception("The purge option requires that you specify one or more folder names explicitly, to avoid disasters!");
				
				setPurge(false);
				
				Folder folder = store.getDefaultFolder();
				
				processFolder(folder, term);
			} else {
				for (String f : folders) {
					Folder folder = store.getFolder(f);
					
					processFolder(folder, term);
				}
			}

			store.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void processFolder(Folder folder, SearchTerm term) throws Exception {
		if (recursive && purge && !Boolean.getBoolean(DANGER_MODE))
			throw new Exception("The purge and recursive options are mutually exclusive, to avoid disasters!");
		
		try {
			if (!quiet && messageHandler == null)
				System.out.println("Searching folder " + folder.getFullName() + "\n");

			int type = folder.getType();

			if ((type & Folder.HOLDS_MESSAGES) != 0)
				processMessages(folder, term);

			if ((type & Folder.HOLDS_FOLDERS) != 0 && recursive)
				processSubFolders(folder, term);
			
		} catch (MessagingException e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void processMessages(Folder folder, SearchTerm term) throws MessagingException, IOException {
		try {
			folder.open(purge ? Folder.READ_WRITE : Folder.READ_ONLY);
		}
		catch (MessagingException e) {
			System.err.println("***** Failed to open " + folder.getFullName() + " : " + e.getMessage() + "\n");
			return;
		}
		
		int counter = 0;
		
		Message[] messages = folder.search(term);
		
		if (sort)
			Arrays.sort(messages, comparator);
		
		if (messages != null) {
			if (copyToFolder != null)
				folder.copyMessages(messages, copyToFolder);
			
			for (int i = 0; i < messages.length; i++) {
				if (quiet) {
					counter++;
				} else if (messageHandler != null) {
					messageHandler.handleMessage(messages[i]);
				} else {
					System.out.println("Message " + i + ":");
					displayMessage(messages[i], System.out);
					System.out.println();
				}
				
				if (purge)
					messages[i].setFlag(Flags.Flag.DELETED, true);
			}				
		}
		
		folder.close(purge);
		
		if (quiet)
			System.out.println("Messages " + (purge ? "purged" : "found") + " : " + counter);
	}
	
	private void processSubFolders(Folder folder, SearchTerm term)
			throws Exception {
		Folder[] subfolders = folder.list();
		
		for (int i = 0; i < subfolders.length; i++)
			processFolder(subfolders[i], term);
	}

}
