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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.search.FlagTerm;

public class ExtractAllMessages extends AbstractMailClient {
	private final DecimalFormat fmt = new DecimalFormat("000000");
	
	private static final int BUFFER_SIZE = 65536;
	
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
		String messageName = "message." + fmt.format(index);
		
		Object content = message.getContent();
		
		if (content instanceof Multipart) {
			Multipart mp = (Multipart)content;
			
			String filename = messageName + ".txt";
			
			PrintStream ps = new PrintStream(new FileOutputStream(new File(directory, filename)));
			
			int parts = mp.getCount();
			
			long totalBytes = 0;
			
			for (int j = 0; j < parts; j++) {
				Part part = mp.getBodyPart(j);
				ps.println("Part " + j + "\nContent-Type: " + part.getContentType() + "\nSize: " + part.getSize());
				ps.println("Content-Disposition: " + part.getDisposition());
				ps.println("Content-Description: " + part.getDescription());
				ps.println("Filename: " + part.getFileName());
				
				if (part instanceof MimePart)
					ps.println("Content-ID: " + ((MimePart)part).getContentID());
				
				String partFilename = messageName + "." + j;
				
				File partFile = new File(directory, partFilename);
				
				FileOutputStream fos = new FileOutputStream(partFile);
				
				InputStream is = part.getInputStream();
				
				copyFile(is,fos);

				is.close();
				
				fos.close();
				
				long bytes = partFile.length();
				
				totalBytes += bytes;
				
				ps.println("Written (" + bytes + " bytes) to file " + partFile.getAbsolutePath());
			}
			
			ps.close();
			
			System.out.println("Wrote message " + index + " (" + totalBytes + " bytes) in " + parts + " parts.");
		} else {
			String filename = messageName + ".msg";
			
			File file = new File(directory, filename);
			
			FileOutputStream fos = new FileOutputStream(file);
			
			message.writeTo(fos);
			
			fos.close();
			
			long bytes = file.length();
			
			System.out.println("Wrote message " + index + " (" + bytes + " bytes) to file " + file.getAbsolutePath());			
		}
	}
	
	private void copyFile(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		
		int bytes;
		
		while ((bytes = is.read(buffer)) > 0) {
			os.write(buffer, 0, bytes);
		}
	}
}
