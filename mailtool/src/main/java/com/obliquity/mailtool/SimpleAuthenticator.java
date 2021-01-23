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

import java.awt.*;
import javax.swing.*;

import java.io.Console;
import java.net.*;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class SimpleAuthenticator extends Authenticator {
	String username;
	String password;

    public SimpleAuthenticator(String username) {
		this.username = username;
	}

	protected PasswordAuthentication getPasswordAuthentication() {
		String envPassword = System.getenv("IMAP_PASSWORD");
		
		if (envPassword != null)
			return new PasswordAuthentication(username, envPassword);
		
		// given a prompt?
		String prompt = getRequestingPrompt();
		if (prompt == null)
			prompt = "Please login...";

		// protocol
		String protocol = getRequestingProtocol();
		if (protocol == null)
			protocol = "Unknown protocol";

		// get the host
		String host = null;
		InetAddress inet = getRequestingSite();
		if (inet != null)
			host = inet.getHostName();
		if (host == null)
			host = "Unknown host";

		// port
		String port = "";
		int portnum = getRequestingPort();
		if (portnum != -1)
			port = ", port " + portnum + " ";

		// Build the info string
		String info = "Connecting to " + protocol + " mail service on host "
				+ host + port;

		if (GraphicsEnvironment.isHeadless()) {
			System.out.println(info);
			
			Console console = System.console();
			
			System.out.println("Logging in as " + username);
			
			char[] pwchars = console.readPassword("Password: ", username);
			
			return new PasswordAuthentication(username, new String(pwchars));
		} else {
			PasswordAuthenticationPanel panel = new PasswordAuthenticationPanel(info, prompt, username); 

			int result = JOptionPane.showConfirmDialog(null, panel, "Login",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (result == JOptionPane.OK_OPTION)
				return new PasswordAuthentication(panel.getUsername(), new String(panel.getPassword()));
			else
				return null;
		}
	}

	class PasswordAuthenticationPanel extends JPanel {
		private GridBagLayout layout;
		private JPasswordField password;
		private JTextField txtUsername;
		
		PasswordAuthenticationPanel(String info, String prompt, String username) {
			super(null);
			buildUI(info, prompt, username);
		}
		
		private void buildUI(String info, String prompt, String username) {
			layout = new GridBagLayout();
			setLayout(layout);
			
			GridBagConstraints c = new GridBagConstraints();

			c.insets = new Insets(2, 2, 2, 2);

			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 0.0;
			add(constrain(new JLabel(info), layout, c));
			add(constrain(new JLabel(prompt), layout, c));

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.EAST;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			add(constrain(new JLabel("Username:"), layout, c));

			c.anchor = GridBagConstraints.EAST;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1.0;

			if (username == null)
			    username = getDefaultUserName();

			txtUsername = new JTextField(username, 20);
			add(constrain(txtUsername, layout, c));

			c.gridwidth = 1;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.EAST;
			c.weightx = 0.0;
			add(constrain(new JLabel("Password:"), layout, c));

			c.anchor = GridBagConstraints.EAST;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1.0;
			password = new JPasswordField("", 20);
			add(constrain(password, layout, c));
		}
		
		private Component constrain(Component cmp, GridBagLayout gb,
				GridBagConstraints c) {
			gb.setConstraints(cmp, c);
			return (cmp);
		}
		
		String getUsername() {
			return txtUsername.getText();
		}
		
		char[] getPassword() {
			return password.getPassword();
		}
	}
}
