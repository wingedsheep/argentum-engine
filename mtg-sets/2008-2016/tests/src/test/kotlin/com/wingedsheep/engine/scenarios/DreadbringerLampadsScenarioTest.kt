package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dreadbringer Lampads (JOU #67) — {4}{B} Enchantment Creature — Nymph 4/2.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, target
 *   creature gains intimidate until end of turn.
 */
class DreadbringerLampadsScenarioTest : ScenarioTestBase() {
    init {
        test("its own entry triggers constellation: the target gains intimidate") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Dreadbringer Lampads")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Dreadbringer Lampads").error shouldBe null
            game.resolveStack()

            withClue("constellation asks for a target") {
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears gained intimidate") {
                game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe true
            }
        }

        test("another enchantment entering under your control triggers it too") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Dreadbringer Lampads")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Glorious Anthem")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Glorious Anthem").error shouldBe null
            game.resolveStack()

            withClue("constellation asks for a target") {
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears gained intimidate") {
                game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe true
            }
        }
    }
}
