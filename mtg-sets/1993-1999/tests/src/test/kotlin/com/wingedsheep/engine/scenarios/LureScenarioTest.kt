package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lure (LEA #211) - {1}{G}{G} Enchantment — Aura.
 *
 * "Enchant creature
 *  All creatures able to block enchanted creature do so."
 */
class LureScenarioTest : ScenarioTestBase() {

    init {
        context("Lure") {
            test("cast onto a creature, every able defender must block it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lure")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lure", bears).error shouldBe null
                game.resolveStack()
                (game.findPermanent("Lure") != null) shouldBe true

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("No blocks is illegal while defenders are able to block the lured creature") {
                    game.declareNoBlockers().outcome shouldNotBe Outcome.Done
                }
                withClue("Only one of two able creatures blocking is illegal") {
                    game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).outcome shouldNotBe Outcome.Done
                }
                withClue("All able creatures blocking the lured creature is legal") {
                    game.declareBlockers(
                        mapOf(
                            "Savannah Lions" to listOf("Grizzly Bears"),
                            "Hill Giant" to listOf("Grizzly Bears"),
                        )
                    ).outcome shouldBe Outcome.Done
                }
            }

            test("a tapped creature is not able to block, so it isn't forced to") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Lure", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("The untapped Lions blocking alone satisfies Lure") {
                    game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).outcome shouldBe Outcome.Done
                }
            }

            test("Lure only binds the enchanted creature - other attackers may go unblocked") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Lure", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Blocking the unenchanted Hill Giant instead of the lured Bears is illegal") {
                    game.declareBlockers(mapOf("Savannah Lions" to listOf("Hill Giant"))).outcome shouldNotBe Outcome.Done
                }
                withClue("Blocking the lured Bears is legal") {
                    game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).outcome shouldBe Outcome.Done
                }
            }
        }
    }
}
