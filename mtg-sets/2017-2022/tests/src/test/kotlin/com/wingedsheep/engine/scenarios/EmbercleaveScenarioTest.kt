package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Embercleave (ELD #120) — Flash; costs {1} less for each attacking creature you control; on
 * entering, attaches to target creature you control; equipped creature gets +1/+1 and has double
 * strike and trample; Equip {3}.
 */
class EmbercleaveScenarioTest : ScenarioTestBase() {

    init {
        context("Embercleave") {

            test("flashed in mid-combat for {2}{R}{R} with two attackers, attaches and grants its bonuses") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Embercleave")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("two attackers reduce {4}{R}{R} to {2}{R}{R}, payable with four Mountains") {
                    game.castSpell(1, "Embercleave").error shouldBe null
                }
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bears)).error shouldBe null
                }
                game.resolveStack()

                val cleave = game.findPermanent("Embercleave")!!
                game.state.getEntity(cleave)!!.get<AttachedToComponent>()?.targetId shouldBe bears
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
                projected.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }

            test("without attackers four lands can't pay for it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Embercleave")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Embercleave").error shouldNotBe null
            }
        }
    }
}
