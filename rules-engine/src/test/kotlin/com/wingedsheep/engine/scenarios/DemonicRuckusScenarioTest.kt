package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Demonic Ruckus (OTJ #120) — {1}{R} Enchantment — Aura.
 *
 * "Enchant creature
 *  Enchanted creature gets +1/+1 and has menace and trample.
 *  When this Aura is put into a graveyard from the battlefield, draw a card.
 *  Plot {R}"
 *
 * Same static-buff + put-into-graveyard-draw shape as Reach for the Sky (see
 * OtjBatchBAurasScenarioTest), with two granted keywords and a Plot kicker.
 */
class DemonicRuckusScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Demonic Ruckus") {
            test("grants +1/+1, menace, and trample to the enchanted creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Demonic Ruckus")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Demonic Ruckus", creature)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant should be 4/4 (3/3 +1/+1) with menace and trample") {
                    projector.getProjectedPower(game.state, creature) shouldBe 4
                    projector.getProjectedToughness(game.state, creature) shouldBe 4
                    projector.hasProjectedKeyword(game.state, creature, Keyword.MENACE) shouldBe true
                    projector.hasProjectedKeyword(game.state, creature, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("draws a card when put into a graveyard from the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Demonic Ruckus")
                    .withCardInHand(2, "Disenchant")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Demonic Ruckus", creature)
                game.resolveStack()
                val aura = game.findPermanent("Demonic Ruckus")!!

                val handBefore = game.handSize(1)

                // Hand priority to the opponent so they may respond on player 1's turn.
                game.passPriority()

                // Opponent destroys the Aura → it goes to the graveyard → draw trigger fires.
                val disenchant = game.castSpell(2, "Disenchant", aura)
                withClue("Disenchant cast should succeed: ${disenchant.error}") {
                    disenchant.error shouldBe null
                }
                game.resolveStack()

                withClue("Demonic Ruckus should be in its owner's graveyard") {
                    game.isInGraveyard(1, "Demonic Ruckus") shouldBe true
                }
                withClue("Controller should have drawn a card from the put-into-graveyard trigger") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }
}
