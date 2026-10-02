package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.SerializableModification

/** Membership is fixed at resolution; the alternative filter reads current projected state. */
class CantBeBlockedExceptByCollectionRule(private val predicates: PredicateEvaluator) : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        for (floating in ctx.state.floatingEffects) {
            val restriction = floating.effect.modification as? SerializableModification.CantBeBlockedExceptByCollection
                ?: continue
            if (ctx.attackerId !in floating.effect.affectedEntities) continue
            if (floating.referencedObjects.none { it.entityId == ctx.attackerId && ctx.state.isCurrentObject(it) }) continue
            val blocker = ctx.state.objectRef(ctx.blockerId)
            if (blocker != null && blocker in restriction.blockers) continue
            if (predicates.matches(
                    ctx.state, ctx.projected, ctx.blockerId, restriction.alternativeFilter,
                    PredicateContext(controllerId = floating.controllerId, sourceId = floating.sourceId)
                )) continue
            return "This creature can't block that attacker (not in the chosen group and doesn't match ${restriction.alternativeFilter.description})"
        }
        return null
    }
}
