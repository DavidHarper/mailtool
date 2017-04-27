package com.obliquity.mailtool;

import java.util.Comparator;

import javax.mail.Message;
import javax.mail.MessagingException;

public class MessageDateComparator implements Comparator<Message>{
	public int compare(Message msg1, Message msg2) {
		try {
			return msg1.getSentDate().compareTo(msg2.getSentDate());
		} catch (MessagingException e) {
			e.printStackTrace();
			return 0;
		}
	}
}
