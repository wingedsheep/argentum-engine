package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Fetid Gargantua (MH3) — "{2}{B}: Adapt 2." and "Whenever one or more +1/+1 counters are put on this
 * creature, you may draw two cards. If you do, you lose 2 life."
 */
class FetidGargantuaScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.settle(answerMay: Boolean): Int {
        var prompts = 0
        var guard = 0
        while (guard++ < 20) {
            when (val d = getPendingDecision()) {
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay().error shouldBe null
                is YesNoDecision -> { prompts++; answerYesNo(answerMay).error shouldBe null }
                null -> if (state.stack.isNotEmpty()) resolveStack() else return prompts
                else -> error("unexpected decision $d")
            }
        }
        return prompts
    }

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Fetid Gargantua")
        .withLandsOnBattlefield(1, "Swamp", 6)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.adapt(answerMay: Boolean): Int {
        val gargantua = findPermanent("Fetid Gargantua")!!
        val ability = cardRegistry.requireCard("Fetid Gargantua").script.activatedAbilities[0].id
        execute(ActivateAbility(playerId = player1Id, sourceId = gargantua, abilityId = ability)).error shouldBe null
        return settle(answerMay)
    }

    init {
        test("adapt puts two counters and accepting the trigger draws two and loses 2 life") {
            val game = game()
            val gargantua = game.findPermanent("Fetid Gargantua")!!
            val hand = game.handSize(1)
            game.adapt(answerMay = true) shouldBe 1
            game.plusOnes(gargantua) shouldBe 2
            game.state.projectedState.getPower(gargantua) shouldBe 6
            game.handSize(1) shouldBe hand + 2
            game.getLifeTotal(1) shouldBe 18
            game.getLifeTotal(2) shouldBe 20
        }

        test("declining the trigger draws nothing and loses no life") {
            val game = game()
            val gargantua = game.findPermanent("Fetid Gargantua")!!
            val hand = game.handSize(1)
            game.adapt(answerMay = false) shouldBe 1
            game.plusOnes(gargantua) shouldBe 2
            game.handSize(1) shouldBe hand
            game.getLifeTotal(1) shouldBe 20
        }

        test("adapting again with counters already on it places none and does not trigger") {
            val game = game()
            val gargantua = game.findPermanent("Fetid Gargantua")!!
            game.adapt(answerMay = false)
            val hand = game.handSize(1)
            game.adapt(answerMay = true) shouldBe 0
            game.plusOnes(gargantua) shouldBe 2
            game.handSize(1) shouldBe hand
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
