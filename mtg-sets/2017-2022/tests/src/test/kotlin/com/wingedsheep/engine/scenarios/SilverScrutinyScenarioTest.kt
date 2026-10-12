package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Silver Scrutiny (DMU #65).
 *
 * "You may cast this spell as though it had flash if X is 3 or less. Draw X cards."
 *
 * The flash hangs on the announced X: at instant speed the offer's X ceiling is capped at 3 and a
 * cast announcing X = 4 is rejected; in its controller's own main phase X is bounded only by mana.
 */
class SilverScrutinyScenarioTest : ScenarioTestBase() {

    private fun game(onOwnTurn: Boolean) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Silver Scrutiny")
        .withLandsOnBattlefield(1, "Island", 8)
        .apply { repeat(8) { withCardInLibrary(1, "Island") } }
        .withActivePlayer(if (onOwnTurn) 1 else 2)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.scrutinyOffer() =
        getLegalActions(1).firstOrNull { it.actionType == "CastSpell" && it.description.contains("Silver Scrutiny") }

    init {
        context("Silver Scrutiny") {

            test("at instant speed the offer caps X at 3") {
                val game = game(onOwnTurn = false)
                val offer = game.scrutinyOffer().shouldNotBeNull()
                withClue("eight Islands afford X = 6, but flash only covers X ≤ 3") {
                    offer.maxAffordableX shouldBe 3
                }
            }

            test("at instant speed X = 3 is cast and draws three") {
                val game = game(onOwnTurn = false)
                game.castXSpell(1, "Silver Scrutiny", 3).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 3
            }

            test("at instant speed X = 4 is rejected") {
                val game = game(onOwnTurn = false)
                game.castXSpell(1, "Silver Scrutiny", 4).error shouldNotBe null
                game.handSize(1) shouldBe 1
            }

            test("in its controller's main phase X is bounded only by mana") {
                val game = game(onOwnTurn = true)
                game.scrutinyOffer().shouldNotBeNull().maxAffordableX shouldBe 6
                game.castXSpell(1, "Silver Scrutiny", 6).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 6
            }
        }
    }
}
