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

package com.obliquity.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnectionManager {
	public static Connection getConnection(Object target) throws DatabaseConnectionManagerException, IOException, ClassNotFoundException, SQLException  {
		String className = target.getClass().getName().toLowerCase();
		
		String propsFilePropertyName = className + ".dbprops";
		
		String propsFileName = System.getProperty(propsFilePropertyName);
		
		if (propsFileName == null)
			throw new DatabaseConnectionManagerException("Database connection filename was not specified via property " + propsFilePropertyName);
		
		File propsFile = new File(propsFileName);
				
		return getConnection(propsFile);
	}
	
	public static Connection getConnection(File propsFile) throws IOException, ClassNotFoundException, SQLException {
		InputStream is = new FileInputStream(propsFile);

		Properties props = new Properties();
		props.load(is);
		is.close();
				
		return getConnection(props);
	}
	
	public static Connection getConnection(Properties props) throws ClassNotFoundException, SQLException  {
		String url = props.getProperty("url");
		String username = props.getProperty("username");
		String password = props.getProperty("password");
		
		String driver = props.getProperty("driver");
		
		if (driver != null)
			Class.forName(driver);
		
		return DriverManager.getConnection(url, username, password);
	}

}
