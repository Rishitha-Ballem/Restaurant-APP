# 🍽️ Restaurant Management App

This is a microservices-based Restaurant Management Application developed using **Java**, **Spring Boot**, and **Spring Cloud**. The project provides APIs for handling restaurant-related data such as menus, orders, customers, and employees.

## 📁 Project Structure

This project follows a modular microservice architecture. It includes the following core modules:

- `restaurant-service`: Handles restaurant information and operations
- `menu-service`: Manages menu items and categories
- `order-service`: Manages customer orders and statuses
- `employee-service`: Maintains employee records
- `api-gateway`: Routes requests to appropriate services
- `service-registry`: Eureka-based service discovery
- `config-server`: Centralized configuration management

## 🔧 Technologies Used

- Java 17
- Spring Boot
- Spring Cloud (Eureka, Gateway, Config)
- REST APIs
- Maven
- IntelliJ IDEA
- Git & GitHub

## 🚀 Features

- CRUD operations for restaurants, menus, employees, and orders
- Service discovery using Eureka Server
- API Gateway for routing and load balancing
- Centralized configuration using Spring Cloud Config
- Modular microservices structure
- Environment-specific property management

## 🏗️ Getting Started

1. Clone the repository:

   ```bash
   git clone https://github.com/Rishitha-Ballem/Restaurant-APP.git
   cd Restaurant-APP
