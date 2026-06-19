# Smart Basket MCP

## AI-Native Grocery Memory Layer for Swiggy Instamart

Version: 1.0

Status: Project Proposal / Build Specification

---

# Executive Summary

Smart Basket MCP is an MCP server that sits on top of Swiggy Instamart MCP and provides persistent grocery memory, reusable baskets, intelligent substitutions, refill predictions, and AI-generated shopping baskets.

The goal is not to build another grocery application.

The goal is to build an intelligence layer that can be used by any MCP-compatible AI client such as:

* ChatGPT
* Claude
* Gemini
* Cursor
* Kiro
* Open WebUI
* Continue.dev
* Custom MCP Clients

Swiggy Instamart MCP already handles:

* Product Search
* Product Discovery
* Cart Management
* Checkout
* Order Tracking
* Order History

Smart Basket MCP adds:

* Basket Management
* Grocery Memory
* Learned Substitutions
* AI Basket Generation
* Refill Prediction
* Shopping Preferences

---

# Problem Statement

Most grocery purchases are repetitive.

Example:

Every few days users buy:

* Amul Full Cream Milk
* Eggs
* Bread
* Curd

Current process:

1. Open Instamart
2. Search Milk
3. Add Milk
4. Search Eggs
5. Add Eggs
6. Search Bread
7. Add Bread
8. Checkout

This process repeats indefinitely.

Despite years of order history, grocery apps do not effectively learn:

* What users buy together
* What users buy repeatedly
* Preferred substitutions
* Refill patterns

---

# Vision

Allow users to say:

> Add my Dairy Basket

or

> What groceries am I likely running out of?

or

> Order what my mom usually buys

or

> Add my weekly groceries

or

> Create a basket x with  n of a's, m of b's, o of c's, ... prodcuts

or 

> What are in my basket x

And have the AI complete most of the work.

---

# Architecture

```text
User
  ↓
ChatGPT / Claude / Gemini
  ↓
Smart Basket MCP
  ↓
Swiggy Instamart MCP
  ↓
Instamart Cart
  ↓
Checkout
```

---

# Core Philosophy

## AI Assists

The system should:

* Remember
* Suggest
* Predict
* Organize

---

## User Decides

The system should never:

* Place orders automatically
* Complete payments automatically
* Confirm substitutions automatically

Final approval must always belong to the user.

---

# Key Features

---

# Feature 1: User Baskets

Users can create reusable baskets.

Examples:

* Dairy Basket
* Breakfast Basket
* Monthly Grocery
* Parents Basket
* Gym Basket

Example:

Dairy Basket

* Amul Full Cream Milk 1L × 2
* Eggs × 10
* Curd × 1

User command:

```text
Add my Dairy Basket
```

Expected Result:

All products added to Instamart cart.

---

# Feature 2: Save Current Cart as Basket

User manually creates a cart once.

Then says:

```text
Save this as Dairy Basket
```

The basket is stored for future reuse.

---

# Feature 3: AI Basket

Automatically generated basket.

Built using:

* Order History
* Frequently Ordered Items
* Purchase Frequency
* Purchase Recency

Example:

AI Basket

* Milk
* Eggs
* Bread
* Curd

Generated dynamically.

---

# Feature 4: Learned Substitutions

This is the most important feature.

Example:

Preferred Product

```text
Amul Full Cream Milk 1L × 2
```

If unavailable:

```text
Amul Full Cream Milk 500ml × 4
```

If unavailable:

Tell user and ask for substitue and if user adds a substitute remeber that and next time use that if the actual product is not available but then also notify the user that the actual product is out of stock so add substitute

```text
Mother Dairy Full Cream Milk 1L × 2
```

The system stores substitution priorities.

---

# Feature 5: Adaptive Learning

Example:

Product unavailable.

User manually selects:

```text
Mother Dairy Full Cream Milk
```

System records:

```text
Milk Preference #3
```

Future recommendations use learned behavior.

The AI should learn from user decisions.

The AI should never invent preferences.

---

# Feature 6: Smart Refill Prediction

The system should detect likely replenishment needs.

Example:

Milk purchased:

* 12 times

Average gap:

* 3 days

Last purchased:

* 4 days ago

Recommendation:

```text
You may be running low on milk.
```

---

# Feature 7: Household Shopping Memory

The system should learn:

* Commonly purchased products
* Frequently purchased groups
* Preferred brands
* Preferred quantities
* Preferred substitutes

Example:

Mom's Weekly Basket

* Amul Full Cream Milk
* Eggs
* Curd
* Bread

User command:

```text
Order what my mom usually buys
```

---

# MCP Tools To Implement

---

## create_basket

Creates a new basket.

Input:

```json
{
  "name": "Dairy Basket"
}
```

Output:

```json
{
  "success": true
}
```

---

## get_basket

Returns basket details.

Input:

```json
{
  "basketName": "Dairy Basket"
}
```

Output:

```json
{
  "items": [...]
}
```

---

## update_basket

Add or remove products.

Input:

```json
{
  "basketName": "Dairy Basket",
  "action": "add",
  "product": "Eggs"
}
```

---

## delete_basket

Deletes basket.

---

## list_baskets

Returns all baskets.

Output:

```json
{
  "baskets": [
    "Dairy Basket",
    "Breakfast Basket"
  ]
}
```

---

## learn_substitution

Stores user substitution preference.

Input:

```json
{
  "preferred": "Amul Full Cream 1L",
  "fallback": "Amul Full Cream 500ml"
}
```

---

## get_substitutions

Returns substitution chain.

---

## generate_ai_basket

Generates basket from order history.

---

## suggest_refill

Returns likely required products.

---

## add_basket_to_instamart

Workflow:

1. Retrieve Basket
2. Search Products
3. Check Availability
4. Apply Substitutions
5. Update Instamart Cart
6. Return Cart Summary

---

# Swiggy MCP Integration

The following Swiggy MCP capabilities should be utilized:

* Product Search
* Product Discovery
* Cart Retrieval
* Cart Update
* Order History
* Frequently Ordered Products
* Checkout

Smart Basket MCP should never duplicate these capabilities.

Instead it should orchestrate them.

---

# Database Design

---

## baskets

```sql
CREATE TABLE baskets (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    created_at DATETIME
);
```

---

## basket_items

```sql
CREATE TABLE basket_items (
    id TEXT PRIMARY KEY,
    basket_id TEXT NOT NULL,
    product_id TEXT NOT NULL,
    quantity INTEGER NOT NULL
);
```

---

## substitutions

```sql
CREATE TABLE substitutions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    preferred_product TEXT NOT NULL,
    fallback_product TEXT NOT NULL,
    priority INTEGER NOT NULL
);
```

---

## purchase_insights

```sql
CREATE TABLE purchase_insights (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    product_id TEXT NOT NULL,
    avg_purchase_gap INTEGER,
    last_purchase_date DATETIME,
    confidence_score REAL
);
```

---

# Recommended Tech Stack

## MCP Server

* Node.js
* TypeScript
* MCP SDK

Reason:

Native MCP ecosystem support.

---

## Database

Initial MVP:

* SQLite

Future:

* PostgreSQL

---

## ORM

* Drizzle ORM

or

* Prisma

---

# AI Logic

Version 1 should be rule-based.

No LLM required.

Use:

* Frequency
* Recency
* Quantity consistency

to generate recommendations.

Example:

```text
Milk ordered 12 times

Average purchase gap:
3 days

Last purchase:
4 days ago

Recommend reorder.
```

---

# User Scenarios

## Scenario 1

User:

```text
Add my Dairy Basket
```

Flow:

```text
get_basket
↓
search_products
↓
apply_substitutions
↓
update_cart
↓
get_cart
↓
return_summary
```

---

## Scenario 2

User:

```text
What should I reorder this week?
```

Flow:

```text
get_orders
↓
analyze_frequency
↓
generate_ai_basket
↓
return_recommendations
```

---

## Scenario 3

User:

```text
Order what my mom usually buys
```

Flow:

```text
retrieve_ai_basket
↓
check_availability
↓
apply_substitutions
↓
populate_cart
↓
show_summary
```

---

# Project Roadmap

## Phase 1

Basket Management

Deliverables:

* create_basket
* get_basket
* update_basket
* delete_basket
* list_baskets

---

## Phase 2

Swiggy MCP Integration

Deliverables:

* Product Search
* Cart Updates
* Cart Retrieval

---

## Phase 3

Substitution Engine

Deliverables:

* Learned substitutions
* Product fallback chain
* User preference tracking

---

## Phase 4

AI Basket Engine

Deliverables:

* Order analysis
* AI Basket generation
* Basket recommendations

---

## Phase 5

Refill Prediction

Deliverables:

* Purchase pattern analysis
* Refill suggestions
* Confidence scoring

---

# Success Metrics

Technical:

* Basket creation success rate
* Cart population success rate
* Substitution accuracy

Business:

* Reduced ordering friction
* Faster grocery ordering
* Higher reorder frequency
* Increased user retention

---

# Future Enhancements

* Voice-first ordering
* WhatsApp integration
* Shared family baskets
* Multi-user households
* Recipe-to-basket generation
* Smart meal planning
* Calendar-based grocery planning
* Household inventory tracking

---

# Final Vision

Smart Basket MCP becomes the memory layer for grocery shopping.

Swiggy Instamart MCP handles commerce.

Smart Basket MCP handles:

* Memory
* Preferences
* Baskets
* Learning
* Prediction

Together they create an AI-native grocery shopping experience where users no longer need to repeatedly search for the same products every week.

---

## To get the access for Swiggy

Getting Access

Here's what we need from you and what we check on our end. No surprises — just a straightforward process.
What to Include in Your Application

    Who you are — company details or individual developer profile
    What you're building — a brief description of your use case
    How it works — integration architecture overview
    Redirect URI(s) for authentication flows
    Static IP ranges or gateway IP(s)
    Security contact for your team
    Data handling and privacy declaration
    Environment and infrastructure setup details
    Acknowledgement of Swiggy MCP terms
    Security audit summary(Optional)
    SOC2 / ISO certification (if available)(Optional)
    Expected traffic and scaling plan(Optional)

What We Check on Our End
1
Security Check

We review your security setup and infrastructure to keep users safe.
2
Compliance Review

Quick check on data handling and privacy practices — nothing scary.
3
Use Case Fit

We make sure your idea is a good fit for the platform and our users.
4
Gradual Rollout

We validate together, ramp up access gradually, and go live when everything is solid.
5
Ongoing Partnership

We stay in touch — usage monitoring, support, and a direct line to the team.


The Ground Rules

Build freely within these boundaries. We keep it simple — here's what's encouraged, what's not, and what's a hard no.
What We Allow

Encouraged

Building apps, agents, or tools that make ordering, discovery, or dining better for users

AI-powered assistants and copilots that use MCP to automate commerce workflows

Creative side projects, hackathon builds, and experimental prototypes

Integrations that follow Swiggy’s security and branding guidelines

Sharing demos and walkthroughs with us — we love seeing what you build

Commercial partnerships where both sides win


Not Allowed (Restricted)

Reselling or sharing your MCP access with unapproved third parties

Building aggregation layers that hide Swiggy's brand or confuse users

Misrepresenting prices, availability, or delivery times

Scraping or extracting data beyond what the APIs provide

Using the APIs for competitive intelligence or benchmarking

Bypassing rate limits, logging, or any platform safeguards


rohibited Conduct

Zero Tolerance
01

Manipulating order flows, incentives, or ranking systems
02

Dark patterns, deceptive UX, or misattributing where data comes from
03

Generating fake traffic or abusing rate limits
04

Harvesting data beyond your agreed scope
05

Reverse engineering MCP internals
06

Circumventing whitelisting or access controls
07

Violating user privacy or security regulations


A Few Things to Know

The important stuff. Fair rules that keep the platform healthy for everyone.
01
Stay in Scope

Use the APIs for what they're built for. Want to expand into new capabilities? Let's talk.
02
Respect the Brand

Follow Swiggy's attribution guidelines. Users should know when they're interacting with Swiggy services.
03
User Data is Sacred

Transaction data from MCP stays governed by Swiggy's platform terms. Handle it responsibly.
04
We Keep Watch

We monitor API usage for quality and safety. Play fair and you'll never have an issue.
Legal Framework

Custom terms can be negotiated for enterprise partners. Typical turnaround: 4+ weeks.
MCP integration agreement
Data protection & privacy terms
Liability and misuse provisions
Termination and revocation rights