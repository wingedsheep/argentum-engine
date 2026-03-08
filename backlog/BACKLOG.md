# Consolidated Backlog

Prioritized checklist across all improvement areas. Each item references its source document for details.

---

## Priority 1 — High Impact, Do First

### Game Server Architecture
- [ ] Replace callback injection with Spring events ([architecture #2](game-server-architecture.md#2-replace-callback-injection-with-spring-events)) — unblocks handler splits
- [ ] Break up GameSession god class (1,011 lines) ([architecture #1](game-server-architecture.md#1-break-up-gamesession-god-class-1011-lines))
- [ ] Break up ConnectionHandler (519 lines) ([architecture #3](game-server-architecture.md#3-break-up-connectionhandler-519-lines))
- [ ] Break up GamePlayHandler (696 lines) ([architecture #4](game-server-architecture.md#4-break-up-gameplayhandler-696-lines))
- [ ] Split LobbyHandler by feature ([architecture #5](game-server-architecture.md#5-split-lobbyhandler-by-feature))
- [ ] Formalize PlayerIdentity state machine ([architecture #6](game-server-architecture.md#6-formalize-playeridentity-state-machine))

### Game Server Scalability
- [x] Delta state updates — partial diffs instead of full state on every action ([scalability #1](game-server-scalability.md#1-full-state-sent-on-every-action-no-delta-updates)) — *in progress, almost finished*
- [ ] Move JSON serialization outside per-session lock ([scalability #2](game-server-scalability.md#2-json-serialization-under-per-session-lock-messagesenderkt32))
- [ ] Cache legal actions on GameState (lazy property) ([scalability #3](game-server-scalability.md#3-legal-actions-recalculated-from-scratch-every-update-legalactionscalculator))
- [ ] Single traversal for both player state transforms ([scalability #4](game-server-scalability.md#4-state-projection-called-twice-per-action-clientstatetransformerkt61))
- [ ] Cap/stream replay snapshots ([scalability #5](game-server-scalability.md#5-unbounded-replay-snapshot-accumulation-gamesessionkt94))
- [ ] Cap/stream game logs ([scalability #6](game-server-scalability.md#6-unbounded-game-log-accumulation-gamesessionkt87))

### SDK Composability
- [ ] `PayCost.PayLife` ([sdk-gaps #1a](sdk-composability-gaps.md#1a-paycostpaylifeamount-dynamicamount))
- [ ] `PayCost.Exile` ([sdk-gaps #1c](sdk-composability-gaps.md#1c-paycostexilefilter-gameobjectfilter-zone-zone-count-int))
- [x] `LifeLostEvent` ([sdk-gaps #2c](sdk-composability-gaps.md#2c-lifechangedevent-unified-life-tracking))
- [x] `HasKeyword` condition ([sdk-gaps #3b](sdk-composability-gaps.md#3b-haskeywordkeyword-keyword-condition-for-source-entity))
- [x] `AddCreatureTypeEffect` ([sdk-gaps #6a](sdk-composability-gaps.md#6a-addcreaturetypeeffectsubtype-creaturetype-target-effecttarget-duration-duration))

### Web Client
- [ ] Break up god components (DecisionUI, GameCard, DeckBuilderOverlay, GameUI, App) ([web-client #1](web-client-improvements.md#1-break-up-god-components))
- [ ] Add React error boundaries ([web-client #3](web-client-improvements.md#3-add-react-error-boundaries))
- [ ] Consolidate styling — replace hard-coded colors/sizes with CSS variables ([web-client #2](web-client-improvements.md#2-consolidate-styling-approach))

---

## Priority 2 — Medium Impact

### Game Server Architecture
- [ ] Fix thread safety gaps (concurrent collections) ([architecture #7](game-server-architecture.md#7-fix-thread-safety-gaps))
- [ ] Standardize error handling strategy ([architecture #8](game-server-architecture.md#8-standardize-error-handling-strategy))
- [ ] Add unit tests for extracted classes ([architecture #9](game-server-architecture.md#9-improve-unit-test-coverage))
- [ ] Deduplicate state masking logic ([architecture #10](game-server-architecture.md#10-deduplicate-state-masking))
- [ ] Clean up protocol message types (polymorphic serialization) ([architecture #11](game-server-architecture.md#11-clean-up-protocol-message-types))

### Game Server Scalability
- [ ] Configure server thread pool explicitly ([scalability #7](game-server-scalability.md#7-no-server-thread-pool-configuration-applicationyml))
- [ ] Enable virtual threads ([scalability #7](game-server-scalability.md#7-no-server-thread-pool-configuration-applicationyml))
- [ ] Prune session lock map on disconnect ([scalability #8](game-server-scalability.md#8-session-lock-map-never-pruned-sessionregistrykt79))
- [ ] Scale disconnect scheduler beyond 2 threads ([scalability #9](game-server-scalability.md#9-disconnect-scheduler-is-fixed-at-2-threads-sessionregistrykt30))
- [ ] Fix Redis cache divergence on TTL expiry ([scalability #10](game-server-scalability.md#10-redis-cache-divergence))

### SDK Composability
- [ ] `HighestPowerAmong` / `LowestToughnessAmong` DynamicAmount ([sdk-gaps #4a](sdk-composability-gaps.md#4a-highestpoweramongfilter-gameobjectfilter--lowesttoughnessamong))
- [ ] `OpponentLifeTotal` / `OpponentHandSize` DynamicAmount ([sdk-gaps #4b](sdk-composability-gaps.md#4b-opponentlifetotal--opponenthandsize))
- [ ] `SpellCastFromZoneEvent` (cast source tracking) ([sdk-gaps #2b](sdk-composability-gaps.md#2b-spellcastfromzoneevent-cast-source-tracking))
- [ ] `PayCost.TapPermanents` (Convoke, tap abilities) ([sdk-gaps #1b](sdk-composability-gaps.md#1b-paycosttappermanentscount-int-filter-gameobjectfilter))
- [ ] `PayCost.Composite` ([sdk-gaps #1d](sdk-composability-gaps.md#1d-paycostcompositecosts-listpaycost))
- [x] `CantAttackEffect` ([sdk-gaps #9a](sdk-composability-gaps.md#9a-cantattackeffectfilter-gameobjectfilter-duration-duration))
- [ ] `ConditionalStaticAbility` ([sdk-gaps #7a](sdk-composability-gaps.md#7a-conditionalstaticabilitycondition-condition-ability-staticabilitytype))

### Web Client
- [ ] Modularize message handlers by domain ([web-client #4](web-client-improvements.md#4-modularize-message-handlers))
- [ ] Extract custom hooks from components ([web-client #5](web-client-improvements.md#5-extract-custom-hooks-from-components))
- [ ] Improve shared component library (Modal, CardGrid) ([web-client #6](web-client-improvements.md#6-improve-shared-component-library))
- [ ] Lazy load feature routes ([web-client #7](web-client-improvements.md#7-lazy-load-feature-routes))
- [ ] Add accessibility fundamentals ([web-client #8](web-client-improvements.md#8-add-accessibility-fundamentals))

---

## Priority 3 — Nice to Have

### Game Server Architecture
- [ ] Extract auto-pass rules into data-driven configuration ([architecture #12](game-server-architecture.md#12-extract-auto-pass-rules-into-data-driven-configuration))
- [ ] Add structured logging with MDC context ([architecture #13](game-server-architecture.md#13-add-structured-logging-with-context))
- [ ] Consider coroutine-based concurrency ([architecture #14](game-server-architecture.md#14-consider-coroutine-based-concurrency))
- [ ] Consolidate configuration classes ([architecture #15](game-server-architecture.md#15-consolidate-configuration-classes))

### Game Server Scalability
- [ ] Dynamic card registry (ServiceLoader hot-reload) ([scalability #11](game-server-scalability.md#11-card-registry-is-hard-coded-gamebeansconfigt20-35))
- [ ] StateProjector O(n^2) dependency resolution ([scalability #13](game-server-scalability.md#13-stateprojector-on-dependency-resolution))

### SDK Composability
- [ ] `ManaWasSpentOfColor` condition ([sdk-gaps #3a](sdk-composability-gaps.md#3a-manawaspentofcolorcolor-color))
- [ ] Mana events (`ManaProducedEvent`, `ManaSpentEvent`) ([sdk-gaps #2a](sdk-composability-gaps.md#2a-mana-events-manaproducedevent-manaspentevent))
- [ ] Damage prevention/redirection primitives ([sdk-gaps #5a/5b](sdk-composability-gaps.md#gap-5-damage-prevention--redirection))
- [ ] Copy effects with modifications ([sdk-gaps #8a](sdk-composability-gaps.md#8a-copywithmodificationsmodifications-listcopymodification))
- [ ] `MustBlockEffect` / `AttackRestriction` ([sdk-gaps #9b/9c](sdk-composability-gaps.md#9b-mustblockeffecttarget-effecttarget-duration-duration))
- [ ] `NumberOfColorsAmong` DynamicAmount ([sdk-gaps #4c](sdk-composability-gaps.md#4c-numberofcolorsamongfilter-gameobjectfilter))
- [ ] `RemoveCreatureTypeEffect` ([sdk-gaps #6b](sdk-composability-gaps.md#6b-removecreaturetypeeffectsubtype-creaturetype-target-effecttarget-duration-duration))

### Web Client
- [ ] Type-safe message handling (discriminated union) ([web-client #9](web-client-improvements.md#9-type-safe-message-handling))
- [ ] Improve delta applicator robustness ([web-client #10](web-client-improvements.md#10-improve-delta-applicator-robustness))
- [ ] Standardize store access patterns ([web-client #11](web-client-improvements.md#11-standardize-store-access-patterns))
- [ ] Add unit/integration testing infrastructure (Vitest) ([web-client #12](web-client-improvements.md#12-testing-infrastructure))
