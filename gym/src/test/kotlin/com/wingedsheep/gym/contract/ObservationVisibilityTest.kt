package com.wingedsheep.gym.contract

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.FACE_DOWN_DISPLAY_NAME
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.OpponentsPlayWithHandsRevealed
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * How a `TrainingObservation` encodes the identity answers `Visibility` gives.
 *
 * The answers themselves are the engine's, and `CardIdentityVisibilityTest` in `rules-engine`
 * asserts them there. What belongs here is Gym's *representation* of them, which is deliberately
 * unlike the client's: a hidden zone reports its true size and carries only the identities this
 * perspective may know, never an opaque slot per unknown card.
 */
class ObservationVisibilityTest : ScenarioTestBase() {

    private val openThoughts = card("Open Thoughts") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility { ability = OpponentsPlayWithHandsRevealed }
    }

    private val publicTop = card("Public Top") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility { ability = RevealTopOfLibrary }
    }

    private val privateTop = card("Private Top") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility { ability = LookAtTopOfLibrary() }
    }

    init {
        cardRegistry.register(listOf(openThoughts, publicTop, privateTop))

        test("a hidden hand reports its size and none of its cards") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Forest")
                .withCardInHand(2, "Mountain")
                .build()
            val view = observe(game.state, game.player1Id)

            zone(view, game.player1Id, Zone.HAND).let {
                it.hidden shouldBe false
                it.cards.size shouldBe it.size
            }
            zone(view, game.player2Id, Zone.HAND).let {
                it.hidden shouldBe true
                it.size shouldBe 1
                it.cards shouldBe emptyList()
            }
        }

        test("a whole-hand reveal drops the hidden flag for the entitled perspective") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, openThoughts.name)
                .withCardInHand(1, "Forest")
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Hill Giant")
                .build()

            zone(observe(game.state, game.player1Id), game.player2Id, Zone.HAND).let {
                it.hidden shouldBe false
                it.cards.size shouldBe it.size
            }
            zone(observe(game.state, game.player2Id), game.player1Id, Zone.HAND).let {
                it.hidden shouldBe true
                it.cards shouldBe emptyList()
            }
        }

        // The representation this whole change exists for: a zone stays hidden while carrying the
        // one identity the perspective legitimately knows. The unknown slot is not emitted at all,
        // so nothing in the schema says *where* among the unknowns the known card sits.
        test("a hidden zone carries its known subset while still reporting its full size") {
            val base = scenario()
                .withPlayers()
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Hill Giant")
                .build()
            val bystander = EntityId.of("player-3")
            val withBystander = addBystander(base.state, bystander)
            val known = withBystander.getHand(base.player2Id).first { id ->
                withBystander.getEntity(id)?.get<CardComponent>()?.name == "Mountain"
            }
            val state = withBystander.updateEntity(known) {
                it.with(RevealedToComponent.to(base.player1Id))
            }

            zone(observe(state, base.player1Id), base.player2Id, Zone.HAND).let {
                it.hidden shouldBe true
                it.size shouldBe 2
                it.cards.map { card -> card.entityId } shouldContainExactly listOf(known)
            }
            zone(observe(state, bystander), base.player2Id, Zone.HAND).cards shouldBe emptyList()
        }

        test("a known top card is the library's only entry, public or private") {
            val privateGame = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, privateTop.name)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Hill Giant")
                .build()
            val privateKnown = privateGame.state.getLibrary(privateGame.player1Id).first()

            zone(observe(privateGame.state, privateGame.player1Id), privateGame.player1Id, Zone.LIBRARY)
                .let {
                    it.hidden shouldBe true
                    it.size shouldBe 2
                    it.cards.map { card -> card.entityId } shouldContainExactly listOf(privateKnown)
                }
            zone(observe(privateGame.state, privateGame.player2Id), privateGame.player1Id, Zone.LIBRARY)
                .cards shouldBe emptyList()

            val publicGame = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, publicTop.name)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .build()
            val publicKnown = publicGame.state.getLibrary(publicGame.player1Id).first()
            listOf(publicGame.player1Id, publicGame.player2Id).forEach { viewer ->
                zone(observe(publicGame.state, viewer), publicGame.player1Id, Zone.LIBRARY)
                    .cards.map { it.entityId } shouldContainExactly listOf(publicKnown)
            }
        }

        test("every modeled player zone is represented with engine-owned visibility") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(2, "Mountain")
                .withCardInSideboard(1, "Forest")
                .withCardInSideboard(2, "Hill Giant")
                .build()
            val view = observe(game.state, game.player1Id)

            game.state.turnOrder.forEach { playerId ->
                view.zones
                    .filter { it.ownerId == playerId }
                    .map { it.zoneType }
                    .shouldContainExactly(TRAINING_OBSERVATION_ZONE_ORDER)
            }

            zone(view, game.player2Id, Zone.COMMAND).let {
                it.hidden shouldBe false
                it.cards.single().name shouldBe "Mountain"
            }
            zone(view, game.player1Id, Zone.SIDEBOARD).let {
                it.hidden shouldBe false
                it.cards.single().name shouldBe "Forest"
            }
            zone(view, game.player2Id, Zone.SIDEBOARD).let {
                it.hidden shouldBe true
                it.size shouldBe 1
                it.cards shouldBe emptyList()
            }
        }

        test("a face-down object keeps its public characteristics and loses its identity") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInHand(2, "Hill Giant")
                .build()
            val permanent = game.state.getBattlefield().single()
            val spell = game.state.getHand(game.player2Id).single()
            val hiddenState = game.state
                .updateEntity(permanent) { it.with(FaceDownComponent) }
                .removeFromZone(ZoneKey(game.player2Id, Zone.HAND), spell)
                .updateEntity(spell) {
                    it.with(SpellOnStackComponent(casterId = game.player2Id, castFaceDown = true))
                }
                .copy(stack = listOf(spell))

            val opponentView = observe(hiddenState, game.player1Id)
            zone(opponentView, game.player2Id, Zone.BATTLEFIELD).cards.single().let {
                it.entityId shouldBe permanent
                it.name shouldBe FACE_DOWN_DISPLAY_NAME
                it.cardDefinitionId shouldBe null
                it.oracleText shouldBe ""
                withClue("the public face-down characteristics survive — it is a 2/2 creature") {
                    it.types shouldBe setOf("CREATURE")
                    it.power shouldBe 2
                    it.toughness shouldBe 2
                }
            }
            opponentView.stack.single().let {
                it.name shouldBe FACE_DOWN_DISPLAY_NAME
                it.oracleText shouldBe ""
            }

            // Independent boundary check: neither zone features nor the sibling stack/action/
            // decision fields can recover the forbidden printed identities in this ordinary state.
            val serialized = Json.encodeToString(TrainingObservation.serializer(), opponentView)
            serialized.contains("Craw Wurm") shouldBe false
            serialized.contains("Hill Giant") shouldBe false

            val controllerView = observe(hiddenState, game.player2Id)
            zone(controllerView, game.player2Id, Zone.BATTLEFIELD).cards.single().name shouldBe "Craw Wurm"
            controllerView.stack.single().name shouldBe "Hill Giant"

            val debugView = observe(hiddenState, game.player1Id, revealAll = true)
            zone(debugView, game.player2Id, Zone.BATTLEFIELD).cards.single().name shouldBe "Craw Wurm"
            debugView.stack.single().name shouldBe "Hill Giant"
        }

    }

    private fun observe(
        state: GameState,
        viewer: EntityId,
        revealAll: Boolean = false,
    ): TrainingObservation = ObservationBuilder(cardRegistry)
        .build(state, viewer, legalActions = emptyList(), revealAll = revealAll)
        .observation as TrainingObservation

    private fun zone(
        observation: TrainingObservation,
        gameOwner: EntityId,
        zone: Zone,
    ): ZoneView = observation.zones.single { it.ownerId == gameOwner && it.zoneType == zone }

    /** A third seat, so "revealed to a player" can be told apart from "revealed to everyone". */
    private fun addBystander(state: GameState, playerId: EntityId): GameState {
        var result = state.withEntity(
            playerId,
            ComponentContainer.of(
                PlayerComponent("Bystander"),
                LifeTotalComponent(20),
                ManaPoolComponent(),
            ),
        ).copy(turnOrder = state.turnOrder + playerId)
        for (zone in TRAINING_OBSERVATION_ZONE_ORDER) {
            result = result.copy(zones = result.zones + (ZoneKey(playerId, zone) to emptyList()))
        }
        return result
    }
}
