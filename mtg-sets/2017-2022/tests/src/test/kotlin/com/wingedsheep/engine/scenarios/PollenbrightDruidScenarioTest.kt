package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pollenbright Druid (WAR #173) — {1}{G} Creature — Elf Druid 1/1.
 *
 *   When this creature enters, choose one —
 *   • Put a +1/+1 counter on target creature.
 *   • Proliferate.
 */
class PollenbrightDruidScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.chooseMode(index: Int) {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun setup(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Pollenbright Druid")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("mode one: put a +1/+1 counter on target creature") {
            val game = setup()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Pollenbright Druid").error shouldBe null
            game.resolveStack()
            game.chooseMode(0)
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            plusOnes(game, giant) shouldBe 1
        }

        test("mode two: proliferate") {
            val game = setup()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, 2)

            game.castSpell(1, "Pollenbright Druid").error shouldBe null
            game.resolveStack()
            game.chooseMode(1)
            game.resolveStack()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            plusOnes(game, giant) shouldBe 3
        }
    }
}
