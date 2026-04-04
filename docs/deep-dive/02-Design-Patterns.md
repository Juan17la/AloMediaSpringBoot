# 02 - Design Patterns

## 1. Why Patterns Matter in This Codebase

Patterns are used where they remove complexity in repetitive or cross-cutting behavior. The goal is not abstraction for abstraction's sake; each pattern supports a concrete operational need.

## 2. Builder Pattern

### Implementation

- Class: ProjectBuilder

### Problem Solved

Project creation requires consistent defaults, especially for timeline initialization and status assignment. Spreading this logic across services risks drift.

### Benefits

- Deterministic default timeline structure.
- Cleaner service method in create flow.
- Single place to evolve default project initialization.

### Consideration

Builder currently contains a `description` setter but entity model does not persist description. This should be aligned.

## 3. Command Pattern (Project History)

### Implementation

- Interface: HistoryCommand
- Commands:
  - CreateProjectHistoryCommand
  - EditProjectHistoryCommand
  - ShareProjectHistoryCommand
  - ExportProjectHistoryCommand

### Problem Solved

Different operations must record history with shared persistence behavior but different event types.

### Benefits

- Encapsulates each history event as an executable object.
- Simplifies service orchestration (`executeCommand`).
- Makes additional event types easy to add.

### Current Limitation

Timeline snapshots are currently passed as `null` by calling services. Snapshot enrichment is a natural next enhancement.

## 4. Observer Pattern (Notifications)

### Implementation

- Observable: ProjectNotificationService
- Observer interface: NotificationObserver
- Concrete observer: EmailNotificationObserver

### Problem Solved

A single domain event (project shared) triggers multiple side effects: persist notification and send email.

### Benefits

- Producer (sharing flow) is decoupled from side effect implementations.
- Easy to add future observers (websocket push, analytics, audit streams).

### Operational Note

Observer execution is synchronous. Failures in observer behavior can impact request latency.

## 5. Factory Pattern (Reports)

### Implementation

- Base abstraction: ReportFactory
- Concrete factories:
  - JsonReportFactory
  - CsvReportFactory
  - SummaryReportFactory
- Selector: ReportFactoryProvider

### Problem Solved

Admin report endpoint serves multiple output formats with different serialization structures.

### Benefits

- Open/closed extension for new formats.
- Keeps report formatting logic outside controller/service orchestration.
- Explicit, testable format behavior.

## 6. Pattern Interaction

These patterns are complementary:

- Service layer orchestrates use cases.
- Builder initializes domain object state.
- Command logs domain events.
- Observer reacts to domain-level occurrences.
- Factory formats reporting output.

This combination creates a codebase that is practical to extend without broad refactoring.
