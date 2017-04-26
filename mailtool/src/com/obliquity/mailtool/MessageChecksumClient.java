package com.obliquity.mailtool;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.security.MessageDigest;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.FlagTerm;

public class MessageChecksumClient extends AbstractMailClient {
	public MessageChecksumClient(String folderURI) throws URISyntaxException, MessagingException {
		super(folderURI);
	}
	public static void main(String[] args) {
		String folderURI = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-uri"))
				folderURI = args[++i];
			else {
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}
 				
		}
		
		MessageChecksumClient checksummer = null;
		
		try {
			checksummer = new MessageChecksumClient(folderURI);
			
			checksummer.run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void run() throws Exception {
		try {
			Folder folder = getMainFolder();
			
			int flags = folder.getType();
			
			if ((flags & Folder.HOLDS_MESSAGES) == 0)
				throw new Exception("Folder " + folder.getFullName() + " cannot contain messages.");
						
			FlagTerm notDeleted = new FlagTerm(new Flags(Flags.Flag.DELETED), false);
			
			folder.open(Folder.READ_ONLY);
			
			Message[] messages = folder.search(notDeleted);

			MessageDigest digester = MessageDigest.getInstance("MD5");
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			for (Message message : messages) {
				displayMessage(message, System.out);
				
				baos.reset();
				
				message.writeTo(baos);
				
				byte[] bytes = baos.toByteArray();
				
				digester.reset();
				
				byte[] digest = digester.digest(bytes);
				
				System.out.print("Digest:");
				
				for (int i = 0; i < digest.length; i++) {
					if ((i%4) == 0)
						System.out.print(' ');
						
					System.out.format(" %02X", digest[i]);
				}
				
				System.out.println();
				System.out.println();
			}
			
			folder.close(false);
		} catch (MessagingException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
