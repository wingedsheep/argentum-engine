package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SearchCardInfo
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.mtg.sets.definitions.one.cards.SerumCoreChimera
import com.wingedsheep.mtg.sets.definitions.pls.cards.FlametongueKavu
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * A question with exactly one legal answer travels with that answer, so the browser can answer it
 * for the player instead of stopping the game (#3340, step 0).
 */
class ForcedDecisionAutoAnswerTest : ScenarioTestBase() {
    init {
        cardRegistry.register(FlametongueKavu)
        cardRegistry.register(SerumCoreChimera)

        /**
         * Flametongue Kavu's enters trigger must target a creature; alone, the Kavu is the only one.
         * [hidden] is the rest of each player's hand and library: cards nobody else can see, which
         * the forks below vary.
         */
        fun kavuTriggerGame(withOpposingCreature: Boolean, hidden: String = "Mountain"): TestGame {
            val builder = scenario().withPlayers()
                .withCardInHand(1, "Flametongue Kavu")
                .withCardInHand(1, hidden)
                .withCardInHand(2, hidden)
                .withCardInLibrary(1, hidden)
                .withCardInLibrary(2, hidden)
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            if (withOpposingCreature) builder.withCardOnBattlefield(2, "Grizzly Bears")
            val game = builder.build()
            game.castSpell(1, "Flametongue Kavu").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            return game
        }

        test("the decider gets the lone legal answer, routed so it can be submitted unchanged") {
            val game = kavuTriggerGame(withOpposingCreature = false)
            val session = newSession(game)
            val update = session.createStateUpdate(game.player1Id, emptyList())
            val answer = autoAnswerOf(update).shouldBeInstanceOf<TargetsResponse>()
            val decision = pendingDecisionOf(update).shouldNotBeNull()
            answer.decisionId shouldBe decision.id
            answer.selectedTargets shouldBe mapOf(0 to listOf(game.findPermanent("Flametongue Kavu")!!))

            session.executeClientAction(game.player1Id, SubmitDecision(game.player1Id, answer), "auto")
                .shouldBeInstanceOf<GameSession.ActionResult.Success>()
            session.getStateForTesting()!!.pendingDecision.shouldBeNull()
        }

        test("the other seat still sees the question being decided, and gets no answer") {
            val game = kavuTriggerGame(withOpposingCreature = false)
            val update = newSession(game).createStateUpdate(game.player2Id, emptyList())
            autoAnswerOf(update).shouldBeNull()
            opponentStatusOf(update).shouldNotBeNull()
        }

        test("a real choice carries no answer") {
            val game = kavuTriggerGame(withOpposingCreature = true)
            autoAnswerOf(newSession(game).createStateUpdate(game.player1Id, emptyList())).shouldBeNull()
        }

        test("the Kavu's forced target is public: everyone can see it is the only creature") {
            val game = kavuTriggerGame(withOpposingCreature = false)
            val update = newSession(game).createStateUpdate(game.player1Id, emptyList())
            autoAnswerOf(update).shouldNotBeNull()
            autoAnswerPublicOf(update) shouldBe true
        }

        test("forking the hidden hands and libraries never changes whether the Kavu's question is public") {
            // A creature card in a hand or library is not a legal target: the question stays
            // forced, and stays public, whatever is hidden.
            for (hidden in listOf("Mountain", "Grizzly Bears", "Flametongue Kavu")) {
                val game = kavuTriggerGame(withOpposingCreature = false, hidden = hidden)
                val update = newSession(game).createStateUpdate(game.player1Id, emptyList())
                autoAnswerOf(update).shouldNotBeNull()
                autoAnswerPublicOf(update) shouldBe true
            }
        }

        test("a forced question whose forcedness hangs on a hidden card is never public") {
            // Serum-Core Chimera: draw a card, then you may discard a nonland card. Forking the
            // drawn card: a land leaves "no" as the only answer, a creature makes it a real choice.
            // The forced fork carries an answer but not the public flag, so its timing can't tell
            // the opponent what was drawn.
            fun chimeraGame(drawn: String): TestGame {
                val game = scenario().withPlayers()
                    .withCardInHand(1, "Forest")
                    .withCardInLibrary(1, drawn)
                    .withCardOnBattlefield(1, "Serum-Core Chimera")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val chimera = game.findPermanent("Serum-Core Chimera")!!
                game.state = game.state.updateEntity(chimera) {
                    it.with(CountersComponent().withAdded(CounterType.OIL, 3))
                }
                val abilityId = SerumCoreChimera.activatedAbilities.single().id
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = chimera, abilityId = abilityId))
                    .error shouldBe null
                game.resolveStack()
                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                return game
            }

            val landDrawn = chimeraGame("Island")
            val forced = newSession(landDrawn).createStateUpdate(landDrawn.player1Id, emptyList())
            autoAnswerOf(forced) shouldBe YesNoResponse(pendingDecisionOf(forced)!!.id, false)
            autoAnswerPublicOf(forced) shouldBe false

            val creatureDrawn = chimeraGame("Grizzly Bears")
            val open = newSession(creatureDrawn).createStateUpdate(creatureDrawn.player1Id, emptyList())
            autoAnswerOf(open).shouldBeNull()
            autoAnswerPublicOf(open) shouldBe false
        }

        test("a question is public only when its candidates came from zones everyone can see") {
            val game = kavuTriggerGame(withOpposingCreature = false)
            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.context.candidateZones shouldBe setOf(Zone.BATTLEFIELD)
            val visibility = Visibility(cardRegistry, conditionEvaluator = PredicateEvaluator(cardRegistry = cardRegistry).conditions)
            ForcedDecisionAutoAnswer.isPublic(game.state, decision, visibility) shouldBe true

            // Unknown provenance fails closed.
            val unknown = decision.copy(context = decision.context.copy(candidateZones = null))
            ForcedDecisionAutoAnswer.isPublic(game.state, unknown, visibility) shouldBe false
            // A pool drawn from a hand or library is hidden however few cards it offers.
            for (zone in listOf(Zone.HAND, Zone.LIBRARY, Zone.SIDEBOARD)) {
                val hidden = decision.copy(context = decision.context.copy(candidateZones = setOf(Zone.BATTLEFIELD, zone)))
                ForcedDecisionAutoAnswer.isPublic(game.state, hidden, visibility) shouldBe false
            }
            // A face-down permanent's identity is hidden, and a filter could have read it.
            val land = game.findPermanents("Mountain").first()
            val withFaceDown = game.state.updateEntity(land) { it.with(FaceDownComponent) }
            ForcedDecisionAutoAnswer.isPublic(withFaceDown, decision, visibility) shouldBe false
            // Only target and card-selection questions can be public: a forced "no" never is.
            val no = YesNoDecision(
                id = "may", playerId = game.player1Id, prompt = "You may", yesAvailable = false,
                context = DecisionContext(candidateZones = setOf(Zone.BATTLEFIELD)),
            )
            ForcedDecisionAutoAnswer.isPublic(game.state, no, visibility) shouldBe false
        }

        val player = EntityId.of("player")
        val card = EntityId.of("card")
        val context = DecisionContext()

        test("a question that is how the decider looks at cards is still asked") {
            // "Look at the top three; you may reveal a creature card from among them" with none.
            val look = SelectCardsDecision(
                id = "look", playerId = player, prompt = "Reveal a creature card", context = context,
                options = emptyList(), minSelections = 0, maxSelections = 0,
                nonSelectableOptions = listOf(card),
            )
            ForcedDecisionAutoAnswer.responseFor(look, look).shouldBeNull()

            val putBack = ReorderLibraryDecision(
                id = "order", playerId = player, prompt = "Put it back", context = context,
                cards = listOf(card), cardInfo = mapOf(card to SearchCardInfo("Forest", "", "Land")),
            )
            ForcedDecisionAutoAnswer.responseFor(putBack, putBack).shouldBeNull()

            // A failed library search names what was looked for; it is answered.
            val search = look.copy(librarySearch = true)
            ForcedDecisionAutoAnswer.responseFor(search, search)
                .shouldBeInstanceOf<CardsSelectedResponse>().selectedCards shouldBe emptyList()
        }

        test("a may whose yes is impossible is answered no; an open may is asked") {
            // "You may discard a creature card" with none in hand: asked, with "no" alone (#3340).
            val may = YesNoDecision(id = "may", playerId = player, prompt = "You may discard a creature card", context = context)
            ForcedDecisionAutoAnswer.responseFor(may, may).shouldBeNull()
            val forced = may.copy(yesAvailable = false)
            ForcedDecisionAutoAnswer.responseFor(forced, forced) shouldBe YesNoResponse("may", false)
        }

        test("the engine's own decision decides forcedness, not the seat's transformed copy") {
            val engine = SelectCardsDecision(
                id = "engine", playerId = player, prompt = "Choose", context = context,
                options = listOf(card), minSelections = 0, maxSelections = 1,
            )
            // Even if the seat's copy came out looking forced, the real question is a choice.
            val seat = engine.copy(id = "epoch:engine", options = emptyList())
            ForcedDecisionAutoAnswer.responseFor(engine, seat).shouldBeNull()
        }
    }

    private fun autoAnswerOf(update: ServerMessage?): DecisionResponse? = when (update) {
        is ServerMessage.StateUpdate -> update.autoAnswer
        is ServerMessage.StateDeltaUpdate -> update.autoAnswer
        else -> error("Expected a state update, got $update")
    }

    private fun autoAnswerPublicOf(update: ServerMessage?): Boolean = when (update) {
        is ServerMessage.StateUpdate -> update.autoAnswerPublic
        is ServerMessage.StateDeltaUpdate -> update.autoAnswerPublic
        else -> error("Expected a state update, got $update")
    }

    private fun pendingDecisionOf(update: ServerMessage?) = when (update) {
        is ServerMessage.StateUpdate -> update.pendingDecision
        is ServerMessage.StateDeltaUpdate -> update.pendingDecision
        else -> error("Expected a state update, got $update")
    }

    private fun opponentStatusOf(update: ServerMessage?) = when (update) {
        is ServerMessage.StateUpdate -> update.opponentDecisionStatus
        is ServerMessage.StateDeltaUpdate -> update.opponentDecisionStatus
        else -> error("Expected a state update, got $update")
    }

    private fun newSession(game: TestGame): GameSession = GameSession(cardRegistry = cardRegistry).also { session ->
        val firstSocket = mockk<WebSocketSession>(relaxed = true) { every { id } returns "first" }
        val secondSocket = mockk<WebSocketSession>(relaxed = true) { every { id } returns "second" }
        session.injectStateForTesting(game.state, mapOf(
            game.player1Id to PlayerSession(firstSocket, game.player1Id, "First"),
            game.player2Id to PlayerSession(secondSocket, game.player2Id, "Second"),
        ))
    }
}
