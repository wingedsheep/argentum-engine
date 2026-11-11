package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Ruinous Path — destroys a creature; awakened, it also makes a land a 4/4 Elemental with haste. */
class RuinousPathScenarioTest : ScenarioTestBase() {

    private fun board(swamps: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Ruinous Path")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", swamps)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast for {1}{B}{B}, it destroys the creature and no land is awakened") {
            val game = board(swamps = 3)
            game.castSpell(1, "Ruinous Path", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.findPermanents("Swamp").none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("cast for its awaken cost, it destroys the creature and the land becomes a 4/4 hasty Elemental") {
            val game = board(swamps = 7)
            val land = game.findPermanents("Swamp").first()
            game.castSpellWithAwaken(1, "Ruinous Path", listOf(game.findPermanent("Grizzly Bears")!!, land))
                .error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasSubtype(land, "Swamp") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 4
            game.state.getEntity(land)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
        }
    }
}
