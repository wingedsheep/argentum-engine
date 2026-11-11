package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rising Miasma — all creatures get -2/-2 until end of turn; awakened, the land it animates becomes a
 * creature only after the -2/-2 is locked in, so it ends up a 3/3 (2015-08-25 ruling).
 */
class RisingMiasmaScenarioTest : ScenarioTestBase() {

    private fun board(swamps: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Rising Miasma")
        .withLandsOnBattlefield(1, "Swamp", swamps)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast for {3}{B}, every creature on both sides gets -2/-2") {
            val game = board(swamps = 4)
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Rising Miasma").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            val projected = game.state.projectedState
            projected.getPower(giant) shouldBe 1
            projected.getToughness(giant) shouldBe 1
        }

        test("cast for its awaken cost, the awakened land is a 3/3 untouched by the -2/-2") {
            val game = board(swamps = 7)
            val land = game.findPermanents("Swamp").first()
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpellWithAwaken(1, "Rising Miasma", listOf(land)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            val projected = game.state.projectedState
            projected.getPower(giant) shouldBe 1
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 3
            projected.getToughness(land) shouldBe 3
        }
    }
}
