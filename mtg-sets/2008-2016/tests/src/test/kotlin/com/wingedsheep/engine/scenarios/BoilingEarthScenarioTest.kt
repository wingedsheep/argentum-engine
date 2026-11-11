package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Boiling Earth — 1 damage to each creature your opponents control; awakened, it also makes a land
 * you control a 4/4 hasty Elemental, which the damage never touches.
 */
class BoilingEarthScenarioTest : ScenarioTestBase() {

    private fun board(mountains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Boiling Earth")
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withCardOnBattlefield(1, "Llanowar Elves")
        .withCardOnBattlefield(2, "Llanowar Elves")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast for {1}{R}, it deals 1 damage to each creature an opponent controls and none to yours") {
            val game = board(mountains = 2)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Boiling Earth").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            game.isInGraveyard(1, "Llanowar Elves") shouldBe false
            game.state.getEntity(bears)?.get<DamageComponent>()?.amount shouldBe 1
            game.findPermanents("Mountain").none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("cast for its awaken cost, it also makes a land a 4/4 hasty Elemental that takes no damage") {
            val game = board(mountains = 7)
            val land = game.findPermanents("Mountain").first()
            game.castSpellWithAwaken(1, "Boiling Earth", listOf(land)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            game.isInGraveyard(1, "Llanowar Elves") shouldBe false
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 4
            projected.getToughness(land) shouldBe 4
            (game.state.getEntity(land)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }
    }
}
