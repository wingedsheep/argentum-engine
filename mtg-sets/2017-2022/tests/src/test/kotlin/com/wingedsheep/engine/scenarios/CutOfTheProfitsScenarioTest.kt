package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cut of the Profits (SNC #72) — {X}{B}{B} Sorcery.
 *
 *   Casualty 3. You draw X cards and you lose X life.
 *
 * Pins that the casualty copy inherits the original's X (CR 707.10 / the card's ruling), so paying
 * casualty doubles both the draw and the life loss.
 */
class CutOfTheProfitsScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.ScenarioBuilder.library(): ScenarioTestBase.ScenarioBuilder {
        var b = this
        repeat(6) { b = b.withCardInLibrary(1, "Swamp") }
        return b
    }

    init {
        test("without casualty, X = 2 draws two cards and loses 2 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Cut of the Profits")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .library()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cut = game.findCardsInHand(1, "Cut of the Profits").single()
            game.execute(CastSpell(game.player1Id, cut, xValue = 2)).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 2
            game.getLifeTotal(1) shouldBe 18
        }

        test("with casualty, the copy keeps X — four cards drawn and 4 life lost") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Cut of the Profits")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .library()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cut = game.findCardsInHand(1, "Cut of the Profits").single()
            val giant = game.findPermanent("Hill Giant")!!
            game.execute(CastSpell(game.player1Id, cut, xValue = 2, casualtyCreature = giant)).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant was sacrificed for casualty") { game.findPermanent("Hill Giant") shouldBe null }
            withClue("Copy and original each draw X = 2") { game.handSize(1) shouldBe 4 }
            withClue("Copy and original each lose X = 2 life") { game.getLifeTotal(1) shouldBe 16 }
        }
    }
}
