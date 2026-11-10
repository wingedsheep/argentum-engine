package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PlayerActionPermissionsChangedEvent
import com.wingedsheep.engine.state.components.player.SpellCastRestriction
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import kotlin.reflect.KClass

/**
 * Executor for CantCastSpellsEffect.
 * Adds CantCastSpellsComponent to the target player, preventing them from casting spells.
 *
 * The ban is read at every later cast, long after this resolution's context is gone, so a
 * dynamic mana-value bound in its filter is locked in now (CR 608.2h): Living Breakthrough's
 * "spells with the same mana value as that spell" fixes the triggering spell's mana value when
 * the trigger resolves. A chosen card name is frozen the same way: Academic Probation's
 * "opponents can't cast spells with the chosen name" reads the name out of the pipeline now, since
 * a later cast check has no pipeline to read it from. A name referenced through an entity is frozen
 * too: Reflector Mage's "can't cast spells with the same name as that creature" reads the target's
 * name now, from the projection first (a Clone copying a creature, or a transformed double-faced
 * card, is named as it was on the battlefield — the card's ruling), since by the next cast check
 * the reference is gone. A nameless reference (a face-down creature) bans nothing. A [CantCastSpellsEffect.moreAllowed]
 * allowance is fixed the same way: the ban binds once the player's spell count this turn reaches
 * its count now plus the allowance (Irencrag Feat).
 */
class CantCastSpellsExecutor(
    private val amounts: DynamicAmountEvaluator
) : EffectExecutor<CantCastSpellsEffect> {

    override val effectType: KClass<CantCastSpellsEffect> = CantCastSpellsEffect::class

    override fun execute(
        state: GameState,
        effect: CantCastSpellsEffect,
        context: EffectContext
    ): EffectResult {
        val targetIds = context.resolvePlayerTargets(effect.target, state)
            .filter { state.turnOrder.contains(it) }
        if (targetIds.isEmpty()) {
            return EffectResult.error(state, "No valid target for can't cast spells effect")
        }

        val removeOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            is Duration.EndOfYourNextTurn -> PlayerEffectRemoval.EndOfYourNextTurn
            is Duration.UntilYourNextTurn -> PlayerEffectRemoval.UntilYourNextTurn
            is Duration.DuringAffectedPlayersNextTurn -> PlayerEffectRemoval.DuringYourNextTurn
            else -> PlayerEffectRemoval.EndOfTurn
        }
        val expiresAfterTurn = when (removeOn) {
            PlayerEffectRemoval.EndOfYourNextTurn, PlayerEffectRemoval.DuringYourNextTurn -> state.turnNumber + 1
            else -> null
        }

        val spellFilter = lockIn(effect.spellFilter, state, context)
        val newState = targetIds.fold(state) { acc, targetId ->
            acc.updateEntity(targetId) { container ->
                val existing = container.get<CantCastSpellsComponent>()?.restrictions.orEmpty()
                container.with(CantCastSpellsComponent(existing + SpellCastRestriction(
                    spellFilter = spellFilter,
                    removeOn = removeOn,
                    sourceId = context.sourceId,
                    controllerId = context.controllerId,
                    expiresAfterTurn = expiresAfterTurn,
                    activeFromSpellCount = effect.moreAllowed.takeIf { it > 0 }
                        ?.let { (acc.playerSpellsCastThisTurn[targetId] ?: 0) + it }
                )))
            }
        }

        return EffectResult.success(newState, targetIds.map(::PlayerActionPermissionsChangedEvent))
    }

    private fun lockIn(filter: GameObjectFilter, state: GameState, context: EffectContext): GameObjectFilter {
        val predicates = filter.cardPredicates.map { lockIn(it, state, context) }
        val anyOf = filter.anyOf.map { lockIn(it, state, context) }
        return if (predicates == filter.cardPredicates && anyOf == filter.anyOf) filter
        else filter.copy(cardPredicates = predicates, anyOf = anyOf)
    }

    private fun lockIn(predicate: CardPredicate, state: GameState, context: EffectContext): CardPredicate =
        when (predicate) {
            is CardPredicate.ManaValueEqualsDynamic ->
                CardPredicate.ManaValueEquals(amounts.evaluate(state, predicate.amount, context))
            is CardPredicate.ManaValueAtMostDynamic ->
                CardPredicate.ManaValueAtMost(amounts.evaluate(state, predicate.amount, context))
            is CardPredicate.NameEqualsChosen ->
                context.pipeline.chosenValues[predicate.variableName]?.let(CardPredicate::NameEquals) ?: predicate
            is CardPredicate.SharesNameWith -> lockedName(predicate.entity, state, context)
            is CardPredicate.And -> CardPredicate.And(predicate.predicates.map { lockIn(it, state, context) })
            is CardPredicate.Or -> CardPredicate.Or(predicate.predicates.map { lockIn(it, state, context) })
            is CardPredicate.Not -> CardPredicate.Not(lockIn(predicate.predicate, state, context))
            else -> predicate
        }

    /**
     * The referenced entity's name as of now, as a fixed [CardPredicate.NameEquals]. Projected
     * first so a copy or a renamed permanent is read under the name it has on the battlefield; base
     * card data second for a reference with no projection entry. A face-down or nameless reference
     * — or one that no longer resolves — has no name (CR 708.2), so the ban matches nothing
     * ([MATCHES_NOTHING]). Not `NameEquals("")`: a spell announced face down is checked under an
     * empty name, so that would ban every face-down cast (morph, disguise).
     */
    private fun lockedName(entity: EffectTarget.SingleEntity, state: GameState, context: EffectContext): CardPredicate {
        val projected = state.projectedState
        val referenceId = TargetResolutionUtils.resolveEntity(entity, context, state, projected)
        val name = referenceId?.takeUnless { projected.isFaceDown(it) }?.let { id ->
            projected.getName(id)?.takeIf { it.isNotBlank() }
                ?: state.getEntity(id)?.get<CardComponent>()?.name
        }
        return name?.takeIf { it.isNotBlank() }?.let(CardPredicate::NameEquals) ?: MATCHES_NOTHING
    }

    private companion object {
        /** An empty disjunction: true of no object, on every evaluation path. */
        val MATCHES_NOTHING: CardPredicate = CardPredicate.Or(emptyList())
    }
}
