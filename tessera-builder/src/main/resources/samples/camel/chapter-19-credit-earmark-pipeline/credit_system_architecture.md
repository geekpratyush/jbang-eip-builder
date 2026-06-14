# Credit Limit & Earmarking System - Architecture & Implementation Guide

## Table of Contents
1. [System Overview](#system-overview)
2. [Domain Concepts](#domain-concepts)
3. [Actor Services (Partners)](#actor-services)
4. [Message Flow & Pipeline](#message-flow)
5. [Approval Rules Engine](#approval-rules)
6. [Technical Architecture](#technical-architecture)
7. [Database Schema](#database-schema)
8. [Camel Route Design](#camel-route-design)
9. [Event Flow Diagram](#event-flow)
10. [UI Pipeline Visualization](#ui-pipeline)

---

## System Overview

This system simulates a real-world bank's **Credit Limit Management** and **Earmarking** process across multiple partner services. It demonstrates how a facility request flows through various approval stages, with rules-based decision making, pool management, and multi-protocol messaging (Kafka, IBM MQ).

### Key Features
- **Pipeline Visualization**: Watch requests flow from initiation to completion
- **Multiple Outcomes**: Success, Failure, Rejection, or Excess Approval paths
- **Real-time Simulation**: Uses Faker to generate realistic test data
- **Dual Database**: MongoDB for events/logs, SQL for transactional data
- **Multi-Protocol Messaging**: Kafka for async events, IBM MQ for partner integration

---

## Domain Concepts

### Credit Limit
The maximum total exposure a bank allows for a counterparty across all products.

```
Available Limit = Approved Limit - (Booked Exposure + Earmarked Amount)
```

### Earmarking
Reservation of limit capacity before actual cash flow occurs. Types:
- **Hard Earmark**: Deducts from available limit immediately (e.g., LC issuance)
- **Soft Earmark**: Tentative reservation, may expire (e.g., pending approval)
- **Contingent Earmark**: For potential future exposure (e.g., derivatives PFE)

### Facility
A structured credit arrangement with specific terms:
- Revolving Credit Facility (RCF)
- Term Loan
- Overdraft
- Letter of Credit (LC)
- Bank Guarantee

### Pool
Aggregation mechanism for risk management:
- **Obligor Pool**: Single entity/group exposure
- **Product Pool**: By instrument type
- **Sector Pool**: Industry concentration
- **Geographic Pool**: Country risk
- **Collateral Pool**: Secured assets

---

## Actor Services (Partners)

| Service | Role | Protocol | Description |
|---------|------|----------|-------------|
| **Faker Service** | Initiator | Internal | Generates realistic test requests |
| **CRM Gateway** | Entry Point | REST/Kafka | Receives and validates incoming requests |
| **Credit Engine** | Core Processor | Internal | Calculates exposures, checks limits |
| **Risk Scoring** | Analyzer | Kafka | Provides risk ratings and scores |
| **Approval Workflow** | Decision Maker | IBM MQ | Routes for approval based on rules |
| **Pool Manager** | Allocator | Internal | Manages pool allocations and limits |
| **Earmarking Service** | Reserver | Kafka | Handles earmark creation/release |
| **Notification Service** | Communicator | Kafka/MQ | Sends alerts to partners |
| **Audit Logger** | Recorder | MongoDB | Immutable event log |
| **Reporting Service** | Observer | SQL/REST | Generates management reports |

---

## Message Flow & Pipeline

### Pipeline Stages

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   STAGE 1   │───▶│   STAGE 2   │───▶│   STAGE 3   │───▶│   STAGE 4   │
│  Request    │    │  Validate   │    │   Assess    │    │  Decision   │
│ Generation  │    │  & Enrich   │    │   Risk      │    │   Engine    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                  │                  │                  │
       ▼                  ▼                  ▼                  ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   STAGE 5   │    │   STAGE 6   │    │   STAGE 7   │    │   STAGE 8   │
│   Earmark   │───▶│  Approval   │───▶│  Book/      │───▶│  Notify &   │
│  Creation   │    │  Workflow   │    │  Reject     │    │  Archive    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### Stage Details

#### Stage 1: Request Generation (Faker Service)
- Generates random but realistic facility requests
- Creates customer profiles, facility types, amounts
- Publishes to Kafka topic: `credit.requests.new`

#### Stage 2: Validation & Enrichment (CRM Gateway)
- Validates customer ID, facility type, amount
- Enriches with customer master data from SQL
- Checks duplicate requests
- Publishes to: `credit.requests.validated`

#### Stage 3: Risk Assessment (Risk Scoring)
- Calculates probability of default (PD)
- Determines risk rating (AAA to D)
- Assesses collateral coverage
- Publishes to: `credit.risk.assessed`

#### Stage 4: Decision Engine (Credit Engine)
- Checks against credit limit
- Evaluates pool capacity
- Applies approval rules
- Routes to: `credit.decision.required` or `credit.decision.auto`

#### Stage 5: Earmarking (Earmarking Service)
- Creates hard/soft earmark
- Updates pool allocations
- Sets expiry dates
- Publishes to: `credit.earmark.created`

#### Stage 6: Approval Workflow
- Routes based on amount and risk
- Single approval, dual approval, or committee
- Tracks approval timestamps
- Uses IBM MQ for partner notifications

#### Stage 7: Booking or Rejection
- Converts earmark to booked exposure (success)
- Or releases earmark (rejection)
- Updates SQL transactional tables
- Publishes final status

#### Stage 8: Notification & Archive
- Sends notifications to partners via MQ
- Archives events to MongoDB
- Updates reporting tables
- Triggers downstream processes

---

## Approval Rules Engine

### Rule Categories

#### 1. Auto-Approval Rules
```yaml
rule_auto_approve_small:
  condition: amount < $100,000 AND risk_rating <= BBB AND customer_tenure > 2_years
  action: AUTO_APPROVE
  authority: SYSTEM

rule_auto_approve_collateralized:
  condition: collateral_coverage > 150% AND amount < $500,000
  action: AUTO_APPROVE
  authority: SYSTEM
```

#### 2. Single Approval Rules
```yaml
rule_single_approval:
  condition: amount < $1,000,000 AND risk_rating <= BB
  action: REQUIRE_SINGLE_APPROVAL
  authority: RELATIONSHIP_MANAGER

rule_single_approval_mid:
  condition: amount < $5,000,000 AND risk_rating <= B
  action: REQUIRE_SINGLE_APPROVAL
  authority: CREDIT_MANAGER
```

#### 3. Dual Approval Rules
```yaml
rule_dual_approval:
  condition: amount >= $5,000,000 OR risk_rating > B
  action: REQUIRE_DUAL_APPROVAL
  authority: [CREDIT_MANAGER, HEAD_OF_CREDIT]
```

#### 4. Committee Approval Rules
```yaml
rule_committee:
  condition: amount >= $50,000,000 OR risk_rating = D OR sector = RESTRICTED
  action: REQUIRE_COMMITTEE
  authority: CREDIT_COMMITTEE
```

#### 5. Excess Approval Rules (Over-limit)
```yaml
rule_excess_within_buffer:
  condition: requested_amount > available_limit AND (requested_amount - available_limit) < 10%_of_limit
  action: REQUIRE_EXCESS_APPROVAL
  authority: HEAD_OF_CREDIT
  note: "Within 10% buffer zone"

rule_excess_significant:
  condition: requested_amount > available_limit AND (requested_amount - available_limit) >= 10%_of_limit
  action: REQUIRE_COMMITTEE_EXCEPTION
  authority: BOARD_RISK_COMMITTEE
  note: "Significant excess requires board approval"
```

#### 6. Rejection Rules
```yaml
rule_reject_limit_breach:
  condition: requested_amount > (approved_limit * 1.10) AND no_exception_authority
  action: AUTO_REJECT
  reason: "Exceeds 110% of approved limit without exception authority"

rule_reject_risk:
  condition: risk_rating = D AND no_mitigation
  action: AUTO_REJECT
  reason: "Default risk rating without mitigation"

rule_reject_sector:
  condition: sector IN restricted_sectors AND amount > sector_limit
  action: AUTO_REJECT
  reason: "Sector concentration limit exceeded"
```

### Rule Execution Flow
```
Input: Facility Request
  │
  ▼
┌─────────────────┐
│ Check Rejection │──Yes──▶ REJECT
│ Rules (Priority)│
└─────────────────┘
  │ No
  ▼
┌─────────────────┐
│ Check Auto-     │──Yes──▶ AUTO_APPROVE
│ Approval Rules  │
└─────────────────┘
  │ No
  ▼
┌─────────────────┐
│ Check Excess    │──Yes──▶ EXCESS_APPROVAL_PATH
│ Rules           │
└─────────────────┘
  │ No
  ▼
┌─────────────────┐
│ Check Approval  │
│ Level Rules     │──▶ ROUTE_TO_APPROVER
└─────────────────┘
```

---

## Technical Architecture

### Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Integration** | Apache Camel | Route orchestration, EIP patterns |
| **Event Streaming** | Apache Kafka | Async messaging between services |
| **Enterprise MQ** | IBM MQ | Partner integration, guaranteed delivery |
| **Document Store** | MongoDB | Event sourcing, audit logs, pipeline state |
| **Relational DB** | PostgreSQL | Transactional data, limits, pools |
| **UI Framework** | React + WebSocket | Real-time pipeline visualization |
| **Data Generation** | Java Faker | Realistic test data |

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                           UI Layer (React)                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │   Pipeline   │  │   Control    │  │   Status     │               │
│  │  Visualizer  │  │    Panel     │  │   Dashboard  │               │
│  └──────────────┘  └──────────────┘  └──────────────┘               │
└─────────────────────────────────────────────────────────────────────┘
                              │ WebSocket
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        API Gateway (Camel)                           │
│              REST / WebSocket / Content-Based Router                 │
└─────────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  Kafka Routes   │ │  IBM MQ Routes  │ │  Internal Routes│
│  (Async Events) │ │ (Partner Conn)  │ │  (Direct/Seda)  │
└─────────────────┘ └─────────────────┘ └─────────────────┘
          │                   │                   │
          ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Service Layer (Camel Context)                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │  Faker   │ │  Credit  │ │  Risk    │ │  Earmark │ │  Approval│ │
│  │ Service  │ │  Engine  │ │  Scoring │ │  Service │ │ Workflow │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│  │  Pool    │ │  Notify  │ │  Audit   │ │  Report  │             │
│  │ Manager  │ │ Service  │ │  Logger  │ │ Service  │             │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘             │
└─────────────────────────────────────────────────────────────────────┘
          │                   │                   │
          ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│    MongoDB      │ │   PostgreSQL    │ │   Kafka/MQ      │
│  (Events/Logs)  │ │ (Transactions)  │ │  (Messaging)    │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

---

## Database Schema

### MongoDB Collections (Event Store)

```javascript
// Collection: pipeline_events
{
  _id: ObjectId,
  pipeline_id: "uuid",
  stage: "STAGE_1_REQUEST_GEN",
  status: "IN_PROGRESS", // IN_PROGRESS, COMPLETED, FAILED, SKIPPED
  timestamp: ISODate(),
  payload: { /* stage-specific data */ },
  metadata: {
    service: "faker-service",
    version: "1.0",
    trace_id: "uuid"
  }
}

// Collection: audit_trail
{
  _id: ObjectId,
  facility_id: "uuid",
  event_type: "CREDIT_LIMIT_CHECK",
  actor: "credit-engine",
  action: "CHECKED",
  details: {
    requested_amount: 1000000,
    available_limit: 5000000,
    result: "WITHIN_LIMIT"
  },
  timestamp: ISODate(),
  compliance: {
    regulation: "BASEL_III",
    retention_years: 7
  }
}

// Collection: pipeline_snapshots
{
  _id: ObjectId,
  pipeline_id: "uuid",
  current_stage: "STAGE_5_EARMARK",
  overall_status: "IN_PROGRESS",
  stages_completed: ["STAGE_1", "STAGE_2", "STAGE_3", "STAGE_4"],
  stages_pending: ["STAGE_5", "STAGE_6", "STAGE_7", "STAGE_8"],
  created_at: ISODate(),
  updated_at: ISODate()
}
```

### PostgreSQL Schema (Transactional)

```sql
-- Table: customers
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(50),
    country_code CHAR(2),
    sector_code VARCHAR(10),
    risk_rating VARCHAR(5),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Table: credit_limits
CREATE TABLE credit_limits (
    limit_id UUID PRIMARY KEY,
    customer_id UUID REFERENCES customers(customer_id),
    facility_type VARCHAR(50) NOT NULL,
    approved_limit DECIMAL(18,2) NOT NULL,
    available_limit DECIMAL(18,2) NOT NULL,
    currency CHAR(3) DEFAULT 'USD',
    effective_date DATE NOT NULL,
    expiry_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table: pools
CREATE TABLE pools (
    pool_id UUID PRIMARY KEY,
    pool_type VARCHAR(50) NOT NULL, -- OBLIGOR, PRODUCT, SECTOR, GEOGRAPHIC
    pool_name VARCHAR(100) NOT NULL,
    parent_pool_id UUID REFERENCES pools(pool_id),
    total_limit DECIMAL(18,2) NOT NULL,
    utilized_amount DECIMAL(18,2) DEFAULT 0,
    currency CHAR(3) DEFAULT 'USD',
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- Table: pool_memberships
CREATE TABLE pool_memberships (
    membership_id UUID PRIMARY KEY,
    pool_id UUID REFERENCES pools(pool_id),
    member_id UUID NOT NULL, -- customer_id or facility_id
    member_type VARCHAR(20) NOT NULL, -- CUSTOMER, FACILITY
    allocated_limit DECIMAL(18,2) NOT NULL,
    utilized_amount DECIMAL(18,2) DEFAULT 0
);

-- Table: earmarks
CREATE TABLE earmarks (
    earmark_id UUID PRIMARY KEY,
    facility_id UUID,
    customer_id UUID REFERENCES customers(customer_id),
    earmark_type VARCHAR(20) NOT NULL, -- HARD, SOFT, CONTINGENT
    amount DECIMAL(18,2) NOT NULL,
    currency CHAR(3) DEFAULT 'USD',
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, RELEASED, CONVERTED, EXPIRED
    expiry_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    released_at TIMESTAMP
);

-- Table: facilities
CREATE TABLE facilities (
    facility_id UUID PRIMARY KEY,
    customer_id UUID REFERENCES customers(customer_id),
    facility_type VARCHAR(50) NOT NULL,
    requested_amount DECIMAL(18,2) NOT NULL,
    approved_amount DECIMAL(18,2),
    currency CHAR(3) DEFAULT 'USD',
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, BOOKED, CANCELLED
    risk_rating VARCHAR(5),
    approval_level VARCHAR(50),
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    booked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table: approval_history
CREATE TABLE approval_history (
    history_id UUID PRIMARY KEY,
    facility_id UUID REFERENCES facilities(facility_id),
    stage VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL, -- SUBMITTED, APPROVED, REJECTED, RETURNED
    actor VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50),
    comments TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table: rule_executions
CREATE TABLE rule_executions (
    execution_id UUID PRIMARY KEY,
    facility_id UUID REFERENCES facilities(facility_id),
    rule_id VARCHAR(100) NOT NULL,
    rule_category VARCHAR(50) NOT NULL,
    condition_met BOOLEAN NOT NULL,
    action_taken VARCHAR(50) NOT NULL,
    reason TEXT,
    executed_at TIMESTAMP DEFAULT NOW()
);
```

---

## Camel Route Design

### Route Structure

```java
// Main Camel Configuration
@Configuration
public class CreditSystemRoutes extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Error Handling
        errorHandler(deadLetterChannel("kafka: credit.errors.dlq")
            .useOriginalMessage()
            .maximumRedeliveries(3)
            .redeliveryDelay(1000));
        
        // ============================================
        // STAGE 1: Request Generation (Faker Service)
        // ============================================
        from("timer:generateRequest?period=5000") // Every 5 seconds for demo
            .routeId("STAGE-1-Request-Generation")
            .bean("fakerService", "generateFacilityRequest")
            .setHeader("pipelineId", simple("${uuid}"))
            .setHeader("stage", constant("STAGE_1"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("kafka:credit.requests.new")
            .log("Generated new facility request: ${body}");
        
        // ============================================
        // STAGE 2: Validation & Enrichment
        // ============================================
        from("kafka:credit.requests.new")
            .routeId("STAGE-2-Validation-Enrichment")
            .unmarshal().json(FacilityRequest.class)
            .bean("validationService", "validate")
            .choice()
                .when(simple("${header.valid} == false"))
                    .setHeader("stage", constant("STAGE_2_FAILED"))
                    .setHeader("status", constant("REJECTED"))
                    .setHeader("reason", simple("${header.validationErrors}"))
                    .to("direct:pipeline-rejection")
                .otherwise()
                    .bean("enrichmentService", "enrichWithCustomerData")
                    .setHeader("stage", constant("STAGE_2"))
                    .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
                    .to("kafka:credit.requests.validated")
            .end();
        
        // ============================================
        // STAGE 3: Risk Assessment
        // ============================================
        from("kafka:credit.requests.validated")
            .routeId("STAGE-3-Risk-Assessment")
            .bean("riskScoringService", "calculateRisk")
            .setHeader("stage", constant("STAGE_3"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("kafka:credit.risk.assessed")
            .log("Risk assessment completed. Rating: ${header.riskRating}");
        
        // ============================================
        // STAGE 4: Decision Engine
        // ============================================
        from("kafka:credit.risk.assessed")
            .routeId("STAGE-4-Decision-Engine")
            .bean("creditEngine", "checkLimitAndPools")
            .bean("ruleEngine", "evaluateRules")
            .setHeader("stage", constant("STAGE_4"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .choice()
                .when(simple("${header.decision} == 'AUTO_REJECT'"))
                    .to("direct:pipeline-rejection")
                .when(simple("${header.decision} == 'AUTO_APPROVE'"))
                    .to("kafka:credit.decision.auto")
                .when(simple("${header.decision} == 'EXCESS_APPROVAL'"))
                    .to("kafka:credit.decision.excess")
                .otherwise()
                    .to("kafka:credit.decision.required")
            .end();
        
        // ============================================
        // STAGE 5: Earmarking
        // ============================================
        from("kafka:credit.decision.auto")
            .routeId("STAGE-5-Earmarking-Auto")
            .bean("earmarkingService", "createHardEarmark")
            .setHeader("stage", constant("STAGE_5"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("kafka:credit.earmark.created")
            .to("direct:pipeline-booking");
        
        from("kafka:credit.decision.required")
            .routeId("STAGE-5-Earmarking-Pending")
            .bean("earmarkingService", "createSoftEarmark")
            .setHeader("stage", constant("STAGE_5"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("kafka:credit.earmark.pending");
        
        from("kafka:credit.decision.excess")
            .routeId("STAGE-5-Earmarking-Excess")
            .bean("earmarkingService", "createSoftEarmark")
            .setHeader("stage", constant("STAGE_5_EXCESS"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("kafka:credit.earmark.excess");
        
        // ============================================
        // STAGE 6: Approval Workflow
        // ============================================
        from("kafka:credit.earmark.pending")
            .routeId("STAGE-6-Approval-Normal")
            .bean("approvalWorkflow", "routeForApproval")
            .setHeader("stage", constant("STAGE_6"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("ibm-mq:queue:CREDIT.APPROVAL.REQUESTS")
            .log("Sent approval request to MQ: ${header.approvalLevel}");
        
        from("kafka:credit.earmark.excess")
            .routeId("STAGE-6-Approval-Excess")
            .bean("approvalWorkflow", "routeForExcessApproval")
            .setHeader("stage", constant("STAGE_6_EXCESS"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("ibm-mq:queue:CREDIT.EXCESS.APPROVALS")
            .log("Sent excess approval request to MQ: ${header.approvalLevel}");
        
        // Approval Response from MQ
        from("ibm-mq:queue:CREDIT.APPROVAL.RESPONSES")
            .routeId("STAGE-6-Approval-Response")
            .unmarshal().json(ApprovalResponse.class)
            .choice()
                .when(simple("${body.approved} == true"))
                    .to("kafka:credit.approval.granted")
                .otherwise()
                    .to("direct:pipeline-rejection")
            .end();
        
        // ============================================
        // STAGE 7: Booking / Rejection
        // ============================================
        from("kafka:credit.approval.granted")
            .routeId("STAGE-7-Booking")
            .bean("bookingService", "convertEarmarkToBooking")
            .setHeader("stage", constant("STAGE_7"))
            .setHeader("status", constant("BOOKED"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("sql:INSERT INTO facilities (...) VALUES (...)")
            .to("kafka:credit.facility.booked")
            .to("direct:pipeline-completion");
        
        // ============================================
        // STAGE 8: Notification & Archive
        // ============================================
        from("direct:pipeline-completion")
            .routeId("STAGE-8-Completion")
            .bean("notificationService", "sendNotifications")
            .setHeader("stage", constant("STAGE_8"))
            .setHeader("status", constant("COMPLETED"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("ibm-mq:queue:CREDIT.PARTNER.NOTIFICATIONS")
            .bean("reportingService", "updateReports")
            .log("Pipeline completed successfully: ${header.pipelineId}");
        
        // Rejection Handler
        from("direct:pipeline-rejection")
            .routeId("Pipeline-Rejection")
            .bean("earmarkingService", "releaseEarmark")
            .setHeader("stage", constant("STAGE_REJECTED"))
            .setHeader("status", constant("REJECTED"))
            .to("mongodb:creditDb?database=credit_system&collection=pipeline_events&operation=insert")
            .to("sql:INSERT INTO facilities (...) VALUES (...)")
            .bean("notificationService", "sendRejectionNotification")
            .to("ibm-mq:queue:CREDIT.PARTNER.NOTIFICATIONS")
            .log("Pipeline rejected: ${header.reason}");
        
        // ============================================
        // WebSocket for UI Updates
        // ============================================
        from("kafka:credit.*")
            .routeId("WebSocket-Broadcast")
            .bean("pipelineStateService", "updatePipelineState")
            .to("websocket:credit.pipeline.updates");
    }
}
```

### EIP Patterns Used

| Pattern | Usage |
|---------|-------|
| **Content-Based Router** | Route decisions based on approval level |
| **Dead Letter Channel** | Error handling with retry |
| **Splitter** | Break down multi-pool requests |
| **Aggregator** | Collect approval responses |
| **Wire Tap** | Audit logging without blocking main flow |
| **Circuit Breaker** | Fallback for external services |
| **Idempotent Consumer** | Prevent duplicate processing |

---

## Event Flow Diagram

### Success Path
```
[Faker] ──▶ [Kafka: requests.new] ──▶ [CRM Gateway] 
   │
   ▼
[Validation OK] ──▶ [Kafka: validated] ──▶ [Risk Scoring]
   │
   ▼
[Risk Rating: BBB] ──▶ [Kafka: risk.assessed] ──▶ [Credit Engine]
   │
   ▼
[Within Limit] ──▶ [Rule: Auto-Approve] ──▶ [Kafka: decision.auto]
   │
   ▼
[Hard Earmark] ──▶ [Kafka: earmark.created] ──▶ [Booking]
   │
   ▼
[Booked] ──▶ [Kafka: facility.booked] ──▶ [Notification] ──▶ [MQ: Partner]
   │
   ▼
[MongoDB: Archive] ──▶ [SQL: Update] ──▶ [WebSocket: UI Update] ──▶ ✅ SUCCESS
```

### Rejection Path
```
[Faker] ──▶ ... ──▶ [Credit Engine]
   │
   ▼
[Exceeds Limit + No Exception] ──▶ [Rule: Auto-Reject]
   │
   ▼
[Release Earmark] ──▶ [SQL: Rejected Record] ──▶ [Notification]
   │
   ▼
[MongoDB: Archive] ──▶ [WebSocket: UI Update] ──▶ ❌ REJECTED
```

### Excess Approval Path
```
[Faker] ──▶ ... ──▶ [Credit Engine]
   │
   ▼
[Exceeds Limit by 5%] ──▶ [Rule: Excess Approval] ──▶ [Soft Earmark]
   │
   ▼
[MQ: Head of Credit] ──▶ [Approval Pending] ──▶ [MQ: Response Received]
   │
   ▼
[Approved] ──▶ [Convert to Hard Earmark] ──▶ [Booking] ──▶ ✅ SUCCESS
   │
   ▼
[Rejected] ──▶ [Release Earmark] ──▶ ❌ REJECTED
```

---

## UI Pipeline Visualization

### Pipeline Stage Display

```
┌─────────────────────────────────────────────────────────────────┐
│  Pipeline ID: pipe-550e8400-e29b-41d4-a716-446655440000         │
│  Customer: Acme Corp (SECTOR: Manufacturing)                     │
│  Facility: Revolving Credit Facility | Amount: $2,500,000        │
│  Risk Rating: BB | Approval Level: SINGLE_APPROVAL               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [✅] Stage 1: Request Generated        14:23:01.234            │
│  [✅] Stage 2: Validated & Enriched     14:23:01.456            │
│  [✅] Stage 3: Risk Assessed (BB)      14:23:02.123            │
│  [✅] Stage 4: Decision Engine         14:23:02.789            │
│       └─ Rule: single_approval_mid triggered                    │
│  [✅] Stage 5: Hard Earmark Created    14:23:03.012            │
│       └─ Earmark ID: earm-123, Amount: $2,500,000               │
│  [🔄] Stage 6: Approval Workflow       14:23:03.345            │
│       └─ Awaiting: Credit Manager approval                       │
│       └─ Sent to MQ: CREDIT.APPROVAL.REQUESTS                    │
│  [⏳] Stage 7: Booking (Pending)                                   │
│  [⏳] Stage 8: Notification (Pending)                            │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  Overall Status: IN_PROGRESS                                     │
│  Progress: 62% ████████████████░░░░░░░░░░░░░░░░░░░              │
│  Current Action: Waiting for Credit Manager approval via MQ     │
└─────────────────────────────────────────────────────────────────┘
```

### Control Panel Options

```
┌─────────────────────────────────────────────────────────────────┐
│  SIMULATION CONTROLS                                             │
├─────────────────────────────────────────────────────────────────┤
│  [▶ Start Pipeline]  [⏸ Pause]  [⏹ Stop]  [🔄 Reset]            │
│                                                                  │
│  Configuration:                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Scenario: [Auto-Approve ▼]                               │    │
│  │   • Auto-Approve (Small amounts, low risk)                │    │
│  │   • Single Approval (Medium amounts)                    │    │
│  │   • Dual Approval (Large amounts)                       │    │
│  │   • Committee Approval (Very large / restricted)        │    │
│  │   • Excess Approval (Over-limit within buffer)            │    │
│  │   • Excess Committee (Over-limit > 10%)                   │    │
│  │   • Rejection (Limit breach / Risk / Sector)            │    │
│  │   • Random (Mix of all scenarios)                        │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Speed: [Normal ▼]                                        │    │
│  │   • Slow (3s per stage)                                  │    │
│  │   • Normal (1s per stage)                                │    │
│  │   • Fast (Instant)                                       │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  [🔧 Advanced Settings]                                          │
│  [📊 View Metrics]                                               │
└─────────────────────────────────────────────────────────────────┘
```

### Real-time Metrics Dashboard

```
┌─────────────────────────────────────────────────────────────────┐
│  SYSTEM METRICS                                                  │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │  Pipelines   │  │  Success     │  │  Rejected    │            │
│  │  Today: 147  │  │  Rate: 78%   │  │  Rate: 15%   │            │
│  └──────────────┘ └──────────────┘ └──────────────┘            │
│                                                                  │
│  Kafka Lag:                                                      │
│  credit.requests.new: 0 msgs    credit.approval.granted: 2 msgs │
│  credit.earmark.pending: 5 msgs   credit.errors.dlq: 0 msgs       │
│                                                                  │
│  MQ Queue Depths:                                                │
│  CREDIT.APPROVAL.REQUESTS: 3    CREDIT.EXCESS.APPROVALS: 1       │
│  CREDIT.PARTNER.NOTIFICATIONS: 7                                  │
│                                                                  │
│  Database:                                                       │
│  MongoDB Events: 1,247 docs    PostgreSQL Facilities: 89 rows     │
│  Pool Utilization: 67%                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Implementation Checklist

### Phase 1: Core Infrastructure
- [ ] Set up Kafka cluster (Zookeeper + Brokers)
- [ ] Install IBM MQ Queue Manager
- [ ] Deploy MongoDB replica set
- [ ] Deploy PostgreSQL with connection pooling
- [ ] Configure Camel context with all components

### Phase 2: Service Implementation
- [ ] Faker Service with realistic data generation
- [ ] CRM Gateway with validation logic
- [ ] Credit Engine with limit/pool checks
- [ ] Risk Scoring with rating models
- [ ] Earmarking Service with expiry management
- [ ] Approval Workflow with routing logic
- [ ] Rule Engine with Drools or custom implementation

### Phase 3: Integration & Messaging
- [ ] Kafka producer/consumer configurations
- [ ] IBM MQ channel and queue setup
- [ ] Camel route implementation with error handling
- [ ] Idempotency and deduplication logic
- [ ] Transaction management across databases

### Phase 4: UI & Visualization
- [ ] React frontend with WebSocket client
- [ ] Pipeline stage visualization components
- [ ] Real-time status updates
- [ ] Historical pipeline browser
- [ ] Metrics and reporting dashboard

### Phase 5: Testing & Deployment
- [ ] Unit tests for all services
- [ ] Integration tests for Camel routes
- [ ] Load testing with JMeter/Gatling
- [ ] Chaos engineering (failure injection)
- [ ] Production deployment with monitoring

---

## Appendix: Sample Messages

### Kafka Message: Facility Request
```json
{
  "messageType": "FACILITY_REQUEST",
  "messageId": "msg-uuid-123",
  "timestamp": "2024-01-15T14:23:01Z",
  "payload": {
    "customerId": "CUST-ACME-001",
    "facilityType": "REVOLVING_CREDIT",
    "requestedAmount": 2500000.00,
    "currency": "USD",
    "purpose": "Working capital",
    "termMonths": 12,
    "proposedCollateral": {
      "type": "ACCOUNTS_RECEIVABLE",
      "value": 3000000.00
    }
  }
}
```

### IBM MQ Message: Approval Request
```json
{
  "messageType": "APPROVAL_REQUEST",
  "correlationId": "pipe-uuid-456",
  "priority": "HIGH",
  "payload": {
    "facilityId": "FAC-789",
    "approvalLevel": "CREDIT_MANAGER",
    "requestedAmount": 2500000.00,
    "riskRating": "BB",
    "excessAmount": 0,
    "reason": "Standard approval within delegated authority",
    "dueDate": "2024-01-16T14:23:01Z"
  }
}
```

### MongoDB Event: Earmark Created
```json
{
  "eventType": "EARMARK_CREATED",
  "pipelineId": "pipe-uuid-456",
  "stage": "STAGE_5",
  "timestamp": "2024-01-15T14:23:03Z",
  "payload": {
    "earmarkId": "earm-123",
    "type": "HARD",
    "amount": 2500000.00,
    "customerId": "CUST-ACME-001",
    "facilityId": "FAC-789",
    "expiryDate": "2024-01-22T14:23:03Z",
    "poolsAffected": [
      {"poolId": "POOL-OBL-001", "amount": 2500000.00},
      {"poolId": "POOL-SEC-MFG", "amount": 2500000.00}
    ]
  }
}
```

---

*Document Version: 1.0*
*Last Updated: 2024*
*Author: Credit System Architecture Team*
