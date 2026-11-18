package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Strength from the Fallen (JOU #143) — {1}{G} Enchantment.
 *
 *   Constellation — Whenever this enchantment or another enchantment you control enters, target creature
 *   gets +X/+X until end of turn, where X is the number of creature cards in your graveyard.
 */
class StrengthFromTheFallenScenarioTest : ScenarioTestBase() {
    init {
        test("its own entry pumps the target by the creature cards in your graveyard") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Strength from the Fallen")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Llanowar Elves")
                .withCardInGraveyard(1, "Giant Growth")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Strength from the Fallen").error shouldBe null
            game.resolveStack()

            withClue("constellation fires on the enchantment's own entry") {
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("X = 2: two creature cards in your graveyard; the instant and the opponent's card don't count") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
            }
        }
    }
}
