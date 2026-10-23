package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Florian, Voldaren Scion (MID) — {1}{B}{R} Legendary Creature — Vampire Noble 3/3, first strike.
 *
 * "At the beginning of each of your postcombat main phases, look at the top X cards of your
 * library, where X is the total amount of life your opponents lost this turn. Exile one of those
 * cards and put the rest on the bottom of your library in a random order. You may play the exiled
 * card this turn."
 */
class FlorianVoldarenScionScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.ScenarioBuilder.withBoltLibrary() = apply {
        repeat(5) { withCardInLibrary(1, "Lightning Bolt") }
        withCardInLibrary(2, "Mountain")
        withCardInLibrary(2, "Mountain")
    }

    init {
        test("looks at X = life the opponent lost, exiles one, and it is playable this turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Florian, Voldaren Scion", summoningSickness = false)
                .withCardOnBattlefield(1, "Mountain")
                .withBoltLibrary()
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Florian, Voldaren Scion" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 17
            game.resolveStack()

            val decision = game.getPendingDecision() as SelectCardsDecision
            withClue("The opponent lost 3 life this turn, so Florian looks at the top three cards") {
                decision.options.size shouldBe 3
                decision.minSelections shouldBe 1
                decision.maxSelections shouldBe 1
            }
            game.selectCards(listOf(decision.options.first())).error shouldBe null
            game.resolveStack()

            game.state.getExile(game.player1Id).size shouldBe 1
            withClue("The other two went to the bottom of the library") {
                game.librarySize(1) shouldBe 4
            }

            val bolt = game.state.getExile(game.player1Id).single()
            game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 14
        }

        test("no life lost this turn — the trigger looks at nothing and exiles nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Florian, Voldaren Scion", summoningSickness = false)
                .withBoltLibrary()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.state.getExile(game.player1Id).size shouldBe 0
            game.librarySize(1) shouldBe 5
        }
    }
}
