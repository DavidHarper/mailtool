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

package com.obliquity.mailtool.routerlogs;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import com.obliquity.mailtool.MessageHandler;

public class RouterLogMessageHandler implements MessageHandler {
	private RouterLogParser parser = new RouterLogParser();
	
	public void handleMessage(Message message) throws MessagingException, IOException {
		InputStream is = null;
		
		Object content = message.getContent();
		
		if (content instanceof Multipart) {
			Multipart mp = (Multipart)content;
			
			Part part = mp.getBodyPart(0);
			
			is = part.getInputStream();
		} else {
			is = message.getInputStream();
		}

		try {
			parser.parseContent(is);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
