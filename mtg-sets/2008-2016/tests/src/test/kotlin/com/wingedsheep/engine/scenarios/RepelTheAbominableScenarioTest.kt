package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Repel the Abominable (EMN #38) — "Prevent all damage that would be dealt this
 * turn by non-Human sources."
 *
 * The first source-side group shield over *every* non-Human source rather than over creatures:
 * a Human creature's damage still lands, a non-Human creature's is prevented in both combat
 * directions, and a noncreature source with no creature type at all (a burn spell) counts as a
 * non-Human source, per the 2016-07-13 ruling.
 */
class RepelTheAbominableScenarioTest : ScenarioTestBase() {

    init {
        context("Repel the Abominable") {

            test("a Human's combat damage is dealt; non-Human creatures' damage is prevented both ways") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Repel the Abominable")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Elite Vanguard", summoningSickness = false) // Human Soldier 2/1
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // Giant 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears") // Bear 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Repel the Abominable")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Elite Vanguard" to 2, "Hill Giant" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Elite Vanguard"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the Hill Giant isn't a Human, so its 3 damage to the player is prevented") {
                    game.getLifeTotal(2) shouldBe 20
                }
                withClue("the Bears aren't Humans, so their 2 damage to the 2/1 Vanguard is prevented") {
                    game.isOnBattlefield("Elite Vanguard") shouldBe true
                }
                withClue("the Vanguard is a Human, so its 2 damage kills the Bears") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("a burn spell has no creature type, so it is a non-Human source and its damage is prevented") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Repel the Abominable")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Repel the Abominable").error shouldBe null
                game.resolveStack()

                // The opponent casts it: the shield covers every non-Human source, whoever controls it.
                game.passPriority()
                val bolt = game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
                withClue("the Bolt should be castable: ${bolt.error}") { bolt.error shouldBe null }
                game.resolveStack()

                withClue("the Bolt's 3 damage is prevented") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
