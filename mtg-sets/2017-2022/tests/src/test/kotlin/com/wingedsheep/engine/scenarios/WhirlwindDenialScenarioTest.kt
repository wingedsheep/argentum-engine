package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Whirlwind Denial (THB #81; {2}{U} instant).
 *
 *   For each spell and ability your opponents control, counter it unless its controller pays {4}.
 *
 * Untargeted: it reaches the opponent's spell without targeting it, the opponent may pay to save
 * it, and the Denial's controller's own spells are left alone.
 */
class WhirlwindDenialScenarioTest : ScenarioTestBase() {

    init {
        context("Whirlwind Denial") {

            test("counters the opponent's spell when they can't pay {4}, leaving its caster's own spell") {
                val game = board(boltCasterLands = 1)
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.passPriority()
                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
                game.castSpell(2, "Whirlwind Denial").error shouldBe null

                game.resolveStack()

                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 18
            }

            test("the opponent may pay {4} to keep their spell") {
                val game = board(boltCasterLands = 5)
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.passPriority()
                game.castSpell(2, "Whirlwind Denial").error shouldBe null

                game.passPriority()
                game.passPriority()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitManaSourcesAutoPay().error shouldBe null
                // All five Mountains are tapped: one for the Bolt, four for the tax.
                game.findPermanents("Mountain").count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 5

                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
            }
        }
    }

    private fun board(boltCasterLands: Int): TestGame =
        scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", boltCasterLands)
            .withCardInHand(2, "Whirlwind Denial")
            .withCardInHand(2, "Shock")
            .withLandsOnBattlefield(2, "Island", 3)
            .withLandsOnBattlefield(2, "Mountain", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
}
