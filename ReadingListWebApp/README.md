# Overview
A Sample application built with Spring Boot 2 on Spring WebFlux stack, integrated with Azure Cosmos DB SQL API using the Java SDK 4.0, based on reactor core  to demonstrate an end to end non-blocking application.


Specifically, the following capabilities are demonstrated:
* Spring Boot 2 on WebFlux - Functional Routing, thymeleaf templates, Dependency Injection etc
* Netty HTTP server backend
* Calling REST APIs with WebClient
* Java SDK 4.0
* Spring Reactor 
* Cosmos DB SQL API

# Instructions

## First:
 * Java 8
 * maven
 * Create a Cosmos DB SQL API Account. 

## Then:
* Update the database connection information in application.properties along with a database name and container name of your choice
* mvn spring-boot:run 
* Access the WebApp at http://localhost:8080/readingList
