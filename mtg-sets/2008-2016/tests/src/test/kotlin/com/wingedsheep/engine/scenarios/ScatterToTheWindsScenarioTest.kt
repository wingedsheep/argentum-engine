package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scatter to the Winds — counters target spell; awakened, it also makes a land a 3/3 Elemental
 * with haste. The awaken land target follows the spell target, so the awakened cast is built by
 * hand (a stack target first, then the land).
 */
class ScatterToTheWindsScenarioTest : ScenarioTestBase() {

    /** The opponent, as active player, has Grizzly Bears on the stack and passes priority. */
    private fun boardWithBearsOnStack(islands: Int): ScenarioTestBase.TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Scatter to the Winds")
            .withLandsOnBattlefield(1, "Island", islands)
            .withCardInHand(2, "Grizzly Bears")
            .withLandsOnBattlefield(2, "Forest", 2)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(2, "Grizzly Bears").error shouldBe null
        game.passPriority()
        return game
    }

    init {
        test("cast for {1}{U}{U}, it counters the target spell and no land is awakened") {
            val game = boardWithBearsOnStack(islands = 3)
            game.castSpellTargetingStackSpell(1, "Scatter to the Winds", "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.findPermanents("Island").none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("cast for its awaken cost, it counters the spell and the land becomes a 3/3 hasty Elemental") {
            val game = boardWithBearsOnStack(islands = 6)
            val land = game.findPermanents("Island").first()
            val bears = game.state.stack.single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val scatter = game.state.getHand(game.player1Id).single()
            game.execute(
                CastSpell(
                    game.player1Id, scatter,
                    targets = listOf(ChosenTarget.Spell(bears), ChosenTarget.Permanent(land)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.AWAKEN
                )
            ).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
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
