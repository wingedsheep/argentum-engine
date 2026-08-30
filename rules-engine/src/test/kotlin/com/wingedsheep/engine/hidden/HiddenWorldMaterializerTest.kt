package com.wingedsheep.engine.hidden

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.MadnessComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class HiddenWorldMaterializerTest : ScenarioTestBase() {

    private val materializer = HiddenWorldMaterializer(cardRegistry)

    init {
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
                .updateEntity(hiddenHandId) { it.with(RevealedToComponent.to(game.player1Id)) }
                .copy(lastCardDrawnThisTurnByPlayer = mapOf(game.player2Id to hiddenHandId))
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
            world.getEntity(hiddenHandId)?.get<RevealedToComponent>() shouldBe
                source.getEntity(hiddenHandId)?.get<RevealedToComponent>()
            cardName(world, hiddenHandId) shouldBe "Fiery Temper"
            cardName(world, hiddenLibraryIds[0]) shouldBe "Mountain"
            cardName(world, hiddenLibraryIds[1]) shouldBe "Island"
            world.getEntity(hiddenHandId)?.has<MadnessComponent>() shouldBe true

            val restored = world.copy(
                entities = world.entities + assignedIds.associateWith { source.entities.getValue(it) },
                rng = source.rng,
            )
            withClue("only assigned entity containers and the future RNG may change") {
                restored shouldBe source
            }
            withClue("materialization is purely functional") {
                game.state.getEntity(hiddenHandId)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
            }
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
            cleanWorld.getEntity(bears)?.has<ControllerComponent>() shouldBe false
        }

        test("a real stack state is refused instead of being guessed through") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(1, "Mountain")
                .withCardInHand(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            val hiddenId = game.state.getHand(game.player2Id).single()
            val target = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Shock", targetId = target).error shouldBe null
            val source = game.state
            source.stack.size shouldBe 1

            val result = materializer.materialize(
                source,
                HiddenWorldMaterializationRequest(
                    slotAssignments = mapOf(hiddenId to cardRegistry.requireCard("Mountain")),
                    futureRng = GameRng.seeded(606L),
                ),
            ).shouldBeInstanceOf<HiddenWorldMaterializationResult.Unsupported>()

            result.reason.kind shouldBe UnsupportedHiddenWorldKind.IN_FLIGHT_REFERENCES
            result.reason.details shouldContain "stackDepth=1"
            source.getEntity(hiddenId)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
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
