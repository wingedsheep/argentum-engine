package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.RevertCopyAtEndOfTurnComponent
import com.wingedsheep.engine.state.components.identity.RevertCopyAtNextEndStepComponent
import com.wingedsheep.engine.state.components.identity.RevertCopyAtYourNextTurnComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for [EachPermanentBecomesCopyOfTargetEffect].
 *
 * Replaces each matching permanent's [CardComponent] with a copy of the target's,
 * mirroring the mechanism used by `EntersAsCopy` (Clone) — copy effects bake into
 * base state rather than living as a Layer 1 continuous effect. See
 * `docs/architecture-principles.md` §2.11.
 *
 * Rule 707: copiable values only. Counters, tapped state, attached auras/equipment,
 * and non-copy floating effects on the target are not copied — those live on other
 * components / floating effects, not on `CardComponent`, so replacing the card
 * component wholesale produces the correct behavior automatically.
 *
 * Used by Mirrorform: "Each nonland permanent you control becomes a copy of target
 * non-Aura permanent."
 *
 * Copy *exceptions* (CR 707.9 — "except it has …") ride two riders on the effect.
 * [EachPermanentBecomesCopyOfTargetEffect.exceptions] carries every characteristic modification
 * (name, added/removed types, keywords, base P/T, colors) and is applied by the shared
 * [CopyExceptionApplier], the same helper the token-copy path uses.
 * [EachPermanentBecomesCopyOfTargetEffect.retainActivatingAbility]
 * re-grants the activated ability that created the copy: replacing the card component drops every
 * printed ability of the original card, so the ability has to come back through the durable
 * granted-activated-ability record, which is keyed by entity and so rides along through further
 * copies. Likeness Looter's "{X}: … becomes a copy of target creature card in your graveyard with
 * mana value X, except it has flying and this ability" is both riders at once.
 */
class EachPermanentBecomesCopyOfTargetExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<EachPermanentBecomesCopyOfTargetEffect> {

    override val effectType: KClass<EachPermanentBecomesCopyOfTargetEffect> =
        EachPermanentBecomesCopyOfTargetEffect::class

    override fun execute(
        state: GameState,
        effect: EachPermanentBecomesCopyOfTargetEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        val targetCard = state.getEntity(targetId)?.get<CardComponent>()
            ?: return EffectResult.success(state)

        // Target must still be on the battlefield to serve as a copy source — unless the effect
        // allows a non-battlefield source (Lazav, Familiar Stranger copies a creature card it just
        // exiled), in which case its copiable characteristics are read wherever it currently is.
        if (!effect.sourceFromAnyZone && targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val affectedTarget = effect.affected
        val affected = if (affectedTarget != null) {
            // "target permanent A becomes a copy of target permanent B" — the affected set is the
            // single resolved [affected] target (Fleeting Reflection). Resolving to nothing is a
            // no-op. Must still be on the battlefield to become a copy.
            val affectedId = context.resolveTarget(affectedTarget, state)
            if (affectedId == null || affectedId !in state.getBattlefield()) {
                emptyList()
            } else {
                listOf(affectedId)
            }
        } else {
            BattlefieldFilterUtils.findMatchingOnBattlefield(
                state,
                effect.filter.baseFilter,
                context,
                excludeSelfId = if (effect.filter.excludeSelf) context.sourceId else null
            )
                // "each OTHER … becomes a copy of that …" — the copy source keeps its own identity
                // (and any counter just placed on it), so exclude the target from the affected set.
                .filterNot { effect.excludeTarget && it == targetId }
        }

        if (affected.isEmpty()) {
            return EffectResult.success(state)
        }

        // Supported durations: `Permanent` (Mirrorform/Clone, baked forever), `EndOfTurn`
        // (reverted at cleanup), `UntilNextEndStep` (reverted on entry to the next end step,
        // coincident with a paired "return it at the beginning of the next end step" trigger —
        // Niko, Light of Hope), and `UntilYourNextTurn` (reverted after the controller's next
        // untap step — Absorbing Man, Taskmaster). Anything else degrades to permanent.
        var newState = state
        for (entityId in affected) {
            val container = newState.getEntity(entityId) ?: continue
            val currentCard = container.get<CardComponent>() ?: continue

            // Preserve the original ownership; only copiable card characteristics change.
            // The "except …" clause (CR 707.9) is applied by the shared [CopyExceptionApplier],
            // the same helper the token-copy path uses, so it rides on the copy's own
            // CardComponent and lasts exactly as long as the copy does.
            val copiedCard = CopyExceptionApplier.apply(targetCard, effect.exceptions)
                .copy(ownerId = currentCard.ownerId)

            // If this permanent is already a copy, keep the existing pre-copy snapshot
            // so a chain of copy effects still reverts to the printed identity on exit.
            val existingCopyOf = container.get<CopyOfComponent>()
            val originalCardSnapshot = existingCopyOf?.originalCardComponent ?: currentCard
            val originalDefinitionId =
                existingCopyOf?.originalCardDefinitionId ?: currentCard.cardDefinitionId

            newState = newState.updateEntity(entityId) { c ->
                var updated = c.with(copiedCard)
                    .with(
                        CopyOfComponent(
                            originalCardDefinitionId = originalDefinitionId,
                            copiedCardDefinitionId = targetCard.cardDefinitionId,
                            originalCardComponent = originalCardSnapshot
                        )
                    )
                // Tag the temporary-copy revert marker. Branch with concrete types so each is stored
                // under its own component key (`with` is a reified generic).
                when (effect.duration) {
                    Duration.EndOfTurn -> updated = updated.with(RevertCopyAtEndOfTurnComponent)
                    Duration.UntilNextEndStep -> updated = updated.with(RevertCopyAtNextEndStepComponent)
                    Duration.UntilYourNextTurn -> updated = updated.with(
                        RevertCopyAtYourNextTurnComponent(context.controllerId)
                    )
                    else -> {}
                }
                updated
            }

            // "…and this ability": re-grant the activated ability that produced this copy. The
            // resolving ability's identity carries the *permanent's* card definition id, which has
            // already drifted to whatever it last copied — so read the granted record first (that's
            // where the ability lives after the first copy) and fall back to the printed definition
            // for the very first activation.
            if (effect.retainActivatingAbility) {
                val identity = context.abilityIdentity
                if (identity != null && newState.grantedActivatedAbilities
                        .none { it.entityId == entityId && it.ability.id == identity.abilityId }
                ) {
                    val ability = state.grantedActivatedAbilities
                        .firstOrNull { it.entityId == entityId && it.ability.id == identity.abilityId }
                        ?.ability
                        ?: cardRegistry.getCard(identity.cardDefinitionId)
                            ?.activatedAbilities?.firstOrNull { it.id == identity.abilityId }
                    if (ability != null) {
                        newState = newState.copy(
                            grantedActivatedAbilities = newState.grantedActivatedAbilities +
                                GrantedActivatedAbility(
                                    entityId = entityId,
                                    ability = ability,
                                    duration = Duration.Permanent
                                )
                        )
                    }
                }
            }
        }

        return EffectResult.success(newState)
    }
}
