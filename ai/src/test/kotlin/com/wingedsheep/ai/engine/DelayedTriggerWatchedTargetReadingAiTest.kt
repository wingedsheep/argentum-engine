package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A delayed trigger that watches its target
 * ([com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect.watchedTarget]) is read off
 * what it fires and on what event. "Whenever it deals combat damage to a player this turn, you
 * draw that many cards" (The Inner Light chapter II, Captain Howler) pays us when the watched
 * creature connects, so the creature to name is one of ours. Unread, the slot was a coin toss the
 * removal tie-break settled on the opponent's creature — which can't even attack on our turn.
 */
class DelayedTriggerWatchedTargetReadingAiTest : ScenarioTestBase() {

    init {
        test("The Inner Light's chapter II watches our attacker, not the opponent's creature") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.DRAW)
                .withCardOnBattlefield(1, "The Inner Light")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .apply { repeat(6) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                .build()
            val saga = game.findPermanent("The Inner Light")!!
            game.state = game.state.updateEntity(saga) { c ->
                c.with(CountersComponent().withCounters(CounterType.LORE, 1))
                    .with(SagaComponent(triggeredChapters = setOf(1)))
            }

            var passes = 0
            while (game.state.pendingDecision !is ChooseTargetsDecision && passes++ < 8) game.passPriority()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()

            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            val picked = response.selectedTargets.values.flatten()
            withClue("a draw on the watched creature's combat damage reads as a gift to our own creature: picked $picked") {
                picked shouldBe listOf(game.findPermanent("Grizzly Bears")!!)
            }
        }
    }
}
