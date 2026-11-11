package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Rush of Ice — taps a creature that then skips its controller's next untap step; awakened, a land
 * also becomes a 3/3 hasty Elemental.
 */
class RushOfIceScenarioTest : ScenarioTestBase() {

    private fun board(islands: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Rush of Ice")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Island", islands)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)?.has<TappedComponent>() == true

    init {
        test("cast for {U}, it taps the creature, which stays tapped through its controller's next untap step") {
            val game = board(islands = 1)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Rush of Ice", bears).error shouldBe null
            game.resolveStack()

            game.isTapped(bears) shouldBe true
            game.findPermanents("Island").none { game.state.projectedState.isCreature(it) } shouldBe true

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.isTapped(bears) shouldBe true
        }

        test("cast for its awaken cost, it taps the creature and the land becomes a 3/3 hasty Elemental") {
            val game = board(islands = 5)
            val bears = game.findPermanent("Grizzly Bears")!!
            val land = game.findPermanents("Island").first()
            game.castSpellWithAwaken(1, "Rush of Ice", listOf(bears, land)).error shouldBe null
            game.resolveStack()

            game.isTapped(bears) shouldBe true
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
