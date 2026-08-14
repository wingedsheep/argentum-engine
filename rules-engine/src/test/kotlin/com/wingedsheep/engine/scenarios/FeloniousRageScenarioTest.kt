package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Felonious Rage (MKM) — "Target creature you control gets +2/+0 and gains haste until end of
 * turn. When that creature dies this turn, create a 2/2 white and blue Detective creature token."
 *
 * The death clause is an entity-scoped delayed trigger, independent of the pump: it fires for any
 * death that turn, not only a combat death, and it fires exactly once ("When", not "Whenever").
 * A creature that survives the turn produces nothing.
 */
class FeloniousRageScenarioTest : ScenarioTestBase() {

    init {
        context("Felonious Rage — pump, haste, and a Detective on death") {

            test("grants +2/+0 and haste, letting a freshly-cast creature attack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Felonious Rage")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Felonious Rage", targetId = bears).error shouldBe null
                game.resolveStack()

                val projected = StateProjector().project(game.state)
                withClue("a 2/2 becomes a 4/2 with haste") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }

                // The engine skips DECLARE_ATTACKERS when there is no legal attacker, so pin the
                // turn — otherwise this would pass by sailing into a later turn where the Bears
                // simply isn't summoning sick any more.
                val turn = game.state.turnNumber
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("haste beats summoning sickness on this very turn") {
                    game.state.turnNumber shouldBe turn
                    game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                }
            }

            test("creates a 2/2 Detective token when the pumped creature dies in combat") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Felonious Rage")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 -> 4/2
                    .withCardOnBattlefield(2, "Craw Wurm") // 6/4 blocker kills it
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Felonious Rage", targetId = bears).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Craw Wurm" to listOf("Grizzly Bears")))
                    .error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                }
                game.resolveStack()

                withClue("the Bears traded with the Wurm and left a Detective behind") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    val detectives = game.findPermanents("Detective Token")
                    detectives.size shouldBe 1
                    val projected = StateProjector().project(game.state)
                    projected.getPower(detectives.single()) shouldBe 2
                    projected.getToughness(detectives.single()) shouldBe 2
                }
            }

            test("the token still appears when the creature dies outside combat") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Felonious Rage")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Felonious Rage", targetId = bears).error shouldBe null
                game.resolveStack()

                game.passPriority() // hand priority to the opponent so they can respond
                game.castSpell(2, "Murder", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("the delayed trigger watches the creature, not the combat") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.findPermanents("Detective Token").size shouldBe 1
                }
            }

            test("no token when the creature survives the turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Felonious Rage")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Felonious Rage", targetId = bears).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)

                withClue("the Bears is alive, so nothing was created — and the positive case above proves the name") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.findPermanents("Detective Token").size shouldBe 0
                }
            }
        }
    }
}
