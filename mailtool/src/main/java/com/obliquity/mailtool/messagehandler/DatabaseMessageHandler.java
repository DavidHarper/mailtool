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
import javax.mail.Message.RecipientType;
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
	private Map<String, Integer> addressIDMap = new HashMap<String, Integer>();
	private boolean debug = false;
	private SimpleMessageHandler debugHandler = new SimpleMessageHandler();
	
	public DatabaseMessageHandler() throws ClassNotFoundException, DatabaseConnectionManagerException, IOException, SQLException {
		conn = DatabaseConnectionManager.getConnection(this);
		prepareStatements();
		conn.setAutoCommit(false);
		
		String debugPropertyName = this.getClass().getName().toLowerCase() + ".debug";
		
		debug = Boolean.getBoolean(debugPropertyName);
		
		debugHandler.setTabular(true);
		
		debugHandler.setPrintStream(System.err);
	}
	
	private PreparedStatement pstmtGetFolderIDbyName, pstmtPutNewFolder, pstmtPutMessage, pstmtPutAttachment, pstmtPutRecipient;
	private PreparedStatement pstmtGetAddressIDbyName, pstmtPutNewAddress;
	
	private void prepareStatements() throws SQLException {
		String sql = "select id from folder where name = ?";
		
		pstmtGetFolderIDbyName = conn.prepareStatement(sql);
		
		sql = "insert into folder(name) values (?)";
		
		pstmtPutNewFolder = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		
		sql = "select id from address where address = ?";
		
		pstmtGetAddressIDbyName = conn.prepareStatement(sql);
		
		sql = "insert into address(address) values (?)";
		
		pstmtPutNewAddress = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		
		sql = "insert into message(folder_id, `from`, sent_date, subject, `size`) values (?,?,?,?,?)";
		
		pstmtPutMessage = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		
		sql = "insert into attachment(message_id, mime_type, filename, `size`) values (?,?,?,?)";
		
		pstmtPutAttachment = conn.prepareStatement(sql);
		
		sql = "insert into recipient(message_id, address_id, type) values(?, ?, ?)";
		
		pstmtPutRecipient = conn.prepareStatement(sql);
	}
	
	private int getFolderIDbyName(String folderName) throws SQLException {
		return findOrSetIDbyName(pstmtGetFolderIDbyName, pstmtPutNewFolder, folderIDMap, folderName);
	}

	private int getAddressIDbyName(String address) throws SQLException {
		return findOrSetIDbyName(pstmtGetAddressIDbyName, pstmtPutNewAddress, addressIDMap, address);
	}
	
	private int findOrSetIDbyName(PreparedStatement pstmtLookup, PreparedStatement pstmtSave, Map<String, Integer> nameToIDmap, String name)
			throws SQLException {
		if (nameToIDmap.containsKey(name))
			return nameToIDmap.get(name);
		
		pstmtLookup.setString(1, name);
		
		ResultSet rs = pstmtLookup.executeQuery();
		
		if (rs.next()) {
			int ID = rs.getInt(1);
			
			rs.close();
			
			nameToIDmap.put(name, ID);
			
			return ID;
		}
		
		pstmtSave.setString(1, name);
		
		int rows = pstmtSave.executeUpdate();
		
		if (rows == 1) {
			rs = pstmtSave.getGeneratedKeys();
			
			rs.next();
			
			int ID = rs.getInt(1);
			
			rs.close();
			
			nameToIDmap.put(name, ID);
			
			conn.commit();
			
			return ID;
		}
		
		return -1;
	}
	
	private void putRecipients(int messageID, Address[] recipients, String recipientType) throws SQLException {
		if (recipients == null)
			return;
		
		for (Address address : recipients) {
			if (address != null) {
				pstmtPutRecipient.setInt(1, messageID);
				
				int addressID = getAddressIDbyName(((InternetAddress)address).getAddress());
			
				pstmtPutRecipient.setInt(2, addressID);
			
				pstmtPutRecipient.setString(3, recipientType);
			
				pstmtPutRecipient.executeUpdate();
			}
		}
	}

	@Override
	public void handleMessage(Message message) throws MessagingException, IOException {
		if (debug)
			debugHandler.handleMessage(message);
		
		try {
			Folder folder = message.getFolder();
			
			int folderID = getFolderIDbyName(folder.getFullName());
			
			Address[] from_list = message.getFrom();
			
			InternetAddress from = from_list == null ? null : ((InternetAddress[])from_list)[0];
			
			Address[] toRecipients = message.getRecipients(RecipientType.TO);
			
			Address[] ccRecipients = message.getRecipients(RecipientType.CC);
			
			Address[] bccRecipients = message.getRecipients(RecipientType.BCC);
			
			Date sentDate = message.getSentDate();
			
			String subject = message.getSubject();
			
			int size = (message instanceof MimeMessage) ? ((MimeMessage)message).getSize() : -1;
			
			pstmtPutMessage.setInt(1, folderID);
			
			pstmtPutMessage.setString(2, from == null ? "NULL" : from.getAddress());

			if (sentDate == null)
				pstmtPutMessage.setNull(3, Types.TIMESTAMP);
			else
				pstmtPutMessage.setTimestamp(3, new Timestamp(sentDate.getTime()));
				
			pstmtPutMessage.setString(4, subject);
			
			pstmtPutMessage.setInt(5, size);
			
			int rows = pstmtPutMessage.executeUpdate();
			
			int messageID = -1;
			
			if (rows == 1) {
				ResultSet rs = pstmtPutMessage.getGeneratedKeys();
				
				rs.next();
				
				messageID = rs.getInt(1);
				
				rs.close();
			}
			
			Object content = message.getContent();
			
			putRecipients(messageID, toRecipients, "TO");
			
			putRecipients(messageID, ccRecipients, "CC");
			
			putRecipients(messageID, bccRecipients, "BCC");
			
			if (content instanceof Multipart) {
				Multipart mp = (Multipart)content;
				
				int parts = mp.getCount();
				
				for (int j = 0; j < parts; j++) {
					Part part = mp.getBodyPart(j);
					
					String contentType = part.getContentType();
					
					String mimeType = contentType.split(";")[0].strip();
					
					String filename = part.getFileName();
					
					size = part.getSize();
					
					// Hack for multipart/alternative parts generated by Mac Mail
					if (size < 0)
						size = 0;
					
					pstmtPutAttachment.setInt(1, messageID);
					pstmtPutAttachment.setString(2, mimeType);
					pstmtPutAttachment.setString(3, filename);
					pstmtPutAttachment.setInt(4, size);
					
					pstmtPutAttachment.executeUpdate();
				}
			}
			
			conn.commit();
		} catch (SQLException e) {
			if (!debug)
				debugHandler.handleMessage(message);
			
			e.printStackTrace();
			
			try {
				if (!conn.isValid(5)) {
					System.err.println("The database connection is no longer valid.  Bailing out.");
					System.exit(1);
				}
			} catch (SQLException e2) {
				throw new MessagingException("An SQLException occurred whilst handling a previous SQLException", e2);
			}
		}
	}
}
