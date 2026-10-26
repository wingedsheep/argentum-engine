package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kardur, Doomscourge (KHM #223).
 *
 *   When Kardur enters, until your next turn, creatures your opponents control attack each combat
 *   if able and attack a player other than you if able.
 *   Whenever an attacking creature dies, each opponent loses 1 life and you gain 1 life.
 *
 * Two-player: the only player the opponent's creatures can attack is Kardur's controller, so the
 * requirement is "attack each combat if able" (multiplayer steering is pinned in the engine's
 * MustAttackGroupEffectTest). The drain triggers on any attacking creature dying, whoever controls it.
 */
class KardurDoomscourgeScenarioTest : ScenarioTestBase() {

    init {
        context("enters trigger") {
            test("the opponent's creatures must attack on their next turn") {
                val game = scenario()
                    .withPlayers("Kardur", "Opponent")
                    .withCardInHand(1, "Kardur, Doomscourge")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .apply { repeat(6) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Forest") } }
                    .build()

                game.castSpell(1, "Kardur, Doomscourge").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Kardur, Doomscourge") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("it is the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }

                withClue("the Bears can't stay home") {
                    game.declareAttackers(emptyMap()).error shouldNotBe null
                }
                withClue("attacking Kardur's controller obeys it when no other player exists") {
                    game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                }
            }
        }

        context("attacking creature dies") {
            test("an opposing attacker dying drains each opponent for 1") {
                val game = scenario()
                    .withPlayers("Kardur", "Opponent")
                    .withCardOnBattlefield(1, "Kardur, Doomscourge")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .apply { repeat(6) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Forest") } }
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Kardur, Doomscourge" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("each opponent loses 1") { game.getLifeTotal(2) shouldBe 19 }
                withClue("Kardur's controller gains 1") { game.getLifeTotal(1) shouldBe 21 }
            }

            test("your own attacking creature dying triggers it too") {
                val game = scenario()
                    .withPlayers("Kardur", "Opponent")
                    .withCardOnBattlefield(1, "Kardur, Doomscourge")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .apply { repeat(6) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Forest") } }
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Kardur, Doomscourge" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                withClue("drain 1 from the attacking Bears, plus Kardur's 4 combat damage") {
                    game.getLifeTotal(2) shouldBe 15
                }
                game.getLifeTotal(1) shouldBe 21
            }

            test("a blocking creature dying doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Kardur", "Opponent")
                    .withCardOnBattlefield(1, "Kardur, Doomscourge")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .apply { repeat(6) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Forest") } }
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Kardur, Doomscourge" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Kardur, Doomscourge"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
