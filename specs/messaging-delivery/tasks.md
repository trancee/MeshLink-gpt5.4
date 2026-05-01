# Tasks: Messaging & Delivery Pipeline

**Status**: In progress — messaging foundation types bootstrapped

## Phase 1: Foundation

- [x] T001 Define `MessagingConfig` (rate limits, timeouts, buffer sizes, appIdHash)
- [x] T002 [P] Define `MessageIdKey`, `PeerPair`, `InboundMessage` types
- [x] T003 [P] Define `DeliveryOutcome` sealed hierarchy (Delivered, DeliveryFailed)
- [x] T004 [P] Define `SendResult`, `QueuedReason` types
- [x] T005 Write `MessagingFoundationTest`

## Phase 2: Delivery Pipeline

- [ ] T006 Implement `DeliveryPipeline` (send, receive, ack, timeout orchestration)
- [x] T007 Implement `SlidingWindowRateLimiter` (per-peer-pair enforcement)
- [ ] T008 Write `DeliveryPipelineTest` — core send/receive/ack cycle
- [ ] T009 Write `DeliveryPipelineCancelTest` — cancellation behavior
- [ ] T010 Write `DeliveryPipelineDiagnosticTest` — diagnostic emissions
- [ ] T011 Write `DeliveryPipelineBranchCoverageTest` — edge paths
- [ ] T012 Write `DeliveryOutcomeMapperTest`

## Phase 3: Cut-Through Relay

- [ ] T013 Implement `CutThroughBuffer` (FlatBuffer byte surgery, visited_hops append)
- [ ] T014 Wire cut-through into DeliveryPipeline relay path
- [ ] T015 Write `CutThroughBufferTest` — byte surgery correctness
- [ ] T016 Write `CutThroughBufferDiagnosticTest`
- [ ] T017 Write `DeliveryPipelineCutThroughTest`

## Phase 4: Integration

- [ ] T018 Write `CutThroughRelayIntegrationTest` (MeshTestHarness, 3+ nodes)
- [ ] T019 Write `DiagnosticEmissionIntegrationTest`

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify
```

6 actionable tasks complete, 13 remain pending.
