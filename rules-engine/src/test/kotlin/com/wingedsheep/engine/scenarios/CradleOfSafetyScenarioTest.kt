package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Cradle of Safety (VOW #54) — {1}{U} Enchantment — Aura.
 *
 *   Flash
 *   Enchant creature you control
 *   When this Aura enters, enchanted creature gains hexproof until end of turn.
 *   Enchanted creature gets +1/+1.
 *
 * Exercises the ETB hexproof grant and the static +1/+1 buff on the enchanted creature.
 */
class CradleOfSafetyScenarioTest : ScenarioTestBase() {

    init {
        context("Cradle of Safety") {

            test("has flash") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cradle of Safety")
                    .build()

                val cardDef = cardRegistry.getCard("Cradle of Safety")!!
                withClue("Cradle of Safety has flash") {
                    cardDef.keywords.contains(Keyword.FLASH) shouldBe true
                }
            }

            test("enchanting a creature you control gives +1/+1 and hexproof until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cradle of Safety")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Grizzly Bears does not start with hexproof") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe false
                }

                game.castSpell(1, "Cradle of Safety", targetId = bears).error shouldBe null
                game.resolveStack() // Aura resolves and attaches -> ETB trigger grants hexproof

                withClue("Cradle of Safety is attached to Grizzly Bears") {
                    game.isOnBattlefield("Cradle of Safety") shouldBe true
                }
                withClue("Grizzly Bears gets +1/+1 (becomes 3/3)") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
                withClue("Grizzly Bears gains hexproof until end of turn") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
                }
            }
        }
    }
}
