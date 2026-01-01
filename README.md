# Pawction 🐾

**Team Members:** Dinesh Adhikari | Khagendra Dhungel | Thinh Vo

Pawction is a real-time pet auction platform that connects pet sellers and buyers in a secure, structured, and efficient way. Traditional methods like word-of-mouth or social media are slow and unreliable, leaving sellers uncertain and buyers without fair competition. Pawction provides a safe, trustworthy, and heartwarming space where pets meet their perfect homes, with real-time bidding and community-driven trust.

---

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Scrum Roles](#scrum-roles)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)

---

## Overview

Pawction enables users to:  
- Register and manage profiles  
- List items (pet adoption auctions)  
- Place real-time bids on active auctions  
- Receive notifications when the auction ends 
- Handle payments securely

---

## Features

- **Auction Creation:** title, description, starting price, and end time  
- **Placing Bids:** with validation and concurrency safety  
- **Real-Time Updates:** highest bids and notifications  
- **Purchase Handling:** complete payment after winning bid  
- **Search & Filter:** find specific pets by criteria  

---

## Architecture

**High-Level Flow:**

**Frontend (Web)**  
- UI: Login/Register, Browse Pets, Place Bids, Add Auctions, Add funds, View History  

**Backend Services:**  
- **User Service:** registration, authentication  
- **Pet Service:** register/update pets, manage images  
- **Auction Service:** create/update details/cancel/end auctions  
- **Bidding Service:** place bids, validate, finalize  
- **Settlement Service:** winner selection, payment  
- **Account/Wallet Service:** deposit, hold, forfeit, release  
- **Auction Policy:** bid increments, deposit rules
- **Socket:** update bids and current winner
- **File Storage Service:** store multiple images of pet

**Database/Entity Layer:**  
- User table
- Account table
- Pet table
- Auction & bid tables
- Deposit Hold & Transaction tables 

**Other Technical Notes:**  
- Real-time bid handling via WebSocket  
- Relational DB for users, auctions, bids, and transactions  

---

## Scrum Roles

- **Product Owners:** Thinh Vo, Khagendra Dhungel, Dinesh Adhikari  
- **Scrum Master:** Thinh Vo  
- **DevOps:** Khagendra Dhungel  
- **Frontend Team:** Dinesh Adhikari  
- **Backend Team:** Dinesh Adhikari, Thinh Vo  

---

## Getting Started

### Prerequisites
- Java 21 (OpenJDK 21)  
- Maven  
- Spring Boot  

### Installation
```bash
git clone https://github.com/ThinhNVo/Pawction
cd pawctions
mvn clean install
mvn spring-boot:run
