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

import java.util.Comparator;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;

public class MessageDateComparator implements Comparator<Message>{
	public int compare(Message msg1, Message msg2) {
		try {
			Date date1 = getMessageDate(msg1);
			Date date2 = getMessageDate(msg2);
			
			return date1 != null && date2 != null ? date1.compareTo(date2) : 0;
		} catch (MessagingException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	private Date getMessageDate(Message msg) throws MessagingException {
		Date date = msg.getSentDate();
		
		return date != null ? date : msg.getReceivedDate();
	}
}
