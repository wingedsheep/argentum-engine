package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Regression coverage for relational single-player loops with no referent. */
class ForEachRelationalPlayerTest : FunSpec({

    test("an unresolved ControllerOf reference iterates nobody rather than every active player") {
        var executions = 0
        val executor = ForEachExecutor { state, _, _ ->
            executions += 1
            EffectResult.success(state)
        }
        val player1 = EntityId("player-1")
        val player2 = EntityId("player-2")
        val state = GameState(turnOrder = listOf(player1, player2))
        val effect = Effects.ForEachPlayer(
            Player.ControllerOf("optional target creature"),
            listOf(Effects.DrawCards(1)),
        ) as ForEachEffect

        val result = executor.execute(
            state,
            effect,
            EffectContext(sourceId = null, controllerId = player1, targets = emptyList()),
        )

        result.error shouldBe null
        executions shouldBe 0
        result.state shouldBe state
    }
})
