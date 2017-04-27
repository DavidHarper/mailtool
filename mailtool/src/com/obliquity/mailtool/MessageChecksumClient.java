package com.obliquity.mailtool;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
			
			Arrays.sort(messages, new MessageDateComparator());

			MessageDigest digester = MessageDigest.getInstance("MD5");
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			Map<String, Message> messageMap = new HashMap<String, Message>();

			for (Message message : messages) {
				displayMessage(message, System.out);
				
				baos.reset();
				
				message.writeTo(baos);
				
				byte[] bytes = baos.toByteArray();
				
				digester.reset();
				
				byte[] digest = digester.digest(bytes);
				
				String digestString = toHexString(digest);
				
				boolean alreadySeen = messageMap.containsKey(digestString);
				
				System.out.println ("Digest: " + digestString + (alreadySeen ? " [ALREADY SEEN]" : ""));
				
				if (!alreadySeen)
					messageMap.put(digestString,  message);
				
				System.out.println();
			}
			
			folder.close(false);
		} catch (MessagingException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/*
	 * The following code was borrowed from StackOverflow
	 * http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java/9855338#9855338
	 */
	
	private final char[] HEXADECIMAL_DIGITS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	private String toHexString(byte[] bytes) {    
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j*2] = HEXADECIMAL_DIGITS[v >>> 4];
	        hexChars[j*2 + 1] = HEXADECIMAL_DIGITS[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
