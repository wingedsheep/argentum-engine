package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ForEachTargetContextTest : FunSpec({

    test("a surviving target is rebound after an earlier target becomes illegal") {
        val survivingTarget = ChosenTarget.Permanent(EntityId("surviving-target"))
        val resolvedTargets = mutableListOf<ChosenTarget?>()
        val executor = ForEachExecutor { state, _, context ->
            resolvedTargets += context.positionalTarget(0)
            EffectResult.success(state)
        }
        val player = EntityId("player")

        val result = executor.execute(
            GameState(turnOrder = listOf(player)),
            ForEachTargetEffect(listOf(Effects.DrawCards(1))),
            EffectContext(
                sourceId = null,
                controllerId = player,
                targets = listOf(survivingTarget),
                alignedTargets = listOf(null, survivingTarget),
            ),
        )

        result.error shouldBe null
        resolvedTargets shouldBe listOf(survivingTarget)
    }
})
