package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Coastal Discovery — draws two; awakened, it also makes a land a 4/4 Elemental with haste. */
class CoastalDiscoveryScenarioTest : ScenarioTestBase() {

    private fun board(islands: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Coastal Discovery")
        .withLandsOnBattlefield(1, "Island", islands)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast for {3}{U}, it draws two cards") {
            val game = board(islands = 4)
            game.castSpell(1, "Coastal Discovery").error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 2
            game.findPermanents("Island").none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("cast for its awaken cost, it draws two and the land becomes a 4/4 hasty Elemental") {
            val game = board(islands = 6)
            val land = game.findPermanents("Island").first()
            game.castSpellWithAwaken(1, "Coastal Discovery", listOf(land)).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 2
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 4
            projected.getToughness(land) shouldBe 4
        }
    }
}
