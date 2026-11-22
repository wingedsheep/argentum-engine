package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Render Silent — "Counter target spell. Its controller can't cast spells this turn."
 *
 * The ban lands on the countered spell's controller, whoever that is: the opponent who answered
 * stays free to cast, and a player countering their own spell locks themselves out.
 */
class RenderSilentScenarioTest : ScenarioTestBase() {

    init {
        test("counters the spell and its controller can't cast spells this turn; the caster still can") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(2, "Render Silent")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Plains", 1)
                .withLandsOnBattlefield(2, "Island", 2)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpellTargetingStackSpell(2, "Render Silent", "Lightning Bolt").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.getLifeTotal(2) shouldBe 20

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldNotBe null

            game.passPriority().error shouldBe null
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
        }

        test("countering your own spell bans you from casting spells") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Render Silent")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.castSpellTargetingStackSpell(1, "Render Silent", "Lightning Bolt").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldNotBe null
        }
    }
}
