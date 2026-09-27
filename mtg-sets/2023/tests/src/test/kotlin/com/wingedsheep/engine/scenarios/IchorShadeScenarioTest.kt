package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ichor Shade — "At the beginning of your end step, if an artifact or creature was put into a
 * graveyard from the battlefield this turn, put a +1/+1 counter on this creature."
 *
 * Game-wide: any player's artifact or creature, tokens included. A land doesn't count.
 */
class IchorShadeScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(): Int =
        state.getEntity(findPermanent("Ichor Shade")!!)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun base() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Ichor Shade")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.toEndStepAndResolve() {
        passUntilPhase(Phase.ENDING, Step.END)
        resolveStack()
    }

    init {
        test("an opponent's noncreature artifact token destroyed this turn — counter") {
            val game = base()
                .withCardOnBattlefield(2, "Mind Stone", isToken = true)
                .withCardInHand(1, "Shatter")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .build()

            game.castSpell(1, "Shatter", game.findPermanent("Mind Stone")).error shouldBe null
            game.resolveStack()
            game.toEndStepAndResolve()
            game.plusCounters() shouldBe 1
        }

        test("a creature died this turn — counter") {
            val game = base()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .build()

            game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")).error shouldBe null
            game.resolveStack()
            game.toEndStepAndResolve()
            game.plusCounters() shouldBe 1
        }

        test("only a land was put into a graveyard — no counter") {
            val game = base()
                .withLandsOnBattlefield(2, "Forest", 1)
                .withCardInHand(1, "Stone Rain")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .build()

            game.castSpell(1, "Stone Rain", game.findPermanent("Forest")).error shouldBe null
            game.resolveStack()
            game.toEndStepAndResolve()
            game.plusCounters() shouldBe 0
        }
    }
}
