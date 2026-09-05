package com.wingedsheep.engine.hidden

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ContinuationFrame
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.TypedEntityReferences
import com.wingedsheep.engine.core.InFlightReferenceProjector
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.MadnessComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class HiddenWorldMaterializerTest : ScenarioTestBase() {

    private val chooseThenShuffle = CardDefinition.sorcery(
        name = "Choose Then Shuffle",
        manaCost = ManaCost.parse("{B}"),
        oracleText = "Choose a color. Shuffle your library.",
        script = CardScript.spell(
            CompositeEffect(
                listOf(
                    ChooseOptionEffect(OptionType.COLOR, storeAs = "chosenColor"),
                    ShuffleLibraryEffect(),
                )
            )
        ),
    )

    private val materializer = HiddenWorldMaterializer(cardRegistry)

    init {
        cardRegistry.register(chooseThenShuffle)

        test("zone membership retains multiplicity and sorted refusal order") {
            val game = scenario().withPlayers()
                .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Swamp")
                .build()
            val ids = game.state.getLibrary(game.player2Id).sortedBy { it.value }
            val first = ids.first()
            val later = ids.last()
            val source = game.state.updateEntity(later) {
                it.with(RevealedToComponent.to(game.player1Id))
            }
            val library = ZoneKey(game.player2Id, Zone.LIBRARY)
            val hand = ZoneKey(game.player2Id, Zone.HAND)
            val battlefield = ZoneKey(game.player2Id, Zone.BATTLEFIELD)
            val withoutFirst = source.zones.mapValues { (_, cards) -> cards.filterNot { it == first } }
            val cases = listOf(
                withoutFirst to "entity is not in a zone",
                (source.zones + (library to (source.zones.getValue(library) + first))) to
                    "entity occurs in zones 2 times",
                (source.zones + (hand to listOf(first))) to "entity occurs in zones 2 times",
                (withoutFirst + (battlefield to listOf(first))) to
                    "supported slots are HAND/LIBRARY; found BATTLEFIELD",
            )
            for ((zones, expectedDetail) in cases) {
                val malformed = source.copy(zones = zones)
                val result = materializer.materialize(
                    malformed,
                    HiddenWorldMaterializationRequest(
                        // Insertion order opposes the required sorted failure order. The later
                        // slot also fails, but its runtime-state refusal must not take precedence.
                        ids.reversed().associateWith { cardRegistry.requireCard("Island") },
                        GameRng.seeded(901L),
                    ),
                ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()
                withClue(expectedDetail) {
                    result.reason.kind shouldBe UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT
                    result.reason.entityId shouldBe first
                    result.reason.details shouldBe listOf(expectedDetail)
                    materializer.materialize(
                        malformed,
                        HiddenWorldMaterializationRequest(
                            mapOf(first to cardRegistry.requireCard("Island")),
                            GameRng.seeded(901L),
                        ),
                    ) shouldBe result
                    materializer.materialize(
                        malformed,
                        HiddenWorldMaterializationRequest(
                            linkedMapOf(later to cardRegistry.requireCard("Island"), first to cardRegistry.requireCard("Island")),
                            GameRng.seeded(901L),
                        ),
                    ) shouldBe result
                    malformed.entities shouldBe source.entities
                    malformed.rng shouldBe source.rng
                }
            }
        }

        test("a Monstrous Emergence hand choice is pinned by the live stack object") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Monstrous Emergence")
                .withCardInHand(2, "Craw Wurm")
                .withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Forest")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val spellId = game.state.getHand(game.player2Id).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Monstrous Emergence"
            }
            val chosenHandId = game.state.getHand(game.player2Id).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Craw Wurm"
            }
            val targetId = game.findPermanent("Grizzly Bears")!!

            game.execute(
                CastSpell(
                    game.player2Id,
                    spellId,
                    listOf(ChosenTarget.Permanent(targetId)),
                    additionalCostPayment = AdditionalCostPayment(beheldCards = listOf(chosenHandId)),
                ),
            ).error shouldBe null
            val source = game.state
            source.getHand(game.player2Id) shouldContain chosenHandId

            val result = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(chosenHandId to cardRegistry.requireCard("Mountain")),
                    futureRng = GameRng.seeded(605L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()

            result.reason.kind shouldBe UnsupportedHiddenWorldKind.IN_FLIGHT_REFERENCES
            result.reason.entityId shouldBe chosenHandId
            source.getEntity(chosenHandId)?.get<CardComponent>()?.name shouldBe "Craw Wurm"
        }

        test("explicit assignments preserve slots and unrelated state while installing future RNG") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Forest")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Craw Wurm")
                .withRngSeed(101L)
                .build()
            val hiddenHandId = game.state.getHand(game.player2Id).single()
            val hiddenLibraryIds = game.state.getLibrary(game.player2Id)
            val assignedIds = listOf(hiddenHandId) + hiddenLibraryIds
            val source = game.state
                .copy(lastCardDrawnThisTurnByPlayer = mapOf(game.player2Id to hiddenHandId))
            val sourceEntities = source.entities.toMap()
            val futureRng = GameRng.seeded(202L)

            val result = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(
                        hiddenHandId to cardRegistry.requireCard("Fiery Temper"),
                        hiddenLibraryIds[0] to cardRegistry.requireCard("Mountain"),
                        hiddenLibraryIds[1] to cardRegistry.requireCard("Island"),
                    ),
                    futureRng = futureRng,
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>()
            val world = result.state

            world.rng shouldBe futureRng
            source.rng shouldBe GameRng.seeded(101L)
            world.entities.keys shouldBe source.entities.keys
            world.zones shouldBe source.zones
            world.lastCardDrawnThisTurnByPlayer shouldBe mapOf(game.player2Id to hiddenHandId)
            cardName(world, hiddenHandId) shouldBe "Fiery Temper"
            world.getEntity(hiddenHandId)?.get<CardComponent>()?.cardDefinitionId shouldBe
                CardEntityFactory.create(cardRegistry.requireCard("Fiery Temper"), game.player2Id)
                    .require<CardComponent>().cardDefinitionId
            world.getEntity(hiddenHandId)?.get<ControllerComponent>() shouldBe ControllerComponent(game.player2Id)
            cardName(world, hiddenLibraryIds[0]) shouldBe "Mountain"
            cardName(world, hiddenLibraryIds[1]) shouldBe "Island"
            withClue("definition-derived components are rebuilt for the assigned identity") {
                world.getEntity(hiddenHandId)?.has<MadnessComponent>() shouldBe true
            }

            val restored = world.copy(
                entities = world.entities + assignedIds.associateWith { source.entities.getValue(it) },
                rng = source.rng,
            )
            withClue("only assigned entity containers and the future RNG may change") {
                restored shouldBe source
            }
            withClue("materialization is purely functional") {
                game.state.getEntity(hiddenHandId)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
                source.entities shouldBe sourceEntities
                world.entities.keys.toList() shouldBe source.entities.keys.toList()
                val retainedEntities = world.entities.toMap()
                materializer.materialize(
                    world,
                    HiddenWorldMaterializationRequest(
                        assignedIds.associateWith { cardRegistry.requireCard("Swamp") },
                        GameRng.seeded(203L),
                    ),
                ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>()
                world.entities shouldBe retainedEntities
            }
        }

        test("a refusal after a valid slot leaves the entire input world unchanged") {
            val game = scenario().withPlayers()
                .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Mountain")
                .build()
            val ids = game.state.getLibrary(game.player2Id).sortedBy { it.value }
            val source = game.state.updateEntity(ids.last()) {
                it.with(RevealedToComponent.to(game.player1Id))
            }
            val originalEntities = source.entities.toMap()
            val originalRng = source.rng
            val result = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    ids.associateWith { cardRegistry.requireCard("Island") },
                    GameRng.seeded(204L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()

            result.reason.kind shouldBe UnsupportedHiddenWorldKind.RUNTIME_STATE
            result.reason.entityId shouldBe ids.last()
            source.entities shouldBe originalEntities
            source.rng shouldBe originalRng
        }

        test("repeated source definitions retain owner-specific validation and per-slot blockers") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInHand(2, "Grizzly Bears")
                .build()
            val source = game.state
            val slots = source.turnOrder.flatMap { source.getHand(it) + source.getLibrary(it) }
            val request = HiddenWorldMaterializationRequest(
                slots.associateWith { cardRegistry.requireCard("Forest") }, GameRng.seeded(321L),
            )
            val world = materializer.materialize(source, request)
                .shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>().state
            for (id in slots) {
                world.getEntity(id)!!.require<CardComponent>().ownerId shouldBe
                    source.getEntity(id)!!.require<CardComponent>().ownerId
            }

            val blockedId = (source.getHand(game.player1Id) + source.getLibrary(game.player1Id))
                .maxBy { it.value }
            val blocked = source.updateEntity(blockedId) { it.with(RevealedToComponent.to(game.player2Id)) }
            val originalEntities = blocked.entities.toMap()
            val result = materializer.materialize(blocked, request)
                .shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()
            result.reason.kind shouldBe UnsupportedHiddenWorldKind.RUNTIME_STATE
            result.reason.entityId shouldBe blockedId
            result.reason.details shouldBe listOf("RevealedToComponent")
            blocked.entities shouldBe originalEntities
            blocked.rng shouldBe source.rng
        }

        test("source validation observes registry changes between materialization requests") {
            val game = scenario().withPlayers().withCardInHand(2, "Fiery Temper").build()
            val registry = CardRegistry(cardRegistry)
            val localMaterializer = HiddenWorldMaterializer(registry)
            val id = game.state.getHand(game.player2Id).single()
            val request = HiddenWorldMaterializationRequest(
                mapOf(id to cardRegistry.requireCard("Forest")), GameRng.seeded(322L),
            )
            localMaterializer.materialize(game.state, request)
                .shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>()

            registry.register(cardRegistry.requireCard("Fiery Temper").copy(keywordAbilities = emptyList()))
            val result = localMaterializer.materialize(game.state, request)
                .shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()
            result.reason.kind shouldBe UnsupportedHiddenWorldKind.RUNTIME_STATE
            result.reason.entityId shouldBe id
            result.reason.details shouldBe listOf("MadnessComponent")
        }

        test("caller-generated assignments are reproducible and do not consume source randomness") {
            val game = scenario()
                .withPlayers()
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withRngSeed(303L)
                .build()
            val slots = game.state.getLibrary(game.player2Id)
            val candidates = listOf("Plains", "Island", "Swamp", "Mountain")
                .map(cardRegistry::requireCard)
            val futureRng = GameRng.seeded(404L)

            fun worldFor(assignmentSeed: Long): GameState {
                val (definitions, _) = GameRng.seeded(assignmentSeed).shuffle(candidates)
                val request = HiddenWorldMaterializationRequest(slots.zip(definitions).toMap(), futureRng)
                return materializer.materialize(game.state, request)
                    .shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>()
                    .state
            }

            worldFor(7L) shouldBe worldFor(7L)
            val worlds = (7L..22L).map { seed ->
                val world = worldFor(seed)
                world.getLibrary(game.player2Id).map { cardName(world, it) }
            }
            worlds.distinct().size shouldNotBe 1
            game.state.rng shouldBe GameRng.seeded(303L)
            worlds.forEach { it.toSet() shouldBe setOf("Plains", "Island", "Swamp", "Mountain") }
        }

        test("a hidden card carrying battlefield last-known information is refused") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val source = ZoneTransitionService.moveToZone(
                game.state,
                bears,
                Zone.HAND,
            ).state
            source.getEntity(bears)?.has<LastKnownPermanentComponent>() shouldBe true

            val result = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(bears to cardRegistry.requireCard("Mountain")),
                    futureRng = GameRng.seeded(505L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()

            result.reason.kind shouldBe UnsupportedHiddenWorldKind.RUNTIME_STATE
            result.reason.entityId shouldBe bears
            result.reason.details shouldContain LastKnownPermanentComponent::class.simpleName
            source.getEntity(bears)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"

            val cleanAgain = ZoneTransitionService.moveToZone(source, bears, Zone.LIBRARY).state
            cleanAgain.getEntity(bears)?.has<LastKnownPermanentComponent>() shouldBe false
            cleanAgain.getEntity(bears)?.has<ControllerComponent>() shouldBe false
            val cleanWorld = materializer.materialize(
                cleanAgain,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(bears to cardRegistry.requireCard("Mountain")),
                    futureRng = GameRng.seeded(506L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>().state
            withClue("a zone transition strips the controller component and the rewrite keeps it stripped") {
                cleanWorld.getEntity(bears)?.has<ControllerComponent>() shouldBe false
            }
        }

        test("a slot someone has already been shown is refused rather than keeping the reveal") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(2, "Grizzly Bears")
                .build()
            val revealedId = game.state.getHand(game.player2Id).single()
            val source = game.state.updateEntity(revealedId) {
                it.with(RevealedToComponent.to(game.player1Id))
            }

            val result = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(revealedId to cardRegistry.requireCard("Mountain")),
                    futureRng = GameRng.seeded(515L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()

            result.reason.kind shouldBe UnsupportedHiddenWorldKind.RUNTIME_STATE
            result.reason.entityId shouldBe revealedId
            withClue("preserving the reveal would point player 1 at a card they never saw") {
                result.reason.details shouldContain RevealedToComponent::class.simpleName
            }
        }

        test("a live stack pins the slots it targets and leaves the rest materializable") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(1, "Mountain")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInLibrary(2, "Hill Giant")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            val handId = game.state.getHand(game.player2Id).single()
            val libraryId = game.state.getLibrary(game.player2Id).single()
            val target = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Shock", targetId = target).error shouldBe null
            val source = game.state
            source.stack.size shouldBe 1

            withClue("a search root with a spell on the stack is the normal MCTS position") {
                val world = materializer.materialize(
                    source,
                    HiddenWorldMaterializationRequest(
                        slotAssignments = mapOf(handId to cardRegistry.requireCard("Mountain")),
                        futureRng = GameRng.seeded(606L),
                    ),
                ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>().state
                cardName(world, handId) shouldBe "Mountain"
                world.stack shouldBe source.stack
            }

            // A stack object can name a card in a hidden zone (Chosen a card in a library or hand).
            // Shock targets a permanent, so the library slot is attached to its TargetsComponent
            // directly to exercise the guard on the shape the engine stores.
            val stackId = source.stack.single()
            val targeted = source.updateEntity(stackId) { container ->
                val existing = container.require<TargetsComponent>()
                container.with(
                    existing.copy(
                        targets = existing.targets +
                            ChosenTarget.Card(libraryId, game.player2Id, Zone.LIBRARY)
                    )
                )
            }

            val result = materializer.materialize(
                targeted,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(libraryId to cardRegistry.requireCard("Mountain")),
                    futureRng = GameRng.seeded(607L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()

            result.reason.kind shouldBe UnsupportedHiddenWorldKind.IN_FLIGHT_REFERENCES
            result.reason.entityId shouldBe libraryId
            result.reason.details shouldContain "slot is conservatively pinned by in-flight execution"
            cardName(targeted, libraryId) shouldBe "Hill Giant"
        }

        test("a Mind Rot pause pins the targeted opponent's referenced hand slots but not library slots") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Mind Rot")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Swamp")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Craw Wurm")
                .withCardInLibrary(2, "Forest")
                .withRngSeed(615L)
                .build()
            game.castSpellTargetingPlayer(1, "Mind Rot", 2).error shouldBe null
            game.resolveStack()

            // These are hidden opponent slots from player 1's search root. Mind Rot has already
            // targeted player 2, but the actual discard selection remains player 2's decision.
            val source = game.state
            val sourceDecision = source.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            sourceDecision.playerId shouldBe game.player2Id
            source.continuationStack.isNotEmpty() shouldBe true
            val referencedHandId = sourceDecision.options.first()
            val unrelatedLibraryId = source.getLibrary(game.player2Id).single()
            val referencedHandName = cardName(source, referencedHandId)
            val sourceRng = source.rng

            val atomicFailure = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(
                        unrelatedLibraryId to cardRegistry.requireCard("Mountain"),
                        referencedHandId to cardRegistry.requireCard("Island"),
                    ),
                    futureRng = GameRng.seeded(616L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()

            atomicFailure.reason.kind shouldBe UnsupportedHiddenWorldKind.IN_FLIGHT_REFERENCES
            atomicFailure.reason.entityId shouldBe referencedHandId
            atomicFailure.reason.details shouldContain "slot is conservatively pinned by in-flight execution"
            cardName(source, referencedHandId) shouldBe referencedHandName
            cardName(source, unrelatedLibraryId) shouldBe "Forest"
            source.rng shouldBe sourceRng

            val world = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(unrelatedLibraryId to cardRegistry.requireCard("Mountain")),
                    futureRng = GameRng.seeded(617L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>().state

            cardName(world, unrelatedLibraryId) shouldBe "Mountain"
            world.rng shouldBe GameRng.seeded(617L)
            val worldDecision = world.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            val discardedIds = worldDecision.options.take(2)
            val resumed = actionProcessor.process(
                world,
                SubmitDecision(
                    game.player2Id,
                    CardsSelectedResponse(worldDecision.id, discardedIds),
                ),
            ).result

            resumed.error shouldBe null
            resumed.state.pendingDecision shouldBe null
            resumed.state.continuationStack shouldBe emptyList()
            resumed.state.stack shouldBe emptyList()
            discardedIds.forEach { discardedId ->
                resumed.state.getGraveyard(game.player2Id) shouldContain discardedId
            }
            cardName(resumed.state, unrelatedLibraryId) shouldBe "Mountain"
            resumed.state.rng shouldBe GameRng.seeded(617L)
        }

        test("an empty assignment traverses a Mind Rot pause and installs the caller RNG") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Mind Rot")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Swamp")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Craw Wurm")
                .withRngSeed(620L)
                .build()
            game.castSpellTargetingPlayer(1, "Mind Rot", 2).error shouldBe null
            game.resolveStack()
            val source = game.state
            source.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            source.continuationStack.isNotEmpty() shouldBe true

            val futureRng = GameRng.seeded(621L)
            val world = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(emptyMap(), futureRng),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>().state

            world shouldBe source.copy(rng = futureRng)
            (world.entities === source.entities) shouldBe true
        }

        test("a paused continuation consumes the caller RNG only when its later shuffle executes") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Choose Then Shuffle")
                .withCardOnBattlefield(1, "Swamp")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withRngSeed(622L)
                .build()
            game.castSpell(1, "Choose Then Shuffle").error shouldBe null
            game.resolveStack()

            val source = game.state
            val decision = source.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            source.continuationStack.isNotEmpty() shouldBe true
            val opponentSlot = source.getLibrary(game.player2Id).single()
            val libraryBefore = source.getLibrary(game.player1Id)
            val futureRng = GameRng.seeded(623L)
            val (expectedOrder, expectedAdvancedRng) = futureRng.shuffle(libraryBefore)

            val world = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(opponentSlot to cardRegistry.requireCard("Mountain")),
                    futureRng = futureRng,
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>().state

            withClue("materialization freezes the already-realized pause and changes only future inputs") {
                world.pendingDecision shouldBe source.pendingDecision
                world.continuationStack shouldBe source.continuationStack
                world.getLibrary(game.player1Id) shouldBe libraryBefore
                cardName(world, opponentSlot) shouldBe "Mountain"
                world.rng shouldBe futureRng
            }

            val resumed = actionProcessor.process(
                world,
                SubmitDecision(
                    game.player1Id,
                    OptionChosenResponse(decision.id, optionIndex = 0),
                ),
            ).result

            resumed.error shouldBe null
            resumed.state.pendingDecision shouldBe null
            resumed.state.continuationStack shouldBe emptyList()
            resumed.state.stack shouldBe emptyList()
            resumed.state.getLibrary(game.player1Id) shouldBe expectedOrder
            resumed.state.rng shouldBe expectedAdvancedRng
            cardName(resumed.state, opponentSlot) shouldBe "Mountain"
        }

        test("an incomplete paused-state projection refuses the whole request before assignments or RNG") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withRngSeed(618L)
                .build()
            val handId = game.state.getHand(game.player2Id).single()
            val libraryId = game.state.getLibrary(game.player2Id).single()
            val pending = SelectCardsDecision(
                id = "untraversable",
                playerId = game.player1Id,
                prompt = "choose",
                context = DecisionContext(),
                options = emptyList(),
                minSelections = 0,
                maxSelections = 0,
            )
            val source = game.state.copy(pendingDecision = pending)
            val sourceRng = source.rng
            val rejectingMaterializer = HiddenWorldMaterializer(
                cardRegistry,
                object : InFlightReferenceProjector {
                    override fun project(stackObject: ComponentContainer) =
                        TypedEntityReferences.Projection.Complete(emptyList())

                    override fun project(decision: PendingDecision) =
                        TypedEntityReferences.Projection.Incomplete("test", "forced")

                    override fun project(frame: ContinuationFrame) =
                        TypedEntityReferences.Projection.Complete(emptyList())
                },
            )

            val result = rejectingMaterializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(
                        handId to cardRegistry.requireCard("Mountain"),
                        libraryId to cardRegistry.requireCard("Island"),
                    ),
                    futureRng = GameRng.seeded(619L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()

            result.reason.kind shouldBe UnsupportedHiddenWorldKind.IN_FLIGHT_REFERENCES
            result.reason.entityId shouldBe null
            rejectingMaterializer.materialize(
                source,
                HiddenWorldMaterializationRequest(emptyMap(), GameRng.seeded(619L)),
            ) shouldBe result
            result.reason.details shouldContain "could not traverse pending decision test: forced"
            cardName(source, handId) shouldBe "Grizzly Bears"
            cardName(source, libraryId) shouldBe "Forest"
            source.rng shouldBe sourceRng
        }

        test("a DFC back face cannot be materialized directly into a hidden zone") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(2, "Grizzly Bears")
                .build()
            val hiddenId = game.state.getHand(game.player2Id).single()

            val result = materializer.materialize(
                game.state,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(
                        hiddenId to cardRegistry.requireCard("Test DFC Back")
                    ),
                    futureRng = GameRng.seeded(650L),
                ),
            )

            val unsupported =
                result.shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()
            unsupported.reason.kind shouldBe UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT
            unsupported.reason.entityId shouldBe hiddenId
            unsupported.reason.details shouldContain
                "replacement HAND/LIBRARY identity is a DFC back face: Test DFC Back"
        }

        test("an unregistered replacement is a typed unsupported request") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(2, "Grizzly Bears")
                .build()
            val hiddenId = game.state.getHand(game.player2Id).single()
            val unregistered = card("Unregistered Hypothesis") {
                manaCost = "{1}"
                typeLine = "Sorcery"
            }

            val result = materializer.materialize(
                game.state,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(hiddenId to unregistered),
                    futureRng = GameRng.seeded(707L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()

            result.reason.kind shouldBe UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT
            result.reason.entityId shouldBe hiddenId
        }

        test("a materialized world can continue through ordinary engine simulation") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Grizzly Bears")
                .build()
            val hiddenId = game.state.getHand(game.player1Id).single()
            val world = materializer.materialize(
                game.state,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(hiddenId to cardRegistry.requireCard("Forest")),
                    futureRng = GameRng.seeded(808L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Materialized>().state

            val simulated = actionProcessor.process(
                world,
                PlayLand(game.player1Id, hiddenId),
            ).result

            simulated.error shouldBe null
            simulated.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD)) shouldContain hiddenId
            cardName(simulated.state, hiddenId) shouldBe "Forest"
        }
    }

    private fun cardName(state: GameState, entityId: EntityId): String? =
        state.getEntity(entityId)?.get<CardComponent>()?.name
}
