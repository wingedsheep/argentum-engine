package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Clutch of Currents — bounces a creature; awakened, it also makes a land a 3/3 Elemental with haste. */
class ClutchOfCurrentsScenarioTest : ScenarioTestBase() {

    private fun board(islands: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Clutch of Currents")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Island", islands)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast for {U}, it returns the creature to its owner's hand and no land is awakened") {
            val game = board(islands = 1)
            game.castSpell(1, "Clutch of Currents", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.findPermanent("Grizzly Bears") shouldBe null
            game.findPermanents("Island").none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("cast for its awaken cost, it bounces the creature and the land becomes a 3/3 hasty Elemental") {
            val game = board(islands = 5)
            val land = game.findPermanents("Island").first()
            game.castSpellWithAwaken(1, "Clutch of Currents", listOf(game.findPermanent("Grizzly Bears")!!, land))
                .error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Grizzly Bears") shouldBe true
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasSubtype(land, "Island") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 3
            projected.getToughness(land) shouldBe 3
            game.state.getEntity(land)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }
    }
}
