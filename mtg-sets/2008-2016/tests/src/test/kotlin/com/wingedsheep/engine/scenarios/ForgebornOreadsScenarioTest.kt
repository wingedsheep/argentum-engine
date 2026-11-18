package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Forgeborn Oreads (JOU #98) — {2}{R}{R} Enchantment Creature — Nymph 4/2.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, this
 *   creature deals 1 damage to any target.
 */
class ForgebornOreadsScenarioTest : ScenarioTestBase() {
    init {
        test("its own entry pings the targeted player") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Forgeborn Oreads")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Forgeborn Oreads").error shouldBe null
            game.resolveStack()

            withClue("constellation asks for a target") {
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
        }

        test("another enchantment entering lets it kill a 1-toughness creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Forgeborn Oreads")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardInHand(1, "Glorious Anthem")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val elves = game.findPermanent("Llanowar Elves")!!
            game.castSpell(1, "Glorious Anthem").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(elves)).error shouldBe null
            game.resolveStack()

            withClue("the 1 damage killed Llanowar Elves") {
                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            }
        }
    }
}
