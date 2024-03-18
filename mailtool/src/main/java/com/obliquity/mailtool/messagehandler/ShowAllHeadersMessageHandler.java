/*
 * mailtool - a package for processing IMAP mail folders
 *
 * Copyright (C) 2024 David Harper at obliquity.com
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

package com.obliquity.mailtool.messagehandler;

import java.io.IOException;
import java.util.Enumeration;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.obliquity.mailtool.MessageHandler;

public class ShowAllHeadersMessageHandler implements MessageHandler {
	public void handleMessage(Message message) throws MessagingException, IOException {
		System.out.println("================================================================================");
		
		System.out.println("HEADERS FOR MESSAGE " + message.getMessageNumber() + " in folder " + message.getFolder().getFullName());
		
		Enumeration<Header> headers = message.getAllHeaders();
		
		while (headers.hasMoreElements()) {
			Header header = headers.nextElement();
			
			System.out.println("NAME:  " + header.getName());
			System.out.println("VALUE: " + header.getValue());
			System.out.println();
		}
	}

}
