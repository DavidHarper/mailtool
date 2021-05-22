-- mailtool - a package for processing IMAP mail folders
--
-- Copyright (C) 2017 David Harper at obliquity.com
-- 
-- This library is free software; you can redistribute it and/or
-- modify it under the terms of the GNU Library General Public
-- License as published by the Free Software Foundation; either
-- version 2 of the License, or (at your option) any later version.
--
-- This library is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
-- Library General Public License for more details.
-- 
-- You should have received a copy of the GNU Library General Public
-- License along with this library; if not, write to the
-- Free Software Foundation, Inc., 59 Temple Place - Suite 330,
-- Boston, MA  02111-1307, USA.
--
-- See the COPYING file located in the top-level-directory of
-- the archive of this library for complete text of license.

CREATE TABLE `folder` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB;

CREATE TABLE `message` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `folder_id` int(10) unsigned NOT NULL,
  `from` varchar(255) NOT NULL,
  `to` varchar(255) DEFAULT NULL,
  `sent_date` datetime DEFAULT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `size` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `folder_id` (`folder_id`),
  CONSTRAINT `message_ibfk_1` FOREIGN KEY (`folder_id`) REFERENCES `folder` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `attachment` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `message_id` int(10) unsigned NOT NULL,
  `mime_type` varchar(255) NOT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `size` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `message_id` (`message_id`),
  CONSTRAINT `attachment_ibfk_1` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`)
) ENGINE=InnoDB;