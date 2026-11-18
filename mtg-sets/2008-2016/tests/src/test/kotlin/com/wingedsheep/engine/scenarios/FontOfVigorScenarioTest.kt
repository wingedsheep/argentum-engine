package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.FontOfVigor
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Font of Vigor (JOU #11) — {1}{W} Enchantment.
 *
 *   {2}{W}, Sacrifice this enchantment: You gain 7 life.
 */
class FontOfVigorScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing it gains 7 life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Font of Vigor")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Font of Vigor")!!,
                    abilityId = FontOfVigor.activatedAbilities.first().id,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("controller gained 7 life and the Font is gone") {
                game.getLifeTotal(1) shouldBe 27
                game.isInGraveyard(1, "Font of Vigor") shouldBe true
            }
        }
    }
}
