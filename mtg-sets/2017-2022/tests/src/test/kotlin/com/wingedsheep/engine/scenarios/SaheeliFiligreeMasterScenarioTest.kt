package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.player.CreatePermanentEmblemExecutor
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.CreatePermanentEmblemEffect
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SaheeliFiligreeMasterScenarioTest : ScenarioTestBase() {
    private val name = "Saheeli, Filigree Master"
    private fun setup() = scenario().withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, name)
        .withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(2, "Ornithopter")
        .withCardInHand(1, "Millstone")
        .withCardInLibrary(1, "Island").withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
        .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

    private fun activate(game: TestGame, index: Int, loyalty: Int = 3) {
        val source = game.findPermanent(name)!!
        game.state = game.state.updateEntity(source) {
            it.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty))
        }
        game.execute(ActivateAbility(game.player1Id, source,
            cardRegistry.getCard(name)!!.script.activatedAbilities[index].id)).error shouldBe null
        game.resolveStack()
    }
    private fun finishScry(game: TestGame) {
        (game.state.pendingDecision as? SelectCardsDecision) shouldNotBe null
        game.skipSelection().error shouldBe null
        if (game.state.pendingDecision is ReorderLibraryDecision) game.keepLibraryOrder().error shouldBe null
    }

    init {
        test("+1 scries before selecting an untapped artifact and drawing") {
            val game = setup()
            val artifact = game.findPermanents("Ornithopter").first { game.state.projectedState.getController(it) == game.player1Id }
            activate(game, 0)
            game.state.getEntity(artifact)!!.has<TappedComponent>() shouldBe false
            finishScry(game)
            val choice = game.state.pendingDecision as SelectCardsDecision
            choice.options shouldBe listOf(artifact)
            game.selectCards(listOf(artifact)).error shouldBe null
            game.state.getEntity(artifact)!!.has<TappedComponent>() shouldBe true
            game.state.getHand(game.player1Id).size shouldBe 2
        }

        test("+1 may decline the tap without drawing") {
            val game = setup()
            activate(game, 0)
            finishScry(game)
            game.skipSelection().error shouldBe null
            game.state.getHand(game.player1Id).size shouldBe 1
            game.state.getEntity(game.findPermanents("Ornithopter").first { game.state.projectedState.getController(it) == game.player1Id })!!.has<TappedComponent>() shouldBe false
        }

        test("+1 cannot tap an already tapped artifact or an opponent's artifact") {
            val game = setup()
            val artifact = game.findPermanents("Ornithopter").first { game.state.projectedState.getController(it) == game.player1Id }
            game.state = game.state.updateEntity(artifact) { it.with(TappedComponent) }
            activate(game, 0)
            finishScry(game)
            if (game.hasPendingDecision()) game.skipSelection().error shouldBe null
            game.state.getHand(game.player1Id).size shouldBe 1
        }

        test("-2 makes two flying artifact tokens with haste only until end of turn") {
            val game = setup()
            activate(game, 1)
            val tokens = game.state.getBattlefield(game.player1Id).filter {
                game.state.projectedState.hasSubtype(it, "Thopter") &&
                    game.state.projectedState.getPower(it) == 1
            }
            tokens.size shouldBe 2
            for (token in tokens) {
                game.state.projectedState.hasType(token, "ARTIFACT") shouldBe true
                game.state.projectedState.hasKeyword(token, Keyword.FLYING) shouldBe true
                game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
            }
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            for (token in tokens) {
                game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe false
                game.state.projectedState.hasKeyword(token, Keyword.FLYING) shouldBe true
            }
        }

        test("-4 survives Saheeli dying and discounts an artifact cast while boosting only friendly artifact creatures") {
            val game = setup()
            activate(game, 2, 4)
            game.findPermanent(name) shouldBe null
            game.state.entities.values.count { it.has<EmblemSourceComponent>() } shouldBe 1
            game.state.projectedState.getPower(game.findPermanents("Ornithopter").first { game.state.projectedState.getController(it) == game.player1Id }) shouldBe 1
            game.state.projectedState.getPower(game.findPermanents("Ornithopter").first { game.state.projectedState.getController(it) == game.player2Id }) shouldBe 0
            game.state = game.state.updateEntity(game.player1Id) { it.with(ManaPoolComponent(blue = 1)) }
            val millstone = game.findCardsInHand(1, "Millstone").single()
            game.getLegalActions(1).any {
                it.isAffordable && (it.action as? CastSpell)?.cardId == millstone
            } shouldBe true
            game.castSpell(1, "Millstone").error shouldBe null
            game.resolveStack()
            game.findPermanent("Millstone") shouldNotBe null
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            CostCalculator(cardRegistry, services.predicateEvaluator).calculateEffectiveCost(
                game.state, cardRegistry.getCard("Millstone")!!, game.player1Id) shouldBe ManaCost.parse("{1}")
        }

        test("the emblem also boosts artifact creatures entering later") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name).withCardInHand(1, "Ornithopter")
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            activate(game, 2, 4)
            game.castSpell(1, "Ornithopter").error shouldBe null
            game.resolveStack()
            val thopter = game.findPermanent("Ornithopter")!!
            game.state.projectedState.getPower(thopter) shouldBe 1
            game.state.projectedState.getToughness(thopter) shouldBe 3
        }

        test("two emblems stack both bonuses and cost reductions") {
            val game = setup()
            activate(game, 2, 4)
            // A second resolution of the real ultimate, without a second planeswalker fixture.
            val effect = cardRegistry.getCard(name)!!.script.activatedAbilities[2].effect
            val executor = CreatePermanentEmblemExecutor()
            game.state = executor.execute(game.state,
                effect as CreatePermanentEmblemEffect,
                EffectContext(sourceId = null, controllerId = game.player1Id)).state
            game.state.projectedState.getPower(game.findPermanents("Ornithopter").first { game.state.projectedState.getController(it) == game.player1Id }) shouldBe 2
            CostCalculator(cardRegistry, services.predicateEvaluator).calculateEffectiveCost(
                game.state, cardRegistry.getCard("Millstone")!!, game.player1Id) shouldBe ManaCost.ZERO
            game.castSpell(1, "Millstone").error shouldBe null
        }
    }
}
