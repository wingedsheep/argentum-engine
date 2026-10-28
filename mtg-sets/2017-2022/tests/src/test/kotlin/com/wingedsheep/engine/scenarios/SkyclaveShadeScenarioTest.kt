package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Skyclave Shade (ZNR #125) — {1}{B} 3/1, Kicker {2}{B}, can't block, enters with two +1/+1
 * counters if kicked. Landfall — Whenever a land you control enters, if this card is in your
 * graveyard and it's your turn, you may cast it from your graveyard this turn.
 *
 * Proves the graveyard-functioning landfall trigger grants a this-turn permission over the card
 * where it lies, and that the permission lets the Shade be cast kicked.
 */
class SkyclaveShadeScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInGraveyard(1, "Skyclave Shade")
        .withCardInHand(1, "Swamp")
        .withLandsOnBattlefield(1, "Swamp", 5)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.shadeCasts() = findCardsInGraveyard(1, "Skyclave Shade").single().let { shade ->
        getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == shade }
    }

    init {
        test("without a landfall it can't be cast from the graveyard") {
            val game = board()
            withClue("no permission before a land enters") { game.shadeCasts().shouldBeEmpty() }
        }

        test("landfall on your turn lets you cast it kicked from the graveyard") {
            val game = board()
            val swamp = game.findCardsInHand(1, "Swamp").single()
            game.execute(PlayLand(game.player1Id, swamp)).error.shouldBeNull()
            withClue("the landfall trigger goes on the stack from the graveyard") {
                game.state.stack.size shouldBe 1
            }
            game.resolveStack()

            val kicked = game.shadeCasts().single { it.actionType == "CastWithKicker" }
            game.execute(kicked.action).error.shouldBeNull()
            game.resolveStack()

            val shade = game.findPermanent("Skyclave Shade")!!
            game.isInGraveyard(1, "Skyclave Shade") shouldBe false
            game.state.getEntity(shade)!!.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.getPower(shade) shouldBe 5
            game.state.projectedState.getToughness(shade) shouldBe 3
        }
    }
}
