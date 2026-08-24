package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Venom — "Whenever enchanted creature blocks or becomes blocked by a non-Wall
 * creature, destroy the other creature at end of combat."
 *
 * The delay is the point: the poisoned creature must still trade combat damage first, and only then
 * does its partner die. So the test checks the partner is *alive through the damage step* and gone
 * by the postcombat main. The Wall case is the negative — a Wall blocker survives, which is what
 * distinguishes the subtype filter from a plain "any blocker".
 */
class VenomScenarioTest : ScenarioTestBase() {

    init {
        context("Venom") {

            test("a non-Wall blocker dies at end of combat, not before damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Venom", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Wall of Granite")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                // The 3/3 Courser blocks the 2/2 Bears: the Bears die in combat, the Courser
                // survives damage — and then Venom kills it anyway.
                game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the blocker survived combat damage but not the venom") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
            }

            test("a Wall blocker is not poisoned") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Venom", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Wall of Granite")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wall of Granite" to listOf("Grizzly Bears"))).error shouldBe null

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Walls are exempt — the trigger never fires against one") {
                    game.isOnBattlefield("Wall of Granite") shouldBe true
                }
            }
        }
    }
}
