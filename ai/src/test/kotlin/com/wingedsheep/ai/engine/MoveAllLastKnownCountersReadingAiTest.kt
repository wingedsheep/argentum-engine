package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Whenever a creature you control dies, put its counters on up to one target creature"
 * ([com.wingedsheep.sdk.scripting.effects.MoveAllLastKnownCountersEffect]). The slot reads FLEXIBLE
 * ([CounterManipulationReadingAiTest]): the counters' kinds decide which side wants them, so the
 * simulation weighs each candidate. This pins the decision that reading has to produce when the
 * counters are +1/+1 and the target may be any creature: ours, never the opponent's.
 */
class MoveAllLastKnownCountersReadingAiTest : ScenarioTestBase() {

    init {
        test("Reluctant Role Model hands a dead creature's counters to our creature, not the opponent's") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(5)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withCardOnBattlefield(1, "Reluctant Role Model")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Craw Wurm")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            // Grizzly Bears grown to 3/3 by a +1/+1 counter: Bolt kills it, and the Role Model's
            // trigger hands that counter to up to one target creature.
            val bears = game.findPermanent("Grizzly Bears")!!
            val roleModel = game.findPermanent("Reluctant Role Model")!!
            game.state = game.state.updateEntity(bears) { c ->
                c.with(CountersComponent().withCounters(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val answer = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .respondToDecision(game.state, decision).shouldBeInstanceOf<TargetsResponse>()
            withClue("the Bears' +1/+1 counter is a gift: it goes on our Role Model, never on their Craw Wurm") {
                answer.selectedTargets.values.flatten() shouldBe listOf(roleModel)
            }
        }
    }
}
