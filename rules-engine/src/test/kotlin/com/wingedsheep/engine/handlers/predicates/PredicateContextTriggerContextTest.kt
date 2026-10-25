package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A trigger-targeting [PredicateContext] carries the trigger's event context into the
 * [EffectContext] a filter's dynamic cap evaluates against, so "mana value X or less, where X is the
 * damage dealt" (Venerable Warsinger) reads the event's damage at target choice instead of 0.
 */
class PredicateContextTriggerContextTest : FunSpec({
    val you = EntityId("you")

    test("a targeting context hands its trigger context to the dynamic-amount context") {
        val context = PredicateContext(controllerId = you, triggerContext = TriggerContext(damageAmount = 3))
        context.toEffectContext().triggerContext?.damageAmount shouldBe 3
    }

    test("without one, a resolution context's trigger context still comes through") {
        val resolution = EffectContext(sourceId = null, controllerId = you, triggerContext = TriggerContext(damageAmount = 2))
        PredicateContext.fromEffectContext(resolution).toEffectContext().triggerContext?.damageAmount shouldBe 2
    }

    test("a context built outside any trigger has none") {
        PredicateContext(controllerId = you).toEffectContext().triggerContext shouldBe null
    }
})
