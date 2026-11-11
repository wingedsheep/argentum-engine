package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Planar Outburst — destroys every nonland creature on both sides, sparing land creatures; awakened,
 * the land it animates becomes a creature after the sweep and survives as a 4/4 hasty Elemental.
 */
class PlanarOutburstScenarioTest : ScenarioTestBase() {

    private fun board(plains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Planar Outburst")
        .withLandsOnBattlefield(1, "Plains", plains)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Dryad Arbor")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast for {3}{W}{W}, it destroys every nonland creature but not a land creature") {
            val game = board(plains = 5)
            game.castSpell(1, "Planar Outburst").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.findPermanent("Dryad Arbor") shouldNotBe null
            game.findPermanents("Plains").none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("cast for its awaken cost, the awakened land survives the sweep as a 4/4 hasty Elemental") {
            val game = board(plains = 8)
            val land = game.findPermanents("Plains").first()
            game.castSpellWithAwaken(1, "Planar Outburst", listOf(land)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.findPermanent("Dryad Arbor") shouldNotBe null
            game.findPermanents("Plains").contains(land) shouldBe true
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 4
            projected.getToughness(land) shouldBe 4
            game.state.getEntity(land)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
        }
    }
}
