package com.wingedsheep.engine.view

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.ForetoldComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.player.HotseatControlComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.OpponentsPlayWithHandsRevealed
import com.wingedsheep.sdk.scripting.PlayersPlayWithHandsRevealed
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * What [Visibility] answers about a single card's identity, and that the client view agrees.
 *
 * These are the *engine* propositions. The Gym module asserts the same states through
 * `ObservationVisibilityTest`, which is the point of routing every consumer through one query:
 * two representations, one semantic answer. When a case here and a case there disagree, one of
 * the consumers has grown a local approximation again.
 */
class CardIdentityVisibilityTest : ScenarioTestBase() {

    private val openThoughts = card("Open Thoughts") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility { ability = OpponentsPlayWithHandsRevealed }
    }

    private val openTable = card("Open Table") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility { ability = PlayersPlayWithHandsRevealed }
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
        get() = Visibility(cardRegistry, conditionEvaluator = services.conditionEvaluator)

    init {
        cardRegistry.register(listOf(openThoughts, openTable, publicTop, privateTop))

        test("a player knows their own hand and not an opponent's") {
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

            val clientView = ClientStateTransformer(cardRegistry, predicateEvaluator = services.predicateEvaluator).transform(state, game.player1Id)
            clientView.cards.keys shouldContain ownCard
            clientView.cards.keys shouldNotContain opposingCard
        }

        test("an individual reveal exposes that card and no other, and only to the entitled player") {
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
            val state = withBystander.updateEntity(known) {
                it.with(RevealedToComponent.to(base.player1Id))
            }
            val player2Hand = ZoneKey(base.player2Id, Zone.HAND)

            withClue("the reveal is per card, not per zone") {
                visibility.isZoneVisibleTo(state, player2Hand, base.player1Id) shouldBe false
                visibility.isCardIdentityVisibleTo(state, player2Hand, known, base.player1Id) shouldBe true
                visibility.isCardIdentityVisibleTo(state, player2Hand, unknown, base.player1Id) shouldBe false
            }
            withClue("a third player was not shown it") {
                visibility.isCardIdentityVisibleTo(state, player2Hand, known, bystander) shouldBe false
            }

            val client = ClientStateTransformer(cardRegistry, predicateEvaluator = services.predicateEvaluator)
            client.transform(state, base.player1Id).cards.keys.let {
                it shouldContain known
                it shouldNotContain unknown
            }
            client.transform(state, bystander).cards.keys shouldNotContain known
        }

        test("a whole-hand reveal is zone visibility for its controller alone") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, openThoughts.name)
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Hill Giant")
                .build()

            visibility.isZoneVisibleTo(
                game.state,
                ZoneKey(game.player2Id, Zone.HAND),
                game.player1Id,
            ) shouldBe true
            visibility.isZoneVisibleTo(
                game.state,
                ZoneKey(game.player1Id, Zone.HAND),
                game.player2Id,
            ) shouldBe false
        }

        test("a symmetric whole-hand reveal opens every hand to every player, whoever controls it") {
            val base = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, openTable.name)
                .withCardInHand(1, "Forest")
                .withCardInHand(2, "Mountain")
                .build()
            val bystander = EntityId.of("player-3")
            val state = addBystander(base.state, bystander)
            val players = listOf(base.player1Id, base.player2Id, bystander)

            for (owner in players) {
                for (viewer in players) {
                    withClue("$viewer sees $owner's hand") {
                        visibility.isZoneVisibleTo(state, ZoneKey(owner, Zone.HAND), viewer) shouldBe true
                    }
                }
            }
            withClue("the reveal is public, so spectators see it too") {
                visibility.isZoneVisibleTo(
                    state,
                    ZoneKey(base.player1Id, Zone.HAND),
                    bystander,
                    isSpectator = true,
                ) shouldBe true
            }

            val controllerCard = state.getHand(base.player1Id).single()
            ClientStateTransformer(cardRegistry, predicateEvaluator = services.predicateEvaluator)
                .transform(state, base.player2Id).cards.keys shouldContain controllerCard
        }

        test("a private top-card effect tells only its controller; a public one tells everyone") {
            val privateGame = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, privateTop.name)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Hill Giant")
                .build()
            val privateLibrary = ZoneKey(privateGame.player1Id, Zone.LIBRARY)
            val privateTopCard = privateGame.state.getLibrary(privateGame.player1Id).first()
            val privateSecond = privateGame.state.getLibrary(privateGame.player1Id)[1]

            visibility.isCardIdentityVisibleTo(
                privateGame.state, privateLibrary, privateTopCard, privateGame.player1Id,
            ) shouldBe true
            withClue("only the top card, not the one under it") {
                visibility.isCardIdentityVisibleTo(
                    privateGame.state, privateLibrary, privateSecond, privateGame.player1Id,
                ) shouldBe false
            }
            visibility.isCardIdentityVisibleTo(
                privateGame.state, privateLibrary, privateTopCard, privateGame.player2Id,
            ) shouldBe false

            val publicGame = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, publicTop.name)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .build()
            val publicLibrary = ZoneKey(publicGame.player1Id, Zone.LIBRARY)
            val publicTopCard = publicGame.state.getLibrary(publicGame.player1Id).first()

            listOf(publicGame.player1Id, publicGame.player2Id).forEach { viewer ->
                visibility.isCardIdentityVisibleTo(
                    publicGame.state, publicLibrary, publicTopCard, viewer,
                ) shouldBe true
            }
            withClue("a public reveal reaches a spectator; a private one never does") {
                visibility.isCardIdentityVisibleTo(
                    publicGame.state, publicLibrary, publicTopCard, publicGame.player1Id,
                    isSpectator = true,
                ) shouldBe true
                visibility.isCardIdentityVisibleTo(
                    privateGame.state, privateLibrary, privateTopCard, privateGame.player1Id,
                    isSpectator = true,
                ) shouldBe false
            }
        }

        test("a face-down object on the battlefield or stack is known only to the player who may look") {
            val game = faceDownGame()

            visibility.isCardIdentityVisibleTo(
                game.state, Zone.BATTLEFIELD, game.permanent, game.controller,
            ) shouldBe true
            visibility.isCardIdentityVisibleTo(
                game.state, Zone.BATTLEFIELD, game.permanent, game.opponent,
            ) shouldBe false
            visibility.isCardIdentityVisibleTo(
                game.state, Zone.STACK, game.spell, game.controller,
            ) shouldBe true
            visibility.isCardIdentityVisibleTo(
                game.state, Zone.STACK, game.spell, game.opponent,
            ) shouldBe false
        }

        test("event presentation captures each stack viewer's visibility at event time") {
            val game = faceDownGame()
            // An explicit reveal grants this opponent access, while a spectator remains outside
            // that private audience. The factory delegates the answer to Visibility rather than
            // recoding revealed-to or face-down rules locally.
            val revealed = game.state.updateEntity(game.spell) {
                it.with(RevealedToComponent.to(game.opponent))
            }
            val presentation = EventPresentationFactory(visibility).castSpellIdentity(
                beforeCast = revealed,
                onStack = revealed,
                castFromZone = null,
                entityId = game.spell,
                semanticName = "Hill Giant",
            )

            presentation.nameFor(game.controller) shouldBe "Hill Giant"
            presentation.nameFor(game.opponent) shouldBe "Hill Giant"
            presentation.nameFor(EntityId.of("spectator"), isSpectator = true) shouldBe
                "Face-down creature"

            val laterState = revealed.updateEntity(game.spell) { it.without<RevealedToComponent>() }
            visibility.isCardIdentityVisibleTo(
                laterState, Zone.STACK, game.spell, game.opponent,
            ) shouldBe false
            // Event projection never reconsiders a later state in which that reveal disappeared.
            presentation.nameFor(game.opponent) shouldBe "Hill Giant"
        }

        test("event presentation includes the input actor controlling a face-down spell's seat") {
            val game = faceDownGame()
            val controlled = game.state.updateEntity(game.controller) {
                it.with(HotseatControlComponent(controllerId = game.opponent))
            }

            withClue("the controlling connection receives the same private identity as the seat") {
                visibility.isCardIdentityVisibleTo(
                    controlled, Zone.STACK, game.spell, game.opponent,
                ) shouldBe true
            }

            val presentation = EventPresentationFactory(visibility).castSpellIdentity(
                beforeCast = controlled,
                onStack = controlled,
                castFromZone = null,
                entityId = game.spell,
                semanticName = "Hill Giant",
            )
            presentation.nameFor(game.controller) shouldBe "Hill Giant"
            presentation.nameFor(game.opponent) shouldBe "Hill Giant"
            presentation.nameFor(game.opponent, isSpectator = true) shouldBe
                "Face-down creature"
        }

        // CR 708.5: "At any time, you may look at a face-down spell you control on the stack or a
        // face-down permanent you control … You can't look at face-down cards in any other zone."
        // Exile is one of those other zones, so ownership alone entitles nobody — this was the
        // controller/caster baseline leaking past the two zones the rule scopes it to.
        test("a face-down card in exile is hidden from its owner too") {
            val game = scenario()
                .withPlayers()
                .withCardInExile(1, "Craw Wurm")
                .build()
            val exiled = game.state.getExile(game.player1Id).single()
            val state = game.state.updateEntity(exiled) { it.with(FaceDownComponent) }
            val exileKey = ZoneKey(game.player1Id, Zone.EXILE)

            visibility.isCardIdentityVisibleTo(state, exileKey, exiled, game.player1Id) shouldBe false
            visibility.isCardIdentityVisibleTo(state, exileKey, exiled, game.player2Id) shouldBe false
        }

        // CR 702.143a: "That player may look at that card as long as it remains in exile." The
        // engine stamps FaceDownComponent on a foretold card to mask it from opponents, so the
        // CR 708.5 scoping above must not take the foreteller's own view away with it.
        test("a foretold card stays visible to the player who foretold it") {
            val game = scenario()
                .withPlayers()
                .withCardInExile(1, "Craw Wurm")
                .build()
            val exiled = game.state.getExile(game.player1Id).single()
            val exileKey = ZoneKey(game.player1Id, Zone.EXILE)
            val state = game.state.updateEntity(exiled) {
                it.with(FaceDownComponent)
                    .with(ForetoldComponent(controllerId = game.player1Id, turnForetold = 1))
            }

            visibility.isCardIdentityVisibleTo(state, exileKey, exiled, game.player1Id) shouldBe true
            withClue("the opponent never gets to look") {
                visibility.isCardIdentityVisibleTo(state, exileKey, exiled, game.player2Id) shouldBe false
            }
        }

        // The other half of the same rule: an effect that says you may look *does* entitle you, and
        // that permission is the only way under a face-down exiled card.
        test("a may-play grant is what opens a face-down exiled card, for the granted player only") {
            val game = scenario()
                .withPlayers()
                .withCardInExile(1, "Craw Wurm")
                .build()
            val exiled = game.state.getExile(game.player1Id).single()
            val exileKey = ZoneKey(game.player1Id, Zone.EXILE)
            val state = game.state
                .updateEntity(exiled) { it.with(FaceDownComponent) }
                .addMayPlayPermission(
                    MayPlayPermission(
                        id = EntityId.of("test-may-play"),
                        cardIds = setOf(exiled),
                        controllerId = game.player2Id,
                        permanent = true,
                        timestamp = 0L,
                    )
                )

            visibility.isCardIdentityVisibleTo(state, exileKey, exiled, game.player2Id) shouldBe true
            withClue("the owner still may not look — the grant went to the other player") {
                visibility.isCardIdentityVisibleTo(state, exileKey, exiled, game.player1Id) shouldBe false
            }
        }

        // A spectator's transform runs from a seat's perspective (SpectatorStateBuilder passes the
        // first seat), so every private answer has to be gated on the spectator flag rather than on
        // "is this the viewer's own zone" — otherwise watching a game shows you that seat's hand.
        test("a spectator is told nothing private about the seat they view from") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Forest")
                .build()
            val state = game.state
            val ownHand = ZoneKey(game.player1Id, Zone.HAND)
            val ownSideboard = ZoneKey(game.player1Id, Zone.SIDEBOARD)
            val card = state.getHand(game.player1Id).single()

            visibility.isZoneVisibleTo(state, ownHand, game.player1Id, isSpectator = true) shouldBe false
            visibility.isZoneVisibleTo(state, ownSideboard, game.player1Id, isSpectator = true) shouldBe false
            visibility.isCardIdentityVisibleTo(
                state, ownHand, card, game.player1Id, isSpectator = true,
            ) shouldBe false
            withClue("the same seat, viewed as a player, still sees its own hand") {
                visibility.isZoneVisibleTo(state, ownHand, game.player1Id) shouldBe true
            }
        }
    }

    private class FaceDownGame(
        val state: GameState,
        val permanent: EntityId,
        val spell: EntityId,
        val controller: EntityId,
        val opponent: EntityId,
    )

    /** Player 2 controls a face-down Craw Wurm and has a face-down Hill Giant on the stack. */
    private fun faceDownGame(): FaceDownGame {
        val game = scenario()
            .withPlayers()
            .withCardOnBattlefield(2, "Craw Wurm")
            .withCardInHand(2, "Hill Giant")
            .build()
        val permanent = game.state.getBattlefield().single()
        val spell = game.state.getHand(game.player2Id).single()
        val state = game.state
            .updateEntity(permanent) { it.with(FaceDownComponent) }
            .removeFromZone(ZoneKey(game.player2Id, Zone.HAND), spell)
            .updateEntity(spell) {
                it.with(SpellOnStackComponent(casterId = game.player2Id, castFaceDown = true))
            }
            .copy(stack = listOf(spell))
        return FaceDownGame(state, permanent, spell, game.player2Id, game.player1Id)
    }

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
        for (zone in listOf(Zone.HAND, Zone.LIBRARY, Zone.GRAVEYARD, Zone.EXILE, Zone.BATTLEFIELD)) {
            result = result.copy(zones = result.zones + (ZoneKey(playerId, zone) to emptyList()))
        }
        return result
    }
}
