package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Earthen Arms — two +1/+1 counters on target permanent; awakened, a land also becomes a 4/4
 * hasty Elemental. The two targets are separate instances of "target", so one land may be both.
 */
class EarthenArmsScenarioTest : ScenarioTestBase() {

    private fun board(forests: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Earthen Arms")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", forests)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("cast for {1}{G}, it puts two +1/+1 counters on the creature and no land is awakened") {
            val game = board(forests = 2)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Earthen Arms", bears).error shouldBe null
            game.resolveStack()

            game.plusOnes(bears) shouldBe 2
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.findPermanents("Forest").none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("cast for its awaken cost, the creature gets two counters and the land becomes a 4/4 hasty Elemental") {
            val game = board(forests = 7)
            val bears = game.findPermanent("Grizzly Bears")!!
            val land = game.findPermanents("Forest").first()
            game.castSpellWithAwaken(1, "Earthen Arms", listOf(bears, land)).error shouldBe null
            game.resolveStack()

            game.plusOnes(bears) shouldBe 2
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasSubtype(land, "Forest") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            game.plusOnes(land) shouldBe 4
            projected.getPower(land) shouldBe 4
            projected.getToughness(land) shouldBe 4
        }

        test("awakened, the same land can be both targets and becomes a 6/6") {
            val game = board(forests = 7)
            val land = game.findPermanents("Forest").first()
            game.castSpellWithAwaken(1, "Earthen Arms", listOf(land, land)).error shouldBe null
            game.resolveStack()

            game.plusOnes(land) shouldBe 6
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.getPower(land) shouldBe 6
            projected.getToughness(land) shouldBe 6
        }
    }
}
