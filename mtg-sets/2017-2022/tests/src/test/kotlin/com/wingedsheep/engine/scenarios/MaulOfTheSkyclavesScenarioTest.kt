package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Maul of the Skyclaves (ZNR #27) — {2}{W} Artifact — Equipment.
 *
 *   When this Equipment enters, attach it to target creature you control.
 *   Equipped creature gets +2/+2 and has flying and first strike.
 *   Equip {2}{W}{W}
 */
class MaulOfTheSkyclavesScenarioTest : ScenarioTestBase() {

    init {
        context("Maul of the Skyclaves' enters trigger") {

            test("attaches to target creature you control, which gets +2/+2, flying and first strike") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Maul of the Skyclaves")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Maul of the Skyclaves").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bears)).error shouldBe null
                }
                game.resolveStack()

                val maul = game.findPermanent("Maul of the Skyclaves")!!
                withClue("the Maul is attached to the Bears") {
                    game.state.getEntity(maul)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                val projected = game.state.projectedState
                withClue("equipped creature gets +2/+2 and has flying and first strike") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 4
                    projected.hasKeyword(bears, Keyword.FLYING.name) shouldBe true
                    projected.hasKeyword(bears, Keyword.FIRST_STRIKE.name) shouldBe true
                }
            }
        }
    }
}
