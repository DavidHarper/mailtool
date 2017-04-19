package com.obliquity.mailtool;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import javax.mail.search.OrTerm;
import javax.mail.search.RecipientTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.SubjectTerm;
import javax.mail.search.SizeTerm;


public class SearchClient extends AbstractMailClient {
	public static void main(String[] args) {
		String protocol = "imap";
		String host = null;
		String user = null;
		int port = 0;
		String folder = null;
		String sender = null;
		String senderLike = null;
		String recipient = null;
		String mimeType = null;
		Date after = null;
		Date before = null;
		String subject = null;
		int largerThan = 0;
		boolean unread = false;
		boolean recursive = false;
		boolean deleted = false;
		boolean purge = false;
		boolean quiet = false;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		
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
			else if (args[i].equalsIgnoreCase("-sender"))
				sender = args[++i];
			else if (args[i].equalsIgnoreCase("-senderlike"))
				senderLike = args[++i];
			else if (args[i].equalsIgnoreCase("-recipient"))
				recipient = args[++i];
			else if (args[i].equalsIgnoreCase("-mimetype"))
				mimeType = args[++i];
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
			else if (args[i].equalsIgnoreCase("-purge"))
				purge = true;
			else if (args[i].equalsIgnoreCase("-help")) {
				printUsage(System.err, null);
				System.exit(0);
			} else {
				printUsage(System.err, "Unknown option: " + args[i]);
				System.exit(1);
			}
		}
		
		if (user == null || host == null) {
			printUsage(System.err, "A mandatory argument is missing");
			System.exit(1);
		}
		
		if (recursive && purge) {
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
		
		if (unread)
			term = addUnreadTerm(term);
		
		if (deleted)
			term = addDeletedTerm(term);
		
		if (largerThan > 0)
			term = addLargerThanTerm(term, largerThan);
		
		if (term == null) {
			printUsage(System.err, "No search terms were specified");
			System.exit(1);
		}
			
		try {
			SearchClient client = new SearchClient(user, host, port, protocol);
			
			client.run(folder, term, recursive, quiet, purge);
		} catch (MessagingException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private static String HELP_TEXT[] = {
		"MANDATORY ARGUMENTS",
		"\t-host\t\tThe name of the IMAP server",
		"\t-user\t\tThe name of the IMAP user",
		"",
		"OPTIONAL ARGUMENTS",
		"\t-port\t\tThe port number of the IMAP server",
		"",
		"\t-imap\t\t[BOOLEAN] Use a non-secure connection",
		"\t-imaps\t\t[BOOLEAN] Use a secure connection",
		"",
		"\t-folder\t\tComma-separated list of the folders to be searched",
		"\t-recursive\t[BOOLEAN] Search all sub-folders recursively",
		"",
		"\t-quiet\t\tDisplay only a summary of the messages",
		"",
		"\t-purge\t\t[BOOLEAN] Mark all matching messages for deletion",
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
		"\t-after\t\tShow only messages sent on or after this date",
		"\t-before\t\tShow only messages sent on or before this date",
		"",
		"\t-older\t\tOnly show messages which are older than this number of days",
		"\t-newer\t\tonly show messages which are newer than this number of days",
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

	public SearchClient(String user, String host, int port, String protocol) throws MessagingException {
		super(user, host, port, protocol);
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

	private static SearchTerm addLargerThanTerm(SearchTerm term, int largerThan) {
		SizeTerm sizeTerm = new SizeTerm(SizeTerm.GT, largerThan);

		return (term == null) ? sizeTerm : new AndTerm(term, sizeTerm);
	}

	public void run(String folderName, SearchTerm term, boolean recursive, boolean quiet, boolean purge) {
		try {
			Store store = getStore();
			
			String[] folders = (folderName == null) ? null : folderName.split(",");
			
			if (folders == null) {
				Folder folder = store.getDefaultFolder();
				
				processFolder(folder, term, recursive, quiet, purge);

			} else {
				for (String f : folders) {
					Folder folder = store.getFolder(f);
					
					processFolder(folder, term, recursive, quiet, purge);
				}
			}

			store.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void processFolder(Folder folder, SearchTerm term, boolean recursive, boolean quiet, boolean purge) throws Exception {
		if (recursive && purge)
			throw new Exception("The purge and recursive options are mutually exclusive, to avoid disasters!");
		
		try {
			System.out.println("Searching folder " + folder.getFullName() + "\n");

			int type = folder.getType();

			if ((type & Folder.HOLDS_MESSAGES) != 0)
				processMessages(folder, term, quiet, purge);

			if ((type & Folder.HOLDS_FOLDERS) != 0 && recursive)
				processSubFolders(folder, term, quiet);
			
		} catch (MessagingException e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void processMessages(Folder folder, SearchTerm term, boolean quiet, boolean purge) throws MessagingException, IOException {
		try {
			folder.open(purge ? Folder.READ_WRITE : Folder.READ_ONLY);
		}
		catch (MessagingException e) {
			System.err.println("***** Failed to open " + folder.getFullName() + " : " + e.getMessage() + "\n");
			return;
		}
		
		int counter = 0;
		
		Message[] messages = folder.search(term);
		
		if (messages != null) {
			for (int i = 0; i < messages.length; i++) {
				if (quiet) {
					counter++;
				} else {
					System.out.println("Message " + i + ":");
					displayMessage(messages[i]);
				}
				
				if (purge)
					messages[i].setFlag(Flags.Flag.DELETED, true);
			}
		}
		
		folder.close(purge);
		
		if (quiet)
			System.out.println("Messages " + (purge ? "purged" : "found") + " : " + counter);
	}
	
	private void processSubFolders(Folder folder, SearchTerm term, boolean quiet)
			throws Exception {
		Folder[] subfolders = folder.list();
		
		for (int i = 0; i < subfolders.length; i++)
			processFolder(subfolders[i], term, true, quiet, false);
	}

}
