package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Tenured Oilcaster (MOM #126) — 2/4 menace; +3/+0 while an opponent has eight or more cards in
 * their graveyard; whenever it attacks or blocks, each player mills a card.
 */
class TenuredOilcasterScenarioTest : ScenarioTestBase() {
    init {
        test("gets +3/+0 only while an opponent has eight or more cards in their graveyard") {
            val seven = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Tenured Oilcaster")
                .apply { repeat(7) { withCardInGraveyard(2, "Swamp") } }
                .apply { repeat(9) { withCardInGraveyard(1, "Swamp") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oil7 = seven.findPermanent("Tenured Oilcaster")!!
            seven.state.projectedState.getPower(oil7) shouldBe 2
            seven.state.projectedState.getToughness(oil7) shouldBe 4

            val eight = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Tenured Oilcaster")
                .apply { repeat(8) { withCardInGraveyard(2, "Swamp") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oil8 = eight.findPermanent("Tenured Oilcaster")!!
            eight.state.projectedState.getPower(oil8) shouldBe 5
            eight.state.projectedState.getToughness(oil8) shouldBe 4
        }

        test("attacking makes each player mill a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Tenured Oilcaster", summoningSickness = false)
                .apply { repeat(3) { withCardInLibrary(1, "Island") } }
                .apply { repeat(3) { withCardInLibrary(2, "Swamp") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Tenured Oilcaster" to 2)).error shouldBe null
            game.resolveStack()

            game.graveyardSize(1) shouldBe 1
            game.graveyardSize(2) shouldBe 1
        }

        test("blocking makes each player mill a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Tenured Oilcaster")
                .apply { repeat(3) { withCardInLibrary(1, "Island") } }
                .apply { repeat(3) { withCardInLibrary(2, "Swamp") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Tenured Oilcaster" to listOf("Grizzly Bears"))).error shouldBe null
            game.resolveStack()

            game.librarySize(1) shouldBe 2
            game.librarySize(2) shouldBe 2
            game.isInGraveyard(2, "Swamp") shouldBe true
        }
    }
}
