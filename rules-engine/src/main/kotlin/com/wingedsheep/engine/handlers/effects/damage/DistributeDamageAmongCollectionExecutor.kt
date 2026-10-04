package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DistributeDamageContinuation
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ChooserResolution
import com.wingedsheep.engine.handlers.effects.DamageUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DistributeDamageAmongCollectionEffect
import kotlin.reflect.KClass

/**
 * Executor for [DistributeDamageAmongCollectionEffect].
 *
 * The recipients are a pipeline collection, not targets, so there was no division to announce on
 * the stack (CR 601.2d is about targets); the chooser divides the damage now, as the effect
 * resolves. Members that have left the battlefield are new objects (CR 400.7) and drop out. The
 * division itself rides the existing [DistributeDamageContinuation], with the *damage source* as
 * its source — so the damage is dealt by that creature, not by the ability's source.
 */
class DistributeDamageAmongCollectionExecutor(
    private val zones: ZoneTransitionService,
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<DistributeDamageAmongCollectionEffect> {

    override val effectType: KClass<DistributeDamageAmongCollectionEffect> =
        DistributeDamageAmongCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: DistributeDamageAmongCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val dealerId = context.resolveTarget(effect.damageSource, state)
            ?: return EffectResult.success(state)
        val total = amountEvaluator.evaluate(state, effect.amount, context)
        if (total <= 0) return EffectResult.success(state)

        val battlefield = state.getBattlefield().toHashSet()
        val recipients = (context.pipeline.storedCollections[effect.collectionName] ?: emptyList())
            .filter { it in battlefield }
            .distinct()
        if (recipients.isEmpty()) return EffectResult.success(state)

        val dealerRef = state.objectRef(dealerId)
        if (recipients.size == 1) {
            return dealDamageToTarget(zones, state, recipients.single(), total, dealerId, damageSourceRef = dealerRef)
        }

        val deciderId = when (val outcome = ChooserResolution.resolve(state, effect.chooser, context, recipients)) {
            is ChooserResolution.Outcome.Resolved -> outcome.playerId
            is ChooserResolution.Outcome.NeedsOpponentPick -> return ChooserResolution.pauseForOpponentPick(
                state, outcome.opponents, effect, context,
                prompt = "Choose which opponent divides the damage"
            )
            is ChooserResolution.Outcome.Unresolvable ->
                return EffectResult.error(state, "DistributeDamageAmongCollection chooser: ${outcome.reason}")
        }

        val dealerName = state.getEntity(dealerId)?.get<CardComponent>()?.name ?: "Creature"
        val decision = { decisionId: String -> DistributeDecision(
            id = decisionId,
            playerId = deciderId,
            prompt = "Divide $total damage from $dealerName among ${recipients.size} creatures",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                phase = DecisionPhase.RESOLUTION
            ),
            totalAmount = total,
            targets = recipients,
            // "Among any number of those": a recipient may be left out, but all of it is dealt.
            minPerTarget = 0,
            allowPartial = false
        ) }

        val continuation = DistributeDamageContinuation(
            sourceId = dealerId,
            controllerId = context.controllerId,
            targets = recipients
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation, eventType = "DISTRIBUTE"))
    }
}
