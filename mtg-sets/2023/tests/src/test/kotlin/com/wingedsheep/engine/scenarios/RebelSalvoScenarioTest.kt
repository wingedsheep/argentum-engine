package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rebel Salvo (ONE #144) — {2}{R} Instant.
 * Affinity for Equipment. 5 damage to target creature or planeswalker; it loses indestructible
 * until end of turn.
 */
class RebelSalvoScenarioTest : ScenarioTestBase() {

    private val costCalculator by lazy { CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator) }

    init {
        context("Rebel Salvo") {

            test("each Equipment you control shaves {1}; other artifacts don't count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Rebel Salvo")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                costCalculator.calculateEffectiveCost(
                    game.state, cardRegistry.requireCard("Rebel Salvo"), game.player1Id
                ).genericAmount shouldBe 1
            }

            test("cast for {R} with two Equipment and destroys an indestructible creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Rebel Salvo")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Darksteel Myr")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myr = game.findPermanent("Darksteel Myr")!!
                game.castSpell(1, "Rebel Salvo", myr).error shouldBe null
                game.resolveStack()

                withClue("Darksteel Myr lost indestructible and died to 5 damage") {
                    game.isOnBattlefield("Darksteel Myr") shouldBe false
                    game.isInGraveyard(2, "Darksteel Myr") shouldBe true
                }
                game.isInGraveyard(1, "Rebel Salvo") shouldBe true
            }
        }
    }
}
