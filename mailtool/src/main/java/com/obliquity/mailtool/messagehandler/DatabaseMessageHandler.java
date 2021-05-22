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

package com.obliquity.mailtool.messagehandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.obliquity.database.DatabaseConnectionManager;
import com.obliquity.database.DatabaseConnectionManagerException;
import com.obliquity.mailtool.MessageHandler;

public class DatabaseMessageHandler implements MessageHandler {
	private Connection conn;
	private Map<String, Integer> folderIDMap = new HashMap<String, Integer>();
	
	public DatabaseMessageHandler() throws ClassNotFoundException, DatabaseConnectionManagerException, IOException, SQLException {
		conn = DatabaseConnectionManager.getConnection(this);
		prepareStatements();
		conn.setAutoCommit(false);
	}
	
	private PreparedStatement pstmtGetFolderIDbyName, pstmtPutNewFolder, pstmtPutMessage, pstmtPutAttachment;
	
	private void prepareStatements() throws SQLException {
		String sql = "select id from folder where name = ?";
		
		pstmtGetFolderIDbyName = conn.prepareStatement(sql);
		
		sql = "insert into folder(name) values (?)";
		
		pstmtPutNewFolder = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		
		sql = "insert into message(folder_id, `from`, `to`, sent_date, subject, `size`) values (?,?,?,?,?,?)";
		
		pstmtPutMessage = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		
		sql = "insert into attachment(message_id, mime_type, filename, `size`) values (?,?,?,?)";
		
		pstmtPutAttachment = conn.prepareStatement(sql);
	}
	
	private int getFolderIDbyName(String folderName) throws SQLException {
		if (folderIDMap.containsKey(folderName))
			return folderIDMap.get(folderName);
		
		pstmtGetFolderIDbyName.setString(1, folderName);
		
		ResultSet rs = pstmtGetFolderIDbyName.executeQuery();
		
		if (rs.next()) {
			int folderID = rs.getInt(1);
			
			rs.close();
			
			folderIDMap.put(folderName, folderID);
			
			return folderID;
		}
		
		pstmtPutNewFolder.setString(1, folderName);
		
		int rows = pstmtPutNewFolder.executeUpdate();
		
		if (rows == 1) {
			rs = pstmtPutNewFolder.getGeneratedKeys();
			
			rs.next();
			
			int folderID = rs.getInt(1);
			
			rs.close();
			
			folderIDMap.put(folderName, folderID);
			
			conn.commit();
			
			return folderID;
		}
		
		return -1;
	}

	@Override
	public void handleMessage(Message message) throws MessagingException, IOException {
		try {
			Folder folder = message.getFolder();
			
			int folderID = getFolderIDbyName(folder.getFullName());
			
			InternetAddress from = (InternetAddress)message.getFrom()[0];
			
			Address[] to_list = message.getAllRecipients();
			InternetAddress to = to_list == null ? null : (InternetAddress)to_list[0];
			
			Date sentDate = message.getSentDate();
			
			String subject = message.getSubject();
			
			int size = (message instanceof MimeMessage) ? ((MimeMessage)message).getSize() : -1;
			
			pstmtPutMessage.setInt(1, folderID);
			
			pstmtPutMessage.setString(2, from.getAddress());
			
			if (to == null)
				pstmtPutMessage.setNull(3, Types.VARCHAR);
			else
				pstmtPutMessage.setString(3, to.getAddress());
						
			pstmtPutMessage.setTimestamp(4, new Timestamp(sentDate.getTime()));
			
			pstmtPutMessage.setString(5, subject);
			
			pstmtPutMessage.setInt(6, size);
			
			int rows = pstmtPutMessage.executeUpdate();
			
			int messageID = -1;
			
			if (rows == 1) {
				ResultSet rs = pstmtPutMessage.getGeneratedKeys();
				
				rs.next();
				
				messageID = rs.getInt(1);
				
				rs.close();
			}
			
			Object content = message.getContent();
			
			if (content instanceof Multipart) {
				Multipart mp = (Multipart)content;
				
				int parts = mp.getCount();
				
				for (int j = 0; j < parts; j++) {
					Part part = mp.getBodyPart(j);
					
					String contentType = part.getContentType();
					
					String filename = part.getFileName();
					
					size = part.getSize();
					
					pstmtPutAttachment.setInt(1, messageID);
					pstmtPutAttachment.setString(2, contentType);
					pstmtPutAttachment.setString(3, filename);
					pstmtPutAttachment.setInt(4, size);
					
					pstmtPutAttachment.executeUpdate();
				}
			}
			
			conn.commit();
		} catch (SQLException e) {
			throw new MessagingException("A database exception occurred", e);
		}
		
		
	}

}
