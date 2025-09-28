# Pawction

Team Members: 
- Dinesh Adhikari 
- Paul Morris 
- Khagendra Dhungel 
- Thinh Vo

Pet breeders, sellers, and even owners often struggle to find the right home for their pets. The process can take months, relying heavily on word-of-mouth or social media groups that lack structure, security, and efficiency. This leaves sellers uncertain about finding a trustworthy buyer, and buyers without a streamlined way to demonstrate genuine interest or compete fairly for a pet they truly want. That’s where our platform comes in, creating a safe, trustworthy, and heartwarming space where pets meet their perfect matches. With real-time bidding and a community built on trust, we make it easier, faster, and more meaningful to find the right home at the right time.

---

## Table of Contents

1. [Overview](#overview)  
2. [Features](#features)  
3. [Architecture](#architecture)  
4. [Getting Started](#getting-started)  
   - [Prerequisites](#prerequisites)  
   - [Installation](#installation)  

---

## Overview

Pawctions enables users to:

- Register and manage profile  
- List items or services (e.g. pet supplies, grooming services, pet adoption auctions)  
- Place real-time bids on active auctions  
- Receive live updates when outbid  
- Handle payments 

---

## Features

- Auction creation (title, description, starting price, end time)  
- Placing bids with validation and concurrency safety  
- Real-time updates of highest bids 
- Ability to purchase item after bid ends 
- Search for specific items (ability to filter by specific criteria)

---

## Architecture

At a high level:

- Clients (Web) 
- API Gateway or controller layer routes requests  
- Backend services: User, Auction, Payment, Notification, Admin  
- Persistence: relational DB (for users, auctions, bids)


---

## Getting Started

### Prerequisites

Make sure you have:

- Java 21  
- Maven
- SpringBoot

### Installation

```bash
git clone https://github.com/ThinhNVo/Pawction
cd pawctions
