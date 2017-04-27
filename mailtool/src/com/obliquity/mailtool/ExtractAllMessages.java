package com.obliquity.mailtool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

public class ExtractAllMessages extends AbstractMailClient {
	private final DecimalFormat fmt = new DecimalFormat("000000");
	
	public ExtractAllMessages(String folderURI) throws URISyntaxException, MessagingException {
		super(folderURI);
	}

	public static void main(String[] args) {
		String folderURI = null;
		String dirname = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-uri"))
				folderURI = args[++i];
			else if (args[i].equalsIgnoreCase("-dir"))
				dirname = args[++i];
			else {
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}
 				
		}
		
		ExtractAllMessages extractor = null;
		
		try {
			extractor = new ExtractAllMessages(folderURI);
			
			extractor.run(dirname);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void run(String dirname) throws Exception {
		try {
			Folder folder = getMainFolder();
			
			int flags = folder.getType();
			
			if ((flags & Folder.HOLDS_MESSAGES) == 0)
				throw new Exception("Folder " + folder.getFullName() + " cannot contain messages.");
			
			File directory = new File(dirname);
			
			if (!directory.exists())
				directory.mkdirs();
			
			if (!directory.isDirectory())
				throw new Exception(dirname + " is not a directory");
			
			FlagTerm notDeleted = new FlagTerm(new Flags(Flags.Flag.DELETED), false);
			
			folder.open(Folder.READ_ONLY);
			
			Message[] messages = folder.search(notDeleted);
			
			Arrays.sort(messages, new MessageDateComparator());
			
			for (int i = 0; i < messages.length; i++)
				copyMessage((MimeMessage)messages[i], directory, i);

			folder.close(false);
		} catch (MessagingException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void copyMessage(MimeMessage message, File directory, int index) throws IOException, MessagingException {
		String filename = "message." + fmt.format(index);
		
		File file = new File(directory, filename);
		
		FileOutputStream fos = new FileOutputStream(file);
		
		message.writeTo(fos);
		
		fos.close();
		
		long bytes = file.length();
		
		System.out.println("Wrote message " + index + " (" + bytes + " bytes) to file " + file.getAbsolutePath());
	}
}
