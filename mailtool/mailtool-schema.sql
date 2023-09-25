DROP TABLE IF EXISTS `attachment`;
DROP TABLE IF EXISTS `recipient`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `address`;
DROP TABLE IF EXISTS `folder`;

CREATE TABLE `folder` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `address` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  address varchar(255) not null,
  PRIMARY KEY (`id`),
  KEY `address` (`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `message` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `folder_id` int unsigned NOT NULL,
  `from` varchar(255) DEFAULT NULL,
  `sent_date` datetime DEFAULT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `size` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `from` (`from`),
  KEY `sent_date` (`sent_date`),
  KEY `folder_id` (`folder_id`),
  CONSTRAINT `message_ibfk_1` FOREIGN KEY (`folder_id`) REFERENCES `folder` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `recipient` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `message_id` int unsigned NOT NULL,
  `address_id` int unsigned NOT NULL,
  `type` enum('TO','CC','BCC') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `message_id` (`message_id`),
  CONSTRAINT `recipient_ibfk_1` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`),
  CONSTRAINT `recipient_ibfk_2` FOREIGN KEY (`address_id`) REFERENCES `address` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `attachment` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `message_id` int unsigned NOT NULL,
  `mime_type` varchar(255) DEFAULT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `size` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `message_id` (`message_id`),
  CONSTRAINT `attachment_ibfk_1` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
