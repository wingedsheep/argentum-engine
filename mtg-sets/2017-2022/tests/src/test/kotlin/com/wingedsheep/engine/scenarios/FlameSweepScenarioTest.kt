package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Flame Sweep (M20 #139) — "Flame Sweep deals 2 damage to each creature except for creatures you
 * control with flying."
 *
 * Pinned: only the caster's own flyer is spared; the caster's ground creature and both of the
 * opponent's creatures — flying or not — take 2.
 */
class FlameSweepScenarioTest : ScenarioTestBase() {

    init {
        context("Flame Sweep") {
            test("spares only creatures you control with flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Air Elemental")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInHand(1, "Flame Sweep")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Flame Sweep").error shouldBe null
                game.resolveStack()

                fun damage(name: String): Int =
                    game.state.getEntity(game.findPermanent(name)!!)?.get<DamageComponent>()?.amount ?: 0

                withClue("your own flyer is spared") { damage("Serra Angel") shouldBe 0 }
                withClue("your ground creature is hit") { damage("Hill Giant") shouldBe 2 }
                withClue("an opponent's flyer is hit") { damage("Air Elemental") shouldBe 2 }
                withClue("an opponent's ground creature is hit") { damage("Craw Wurm") shouldBe 2 }
            }
        }
    }
}
