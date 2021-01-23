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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
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
		boolean deleteDuplicates = false;
		String digestName = "MD5";
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-uri":
				folderURI = args[++i];
				break;
				
			case "-digest":
			case "-digestname":
			case "-algorithm":
				digestName = args[++i];
				break;
				
			case "-deleteduplicates":
				deleteDuplicates = true;
				break;
				
			default:	
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}		
		}
		
		MessageChecksumClient checksummer = null;
		
		try {
			MessageDigest digester = MessageDigest.getInstance(digestName);
			
			checksummer = new MessageChecksumClient(folderURI);
			
			checksummer.run(digester, deleteDuplicates);
		} catch (NoSuchAlgorithmException | URISyntaxException | MessagingException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void run(MessageDigest digester, boolean deleteDuplicates) throws IOException  {
		try {
			Folder folder = getMainFolder();
			
			int flags = folder.getType();
			
			if ((flags & Folder.HOLDS_MESSAGES) == 0) {
				System.err.println("Folder " + folder.getFullName() + " cannot contain messages.");
				return;
			}
						
			FlagTerm notDeleted = new FlagTerm(new Flags(Flags.Flag.DELETED), false);
			
			folder.open(deleteDuplicates? Folder.READ_WRITE : Folder.READ_ONLY);
			
			Message[] messages = folder.search(notDeleted);
			
			Arrays.sort(messages, new MessageDateComparator());
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			Map<String, Message> messageMap = new HashMap<String, Message>();
			
			String algorithm = digester.getAlgorithm();
			
			int deleted = 0;

			for (Message message : messages) {
				displayMessage(message, System.out);
				
				baos.reset();
				
				message.writeTo(baos);
				
				byte[] bytes = baos.toByteArray();
				
				digester.reset();
				
				byte[] digest = digester.digest(bytes);
				
				String digestString = toHexString(digest);
				
				boolean alreadySeen = messageMap.containsKey(digestString);
				
				System.out.print(algorithm + ": " + digestString);
				
				if (alreadySeen) {
					System.out.print(" [ALREADY SEEN");
					
					if (deleteDuplicates) {
						message.setFlag(Flag.DELETED, true);
						System.out.print(", FLAGGED FOR DELETION");
						deleted++;
					}
					
					System.out.print("]");
				} else
					messageMap.put(digestString,  message);
				
				System.out.println();
				System.out.println();
			}
			
			if (deleteDuplicates)
				System.out.println("\nSUMMARY\n\n" + messages.length + " messages were found, " + deleted + " were marked as duplicates");
			
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
