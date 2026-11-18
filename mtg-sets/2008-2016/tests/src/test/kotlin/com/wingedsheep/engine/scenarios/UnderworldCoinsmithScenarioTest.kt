package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.UnderworldCoinsmith
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Underworld Coinsmith (JOU #157) — {W}{B} Enchantment Creature — Human Cleric 2/2.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, you gain 1 life.
 *   {W}{B}, Pay 1 life: Each opponent loses 1 life.
 */
class UnderworldCoinsmithScenarioTest : ScenarioTestBase() {
    init {
        test("its entry gains 1 life, and the activation trades 1 life for 1 of the opponent's") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Underworld Coinsmith")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Underworld Coinsmith").error shouldBe null
            game.resolveStack()
            withClue("constellation on its own entry: you gain 1") {
                game.getLifeTotal(1) shouldBe 21
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Underworld Coinsmith")!!,
                    abilityId = UnderworldCoinsmith.activatedAbilities.first().id,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("you paid 1 life; the opponent lost 1") {
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 19
            }
        }
    }
}
