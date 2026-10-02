package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

class LibraryOfLengScenarioTest : ScenarioTestBase() {
    init {
        val payoff = card("Library Test Discard Payoff") {
            typeLine = "Enchantment"
            triggeredAbility {
                trigger = Triggers.anOpponent.discards()
                effect = Effects.DealDamage(2, EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.TriggeringPlayer))
            }
        }
        cardRegistry.register(payoff)
        fun board(cards: List<String> = listOf("Grizzly Bears")) = scenario()
            .withPlayers("Owner", "Opponent")
            .withCardOnBattlefield(1, "Library of Leng")
            .apply { cards.forEach { withCardInHand(1, it) } }
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(1).withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun apply(game: TestGame, effect: Effect, opponent: Boolean = false) {
            val result = services.effectExecutorRegistry.execute(game.state, effect,
                EffectContext(sourceId = null, controllerId = if (opponent) game.player2Id else game.player1Id,
                    targets = listOf(ChosenTarget.Player(game.player1Id))))
            result.error shouldBe null
            game.state = result.state
        }
        fun choose(game: TestGame, useLibrary: Boolean = true): ExecutionResult {
            val decision = game.state.pendingDecision as ChooseOptionDecision
            decision.playerId shouldBe game.player1Id
            return game.submitDecision(OptionChosenResponse(decision.id, if (useLibrary) 0 else decision.options.lastIndex))
                .also { it.error shouldBe null }
        }
        test("opponent discard effect offers each card before moving and can mix destinations") {
            val game = board(listOf("Grizzly Bears", "Mountain"))
            val bear = game.findCardsInHand(1, "Grizzly Bears").single()
            val mountain = game.findCardsInHand(1, "Mountain").single()
            apply(game, Patterns.Hand.discardHand(EffectTarget.ContextTarget(0)), opponent = true)
            game.handSize(1) shouldBe 2
            choose(game)
            game.handSize(1) shouldBe 2
            choose(game, false)
            game.state.getLibrary(game.player1Id).first() shouldBe bear
            game.state.getGraveyard(game.player1Id) shouldBe listOf(mountain)
            game.handSize(1) shouldBe 0
        }
        test("own discard effect can be declined") {
            val game = board()
            apply(game, Patterns.Hand.discardHand())
            choose(game, false)
            game.graveyardSize(1) shouldBe 1
            game.state.getLibrary(game.player1Id).size shouldBe 1
        }
        test("multiple library discards can be ordered top to bottom") {
            val game = board(listOf("Grizzly Bears", "Mountain", "Island"))
            val bear = game.findCardsInHand(1, "Grizzly Bears").single()
            val mountain = game.findCardsInHand(1, "Mountain").single()
            val island = game.findCardsInHand(1, "Island").single()
            apply(game, Patterns.Hand.discardHand())
            repeat(3) { choose(game) }
            val order = game.state.pendingDecision as SelectCardsDecision
            order.ordered shouldBe true
            game.selectCards(listOf(island, bear, mountain)).error shouldBe null
            game.state.getLibrary(game.player1Id).take(3) shouldBe listOf(island, bear, mountain)
            game.graveyardSize(1) shouldBe 0
        }
        test("discard costs and cleanup discards do not offer Leng") {
            val game = board()
            val id = game.state.getHand(game.player1Id).single()
            val result = services.zones.discardCards(game.state, game.player1Id, listOf(id))
            result.state.pendingDecision shouldBe null
            result.state.getGraveyard(game.player1Id) shouldBe listOf(id)
        }
        test("mill does not offer Leng") {
            val game = board()
            apply(game, Patterns.Library.mill(1))
            game.state.pendingDecision shouldBe null
            game.graveyardSize(1) shouldBe 1
            game.handSize(1) shouldBe 1
        }
        test("a discarded card still triggers a discard payoff without being revealed in the public log") {
            // Enter the test payoff through the normal scenario setup.
            val setup = scenario().withPlayers("Owner", "Opponent")
                .withCardOnBattlefield(1, "Library of Leng").withCardOnBattlefield(2, payoff.name)
                .withCardInHand(1, "Grizzly Bears").withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).withPriorityPlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            apply(setup, Patterns.Hand.discardHand())
            val result = choose(setup)
            val discard = result.events.filterIsInstance<CardsDiscardedEvent>().single()
            discard.undefinedCharacteristics.size shouldBe 1
            val own = com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(discard), setup.player1Id).single()
            val other = com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(discard), setup.player2Id).single()
            own.description.contains("Grizzly Bears") shouldBe true
            other.description.contains("Grizzly Bears") shouldBe false
            setup.resolveStack()
            setup.getLifeTotal(1) shouldBe 18
        }
        test("Strongarm Tactics treats an unrevealed library discard as undefined rather than a creature") {
            val game = board()
            apply(game, Effects.EachPlayerDiscardsOrLosesLife(4))
            choose(game)
            game.getLifeTotal(1) shouldBe 16
            game.getLifeTotal(2) shouldBe 16
        }
        test("Library of Leng removes maximum hand size only while its ability is active") {
            val game = board()
            MaximumHandSize.effective(game.state, game.player1Id, cardRegistry, services.conditionEvaluator, services.dynamicAmountEvaluator) shouldBe null
            val id = game.findPermanent("Library of Leng")!!
            game.state = services.zones.moveToZone(game.state, id, Zone.GRAVEYARD).state
            MaximumHandSize.effective(game.state, game.player1Id, cardRegistry, services.conditionEvaluator, services.dynamicAmountEvaluator) shouldBe 7
        }
    }
}
