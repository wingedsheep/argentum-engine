package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Blitzball (FIN #254) — {3} Artifact.
 *
 * "{T}: Add one mana of any color.
 *  GOOOOAAAALLL! — {T}, Sacrifice this artifact: Draw two cards. Activate only if an opponent
 *  was dealt combat damage by a legendary creature this turn."
 *
 * Exercises the new DEALT_COMBAT_DAMAGE_BY_LEGENDARY_CREATURE turn tracker
 * (AnOpponentWasDealtCombatDamageByLegendaryCreatureThisTurn): the GOOOOAAAALLL ability is gated
 * off until a legendary creature deals combat damage to an opponent, then becomes available.
 */
class BlitzballScenarioTest : ScenarioTestBase() {

    private val goalAbilityId =
        cardRegistry.getCard("Blitzball")!!.script.activatedAbilities[1].id

    init {
        test("GOOOOAAAALLL is not activatable before any legendary combat damage") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Blitzball", tapped = false, summoningSickness = false)
                .withCardOnBattlefield(1, "Yargle, Glutton of Urborg", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val blitzball = game.findPermanent("Blitzball")!!
            val goal = game.getLegalActions(1).find {
                val a = it.action
                a is ActivateAbility && a.sourceId == blitzball && a.abilityId == goalAbilityId
            }
            withClue("GOOOOAAAALLL should be unavailable until a legendary deals combat damage") {
                goal shouldBe null
            }
        }

        test("GOOOOAAAALLL becomes activatable after a legendary creature deals combat damage to an opponent") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Blitzball", tapped = false, summoningSickness = false)
                .withCardOnBattlefield(1, "Yargle, Glutton of Urborg", tapped = false, summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val blitzball = game.findPermanent("Blitzball")!!

            // Attack P2 with the legendary Yargle (9/3) and let combat damage resolve.
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Yargle, Glutton of Urborg" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("P2 should have taken 9 combat damage from Yargle") {
                game.getLifeTotal(2) shouldBe 11
            }

            val goal = game.getLegalActions(1).find {
                val a = it.action
                a is ActivateAbility && a.sourceId == blitzball && a.abilityId == goalAbilityId
            }
            withClue("GOOOOAAAALLL should now be available after a legendary dealt combat damage") {
                (goal != null) shouldBe true
            }

            val activation = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = blitzball,
                    abilityId = goalAbilityId,
                )
            )
            withClue("Activating GOOOOAAAALLL should succeed: ${activation.error}") {
                activation.error shouldBe null
            }
            game.resolveStack()

            withClue("Blitzball was sacrificed as part of the cost") {
                game.findPermanent("Blitzball") shouldBe null
            }
        }
    }
}
