-- MySQL dump 10.13  Distrib 26.7.0, for Win64 (x86_64)
--
-- Host: localhost    Database: workforce_knowledge_intelligence
-- ------------------------------------------------------
-- Server version	26.7.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'd155ffbc-c5fb-11f1-a179-54e1adc3a6ec:1-227';

--
-- Current Database: `workforce_knowledge_intelligence`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `workforce_knowledge_intelligence` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `workforce_knowledge_intelligence`;

--
-- Table structure for table `certifications`
--

DROP TABLE IF EXISTS `certifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certifications` (
  `certification_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `certification_name` varchar(180) NOT NULL,
  `issuing_organization` varchar(180) DEFAULT NULL,
  `issue_date` date DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `credential_url` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`certification_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `certifications`
--

LOCK TABLES `certifications` WRITE;
/*!40000 ALTER TABLE `certifications` DISABLE KEYS */;
INSERT INTO `certifications` VALUES (1,'SQL Fundamentals Demo','Example Learning','2026-01-15',NULL,'https://example.com/sql','2026-10-12 05:33:07'),(2,'Java Foundations Demo','Example Learning','2026-02-10',NULL,'https://example.com/java','2026-10-12 05:33:07');
/*!40000 ALTER TABLE `certifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `competency_frameworks`
--

DROP TABLE IF EXISTS `competency_frameworks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competency_frameworks` (
  `framework_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `framework_name` varchar(150) NOT NULL,
  `description` text,
  `version` varchar(40) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`framework_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `competency_frameworks`
--

LOCK TABLES `competency_frameworks` WRITE;
/*!40000 ALTER TABLE `competency_frameworks` DISABLE KEYS */;
INSERT INTO `competency_frameworks` VALUES (1,'Software Engineer Core Skills','Required skills for software engineers','1.0',1,'2026-10-12 05:33:07','2026-10-12 05:33:07');
/*!40000 ALTER TABLE `competency_frameworks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `departments`
--

DROP TABLE IF EXISTS `departments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departments` (
  `department_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `department_name` varchar(120) NOT NULL,
  `description` text,
  `location` varchar(150) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`department_id`),
  UNIQUE KEY `department_name` (`department_name`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `departments`
--

LOCK TABLES `departments` WRITE;
/*!40000 ALTER TABLE `departments` DISABLE KEYS */;
INSERT INTO `departments` VALUES (1,'Engineering','Software development team','Hyderabad','2026-10-12 05:33:06','2026-10-12 05:33:06'),(2,'Human Resources','Employee development team','Hyderabad','2026-10-12 05:33:06','2026-10-12 05:33:06'),(3,'Data Analytics','Data analysis and reporting','Bengaluru','2026-10-12 05:33:06','2026-10-12 05:33:06');
/*!40000 ALTER TABLE `departments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `designations`
--

DROP TABLE IF EXISTS `designations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `designations` (
  `designation_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `designation_name` varchar(120) NOT NULL,
  `description` text,
  PRIMARY KEY (`designation_id`),
  UNIQUE KEY `designation_name` (`designation_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `designations`
--

LOCK TABLES `designations` WRITE;
/*!40000 ALTER TABLE `designations` DISABLE KEYS */;
INSERT INTO `designations` VALUES (1,'Software Engineer','Develops software'),(2,'Senior Software Engineer','Leads software development'),(3,'HR Analyst','Analyzes workforce data'),(4,'Data Analyst','Analyzes business data');
/*!40000 ALTER TABLE `designations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee_skills`
--

DROP TABLE IF EXISTS `employee_skills`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee_skills` (
  `employee_skill_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `employee_id` bigint unsigned NOT NULL,
  `skill_id` bigint unsigned NOT NULL,
  `proficiency_level` tinyint unsigned NOT NULL DEFAULT '1',
  `years_of_experience` decimal(4,1) NOT NULL DEFAULT '0.0',
  `certification_id` bigint unsigned DEFAULT NULL,
  `is_verified` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`employee_skill_id`),
  UNIQUE KEY `uq_employee_skill` (`employee_id`,`skill_id`),
  KEY `fk_employee_skills_skill` (`skill_id`),
  KEY `fk_employee_skills_certification` (`certification_id`),
  CONSTRAINT `fk_employee_skills_certification` FOREIGN KEY (`certification_id`) REFERENCES `certifications` (`certification_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_employee_skills_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`employee_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_employee_skills_skill` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`skill_id`),
  CONSTRAINT `chk_employee_skill_experience` CHECK ((`years_of_experience` >= 0)),
  CONSTRAINT `chk_employee_skill_proficiency` CHECK ((`proficiency_level` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee_skills`
--

LOCK TABLES `employee_skills` WRITE;
/*!40000 ALTER TABLE `employee_skills` DISABLE KEYS */;
INSERT INTO `employee_skills` VALUES (1,1,1,4,5.0,2,1,'2026-10-12 05:33:07'),(2,1,2,4,4.0,1,1,'2026-10-12 05:33:07'),(3,2,1,2,1.0,NULL,0,'2026-10-12 05:33:07'),(4,2,2,2,0.5,NULL,0,'2026-10-12 05:33:07'),(5,2,3,3,1.5,NULL,0,'2026-10-12 05:33:07'),(6,3,2,4,3.0,1,1,'2026-10-12 05:33:07'),(7,3,5,2,0.5,NULL,0,'2026-10-12 05:33:07'),(8,4,6,4,4.0,NULL,1,'2026-10-12 05:33:07');
/*!40000 ALTER TABLE `employee_skills` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employees`
--

DROP TABLE IF EXISTS `employees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employees` (
  `employee_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `department_id` bigint unsigned NOT NULL,
  `role_id` bigint unsigned NOT NULL,
  `designation_id` bigint unsigned NOT NULL,
  `manager_id` bigint unsigned DEFAULT NULL,
  `first_name` varchar(80) NOT NULL,
  `last_name` varchar(80) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `status` enum('ACTIVE','INACTIVE','ON_LEAVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`employee_id`),
  UNIQUE KEY `email` (`email`),
  KEY `fk_employees_department` (`department_id`),
  KEY `fk_employees_role` (`role_id`),
  KEY `fk_employees_designation` (`designation_id`),
  KEY `fk_employees_manager` (`manager_id`),
  CONSTRAINT `fk_employees_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`department_id`),
  CONSTRAINT `fk_employees_designation` FOREIGN KEY (`designation_id`) REFERENCES `designations` (`designation_id`),
  CONSTRAINT `fk_employees_manager` FOREIGN KEY (`manager_id`) REFERENCES `employees` (`employee_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_employees_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employees`
--

LOCK TABLES `employees` WRITE;
/*!40000 ALTER TABLE `employees` DISABLE KEYS */;
INSERT INTO `employees` VALUES (1,1,2,2,NULL,'Aarav','Sharma','aarav.sharma@example.com','DEMO_PLACEHOLDER','ACTIVE','2026-10-12 05:33:07','2026-10-12 05:33:07'),(2,1,1,1,1,'Diya','Reddy','diya.reddy@example.com','DEMO_PLACEHOLDER','ACTIVE','2026-10-12 05:33:07','2026-10-12 05:33:07'),(3,3,1,4,NULL,'Kabir','Mehta','kabir.mehta@example.com','DEMO_PLACEHOLDER','ACTIVE','2026-10-12 05:33:07','2026-10-12 05:33:07'),(4,2,4,3,NULL,'Ananya','Rao','ananya.rao@example.com','DEMO_PLACEHOLDER','ACTIVE','2026-10-12 05:33:07','2026-10-12 05:33:07');
/*!40000 ALTER TABLE `employees` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `knowledge_gaps`
--

DROP TABLE IF EXISTS `knowledge_gaps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_gaps` (
  `gap_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `employee_id` bigint unsigned NOT NULL,
  `skill_id` bigint unsigned NOT NULL,
  `required_level` tinyint unsigned NOT NULL,
  `current_level` tinyint unsigned NOT NULL DEFAULT '0',
  `gap_level` tinyint unsigned NOT NULL,
  `gap_severity` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'LOW',
  `status` enum('OPEN','IN_PROGRESS','RESOLVED','CLOSED') NOT NULL DEFAULT 'OPEN',
  `identified_date` date NOT NULL DEFAULT (curdate()),
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`gap_id`),
  UNIQUE KEY `uq_employee_skill_gap` (`employee_id`,`skill_id`),
  KEY `fk_knowledge_gaps_skill` (`skill_id`),
  CONSTRAINT `fk_knowledge_gaps_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`employee_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_knowledge_gaps_skill` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`skill_id`),
  CONSTRAINT `chk_gap_current` CHECK ((`current_level` between 0 and 5)),
  CONSTRAINT `chk_gap_level` CHECK ((`gap_level` between 0 and 5)),
  CONSTRAINT `chk_gap_required` CHECK ((`required_level` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `knowledge_gaps`
--

LOCK TABLES `knowledge_gaps` WRITE;
/*!40000 ALTER TABLE `knowledge_gaps` DISABLE KEYS */;
INSERT INTO `knowledge_gaps` VALUES (1,2,1,4,2,2,'HIGH','OPEN','2026-10-01','2026-10-12 05:33:08'),(2,2,2,4,2,2,'MEDIUM','OPEN','2026-10-01','2026-10-12 05:33:08'),(3,2,4,3,1,2,'HIGH','OPEN','2026-10-02','2026-10-12 05:33:08'),(4,3,5,3,2,1,'LOW','IN_PROGRESS','2026-10-03','2026-10-12 05:33:08');
/*!40000 ALTER TABLE `knowledge_gaps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `knowledge_sharing_sessions`
--

DROP TABLE IF EXISTS `knowledge_sharing_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_sharing_sessions` (
  `session_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `mentorship_id` bigint unsigned DEFAULT NULL,
  `title` varchar(180) NOT NULL,
  `description` text,
  `session_date` datetime NOT NULL,
  `duration_hours` decimal(5,2) NOT NULL DEFAULT '1.00',
  `recording_url` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`session_id`),
  KEY `fk_sessions_mentorship` (`mentorship_id`),
  CONSTRAINT `fk_sessions_mentorship` FOREIGN KEY (`mentorship_id`) REFERENCES `mentorships` (`mentorship_id`) ON DELETE SET NULL,
  CONSTRAINT `chk_session_duration` CHECK ((`duration_hours` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `knowledge_sharing_sessions`
--

LOCK TABLES `knowledge_sharing_sessions` WRITE;
/*!40000 ALTER TABLE `knowledge_sharing_sessions` DISABLE KEYS */;
INSERT INTO `knowledge_sharing_sessions` VALUES (1,1,'Java Problem-Solving Session','Practice Java and problem decomposition','2026-10-25 15:00:00',1.50,NULL,'2026-10-12 05:33:08'),(2,2,'SQL Reporting Basics','Learn joins and reporting queries','2026-10-28 11:00:00',1.00,NULL,'2026-10-12 05:33:08');
/*!40000 ALTER TABLE `knowledge_sharing_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `learning_paths`
--

DROP TABLE IF EXISTS `learning_paths`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `learning_paths` (
  `path_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(180) NOT NULL,
  `description` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` enum('DRAFT','ACTIVE','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
  PRIMARY KEY (`path_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `learning_paths`
--

LOCK TABLES `learning_paths` WRITE;
/*!40000 ALTER TABLE `learning_paths` DISABLE KEYS */;
INSERT INTO `learning_paths` VALUES (1,'Backend Foundations','Java, SQL, and REST API learning sequence','2026-10-12 05:33:08','ACTIVE'),(2,'Cloud Starter Path','Introductory cloud learning sequence','2026-10-12 05:33:08','ACTIVE');
/*!40000 ALTER TABLE `learning_paths` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mentorships`
--

DROP TABLE IF EXISTS `mentorships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mentorships` (
  `mentorship_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `mentor_id` bigint unsigned NOT NULL,
  `mentee_id` bigint unsigned NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `status` enum('PROPOSED','ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PROPOSED',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`mentorship_id`),
  KEY `idx_mentorship_mentor` (`mentor_id`),
  KEY `idx_mentorship_mentee` (`mentee_id`),
  CONSTRAINT `fk_mentorship_mentee` FOREIGN KEY (`mentee_id`) REFERENCES `employees` (`employee_id`),
  CONSTRAINT `fk_mentorship_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `employees` (`employee_id`),
  CONSTRAINT `chk_mentorship_dates` CHECK (((`end_date` is null) or (`end_date` >= `start_date`))),
  CONSTRAINT `chk_mentorship_different_people` CHECK ((`mentor_id` <> `mentee_id`))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mentorships`
--

LOCK TABLES `mentorships` WRITE;
/*!40000 ALTER TABLE `mentorships` DISABLE KEYS */;
INSERT INTO `mentorships` VALUES (1,1,2,'2026-10-15',NULL,'ACTIVE','2026-10-12 05:33:08'),(2,3,4,'2026-10-18',NULL,'PROPOSED','2026-10-12 05:33:08');
/*!40000 ALTER TABLE `mentorships` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `path_trainings`
--

DROP TABLE IF EXISTS `path_trainings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `path_trainings` (
  `path_training_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `path_id` bigint unsigned NOT NULL,
  `training_id` bigint unsigned NOT NULL,
  `sequence_no` int unsigned NOT NULL,
  `is_mandatory` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`path_training_id`),
  UNIQUE KEY `uq_path_sequence` (`path_id`,`sequence_no`),
  UNIQUE KEY `uq_path_training` (`path_id`,`training_id`),
  KEY `fk_path_trainings_training` (`training_id`),
  CONSTRAINT `fk_path_trainings_path` FOREIGN KEY (`path_id`) REFERENCES `learning_paths` (`path_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_path_trainings_training` FOREIGN KEY (`training_id`) REFERENCES `training_programs` (`training_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `path_trainings`
--

LOCK TABLES `path_trainings` WRITE;
/*!40000 ALTER TABLE `path_trainings` DISABLE KEYS */;
INSERT INTO `path_trainings` VALUES (1,1,1,1,1),(2,1,2,2,1),(3,1,3,3,0),(4,2,4,1,1);
/*!40000 ALTER TABLE `path_trainings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_competencies`
--

DROP TABLE IF EXISTS `role_competencies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_competencies` (
  `role_competency_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `framework_id` bigint unsigned NOT NULL,
  `skill_id` bigint unsigned NOT NULL,
  `required_level` tinyint unsigned NOT NULL,
  `is_mandatory` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_competency_id`),
  UNIQUE KEY `uq_framework_skill` (`framework_id`,`skill_id`),
  KEY `fk_role_competencies_skill` (`skill_id`),
  CONSTRAINT `fk_role_competencies_framework` FOREIGN KEY (`framework_id`) REFERENCES `competency_frameworks` (`framework_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_competencies_skill` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`skill_id`),
  CONSTRAINT `chk_role_competency_level` CHECK ((`required_level` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_competencies`
--

LOCK TABLES `role_competencies` WRITE;
/*!40000 ALTER TABLE `role_competencies` DISABLE KEYS */;
INSERT INTO `role_competencies` VALUES (1,1,1,4,1,'2026-10-12 05:33:07'),(2,1,2,4,1,'2026-10-12 05:33:07'),(3,1,3,3,0,'2026-10-12 05:33:07'),(4,1,4,3,1,'2026-10-12 05:33:07'),(5,1,5,2,0,'2026-10-12 05:33:07'),(6,1,6,3,1,'2026-10-12 05:33:07');
/*!40000 ALTER TABLE `role_competencies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `role_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `role_name` varchar(80) NOT NULL,
  `description` text,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'EMPLOYEE','Regular employee'),(2,'MANAGER','Manages a team'),(3,'ADMIN','Platform administrator'),(4,'TRAINING_COORDINATOR','Coordinates training');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `session_participants`
--

DROP TABLE IF EXISTS `session_participants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session_participants` (
  `participant_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `session_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `role` enum('HOST','MENTOR','MENTEE','ATTENDEE') NOT NULL DEFAULT 'ATTENDEE',
  `joined_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`participant_id`),
  UNIQUE KEY `uq_session_participant` (`session_id`,`user_id`),
  KEY `fk_participants_employee` (`user_id`),
  CONSTRAINT `fk_participants_employee` FOREIGN KEY (`user_id`) REFERENCES `employees` (`employee_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_participants_session` FOREIGN KEY (`session_id`) REFERENCES `knowledge_sharing_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `session_participants`
--

LOCK TABLES `session_participants` WRITE;
/*!40000 ALTER TABLE `session_participants` DISABLE KEYS */;
INSERT INTO `session_participants` VALUES (1,1,1,'MENTOR','2026-10-12 05:33:09'),(2,1,2,'MENTEE','2026-10-12 05:33:09'),(3,2,3,'HOST','2026-10-12 05:33:09'),(4,2,4,'ATTENDEE','2026-10-12 05:33:09');
/*!40000 ALTER TABLE `session_participants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `skill_categories`
--

DROP TABLE IF EXISTS `skill_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skill_categories` (
  `category_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `category_name` varchar(120) NOT NULL,
  `description` text,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `category_name` (`category_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `skill_categories`
--

LOCK TABLES `skill_categories` WRITE;
/*!40000 ALTER TABLE `skill_categories` DISABLE KEYS */;
INSERT INTO `skill_categories` VALUES (1,'Programming','Programming languages'),(2,'Data','Databases and analytics'),(3,'Cloud','Cloud computing and deployment'),(4,'Professional','Communication and teamwork');
/*!40000 ALTER TABLE `skill_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `skills`
--

DROP TABLE IF EXISTS `skills`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skills` (
  `skill_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `skill_name` varchar(120) NOT NULL,
  `description` text,
  `category_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`skill_id`),
  UNIQUE KEY `skill_name` (`skill_name`),
  KEY `fk_skills_category` (`category_id`),
  CONSTRAINT `fk_skills_category` FOREIGN KEY (`category_id`) REFERENCES `skill_categories` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `skills`
--

LOCK TABLES `skills` WRITE;
/*!40000 ALTER TABLE `skills` DISABLE KEYS */;
INSERT INTO `skills` VALUES (1,'Java','Java programming',1),(2,'SQL','Database queries',2),(3,'React','Frontend development',1),(4,'REST API Design','API development',1),(5,'AWS Basics','Cloud fundamentals',3),(6,'Communication','Professional communication',4);
/*!40000 ALTER TABLE `skills` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `training_enrollments`
--

DROP TABLE IF EXISTS `training_enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `training_enrollments` (
  `enrollment_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `employee_id` bigint unsigned NOT NULL,
  `training_id` bigint unsigned NOT NULL,
  `enrollment_date` date NOT NULL DEFAULT (curdate()),
  `status` enum('ENROLLED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'ENROLLED',
  `completion_date` date DEFAULT NULL,
  `score` decimal(5,2) DEFAULT NULL,
  `feedback` text,
  `certificate_url` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `uq_employee_training` (`employee_id`,`training_id`),
  KEY `fk_training_enrollments_training` (`training_id`),
  CONSTRAINT `fk_training_enrollments_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`employee_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_training_enrollments_training` FOREIGN KEY (`training_id`) REFERENCES `training_programs` (`training_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_enrollment_score` CHECK (((`score` is null) or (`score` between 0 and 100)))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `training_enrollments`
--

LOCK TABLES `training_enrollments` WRITE;
/*!40000 ALTER TABLE `training_enrollments` DISABLE KEYS */;
INSERT INTO `training_enrollments` VALUES (1,2,1,'2026-10-10','ENROLLED',NULL,NULL,'Practice Java fundamentals',NULL,'2026-10-12 05:33:08'),(2,2,2,'2026-10-10','IN_PROGRESS',NULL,NULL,'Practicing SQL joins',NULL,'2026-10-12 05:33:08'),(3,3,4,'2026-10-11','ENROLLED',NULL,NULL,NULL,NULL,'2026-10-12 05:33:08');
/*!40000 ALTER TABLE `training_enrollments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `training_programs`
--

DROP TABLE IF EXISTS `training_programs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `training_programs` (
  `training_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(180) NOT NULL,
  `provider_type` enum('INTERNAL','EXTERNAL','ONLINE') NOT NULL DEFAULT 'ONLINE',
  `provider_name` varchar(180) DEFAULT NULL,
  `description` text,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `mode` enum('ONLINE','OFFLINE','HYBRID') NOT NULL DEFAULT 'ONLINE',
  `duration_hours` decimal(7,2) NOT NULL DEFAULT '0.00',
  `skill_id` bigint unsigned DEFAULT NULL,
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`training_id`),
  KEY `fk_training_programs_skill` (`skill_id`),
  KEY `fk_training_programs_creator` (`created_by`),
  CONSTRAINT `fk_training_programs_creator` FOREIGN KEY (`created_by`) REFERENCES `employees` (`employee_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_training_programs_skill` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`skill_id`) ON DELETE SET NULL,
  CONSTRAINT `chk_training_dates` CHECK (((`end_date` is null) or (`start_date` is null) or (`end_date` >= `start_date`))),
  CONSTRAINT `chk_training_duration` CHECK ((`duration_hours` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `training_programs`
--

LOCK TABLES `training_programs` WRITE;
/*!40000 ALTER TABLE `training_programs` DISABLE KEYS */;
INSERT INTO `training_programs` VALUES (1,'Java Skill Upgrade','ONLINE','Example Learning','Improve Java and OOP skills','2026-10-20','2026-11-10','ONLINE',18.00,1,1,'2026-10-12 05:33:08'),(2,'SQL for Analytics','ONLINE','Example Learning','Learn SQL queries and joins','2026-10-22','2026-11-05','ONLINE',12.00,2,1,'2026-10-12 05:33:08'),(3,'REST API Workshop','INTERNAL','Engineering Team','Learn API design and validation','2026-11-01','2026-11-02','HYBRID',6.00,4,1,'2026-10-12 05:33:08'),(4,'AWS Fundamentals','EXTERNAL','Example Learning','Learn cloud fundamentals',NULL,NULL,'ONLINE',10.00,5,4,'2026-10-12 05:33:08');
/*!40000 ALTER TABLE `training_programs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `training_recommendations`
--

DROP TABLE IF EXISTS `training_recommendations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `training_recommendations` (
  `recommendation_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `skill_id` bigint unsigned NOT NULL,
  `training_id` bigint unsigned NOT NULL,
  `score` decimal(6,3) NOT NULL DEFAULT '0.000',
  `reason` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`recommendation_id`),
  KEY `fk_recommendations_employee` (`user_id`),
  KEY `fk_recommendations_skill` (`skill_id`),
  KEY `fk_recommendations_training` (`training_id`),
  CONSTRAINT `fk_recommendations_employee` FOREIGN KEY (`user_id`) REFERENCES `employees` (`employee_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_recommendations_skill` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`skill_id`),
  CONSTRAINT `fk_recommendations_training` FOREIGN KEY (`training_id`) REFERENCES `training_programs` (`training_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `training_recommendations`
--

LOCK TABLES `training_recommendations` WRITE;
/*!40000 ALTER TABLE `training_recommendations` DISABLE KEYS */;
INSERT INTO `training_recommendations` VALUES (1,2,1,1,0.950,'Improve Java proficiency','2026-10-12 05:33:08'),(2,2,2,2,0.890,'Strengthen SQL skills','2026-10-12 05:33:08'),(3,3,5,4,0.810,'Improve cloud fundamentals','2026-10-12 05:33:08');
/*!40000 ALTER TABLE `training_recommendations` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-12 11:33:50
