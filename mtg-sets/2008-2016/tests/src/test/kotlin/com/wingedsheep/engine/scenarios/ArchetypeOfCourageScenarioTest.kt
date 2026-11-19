package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Archetype of Courage (Born of the Gods #4) — {1}{W}{W} 2/2.
 * "Creatures you control have first strike. Creatures your opponents control lose first strike and
 * can't have or gain first strike."
 *
 * Your Grizzly Bears strike first, and the opponent's printed first striker has lost the keyword:
 * the Bears kill the 3/1 blocker before it deals damage.
 */
class ArchetypeOfCourageScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                "Courage Test Duelist", ManaCost.parse("{2}"), emptySet(), power = 3, toughness = 1,
                oracleText = "First strike", keywords = setOf(Keyword.FIRST_STRIKE)
            )
        )

        context("Archetype of Courage") {

            test("your creatures have first strike; the opponent's lose it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Archetype of Courage")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Courage Test Duelist")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(game.findPermanent("Archetype of Courage")!!, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(game.findPermanent("Courage Test Duelist")!!, Keyword.FIRST_STRIKE) shouldBe false
            }

            test("the Bears kill the former first striker blocking them and survive") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Archetype of Courage")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Courage Test Duelist")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Courage Test Duelist" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the Bears deal first-strike damage; the blocker dies before striking back") {
                    game.isOnBattlefield("Courage Test Duelist") shouldBe false
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
