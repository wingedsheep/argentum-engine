package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.advisor.modules.BloomburrowAdvisorModule
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.bloomburrow.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class OffspringAITest : FunSpec({

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(PortalSet.allCards)
        registry.register(BloomburrowSet.allCards)
        return registry
    }

    test("AI should prefer offspring variant when it has enough mana") {
        val driver = GameTestDriver()
        driver.registerCards(PortalSet.allCards)
        driver.registerCards(BloomburrowSet.allCards)

        val deck = Deck.of("Forest" to 24, "Rust-Shield Rampager" to 16)
        driver.initMirrorMatch(deck)

        val p1 = driver.player1

        // Advance through 12 turn cycles so P1 gets 6+ forests on the battlefield
        repeat(12) {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val hand = driver.state.getHand(driver.activePlayer!!)
            val forest = hand.firstOrNull { entityId ->
                driver.state.getEntity(entityId)?.get<CardComponent>()?.name == "Forest"
            }
            if (forest != null && driver.activePlayer == p1) {
                driver.submitSuccess(PlayLand(p1, forest))
            }
            driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val state = driver.state
        val registry = createRegistry()

        // Get legal actions — both normal and kicker variants should exist
        val enumerator = LegalActionEnumerator.create(registry)
        val legalActions = enumerator.enumerate(state, p1, EnumerationMode.ACTIONS_ONLY)

        val normalCast = legalActions.filter {
            it.actionType == "CastSpell" && it.description.contains("Rust-Shield Rampager")
        }
        val kickerCast = legalActions.filter {
            it.actionType == "CastWithKicker" && it.description.contains("Rust-Shield Rampager")
        }

        normalCast.shouldHaveAtLeastSize(1)
        kickerCast.shouldHaveAtLeastSize(1)
        normalCast.first().affordable shouldBe true
        kickerCast.first().affordable shouldBe true

        // Verify offspring simulation creates the extra 1/1 token
        val simulator = GameSimulator(registry)
        val evaluator = AIPlayer.defaultEvaluator()
        val responder = DecisionResponder(simulator, evaluator)
        simulator.decisionResolver = { s, d -> responder.respond(s, d, d.playerId) }

        val normalResult = simulator.simulate(state, normalCast.first().action)
        val kickerResult = simulator.simulate(state, kickerCast.first().action)

        val normalCreatures = normalResult.state.controlledBattlefield(p1).count {
            normalResult.state.projectedState.isCreature(it)
        }
        val kickerCreatures = kickerResult.state.controlledBattlefield(p1).count {
            kickerResult.state.projectedState.isCreature(it)
        }
        kickerCreatures shouldBe normalCreatures + 1

        // Offspring should score higher
        val normalScore = evaluator.evaluate(normalResult.state, normalResult.state.projectedState, p1)
        val kickerScore = evaluator.evaluate(kickerResult.state, kickerResult.state.projectedState, p1)
        kickerScore shouldBeGreaterThan normalScore

        // AI strategist should choose the offspring variant
        val ai = AIPlayer.create(registry, p1, advisorModules = listOf(BloomburrowAdvisorModule()))
        val chosen = ai.chooseFrom(state, legalActions)

        chosen.actionType shouldBe "CastWithKicker"
        chosen.description shouldContain "Rust-Shield Rampager"
    }
})
