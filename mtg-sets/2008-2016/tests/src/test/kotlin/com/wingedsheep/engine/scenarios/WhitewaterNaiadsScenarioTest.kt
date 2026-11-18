package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Whitewater Naiads (JOU #58) — {3}{U}{U} Enchantment Creature — Nymph 4/4.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, target creature
 *   can't be blocked this turn.
 */
class WhitewaterNaiadsScenarioTest : ScenarioTestBase() {
    init {
        test("its entry makes the targeted creature unblockable this turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Whitewater Naiads")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Whitewater Naiads").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("the Bears can't be blocked; the Naiads themselves were not chosen") {
                game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                game.state.projectedState.hasKeyword(game.findPermanent("Whitewater Naiads")!!, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
            }
        }
    }
}
