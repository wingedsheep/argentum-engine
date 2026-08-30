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
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.OpponentsPlayWithHandsRevealed
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/** Semantic visibility propositions shared by the engine, client view, and Gym observation. */
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
        staticAbility { ability = LookAtTopOfLibrary }
    }

    private val visibility: Visibility
        get() = Visibility(cardRegistry)

    init {
        cardRegistry.register(listOf(openThoughts, publicTop, privateTop))

        test("own and opponent hands use the same identity answer in engine, client, and Gym") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Forest")
                .withCardInHand(2, "Mountain")
                .build()
            val state = game.state
            val ownCard = state.getHand(game.player1Id).single()
            val opposingCard = state.getHand(game.player2Id).single()

            visibility.isCardIdentityVisibleTo(
                state,
                ZoneKey(game.player1Id, Zone.HAND),
                ownCard,
                game.player1Id,
            ) shouldBe true
            visibility.isCardIdentityVisibleTo(
                state,
                ZoneKey(game.player2Id, Zone.HAND),
                opposingCard,
                game.player1Id,
            ) shouldBe false

            val gymView = observe(state, game.player1Id)
            zone(gymView, game.player1Id, Zone.HAND).cards.map { it.entityId } shouldContain ownCard
            zone(gymView, game.player2Id, Zone.HAND).cards.map { it.entityId } shouldNotContain opposingCard

            val clientView = ClientStateTransformer(cardRegistry).transform(state, game.player1Id)
            clientView.cards.keys shouldContain ownCard
            clientView.cards.keys shouldNotContain opposingCard
        }

        test("an opponent-hand reveal is whole-zone visibility only for the entitled controller") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, openThoughts.name)
                .withCardInHand(1, "Forest")
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Hill Giant")
                .build()
            val state = game.state
            val player2Hand = ZoneKey(game.player2Id, Zone.HAND)
            val player1Hand = ZoneKey(game.player1Id, Zone.HAND)

            visibility.isZoneVisibleTo(state, player2Hand, game.player1Id) shouldBe true
            visibility.isZoneVisibleTo(state, player1Hand, game.player2Id) shouldBe false

            val entitled = observe(state, game.player1Id)
            zone(entitled, game.player2Id, Zone.HAND).let {
                it.hidden shouldBe false
                it.cards.size shouldBe it.size
            }

            val unentitled = observe(state, game.player2Id)
            zone(unentitled, game.player1Id, Zone.HAND).let {
                it.hidden shouldBe true
                it.cards shouldBe emptyList()
            }
        }

        test("an individual reveal exposes only that card to the entitled perspective") {
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
            val unknown = withBystander.getHand(base.player2Id).single { it != known }
            val revealed = withBystander.updateEntity(known) {
                it.with(RevealedToComponent.to(base.player1Id))
            }

            visibility.isCardIdentityVisibleTo(
                revealed,
                ZoneKey(base.player2Id, Zone.HAND),
                known,
                base.player1Id,
            ) shouldBe true
            visibility.isCardIdentityVisibleTo(
                revealed,
                ZoneKey(base.player2Id, Zone.HAND),
                known,
                bystander,
            ) shouldBe false

            val entitled = observe(revealed, base.player1Id)
            zone(entitled, base.player2Id, Zone.HAND).let {
                it.hidden shouldBe true
                it.size shouldBe 2
                it.cards.map { card -> card.entityId } shouldContainExactly listOf(known)
            }
            val unentitled = observe(revealed, bystander)
            zone(unentitled, base.player2Id, Zone.HAND).cards shouldBe emptyList()

            val client = ClientStateTransformer(cardRegistry)
            client.transform(revealed, base.player1Id).cards.keys.let {
                it shouldContain known
                it shouldNotContain unknown
            }
            client.transform(revealed, bystander).cards.keys shouldNotContain known

        }

        test("public and private top-card knowledge produce perspective-correct observations") {
            val privateGame = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, privateTop.name)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Hill Giant")
                .build()
            val privateLibrary = privateGame.state.getLibrary(privateGame.player1Id)
            val privateKnown = privateLibrary.first()

            zone(observe(privateGame.state, privateGame.player1Id), gameOwner = privateGame.player1Id, Zone.LIBRARY)
                .cards.map { it.entityId } shouldContainExactly listOf(privateKnown)
            zone(observe(privateGame.state, privateGame.player2Id), gameOwner = privateGame.player1Id, Zone.LIBRARY)
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

        test("face-down battlefield and stack identities are absent from the ordinary sibling surface") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInHand(2, "Hill Giant")
                .build()
            val permanent = game.state.getBattlefield().single()
            val spell = game.state.getHand(game.player2Id).single()
            val handKey = ZoneKey(game.player2Id, Zone.HAND)
            val hiddenState = game.state
                .updateEntity(permanent) { it.with(FaceDownComponent) }
                .removeFromZone(handKey, spell)
                .updateEntity(spell) {
                    it.with(SpellOnStackComponent(casterId = game.player2Id, castFaceDown = true))
                }
                .copy(stack = listOf(spell))

            val opponentView = observe(hiddenState, game.player1Id)
            val faceDownPermanent = zone(
                opponentView,
                game.player2Id,
                Zone.BATTLEFIELD,
            ).cards.single()
            faceDownPermanent.entityId shouldBe permanent
            faceDownPermanent.name shouldBe FACE_DOWN_DISPLAY_NAME
            faceDownPermanent.cardDefinitionId shouldBe null
            faceDownPermanent.oracleText shouldBe ""
            faceDownPermanent.types shouldBe setOf("CREATURE")
            faceDownPermanent.power shouldBe 2
            faceDownPermanent.toughness shouldBe 2
            opponentView.stack.single().let {
                it.name shouldBe FACE_DOWN_DISPLAY_NAME
                it.oracleText shouldBe ""
            }

            val controllerView = observe(hiddenState, game.player2Id)
            zone(controllerView, game.player2Id, Zone.BATTLEFIELD).cards.single().name shouldBe "Craw Wurm"
            controllerView.stack.single().name shouldBe "Hill Giant"

            // Independent boundary check: neither zone features nor the sibling stack/action/
            // decision fields can recover the forbidden printed identities in this ordinary state.
            val serialized = Json.encodeToString(TrainingObservation.serializer(), opponentView)
            serialized.contains("Craw Wurm") shouldBe false
            serialized.contains("Hill Giant") shouldBe false

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

    private fun addBystander(state: GameState, playerId: EntityId): GameState {
        var result = state.withEntity(
            playerId,
            ComponentContainer.of(
                PlayerComponent("Bystander"),
                LifeTotalComponent(20),
                ManaPoolComponent(),
            ),
        ).copy(turnOrder = state.turnOrder + playerId)
        for (zone in listOf(Zone.HAND, Zone.LIBRARY, Zone.GRAVEYARD, Zone.EXILE, Zone.BATTLEFIELD)) {
            result = result.copy(zones = result.zones + (ZoneKey(playerId, zone) to emptyList()))
        }
        return result
    }

}
