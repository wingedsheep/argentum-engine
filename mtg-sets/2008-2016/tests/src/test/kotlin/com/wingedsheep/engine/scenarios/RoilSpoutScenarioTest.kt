package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Roil Spout — puts a creature on top of its owner's library; awakened, it also makes a land a 4/4 Elemental with haste. */
class RoilSpoutScenarioTest : ScenarioTestBase() {

    private fun board(plains: Int, islands: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Roil Spout")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Forest")
        .withLandsOnBattlefield(1, "Plains", plains)
        .withLandsOnBattlefield(1, "Island", islands)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.topOfOpponentLibrary(): String? =
        state.getLibrary(player2Id).firstOrNull()?.let { state.getEntity(it)?.get<CardComponent>()?.name }

    init {
        test("cast for {1}{W}{U}, it puts the creature on top of its owner's library and no land is awakened") {
            val game = board(plains = 2, islands = 1)
            val librarySizeBefore = game.librarySize(2)
            game.castSpell(1, "Roil Spout", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            game.topOfOpponentLibrary() shouldBe "Grizzly Bears"
            game.librarySize(2) shouldBe librarySizeBefore + 1
            (game.findPermanents("Plains") + game.findPermanents("Island"))
                .none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("cast for its awaken cost, it tucks the creature and the land becomes a 4/4 hasty Elemental") {
            val game = board(plains = 3, islands = 3)
            val land = game.findPermanents("Plains").first()
            game.castSpellWithAwaken(1, "Roil Spout", listOf(game.findPermanent("Grizzly Bears")!!, land))
                .error shouldBe null
            game.resolveStack()

            game.topOfOpponentLibrary() shouldBe "Grizzly Bears"
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasSubtype(land, "Plains") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 4
            projected.getToughness(land) shouldBe 4
            game.state.getEntity(land)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
        }
    }
}
