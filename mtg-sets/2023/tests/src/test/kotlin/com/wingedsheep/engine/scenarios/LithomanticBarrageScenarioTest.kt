package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Lithomantic Barrage — 1 damage to a creature or planeswalker, 5 if it is white and/or blue. */
class LithomanticBarrageScenarioTest : ScenarioTestBase() {
    private fun cast(target: String, foe: Boolean = true): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Lithomantic Barrage")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(2, target)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Lithomantic Barrage", game.findPermanent(target)!!).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("a white creature takes 5") {
            cast("Serra Angel").isInGraveyard(2, "Serra Angel") shouldBe true
        }

        test("a blue creature takes 5") {
            cast("Air Elemental").isInGraveyard(2, "Air Elemental") shouldBe true
        }

        test("a red creature takes only 1") {
            val game = cast("Hill Giant")
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe false
        }

        test("it cannot be countered") {
            com.wingedsheep.mtg.sets.definitions.mom.cards.LithomanticBarrage.script.cantBeCountered shouldBe true
        }
    }
}
