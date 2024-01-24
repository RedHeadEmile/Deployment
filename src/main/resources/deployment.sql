DROP TABLE IF EXISTS `service`;
CREATE TABLE IF NOT EXISTS `service` (
  `name` varchar(100) NOT NULL,
  `deployedAt` datetime(6) DEFAULT NULL,
  `compilationTime` bigint unsigned DEFAULT NULL,
  `deploymentTime` bigint unsigned DEFAULT NULL,
  `message` longtext,
  `status` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB;
