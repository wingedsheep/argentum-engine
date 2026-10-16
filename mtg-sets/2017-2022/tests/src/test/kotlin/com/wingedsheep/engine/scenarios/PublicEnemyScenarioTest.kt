package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Public Enemy (SNC #55) — {2}{U} Enchantment — Aura
 *
 *   Enchant creature
 *   All creatures attack enchanted creature's controller each combat if able.
 *   When enchanted creature dies, draw a card.
 */
class PublicEnemyScenarioTest : ScenarioTestBase() {

    init {
        context("Public Enemy") {

            test("on an opponent's creature, your creatures must attack that opponent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Public Enemy", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                withClue("not attacking breaks the requirement") {
                    game.execute(DeclareAttackers(game.player1Id, emptyMap())).error shouldNotBe null
                }
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            }

            test("on your own creature, you are the one everyone must attack — your creatures stay free") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Public Enemy", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.execute(DeclareAttackers(game.player1Id, emptyMap())).error shouldBe null
            }

            test("when the enchanted creature dies, the Aura's controller draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Public Enemy", "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldBe null
                withClue("Bolt left the hand, the dies trigger drew one") { game.handSize(1) shouldBe handBefore }
            }
        }
    }
}
