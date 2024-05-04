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
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class SimpleMailSender {
	public static void main(String[] args) {
		String user = null;
		String sender = null;
		String recipient = null;
		String smtpHost = null;
		String filename = null;
		String subject = null;
		boolean attachFile = false;
		boolean noauth = false;
		boolean dryRun = false;

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-from":
			case "-sender":
				sender = args[++i];
				break;
				
			case "-user":
				user = args[++i];
				break;

			case "-to":
				recipient = args[++i];
				break;
				
			case "-subject":
				subject = args[++i];
				break;

			case "-server":
				smtpHost = args[++i];
				break;

			case "-file":
				filename = args[++i];
				break;

			case "-attach":
				filename = args[++i];
				attachFile = true;
				break;
				
			case "-noauth":
				noauth = true;
				break;
				
			case "-dry-run":
				dryRun = true;
				break;

			default:
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}
		}

		if (sender == null || recipient == null || smtpHost == null) {
			System.err.println("One or more of -from, -to or -server were not specified");
			System.exit(1);
		}
		
		if (user == null)
			user = sender;

		Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.auth", !noauth);
		props.put("mail.smtp.ssl.enable", "true");
		props.put("mail.smtp.port", "465");
		props.put("mail.smtp.ssl.protocols", "TLSv1.2");

		Authenticator auth = noauth ? null : new SimpleAuthenticator(user);

		Session session = Session.getDefaultInstance(props, auth);

		try {
			Message message = new MimeMessage(session);

			message.setFrom(new InternetAddress(sender));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
			message.setSubject(subject == null ? "(No subject)" : subject);
			message.setSentDate(new Date());
			message.setHeader("X-Mailer", "SimpleMailSender");

			String messageText = "Test Mail";

			if (filename != null) {
				MimeBodyPart mbp1 = new MimeBodyPart();
				mbp1.setText(messageText);

				MimeBodyPart mbp2 = new MimeBodyPart();

				if (attachFile) {
					mbp2.attachFile(filename);
				} else {
					FileDataSource fds = new FileDataSource(filename) {
						public String getContentType() {
							return "text/plain";
						}
					};

					mbp2.setDataHandler(new DataHandler(fds));
					mbp2.setFileName(fds.getName());
				}

				MimeMultipart mp = new MimeMultipart();
				mp.addBodyPart(mbp1);
				mp.addBodyPart(mbp2);
				message.setContent(mp);
			} else {
				message.setText(messageText);
			}
			
			
			if (dryRun) {
				System.out.println("========== MESSAGE BEGINS ==========");
				message.writeTo(System.out);
				System.out.println("\n========== MESSAGE ENDS ==========");
			} else
				Transport.send(message);

			System.out.println("Done");

		} catch (MessagingException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
