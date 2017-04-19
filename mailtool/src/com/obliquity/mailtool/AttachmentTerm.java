package com.obliquity.mailtool;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

public class AttachmentTerm extends SearchTerm {
	private String mimeType;
	
	public AttachmentTerm(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public boolean match(Message msg) {
		if (!(msg instanceof MimeMessage))
			return false;
		
		MimeMessage message = (MimeMessage)msg;
		
		try {
			Object content = message.getContent();
			
			if (content instanceof Multipart) {
				Multipart mp = (Multipart)content;
				
				int parts = mp.getCount();
				
				for (int j = 0; j < parts; j++) {
					Part part = mp.getBodyPart(j);
					
					if (part.isMimeType(mimeType))
						return true;
				}
			}
		}
		catch (MessagingException me) {
			me.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return false;
	}

}
