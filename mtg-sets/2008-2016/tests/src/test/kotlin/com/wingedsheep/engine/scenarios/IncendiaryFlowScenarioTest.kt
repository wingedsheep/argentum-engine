package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Incendiary Flow (EMN #133) — {1}{R} Sorcery.
 *
 *   Incendiary Flow deals 3 damage to any target. If a creature dealt damage this way would die
 *   this turn, exile it instead.
 *
 * The exile mark goes on before the damage, so the creature this spell kills is exiled.
 */
class IncendiaryFlowScenarioTest : ScenarioTestBase() {
    init {
        context("Incendiary Flow") {

            test("a creature killed by the damage is exiled instead of dying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Incendiary Flow")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Incendiary Flow", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant is exiled, not put into the graveyard") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe false
                }
            }

            test("a creature that survives the damage stays on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Incendiary Flow")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Incendiary Flow", game.findPermanent("Craw Wurm")!!).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Craw Wurm") shouldBe true
            }

            test("it can target a player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Incendiary Flow")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Incendiary Flow", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}
