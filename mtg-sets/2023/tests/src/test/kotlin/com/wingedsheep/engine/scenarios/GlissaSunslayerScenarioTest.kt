package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Glissa Sunslayer (ONE #202) — "First strike, deathtouch. Whenever Glissa Sunslayer deals combat
 * damage to a player, choose one — • You draw a card and lose 1 life. • Destroy target enchantment.
 * • Remove up to three counters from target permanent."
 */
class GlissaSunslayerScenarioTest : ScenarioTestBase() {

    private fun board(vararg extra: Pair<Int, String>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Glissa Sunslayer", summoningSickness = false)
        .apply { extra.forEach { (player, name) -> withCardOnBattlefield(player, name, summoningSickness = false) } }
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackToModeChoice(): ChooseOptionDecision {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Glissa Sunslayer" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)
        val decision = getPendingDecision()
        check(decision is ChooseOptionDecision) { "expected mode choice, got $decision; stack=${state.stack}; step=${state.step}" }
        decision.shouldNotBeNull()
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseMode(decision: ChooseOptionDecision, description: String) {
        val index = decision.options.indexOfFirst { it.contains(description) }
        check(index >= 0) { "Mode '$description' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun TestGame.addCounters(id: EntityId, vararg counters: Pair<CounterType, Int>) {
        state = state.updateEntity(id) { container ->
            var comp = container.get<CountersComponent>() ?: CountersComponent()
            counters.forEach { (type, amount) -> comp = comp.withAdded(type, amount) }
            container.with(comp)
        }
    }

    private fun TestGame.totalCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.values?.sum() ?: 0

    init {
        test("draw mode: you draw a card and lose 1 life") {
            val game = board()
            val handBefore = game.handSize(1)
            val decision = game.attackToModeChoice()
            withClue("first-strike combat damage landed before the trigger") { game.getLifeTotal(2) shouldBe 17 }
            game.chooseMode(decision, "draw a card")
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
            game.getLifeTotal(1) shouldBe 19
        }

        test("enchantment mode: destroys target enchantment") {
            val game = board(2 to "Glorious Anthem")
            val anthem = game.findPermanent("Glorious Anthem")!!
            val decision = game.attackToModeChoice()
            game.chooseMode(decision, "Destroy target enchantment")
            game.selectTargets(listOf(anthem)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Glorious Anthem") shouldBe false
            game.isInGraveyard(2, "Glorious Anthem") shouldBe true
            withClue("the draw mode was not also applied") { game.getLifeTotal(1) shouldBe 20 }
        }

        test("counter mode: removes up to three counters in total across kinds") {
            val game = board(2 to "Grizzly Bears")
            val bears = game.findPermanent("Grizzly Bears")!!
            game.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE to 2, CounterType.OIL to 2)

            val decision = game.attackToModeChoice()
            game.chooseMode(decision, "Remove up to three counters")
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            var guard = 0
            while (guard++ < 5) {
                val prompt = game.getPendingDecision() as? ChooseNumberDecision ?: break
                game.chooseNumber(minOf(2, prompt.maxValue)).error shouldBe null
            }

            withClue("three of four counters removed — the budget caps the second kind at one") {
                game.totalCounters(bears) shouldBe 1
            }
        }

        test("deathtouch and first strike: a 6/4 blocker dies before dealing damage") {
            val game = board(2 to "Craw Wurm")
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Glissa Sunslayer" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Craw Wurm" to listOf("Glissa Sunslayer"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.isOnBattlefield("Craw Wurm") shouldBe false
            game.isOnBattlefield("Glissa Sunslayer") shouldBe true
            withClue("a blocked Glissa deals no damage to the player, so no trigger") {
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
