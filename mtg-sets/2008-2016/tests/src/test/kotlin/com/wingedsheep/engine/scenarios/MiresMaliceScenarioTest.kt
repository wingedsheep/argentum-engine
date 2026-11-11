package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Mire's Malice — target opponent discards two cards; awakened, it also makes a land a 3/3
 * Elemental with haste. The awaken land target follows the opponent target (a player target,
 * so the cast is built by hand rather than through `castSpellWithAwaken`).
 */
class MiresMaliceScenarioTest : ScenarioTestBase() {

    private fun board(swamps: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Mire's Malice")
        .withLandsOnBattlefield(1, "Swamp", swamps)
        .withCardInHand(2, "Grizzly Bears")
        .withCardInHand(2, "Hill Giant")
        .withCardInHand(2, "Ornithopter")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.discardAsAsked() {
        var guard = 0
        while (hasPendingDecision() && guard++ < 5) {
            val decision = getPendingDecision() as SelectCardsDecision
            selectCards(decision.options.take(decision.minSelections))
        }
    }

    init {
        test("cast for {3}{B}, the opponent discards two cards and no land is awakened") {
            val game = board(swamps = 4)
            game.castSpellTargetingPlayer(1, "Mire's Malice", 2).error shouldBe null
            game.resolveStack()
            game.discardAsAsked()

            game.handSize(2) shouldBe 1
            game.graveyardSize(2) shouldBe 2
            game.findPermanents("Swamp").none { game.state.projectedState.isCreature(it) } shouldBe true
        }

        test("cast for its awaken cost, the opponent discards two and the land becomes a 3/3 hasty Elemental") {
            val game = board(swamps = 6)
            val land = game.findPermanents("Swamp").first()
            val malice = game.state.getHand(game.player1Id).single()
            game.execute(
                CastSpell(
                    game.player1Id, malice,
                    targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(land)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.AWAKEN
                )
            ).error shouldBe null
            game.resolveStack()
            game.discardAsAsked()

            game.handSize(2) shouldBe 1
            game.graveyardSize(2) shouldBe 2
            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasSubtype(land, "Swamp") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 3
            projected.getToughness(land) shouldBe 3
            game.state.getEntity(land)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }
    }
}
