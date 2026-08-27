package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.applyPreventByRemovingCounterToDamage
import com.wingedsheep.engine.core.applyShieldCounterToDamage
import com.wingedsheep.engine.core.DamagePreventedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LoyaltyChangedEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.battlefield.DealtDamageToThisGameComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtByPlayersThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.DamageSourceLki
import com.wingedsheep.engine.state.components.battlefield.DamagedBySourcesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.DamageUnpreventableThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.stack.SpellGrantedKeywordsComponent
import com.wingedsheep.engine.handlers.effects.damage.OptionalDamageRedirect
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.player.DamageSourceIdentity
import com.wingedsheep.engine.state.components.player.DamageSourcesThisTurnComponent
import com.wingedsheep.engine.state.components.player.RedNoncombatDamageDealtThisTurnComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.player.DamageBonusComponent
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.scripting.NoncombatDamageBonus
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CapDamage
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.HealOtherDamage
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.LifeLossFloor
import com.wingedsheep.sdk.scripting.ModifyLifeLoss
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.RedirectDamage
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.DamageCounterRecipient
import com.wingedsheep.sdk.scripting.ReplaceDamageWithCounters
import com.wingedsheep.sdk.scripting.ReplaceDamageWithMill
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Outcome of a chosen-source deflection/reflection shield matching an incoming damage instance.
 * [Prevented] short-circuits damage application with the given result (Deflecting Palm). [Reflected]
 * fires the linked reaction but lets the damage proceed (Eye for an Eye) — the caller merges
 * [state]/[events] and keeps applying the damage.
 */
sealed interface DeflectOutcome {
    data class Prevented(val result: EffectResult) : DeflectOutcome
    data class Reflected(
        val state: GameState,
        val events: List<com.wingedsheep.engine.core.GameEvent>
    ) : DeflectOutcome
}

/**
 * One [DoubleDamage] replacement that currently applies, as reported by
 * [DamageUtils.damageDoublersAffectingPlayer] for the client's player badges (damage dealt *to* a
 * player) and by [DamageUtils.damageDoublersAffectingSource] for the card badge on an
 * equipped/enchanted creature (damage dealt *by* that creature).
 */
data class ActiveDamageDoubler(
    /**
     * The battlefield permanent hosting the replacement (Twinflame Tyrant, Gratuitous Violence, or
     * the Equipment in the attachment-scoped case — Mjölnir, Hammer of Thor).
     */
    val sourceId: EntityId,
    val sourceName: String,
    /** Whether the doubling is scoped to combat or noncombat damage (CR 616 damage-type filter). */
    val damageType: DamageType,
)

/**
 * Utility functions for dealing damage, applying damage prevention/amplification/redirection,
 * tracking damage for triggers, and checking life gain prevention.
 */
object DamageUtils {

    private val predicateEvaluator = PredicateEvaluator()
    private val conditionEvaluator = ConditionEvaluator()
    private val dynamicAmountEvaluator = DynamicAmountEvaluator()
    lateinit var cardRegistry: CardRegistry

    /**
     * Controller of a battlefield permanent that hosts a replacement effect, honoring
     * control-changing effects (CR 613.1b, Layer 2). The "you" / "an opponent" filters on
     * damage and life-loss replacements are resolved relative to this controller, so they
     * must follow the *current* controller, not the printed one — a stolen Bloodletter of
     * Aclazotz or Ali from Cairo retargets to the thief. Falls back to the base
     * [ControllerComponent] only for entities with no projected entry.
     */
    private fun replacementHostController(state: GameState, entityId: EntityId): EntityId? =
        controllerOfObject(state, entityId)

    /**
     * Controller of the object *dealing* damage — the other half of the "a source you control"
     * question ([SourceFilter.YouControl]). A damage source is not always a battlefield permanent:
     * a burn spell resolving off the stack and an ability's source are damage sources too (CR
     * 609.7), and neither has a projected entry, which is why this falls back to the base
     * [ControllerComponent] the same way [replacementHostController] does.
     */
    private fun damageSourceController(
        state: GameState,
        entityId: EntityId,
        projected: ProjectedState = state.projectedState
    ): EntityId? = controllerOfObject(state, entityId, projected)

    /**
     * Projected controller of an object, falling back to its base [ControllerComponent] for
     * objects outside the battlefield (stack, exile) that have no projected entry, and then to the
     * *last-known* controller of one that has left the battlefield altogether.
     *
     * That last step is load-bearing on the source side. A permanent sacrificed to pay its own
     * ability's cost (Fanatical Firebrand: "{T}, Sacrifice this creature: It deals 1 damage to any
     * target") deals its damage from the graveyard, where `stripBattlefieldComponents` has already
     * taken its [ControllerComponent] away — so the projected and base lookups both come back
     * empty. CR 113.7a and CR 608.2h say the source's last known information answers "a source you
     * control" in exactly that case; without this fallback every [SourceFilter.YouControl]
     * replacement (Soul-Scar Mage, Twinflame Tyrant) silently declined for a self-sacrificing
     * source, while the same ability from a source that stayed on the battlefield (Prodigal
     * Sorcerer) worked.
     *
     * @param projected the projection to read control off — [GameState.projectedState] for ordinary
     *   callers, the in-flight one for a mid-projection caller.
     */
    private fun controllerOfObject(
        state: GameState,
        entityId: EntityId,
        projected: ProjectedState = state.projectedState
    ): EntityId? {
        val container = state.getEntity(entityId) ?: return null
        return projected.getController(entityId)
            ?: container.get<ControllerComponent>()?.playerId
            ?: container.get<LastKnownPermanentComponent>()?.snapshot?.controllerId
    }

    /**
     * Deal damage to a target (player or creature).
     *
     * @param state The current game state
     * @param targetId The entity to deal damage to
     * @param amount The amount of damage
     * @param sourceId The source of the damage
     * @param cantBePrevented If true, this damage cannot be prevented by prevention effects
     * @return The execution result with updated state and events
     */
    fun dealDamageToTarget(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?,
        cantBePrevented: Boolean = false,
        isCombatDamage: Boolean = false,
        appliedRedirects: Set<EntityId> = emptySet(),
        /**
         * When true and [targetId] is a creature, any damage in excess of what was needed to be
         * lethal (CR 120.4a) is dealt to that creature's controller instead (Gandalf's Sanction:
         * "Excess damage is dealt to that creature's controller instead.").
         */
        excessToController: Boolean = false
    ): EffectResult {
        if (amount <= 0) return EffectResult.success(state)

        // Check for global "damage can't be prevented" effects (Sunspine Lynx, Leyline of Punishment)
        @Suppress("NAME_SHADOWING")
        val cantBePrevented = cantBePrevented || isDamagePreventionDisabled(state, targetId)

        // Check for damage redirection (Glarecaster, Zealous Inquisitor). Whippoorwill's clause
        // shuts this half off too — "…or dealt instead to another permanent or player" — so a
        // recipient marked unpreventable is never redirected away from.
        val (redirectState, redirectTargetId, redirectAmount) =
            if (state.getEntity(targetId)?.has<DamageUnpreventableThisTurnComponent>() == true) {
                Triple(state, null, 0)
            } else {
                checkDamageRedirection(state, targetId, amount, sourceId = sourceId)
            }
        if (redirectTargetId != null) {
            val redirectResult = dealDamageToTarget(redirectState, redirectTargetId, redirectAmount, sourceId, cantBePrevented, isCombatDamage, appliedRedirects)
            val remainingDamage = amount - redirectAmount
            return if (remainingDamage > 0) {
                // Partial redirection — deal remaining damage to original target
                val afterRedirect = redirectResult.state
                val remainingResult = dealDamageToTarget(afterRedirect, targetId, remainingDamage, sourceId, cantBePrevented, isCombatDamage, appliedRedirects)
                EffectResult.success(remainingResult.state, redirectResult.events + remainingResult.events)
            } else {
                redirectResult
            }
        }

        // Check for static damage-redirection replacement effects (Harsh Judgment). Unlike the
        // floating shields above these are continuous statics; each source applies at most once
        // per damage event (CR 616.1), tracked via [appliedRedirects] to avoid redirect loops.
        if (!cantBePrevented && sourceId != null) {
            val (staticRedirectTo, staticRedirectSource) =
                findStaticDamageRedirect(state, targetId, amount, sourceId, isCombatDamage, appliedRedirects)
            if (staticRedirectTo != null && staticRedirectSource != null) {
                return dealDamageToTarget(
                    state, staticRedirectTo, amount, sourceId, cantBePrevented, isCombatDamage,
                    appliedRedirects + staticRedirectSource
                )
            }
        }

        // Protection from color/subtype: damage from sources of the stated quality is prevented (Rule 702.16)
        if (!cantBePrevented && sourceId != null) {
            // Check if all damage from this source is prevented (Chain of Silence)
            if (isAllDamageFromSourcePrevented(state, sourceId)) {
                return EffectResult.success(state)
            }

            val projected = state.projectedState
            val sourceColors = projected.getColors(sourceId)
            for (colorName in sourceColors) {
                if (projected.hasKeyword(targetId, "PROTECTION_FROM_$colorName")) {
                    // Damage is prevented — return success with no state change
                    return EffectResult.success(state)
                }
            }
            val sourceSubtypes = projected.getSubtypes(sourceId)
            for (subtype in sourceSubtypes) {
                if (projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                    return EffectResult.success(state)
                }
            }
            // Protection from card type, e.g. "protection from creatures" (Pippin, Guard of the Citadel)
            for (cardType in projected.getTypes(sourceId)) {
                if (projected.hasKeyword(targetId, "PROTECTION_FROM_CARDTYPE_${cardType.uppercase()}")) {
                    return EffectResult.success(state)
                }
            }

            // Protection from each opponent (Rule 702.16e)
            if (projected.hasKeyword(targetId, "PROTECTION_FROM_EACH_OPPONENT")) {
                val sourceController = projected.getController(sourceId)
                val targetController = projected.getController(targetId)
                    ?: state.getEntity(targetId)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
                if (sourceController != null && targetController != null && sourceController != targetController) {
                    return EffectResult.success(state)
                }
            }

            // Player-level protection, e.g. The One Ring's "protection from everything" (Rule 702.16).
            // Damage from a source matching one of the player's protection scopes is prevented.
            if (com.wingedsheep.engine.mechanics.targeting.PlayerProtectionRules
                    .isProtectedFromSource(state, targetId, sourceId, casterId = null)
            ) {
                return EffectResult.success(state)
            }
        }

        // Apply damage amplification (e.g., Gratuitous Violence - DoubleDamage). The combat flag has
        // to ride along: a doubler scoped to one damage type (The Rollercrusher Ride — noncombat
        // only) reads it to decide whether it applies.
        var effectiveAmount = applyStaticDamageAmplification(state, targetId, amount, sourceId, isCombatDamage)
        var newState = state

        // Check for damage-to-counters replacement (Force Bubble)
        // This replaces the damage entirely — it is neither dealt nor prevented.
        val isPlayer = newState.getEntity(targetId)?.get<LifeTotalComponent>() != null
        if (isPlayer) {
            val counterResult = applyReplaceDamageWithCounters(newState, targetId, effectiveAmount, sourceId, isCombatDamage)
            if (counterResult != null) return counterResult

            // Damage-to-an-opponent → prevent + each opponent mills that many (The Mindskinner).
            val millResult = applyReplaceDamageWithMill(newState, targetId, effectiveAmount, sourceId)
            if (millResult != null) return millResult
        } else {
            // Damage-to-a-creature self-replacement (Anti-Venom): "if damage would be dealt to
            // <this creature>, prevent it and put that many +1/+1 counters on him." Matches only a
            // Self-recipient replacement whose host is the damaged creature.
            val counterResult = applyReplaceDamageWithCounters(newState, targetId, effectiveAmount, sourceId, isCombatDamage)
            if (counterResult != null) return counterResult
        }

        // CR 122.1c: "If damage would be dealt to this permanent, prevent that damage and remove a
        // shield counter from it." One counter is consumed per damage event however many are on the
        // permanent, and however large the damage is.
        //
        // Deliberately outside the `cantBePrevented` guard below: per the official rulings, a
        // permanent with a shield counter that is dealt *unpreventable* damage takes that damage AND
        // still loses a shield counter. Only the prevention half is skipped.
        //
        // Placed after protection (CR 702.16) and the damage-replacement effects above: both are
        // prevention/replacement effects competing for the same event, and CR 616.1 lets the
        // affected permanent's controller order them. Letting protection win first leaves the shield
        // counter intact, which is the ordering that player would always choose.
        var shieldCounterEvents: List<EngineGameEvent> = emptyList()
        if (!isPlayer) {
            val shielded = applyShieldCounterToDamage(newState, targetId, cantBePrevented)
            if (shielded != null) {
                newState = shielded.state
                if (shielded.damagePrevented) return EffectResult.success(newState, listOf(shielded.event))
                shieldCounterEvents = listOf(shielded.event)
            }
        }

        // The printed sibling of the rule above: "If this creature would be dealt damage, prevent
        // that damage and remove a +1/+1 counter from it" (Unbreathing Horde). Same one-counter-per-
        // event scoping, same position in the CR 616.1 ordering, but it prevents even with no
        // counter left to spend.
        if (!isPlayer) {
            val counterShield = applyPreventByRemovingCounterToDamage(
                newState, targetId, isCombatDamage = isCombatDamage, cantBePrevented = cantBePrevented
            )
            if (counterShield != null) {
                newState = counterShield.state
                val counterEvents = listOfNotNull(counterShield.event)
                if (counterShield.damagePrevented) {
                    return EffectResult.success(newState, shieldCounterEvents + counterEvents)
                }
                shieldCounterEvents = shieldCounterEvents + counterEvents
            }
        }

        // Events from a reflect shield (Eye for an Eye) that fired but let the damage proceed.
        var reflectEvents: List<EngineGameEvent> = emptyList()
        if (!cantBePrevented) {
            // Check for deflection/reflection shields (Deflecting Palm, Eye for an Eye).
            if (sourceId != null) {
                when (val deflect = checkDeflectDamageShield(newState, targetId, effectiveAmount, sourceId)) {
                    is DeflectOutcome.Prevented -> return deflect.result
                    is DeflectOutcome.Reflected -> {
                        newState = deflect.state
                        reflectEvents = deflect.events
                    }
                    null -> {}
                }

                // Check for "prevent all damage from chosen source" shields (Samite Ministration)
                val preventFromSourceResult = checkPreventFromSourceShield(newState, targetId, effectiveAmount, sourceId)
                if (preventFromSourceResult != null) return preventFromSourceResult
            }

            val (shieldState, reducedAmount) = applyDamagePreventionShields(newState, targetId, effectiveAmount, sourceId = sourceId)
            newState = shieldState
            effectiveAmount = reducedAmount
        }
        if (effectiveAmount <= 0) return EffectResult.success(newState, shieldCounterEvents + reflectEvents)

        val events = mutableListOf<EngineGameEvent>()
        events.addAll(shieldCounterEvents)
        events.addAll(reflectEvents)
        // Excess damage (CR 120.4a) is only computed below for the non-wither creature and the
        // battle branch (above its defense) — planeswalker (above loyalty) and wither (damage
        // dealt as -1/-1 counters) paths are not yet modelled and stay at 0 here.
        var creatureExcessDamage = 0

        // Check if target is a player, planeswalker, or creature
        val lifeComponent = newState.getEntity(targetId)?.get<LifeTotalComponent>()
        val projected = newState.projectedState
        if (lifeComponent != null) {
            // CR 120.3a: damage to a player by a source without infect causes that player
            // to lose that much life, so life-loss replacements (Bloodletter of Aclazotz)
            // modify the life total reduction here. Lifelink and other damage-based effects
            // below still see the unmodified `effectiveAmount`, matching the official ruling.
            // CR 810.9 (Two-Headed Giant): damage happens to the player individually but the
            // result applies to the team's shared life total, so read/write through the resolver.
            val currentLife = newState.lifeTotal(targetId)
            var lifeLossAmount = applyStaticLifeLossModification(newState, targetId, effectiveAmount)
            lifeLossAmount = applyLifeLossFloors(newState, targetId, currentLife, lifeLossAmount)
            val newLife = currentLife - lifeLossAmount
            newState = newState.withLifeTotal(targetId, newLife)
            newState = trackDamageReceivedByPlayer(newState, targetId, effectiveAmount, sourceId)
            events.add(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.DAMAGE))
        } else if (projected.isPlaneswalker(targetId)) {
            // It's a planeswalker - remove loyalty counters equal to damage dealt
            if (sourceId != null) newState = markDealtDamageToThisGame(newState, sourceId, targetId)
            val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
            val currentLoyalty = counters.getCount(CounterType.LOYALTY)
            newState = newState.updateEntity(targetId) { container ->
                container.with(counters.withRemoved(CounterType.LOYALTY, effectiveAmount))
            }
            val targetName = newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Planeswalker"
            events.add(LoyaltyChangedEvent(targetId, targetName, -(effectiveAmount.coerceAtMost(currentLoyalty))))
        } else if (projected.isBattle(targetId)) {
            // CR 120.3h — damage dealt to a battle removes that many defense counters from it. The
            // battle isn't destroyed by the damage; state-based actions bin it once its defense
            // reaches 0 (CR 120.5 / 704.5v/w). Excess damage is the amount above its defense
            // (CR 120.4a / 120.10), computed before the counters come off.
            val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
            val currentDefense = counters.getCount(CounterType.DEFENSE)
            creatureExcessDamage = (effectiveAmount - currentDefense).coerceAtLeast(0)
            val removed = effectiveAmount.coerceAtMost(currentDefense)
            newState = newState.updateEntity(targetId) { container ->
                container.with(counters.withRemoved(CounterType.DEFENSE, effectiveAmount))
            }
            val targetName = newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Battle"
            if (removed > 0) {
                events.add(
                    com.wingedsheep.engine.core.CountersRemovedEvent(
                        targetId, CounterType.DEFENSE.name, removed, targetName,
                        remainingCount = currentDefense - removed
                    )
                )
            }
        } else {
            // Heal-on-damage replacement (Wolverine, Fierce Fighter): "instead that damage is
            // dealt, but all other damage already dealt to him is healed" (CR 701.69a). Applied
            // here — after every prevention/replacement above has had its say and the damage is
            // certain to be dealt — and *before* the marking below, so the new damage is the only
            // damage on the creature. It deliberately sits outside the wither branch: wither only
            // changes the form of the damage (CR 702.80a), the damage is still dealt, so the heal
            // still fires.
            newState = applyHealOtherDamage(newState, targetId, effectiveAmount, sourceId, isCombatDamage)

            // It's a creature - mark damage (or place -1/-1 counters if source has wither)
            val hasWither = sourceId != null && (
                projected.hasKeyword(sourceId, Keyword.WITHER) ||
                state.getEntity(sourceId)?.get<SpellGrantedKeywordsComponent>()?.keywords?.contains(Keyword.WITHER.name) == true
            )
            if (hasWither) {
                // Wither (CR 702.80): damage to creatures is dealt in the form of -1/-1 counters
                val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
                newState = newState.updateEntity(targetId) { container ->
                    container.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, effectiveAmount))
                }
                // CR 702.80 / 122.6a: the -1/-1 counters are put on the creature by the wither
                // source's controller, so "whenever you put counters" triggers see them as yours.
                events.add(CountersAddedEvent(targetId, CounterType.MINUS_ONE_MINUS_ONE.name, effectiveAmount,
                    newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Creature",
                    placedBy = newState.projectedState.getController(sourceId)))
                // Wither only changes the FORM of the damage (CR 702.80a); the creature was still
                // dealt damage by this source, so a deathtouch source still marks it for
                // destruction as an SBA (CR 702.2b / 704.5h) even though nothing is marked as
                // normal damage. Record the deathtouch flag without marking damage.
                if (sourceId != null && sourceHasDeathtouch(newState, projected, sourceId)) {
                    newState = newState.updateEntity(targetId) { container ->
                        val existing = container.get<DamageComponent>()
                        container.with(DamageComponent(
                            amount = existing?.amount ?: 0,
                            deathtouchDamageReceived = true
                        ))
                    }
                }
            } else {
                val existingDamage = newState.getEntity(targetId)?.get<DamageComponent>()
                val currentDamage = existingDamage?.amount ?: 0
                val hasDeathtouch = sourceId != null && sourceHasDeathtouch(newState, projected, sourceId)
                // Excess damage (CR 120.4a) — damage in excess of what was needed to be
                // lethal. With deathtouch, any amount of damage greater than 1 is excess —
                // lethal collapses to a flat 1 regardless of marked damage (CR 120.4a refs
                // 702.2).
                val toughness = projected.getToughness(targetId) ?: 0
                val lethalNeeded = if (hasDeathtouch) 1
                else (toughness - currentDamage).coerceAtLeast(0)
                creatureExcessDamage = (effectiveAmount - lethalNeeded).coerceAtLeast(0)
                // "Excess damage is dealt to that creature's controller instead" (Gandalf's
                // Sanction): the creature is marked only with the lethal portion; the excess is
                // dealt to its controller below.
                val markedOnCreature = if (excessToController) effectiveAmount - creatureExcessDamage
                else effectiveAmount
                newState = newState.updateEntity(targetId) { container ->
                    container.with(DamageComponent(
                        amount = currentDamage + markedOnCreature,
                        deathtouchDamageReceived = hasDeathtouch || (existingDamage?.deathtouchDamageReceived == true)
                    ))
                }
            }
            // Mark creature as having been dealt damage this turn
            newState = newState.updateEntity(targetId) { container ->
                container.with(WasDealtDamageThisTurnComponent)
            }
            // Track damage source for "creature dealt damage by this dies" triggers
            if (sourceId != null) {
                newState = trackDamageDealtToCreature(newState, sourceId, targetId)
                newState = trackDamageSourceLki(newState, sourceId, targetId)
                newState = applyDoomedRidersToDamagedCreature(newState, sourceId, targetId)
            }
            // Track per-player damage dealt to this entity this turn (Grothama LTB).
            if (sourceId != null) {
                newState = trackDamageDealtByPlayer(newState, sourceId, targetId, effectiveAmount)
            }
        }

        // Mark source as having dealt damage. Sits below every recipient branch (player /
        // planeswalker / battle / creature, wither included), so one stamp covers all noncombat damage
        // — the effect executors, fight, and the combat continuation resumer all reach here. The turn
        // stamp is what `StatePredicate.HasDealtDamage(thisTurnOnly = true)` reads.
        if (sourceId != null && sourceId in newState.getBattlefield()) {
            newState = newState.updateEntity(sourceId) { container ->
                container.with(HasDealtDamageComponent(newState.turnNumber))
            }
        }
        // Record the source on its controller's per-turn set of damage sources. Unlike the stamp
        // above this is not battlefield-only: a resolving burn spell is a source that dealt damage
        // just as much as a creature is (Case of the Burning Masks).
        if (sourceId != null && effectiveAmount > 0) {
            newState = trackDamageSourceForController(newState, sourceId)
        }

        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val targetContainer = newState.getEntity(targetId)
        val targetName = targetContainer?.get<CardComponent>()?.name
        val targetIsPlayer = targetContainer?.get<LifeTotalComponent>() != null
        val targetIsFaceDown = targetContainer?.has<FaceDownComponent>() == true
        // Capture the recipient's controller + creature-ness now, while it's still on the
        // battlefield, so recipient-based triggers match even if it dies to this damage (LKI).
        val targetControllerId = projected.getController(targetId)
        val targetWasCreature = projected.isCreature(targetId)
        // Capture the recipient creature's toughness as it last existed (CR 603.10) — read from the
        // ORIGINAL state's projection, before this damage marked the creature / SBAs could move it.
        // Read by "damage equal to that creature's toughness" triggers (Taii Wakeen).
        val targetToughnessAtDamage = if (targetWasCreature) projected.getToughness(targetId) else null
        val sourceTargetIds = sourceId
            ?.let(newState::getEntity)
            ?.get<TargetsComponent>()
            ?.targets
            ?.mapNotNull { chosenTarget ->
                when (chosenTarget) {
                    is ChosenTarget.Permanent -> chosenTarget.entityId
                    is ChosenTarget.Player -> chosenTarget.playerId
                    is ChosenTarget.Spell -> chosenTarget.spellEntityId
                    is ChosenTarget.Card -> chosenTarget.cardId
                }
            }
        events.add(
            DamageDealtEvent(
                sourceId,
                targetId,
                effectiveAmount,
                false,
                sourceName = sourceName,
                targetName = targetName,
                targetIsPlayer = targetIsPlayer,
                targetWasFaceDown = targetIsFaceDown,
                targetControllerId = targetControllerId,
                targetWasCreature = targetWasCreature,
                excessAmount = creatureExcessDamage,
                targetToughnessAtDamage = targetToughnessAtDamage,
                sourceTargetIdsAtDamage = sourceTargetIds,
            )
        )

        // Track noncombat damage dealt by red sources, keyed to the source's controller
        // (Temple of Power's transform gate — TurnTracker.RED_NONCOMBAT_DAMAGE_DEALT). A red
        // spell/ability carries no ControllerComponent, so fall back to its caster.
        if (sourceId != null && !isCombatDamage && effectiveAmount > 0) {
            // Combine projected colors (battlefield permanents, so a granted-red source counts) with
            // base card colors (spells on the stack, which have no projection).
            val sourceColors = projected.getColors(sourceId) +
                (state.getEntity(sourceId)?.get<CardComponent>()?.colors?.map { it.name } ?: emptyList())
            val sourceIsRed = Color.RED.name in sourceColors
            if (sourceIsRed) {
                val sourceControllerId = projected.getController(sourceId)
                    ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                    ?: state.getEntity(sourceId)?.get<SpellOnStackComponent>()?.casterId
                if (sourceControllerId != null) {
                    newState = newState.updateEntity(sourceControllerId) { container ->
                        val prior = container.get<RedNoncombatDamageDealtThisTurnComponent>()?.amount ?: 0
                        container.with(RedNoncombatDamageDealtThisTurnComponent(prior + effectiveAmount))
                    }
                }
            }
        }

        // Lifelink: if the source has lifelink, its controller gains life equal to the damage dealt
        // (CR 120.3f / 702.15b). The lifelink damage causes a life-gain event, so ModifyLifeGain
        // (Alhammarret's Archive, Leyline of Hope) replaces the actual amount gained.
        if (sourceId != null) {
            val projected = newState.projectedState
            if (projected.hasKeyword(sourceId, Keyword.LIFELINK.name) ||
                sourceHasGrantedDamageKeyword(newState, sourceId, Keyword.LIFELINK)
            ) {
                val controllerId = projected.getController(sourceId)
                    ?: newState.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                    // A spell source carries no ControllerComponent; its controller is the caster.
                    ?: newState.getEntity(sourceId)?.get<SpellOnStackComponent>()?.casterId
                if (controllerId != null) {
                    val (gainedState, gainEvent) = gainLife(newState, controllerId, effectiveAmount)
                    newState = gainedState
                    if (gainEvent != null) events.add(gainEvent)
                }
            }
        }

        // "Excess damage is dealt to that creature's controller instead" (CR-style redirect for
        // Gandalf's Sanction). Deal the computed excess to the creature's controller, attributed
        // to the same source. excessToController is not propagated to this player-damage call.
        if (excessToController && targetWasCreature && creatureExcessDamage > 0 && targetControllerId != null) {
            val excessResult = dealDamageToTarget(
                newState, targetControllerId, creatureExcessDamage, sourceId,
                cantBePrevented = cantBePrevented, isCombatDamage = isCombatDamage,
                appliedRedirects = appliedRedirects, excessToController = false
            )
            newState = excessResult.state
            events.addAll(excessResult.events)
        }

        return EffectResult.success(newState, events)
    }

    /**
     * Track that [playerId] received [amount] damage this turn.
     * Updates the DamageReceivedThisTurnComponent on the player entity.
     * Also marks the player as having lost life this turn (LifeLostThisTurnComponent) and
     * accumulates [amount] onto LifeLostAmountThisTurnComponent — damage dealt to a player causes
     * that much life loss (CR 120.3c), so it counts toward "the amount of life you lost this turn"
     * (Rowan, Scion of War).
     * Used for Final Punishment: "Target player loses life equal to the damage
     * already dealt to that player this turn."
     */
    fun trackDamageReceivedByPlayer(state: GameState, playerId: EntityId, amount: Int, sourceId: EntityId? = null): GameState {
        if (amount <= 0) return state
        // Every path that deals damage to a player — combat and noncombat alike — funnels through
        // here, so it is the one place to record the source's "has dealt damage to this player this
        // game" memory (The Fallen). Planeswalkers are recorded separately, at their own two
        // loyalty-removal sites.
        val marked = sourceId?.let { markDealtDamageToThisGame(state, it, playerId) } ?: state
        // Is the source an artifact at the moment it dealt the damage? For a source still on the
        // battlefield, its projected types are authoritative (a continuous effect may have added or
        // stripped artifact-ness). Only for a source that has already left do we fall back to its
        // base card types as last-known information. Powers the artifact-source damage accumulator
        // (Reverse Polarity).
        val sourceIsArtifact = sourceId?.let { sid ->
            if (sid in marked.getBattlefield()) {
                marked.projectedState.hasType(sid, "ARTIFACT")
            } else {
                marked.getEntity(sid)?.get<CardComponent>()?.typeLine?.isArtifact == true
            }
        } ?: false
        return marked.updateEntity(playerId) { container ->
            val existing = container.get<com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent>()
                ?: com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent()
            val existingLifeLost = container.get<com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent>()
                ?: com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent()
            var updated = container.with(com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent(existing.amount + amount))
                .with(com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent)
                .with(
                    com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent(
                        existingLifeLost.amount + amount
                    )
                )
            if (sourceIsArtifact) {
                val existingArtifact = container.get<com.wingedsheep.engine.state.components.player.DamageReceivedFromArtifactsThisTurnComponent>()
                    ?: com.wingedsheep.engine.state.components.player.DamageReceivedFromArtifactsThisTurnComponent()
                updated = updated.with(
                    com.wingedsheep.engine.state.components.player.DamageReceivedFromArtifactsThisTurnComponent(existingArtifact.amount + amount)
                )
            }
            updated
        }
    }

    /**
     * Mark that [playerId] gained life this turn.
     * Sets the LifeGainedThisTurnComponent (existence flag) and accumulates the amount on
     * the LifeGainedAmountThisTurnComponent. The amount is used by
     * `DynamicAmount.TurnTracking(player, TurnTracker.LIFE_GAINED)`.
     * Used for conditions like "if you gained life this turn" (Lunar Convocation) and for
     * "amount of life you gained this turn" comparisons (Bre of Clan Stoutarm).
     */
    fun markLifeGainedThisTurn(state: GameState, playerId: EntityId, amount: Int = 0): GameState {
        if (amount < 0) return state
        return state.updateEntity(playerId) { container ->
            val existing = container.get<com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent>()
                ?: com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent()
            container
                .with(com.wingedsheep.engine.state.components.player.LifeGainedThisTurnComponent)
                .with(com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent(existing.amount + amount))
        }
    }

    /**
     * Mark that [playerId] lost life this turn (non-damage life loss, e.g., from LoseLife effects or payments).
     * Sets the LifeLostThisTurnComponent (existence flag) and accumulates [amount] onto the
     * LifeLostAmountThisTurnComponent. The amount is used by
     * `DynamicAmount.TurnTracking(player, TurnTracker.LIFE_LOST_AMOUNT)`.
     * Used for conditions like "if an opponent lost life this turn" (Hired Claw) and for
     * "amount of life you lost this turn" comparisons (Rowan, Scion of War).
     *
     * [amount] defaults to 0 so callers that only need the boolean marker stay correct; every
     * quantitative path passes the life actually lost.
     */
    fun markLifeLostThisTurn(state: GameState, playerId: EntityId, amount: Int = 0): GameState {
        return state.updateEntity(playerId) { container ->
            val existing = container.get<com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent>()
                ?: com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent()
            container
                .with(com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent)
                .with(
                    com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent(
                        existing.amount + amount.coerceAtLeast(0)
                    )
                )
        }
    }

    /**
     * The single shared primitive behind every non-damage life reduction: reduce [playerId]'s
     * life total by [amount], mark them as having lost life this turn, and emit one
     * [LifeChangedEvent] tagged with [reason]. Used by the LoseLife *effect* and by every
     * pay-life *cost* path (`AbilityCost.PayLife`/`PayXLife`, `PayCost.PayLife`).
     *
     * Cost vs. effect deliberately stay separate call sites — a cost is checked, atomic, and
     * can't be responded to, while an effect resolves off the stack — but the actual
     * life-deduction mutation they both perform is identical and lives here so it isn't
     * duplicated. The differences are carried by parameters:
     * - [reason] distinguishes `LIFE_LOSS` (effect) from `PAYMENT` (cost) on the emitted event.
     * - [applyLifeLossModification] runs the amount through [applyStaticLifeLossModification]
     *   first (CR 119.3 life-loss replacements such as Bloodletter of Aclazotz). True for the
     *   LoseLife effect; false for paying life as a cost (a cost payment is not a life-loss
     *   event those replacements modify).
     *
     * (Damage that reduces life routes through [dealDamageToTarget] instead — it has its own
     * prevention/redirection pipeline — though that path applies the same life-loss
     * modification step.)
     *
     * Returns the updated state paired with the emitted event, or `state to null` when
     * [playerId] has no life total (no mutation performed) so cost callers can surface a
     * payment failure.
     */
    fun loseLife(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        reason: LifeChangeReason,
        applyLifeLossModification: Boolean = false,
    ): Pair<GameState, LifeChangedEvent?> {
        // Presence guard stays per-player (every player carries a LifeTotalComponent); the value,
        // however, is the team's shared total (CR 810.9a) — read/write via the resolver.
        if (state.getEntity(playerId)?.get<LifeTotalComponent>() == null) return state to null
        val currentLife = state.lifeTotal(playerId)
        val lossAmount = if (applyLifeLossModification) {
            applyStaticLifeLossModification(state, playerId, amount)
        } else {
            amount
        }
        val newLife = currentLife - lossAmount
        var newState = state.withLifeTotal(playerId, newLife)
        newState = markLifeLostThisTurn(newState, playerId, lossAmount)
        return newState to LifeChangedEvent(playerId, currentLife, newLife, reason)
    }

    /**
     * A damage keyword can be granted onto a spell *object on the stack*, not just printed on or
     * projected onto a battlefield source. Static keyword projection
     * ([StateProjector]/[AffectsFilterResolver]) only reaches battlefield permanents, so a spell
     * that "has lifelink" or "has deathtouch" via a grant is invisible to `projected.hasKeyword`.
     * This mirrors the wither treatment above by consulting the two spell-grant channels:
     *  - [SpellGrantedKeywordsComponent] — a one-shot keyword stamped onto this specific spell
     *    (e.g. Judith, Carnage Connoisseur's "that spell gains deathtouch and lifelink", or a copy
     *    that "gains lifelink").
     *  - [GrantKeywordToOwnSpells] — a continuous "<type> spells you control have <keyword>" static
     *    on a permanent the spell's current controller controls (e.g. Lo and Li, Twin Tutors →
     *    Lesson spells). Resolved live via [GrantedKeywordResolver].
     *
     * Only spells on the stack qualify (gated on [SpellOnStackComponent]); battlefield sources are
     * already covered by projected keywords.
     *
     * [keyword] is a parameter rather than a hard-coded LIFELINK because both damage keywords that
     * read off the *source* — lifelink (CR 702.15b) and deathtouch (CR 702.2b) — reach a spell by
     * exactly these two channels; only the read site differs.
     */
    /**
     * Does [sourceId] have deathtouch for the purpose of the damage it is dealing right now?
     *
     * Projected keywords cover every battlefield source; a *spell* on the stack is not projected,
     * so a spell that gained deathtouch (Judith, Carnage Connoisseur) has to be read off the
     * spell-grant channels instead — the same split the lifelink and wither checks already make.
     */
    private fun sourceHasDeathtouch(
        state: GameState,
        projected: ProjectedState,
        sourceId: EntityId
    ): Boolean =
        projected.hasKeyword(sourceId, Keyword.DEATHTOUCH) ||
            sourceHasGrantedDamageKeyword(state, sourceId, Keyword.DEATHTOUCH)

    private fun sourceHasGrantedDamageKeyword(
        state: GameState,
        sourceId: EntityId,
        keyword: Keyword
    ): Boolean {
        val container = state.getEntity(sourceId) ?: return false
        if (container.get<SpellGrantedKeywordsComponent>()?.keywords?.contains(keyword.name) == true) {
            return true
        }
        // A resolving spell is popped from `state.stack` *before* its effects run, but keeps its
        // [SpellOnStackComponent] until it leaves the stack zone — so gate on the component, not
        // stack membership.
        val spellOnStack = container.get<SpellOnStackComponent>() ?: return false
        val controllerId = container.get<ControllerComponent>()?.playerId ?: spellOnStack.casterId
        val cardDef = container.get<CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: return false
        return GrantedKeywordResolver(cardRegistry).hasKeyword(state, controllerId, cardDef, keyword)
    }

    /**
     * The single shared primitive behind every additive life *gain*: increase [playerId]'s
     * life total by [amount], mark them as having gained life this turn, and emit one
     * [LifeChangedEvent] tagged `LIFE_GAIN`. The mirror of [loseLife] — used by the GainLife
     * and OwnerGainsLife effects, both lifelink paths (noncombat [dealDamageToTarget] and
     * [com.wingedsheep.engine.mechanics.combat.CombatDamageManager]), and the life-gaining
     * damage shield ([checkPreventFromSourceShield]).
     *
     * Unlike life loss, life *gain* can be both prevented and modified before it happens:
     * - [isLifeGainPrevented] is checked first; a prevented gain performs no mutation
     *   (CR 119.5 effects like Sulfuric Vortex / Erebos).
     * - When [applyLifeGainModification] is true the amount is run through
     *   [LifeGainModifiers] (CR 614 ModifyLifeGain replacements — Alhammarret's Archive,
     *   Leyline of Hope — applied once per life-gain event). True for normal "gain N life";
     *   false for the few sites that gain a fixed, already-determined amount the replacement
     *   pipeline must not touch (the prevent-from-source shield's color-matched gain).
     *
     * Returns the updated state paired with the emitted event, or `state to null` when the
     * gain is prevented, modified to ≤ 0, or [playerId] has no life total (no mutation
     * performed) so callers can skip the no-op cleanly.
     */
    fun gainLife(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        applyLifeGainModification: Boolean = true,
    ): Pair<GameState, LifeChangedEvent?> {
        if (isLifeGainPrevented(state, playerId)) return state to null
        val gainAmount = if (applyLifeGainModification) {
            LifeGainModifiers.apply(state, playerId, amount)
        } else {
            amount
        }
        if (gainAmount <= 0) return state to null
        // Presence guard stays per-player; the value is the team's shared total (CR 810.9a).
        if (state.getEntity(playerId)?.get<LifeTotalComponent>() == null) return state to null
        val currentLife = state.lifeTotal(playerId)
        val newLife = currentLife + gainAmount
        // First life gain this turn for this player? Read before markLifeGainedThisTurn sets the
        // flag, so "whenever you gain life for the first time each turn" (Leech Collector) fires
        // only on the turn's first gaining event.
        val firstThisTurn = state.getEntity(playerId)
            ?.has<com.wingedsheep.engine.state.components.player.LifeGainedThisTurnComponent>() != true
        var newState = state.withLifeTotal(playerId, newLife)
        newState = markLifeGainedThisTurn(newState, playerId, gainAmount)
        return newState to LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN, firstThisTurn)
    }

    /**
     * Whether [targetId] has not yet had counters put on it this turn — i.e. the placement about
     * to happen is the first this turn. Read *before* [markCounterPlacedOnCreature] sets the
     * marker, to stamp [com.wingedsheep.engine.core.CountersAddedEvent.firstThisTurn] for
     * "the first time counters have been put on that creature this turn" triggers (Stalwart
     * Successor). Only meaningful for creatures; non-creature targets return false.
     */
    fun isFirstCounterThisTurn(state: GameState, targetId: EntityId): Boolean {
        if (!state.projectedState.isCreature(targetId)) return false
        return state.getEntity(targetId)
            ?.has<com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent>() != true
    }

    /**
     * Mark that [placerId] put one or more counters on the creature [targetId] this turn.
     * Only marks when [targetId] is a creature in the projected state.
     * Sets the PutCounterOnCreatureThisTurnComponent on the placing player's entity (for
     * "if you put a counter on a creature this turn", Lasting Tarfire) and the per-creature
     * [com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent]
     * marker (for "first time counters this turn" triggers, Stalwart Successor).
     *
     * [counterType] is required: it is recorded on the marker so type-scoped counter-history
     * conditions ("you've put one or more +1/+1 counters on ~ this turn" — Beast, Erudite
     * Aerialist; Kid Loki's filter) can tell kinds apart. Paths that place several kinds at once
     * (doubling, moving every kind) call this once per kind rather than once with no kind, because
     * a kind-less mark cannot satisfy a type-scoped filter. The placer is
     * compared against the target's *projected* controller at placement time so the "you've put"
     * axis is recorded too — both facts have to be captured now, since the counters themselves may
     * be gone by the time the condition is read.
     */
    fun markCounterPlacedOnCreature(
        state: GameState,
        placerId: EntityId,
        targetId: EntityId,
        counterType: String
    ): GameState {
        if (!state.projectedState.isCreature(targetId)) return state
        val byController = state.projectedState.getController(targetId) == placerId
        return state
            .updateEntity(placerId) { container ->
                container.with(com.wingedsheep.engine.state.components.player.PutCounterOnCreatureThisTurnComponent)
            }
            .updateEntity(targetId) { container ->
                val existing = container
                    .get<com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent>()
                    ?: com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent()
                container.with(existing.with(counterType, byController))
            }
    }

    /**
     * Stamp the per-permanent [com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent]
     * marker on [targetId] and report whether this is the first counter placement on it this turn
     * (read *before* the marker is set), so the caller can populate
     * [com.wingedsheep.engine.core.CountersAddedEvent.firstThisTurn] for "first time counters this
     * turn" intervening-if triggers (Stalwart Successor).
     *
     * Unlike [isFirstCounterThisTurn] + [markCounterPlacedOnCreature], this performs no
     * projected creature/zone check, so it also covers the enters-with-counters path where the
     * permanent is not yet on the battlefield (CR 122.6a: an object entering with counters has
     * those counters put on it, by its controller unless the effect names another player).
     * Stamping the marker on a non-creature is harmless: the trigger's own "creature you control"
     * filter re-checks type.
     *
     * [counterType] and the placed-by-controller flag are the same two axes
     * [markCounterPlacedOnCreature] records, and every caller should supply them so the type- and
     * placer-scoped readings of `StatePredicate.ReceivedCounterThisTurn` (Kid Loki, Beast, Erudite
     * Aerialist) see this placement. There are two ways to give the placer, and which one a caller
     * uses is decided by whether the target is on the battlefield yet:
     *
     *  - **[placerId]** — the normal, on-battlefield case (explore, moved counters, distributed
     *    counters, wither damage). The flag is derived here by comparing the placer against the
     *    target's *projected* controller, exactly as [markCounterPlacedOnCreature] does, so a call
     *    site can never let a hand-computed boolean drift out of sync with the id it already holds.
     *  - **[byController]** — the enters-with-counters case, where the object is not on the
     *    battlefield yet and so has no projected controller to compare against. CR 122.6a fixes the
     *    answer to `true` there regardless of whose effect caused the counters.
     *
     * [counterType] is required for that reason: a kind-less mark would satisfy neither the
     * type-scoped reading nor the widest one, both of which answer from the recorded kind sets.
     * A caller placing several kinds at once calls this once per kind.
     *
     * This deliberately does *not* set the placer's [com.wingedsheep.engine.state.components.player.PutCounterOnCreatureThisTurnComponent]
     * ("if you put a counter on a creature this turn", Lasting Tarfire), which stays exclusive to
     * [markCounterPlacedOnCreature]. The reason is the enters-with case this function exists for:
     * the object is not on the battlefield when the counters are placed, so the projected
     * creature check that marker depends on cannot be made, and stamping it unconditionally would
     * credit a player for putting a counter on an artifact or an enchantment. Widening it for the
     * on-battlefield callers alone is a real gap (explore does put a counter on a creature), but no
     * card demands it yet and it is a behaviour change to a shared player-scoped tracker rather
     * than a per-permanent one — worth its own change, not a rider on this one.
     */
    fun recordCounterPlacement(
        state: GameState,
        targetId: EntityId,
        counterType: String,
        placerId: EntityId? = null,
        byController: Boolean = false
    ): Pair<GameState, Boolean> {
        val placedByController = byController ||
            (placerId != null && state.projectedState.getController(targetId) == placerId)
        val first = state.getEntity(targetId)
            ?.has<com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent>() != true
        val newState = state.updateEntity(targetId) { container ->
            val existing = container
                .get<com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent>()
                ?: com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent()
            container.with(existing.with(counterType, placedByController))
        }
        return newState to first
    }

    /**
     * Track that [sourceId] dealt damage to [targetCreatureId] this turn.
     * Updates the DamageDealtToCreaturesThisTurnComponent on the source entity.
     * Used for triggers like Soul Collector's "whenever a creature dealt damage by this creature this turn dies".
     */
    fun trackDamageDealtToCreature(state: GameState, sourceId: EntityId, targetCreatureId: EntityId): GameState {
        // Only track if source is still on the battlefield
        if (sourceId !in state.getBattlefield()) return state
        return state.updateEntity(sourceId) { container ->
            val existing = container.get<DamageDealtToCreaturesThisTurnComponent>()
                ?: DamageDealtToCreaturesThisTurnComponent()
            container.with(existing.withCreature(targetCreatureId))
        }
    }

    /**
     * Record [sourceId] as a source that dealt damage this turn, on the player who controls it
     * **right now** — i.e. at the moment the damage is dealt, which is the only moment Case of the
     * Burning Masks's "sources you controlled dealt damage this turn" cares about.
     *
     * The identity stored is (entity, battlefield-entry timestamp), so a permanent that deals
     * damage repeatedly counts once while one that left the battlefield and returned counts as the
     * new object it is (CR 400.7). A source that isn't a permanent — a spell resolving off the
     * stack — carries no timestamp and is identified by its entity alone.
     */
    /**
     * Record on [sourceId] that it has dealt damage to [recipientId] at some point this game — the
     * accumulating memory behind The Fallen. [recipientId] is a player or a planeswalker; the
     * consumer decides which of them the printed line cares about.
     *
     * A no-op when the source entity is gone (a spell that has already left the stack). The set is
     * never cleared per turn and is stripped on a zone change with the rest of the damage memory,
     * so a permanent that leaves and returns starts over (CR 400.7).
     */
    fun markDealtDamageToThisGame(state: GameState, sourceId: EntityId, recipientId: EntityId): GameState {
        val container = state.getEntity(sourceId) ?: return state
        val existing = container.get<DealtDamageToThisGameComponent>()
        if (existing != null && recipientId in existing.recipientIds) return state
        return state.updateEntity(sourceId) { c ->
            val current = c.get<DealtDamageToThisGameComponent>() ?: DealtDamageToThisGameComponent()
            c.with(DealtDamageToThisGameComponent(current.recipientIds + recipientId))
        }
    }

    fun trackDamageSourceForController(state: GameState, sourceId: EntityId): GameState {
        val container = state.getEntity(sourceId) ?: return state
        val controllerId = state.projectedState.getController(sourceId)
            ?: container.get<ControllerComponent>()?.playerId
            ?: container.get<SpellOnStackComponent>()?.casterId
            ?: container.get<CardComponent>()?.ownerId
            ?: return state
        val identity = DamageSourceIdentity(
            entityId = sourceId,
            incarnation = container
                .get<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()
                ?.timestamp
                ?: 0L
        )
        val existing = state.getEntity(controllerId)?.get<DamageSourcesThisTurnComponent>()
        if (existing != null && identity in existing.sources) return state
        return state.updateEntity(controllerId) { playerContainer ->
            val current = playerContainer.get<DamageSourcesThisTurnComponent>()
                ?: DamageSourcesThisTurnComponent()
            playerContainer.with(current.adding(identity))
        }
    }

    /**
     * Record a last-known snapshot of [sourceId] (its controller and creature-subtypes as projected
     * right now) on [targetCreatureId]'s [DamagedBySourcesThisTurnComponent]. Read at death time by
     * observer triggers of the form "whenever another creature dealt damage this turn by [a source
     * matching a filter] dies" (Shelob). Snapshotting on the *damaged* creature means a source that
     * died in the same combat is still evaluated against its damage-time state (CR 608.2h).
     */
    fun trackDamageSourceLki(state: GameState, sourceId: EntityId, targetCreatureId: EntityId): GameState {
        if (targetCreatureId !in state.getBattlefield()) return state
        val projected = state.projectedState
        val controllerId = projected.getController(sourceId)
            ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
            ?: state.getEntity(sourceId)?.get<CardComponent>()?.ownerId
            ?: return state
        val subtypes = projected.getSubtypes(sourceId)
            .map { com.wingedsheep.sdk.core.Subtype(it) }
            .toSet()
        val snapshot = DamageSourceLki(
            sourceControllerId = controllerId,
            sourceSubtypes = subtypes,
            sourceWasCreature = projected.isCreature(sourceId),
        )
        return state.updateEntity(targetCreatureId) { container ->
            val existing = container.get<DamagedBySourcesThisTurnComponent>()
                ?: DamagedBySourcesThisTurnComponent()
            container.with(existing.adding(snapshot))
        }
    }

    /**
     * Apply the damage-time riders of [com.wingedsheep.sdk.scripting.CreaturesDamagedBySourceAreDoomed]
     * carried by [sourceId] to the creature it just damaged — "can't be regenerated" and "if it
     * would die this turn, exile it instead" (Runesword).
     *
     * This runs *inside* damage application rather than from a trigger because a trigger for
     * "whenever this deals damage to a creature" resolves only after state-based actions have
     * already binned the dying creature (CR 704.3), far too late to change where it went. The marks
     * themselves are the same floating effects `MarkExileOnDeathExecutor` and
     * `CantBeRegeneratedExecutor` place.
     *
     * Reads **granted** statics only ([GameState.grantedStaticAbilities]), the point-of-use shape
     * `CombatDamageUtils` documents. No card prints this ability — Runesword grants it for a turn —
     * and reading printed ones here would need a `CardRegistry` this helper doesn't have. Give this
     * reader a registry when a card prints it.
     */
    fun applyDoomedRidersToDamagedCreature(
        state: GameState,
        sourceId: EntityId,
        targetCreatureId: EntityId
    ): GameState {
        if (targetCreatureId !in state.getBattlefield()) return state
        val riders = state.grantedStaticAbilities
            .filter { it.entityId == sourceId }
            .mapNotNull { it.ability as? com.wingedsheep.sdk.scripting.CreaturesDamagedBySourceAreDoomed }
        if (riders.isEmpty()) return state

        val controllerId = state.projectedState.getController(sourceId)
            ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
            ?: state.getEntity(sourceId)?.get<CardComponent>()?.ownerId
            ?: return state
        val context = EffectContext(sourceId = sourceId, controllerId = controllerId)

        var newState = state
        if (riders.any { it.cantBeRegenerated }) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.CantBeRegenerated,
                affectedEntities = setOf(targetCreatureId),
                duration = Duration.EndOfTurn,
                context = context
            )
        }
        if (riders.any { it.exileInsteadOfDying }) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.ExileOnDeath,
                affectedEntities = setOf(targetCreatureId),
                duration = Duration.EndOfTurn,
                context = context
            )
        }
        return newState
    }

    /**
     * Track that [amount] damage was dealt to [targetId] this turn by a source controlled
     * by the controller of [sourceId]. Read at LTB time for "each player draws cards equal
     * to the damage dealt to ~ this turn by sources they controlled" (Grothama).
     */
    fun trackDamageDealtByPlayer(
        state: GameState,
        sourceId: EntityId,
        targetId: EntityId,
        amount: Int,
    ): GameState {
        if (amount <= 0) return state
        if (targetId !in state.getBattlefield()) return state
        val controllerId = state.projectedState.getController(sourceId)
            ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
            ?: state.getEntity(sourceId)?.get<CardComponent>()?.ownerId
            ?: return state
        return state.updateEntity(targetId) { container ->
            val existing = container.get<DamageDealtByPlayersThisTurnComponent>()
                ?: DamageDealtByPlayersThisTurnComponent()
            container.with(existing.adding(controllerId, amount))
        }
    }

    /**
     * Check if life gain is prevented for a player by any PreventLifeGain replacement effect
     * on the battlefield (e.g., Sulfuric Vortex, Erebos).
     */
    fun isLifeGainPrevented(state: GameState, playerId: EntityId): Boolean {
        // Durable, source-independent lock conferred directly on the player
        // (LockLifeGainEffect — Screaming Nemesis). CR 810.9g: where the life total is the team's,
        // "that player can't gain life" means no player on their team can — a lock on either head
        // closes the shared pool. Outside a shared-life format the team is the player alone.
        val lockedSeats = if (state.format.sharesTeamLife) state.teamOf(playerId) else listOf(playerId)
        if (lockedSeats.any { seat ->
                state.getEntity(seat)
                    ?.has<com.wingedsheep.engine.state.components.player.CantGainLifeComponent>() == true
            }
        ) {
            return true
        }
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is PreventLifeGain) continue

                val lifeGainEvent = effect.appliesTo
                if (lifeGainEvent !is com.wingedsheep.sdk.scripting.EventPattern.LifeGainEvent) continue

                val sourceControllerId = replacementHostController(state, entityId)
                when (lifeGainEvent.player) {
                    Player.Each, Player.Any -> return true
                    Player.You -> if (playerId == sourceControllerId) return true
                    // "Your opponents can't gain life." — Gríma Wormtongue (LTR). A real opponent
                    // test: the controller's teammate is not their opponent (CR 102.3 / 810.9g).
                    Player.EachOpponent ->
                        if (sourceControllerId != null && state.isOpponentOf(playerId, sourceControllerId)) return true
                    // "Enchanted player can't gain life." — Grievous Wound. The host is an Aura
                    // attached to the locked player; compare against its attachment target.
                    Player.EnchantedPlayer -> {
                        val enchanted = container
                            .get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                            ?.targetId
                        if (enchanted == playerId) return true
                    }
                    else -> {}
                }
            }
        }
        return false
    }

    /**
     * Check if damage prevention is globally disabled by any DamageCantBePrevented replacement effect
     * on the battlefield (e.g., Sunspine Lynx, Leyline of Punishment).
     */
    fun isDamagePreventionDisabled(state: GameState, recipientId: EntityId? = null): Boolean {
        // Turn-scoped "Damage can't be prevented this turn" (Fear, Fire, Foes!).
        if (state.damageCantBePreventedThisTurn) return true
        // Per-recipient: "damage that would be dealt to that creature this turn can't be prevented"
        // (Whippoorwill). Callers that know who is being damaged pass it; those that don't fall back
        // to the global-only answer they gave before.
        if (recipientId != null &&
            state.getEntity(recipientId)?.has<DamageUnpreventableThisTurnComponent>() == true
        ) {
            return true
        }
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect is DamageCantBePrevented) return true
            }
        }
        return false
    }

    /**
     * Apply damage prevention shields to reduce incoming damage.
     *
     * Finds all PreventNextDamage floating effects targeting the entity,
     * consumes shield amounts, and returns the updated state and remaining damage.
     *
     * @param state The current game state
     * @param targetId The entity receiving damage
     * @param amount The incoming damage amount
     * @return Pair of (updated state with consumed shields, remaining damage after prevention)
     */
    fun applyDamagePreventionShields(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        isCombatDamage: Boolean = false,
        sourceId: EntityId? = null
    ): Pair<GameState, Int> {
        // CR 615.12 — when damage can't be prevented, prevention shields aren't reduced and prevent
        // nothing. When any battlefield "damage can't be prevented" (Spider-Punk) or the "this turn"
        // one-shot (Fear, Fire, Foes!) is active, no shield applies and the damage passes through in full.
        if (isDamagePreventionDisabled(state, targetId)) return state to amount

        var remainingDamage = amount
        val updatedEffects = state.floatingEffects.toMutableList()
        val toRemove = mutableListOf<Int>()

        if (updatedEffects.any {
                val mod = it.effect.modification
                mod is SerializableModification.PreventAllDamageTo &&
                    (!mod.combatOnly || isCombatDamage) &&
                    targetId in it.effect.affectedEntities
            }) {
            return state to 0
        }

        // Recipient-group shields ("prevent all damage that would be dealt to creatures you control
        // this turn"): the filter is re-evaluated now against projected state, with the shield's
        // controller as the "you" reference, so permanents that came under control later this turn
        // are protected too. Honours the combat-only variant, the "you and …" player recipient
        // (a player never matches a permanent filter, so it is checked separately — and a null
        // filter is the "to you" shield that names no permanent at all), and an optional source
        // filter ("… by creatures") evaluated against the damage source the same way.
        val groupShieldEvaluator = PredicateEvaluator()
        if (updatedEffects.any { fe ->
                val mod = fe.effect.modification
                if (mod !is SerializableModification.PreventAllDamageToGroup) return@any false
                if (mod.combatOnly && !isCombatDamage) return@any false
                val predicateContext = PredicateContext(controllerId = fe.controllerId)
                val recipientFilter = mod.filter
                val recipientMatches = (mod.includesController && targetId == fe.controllerId) ||
                    (
                        recipientFilter != null && groupShieldEvaluator.matches(
                            state,
                            state.projectedState,
                            targetId,
                            recipientFilter,
                            predicateContext
                        )
                        )
                if (!recipientMatches) return@any false
                // Fail closed on an unidentifiable source: a "by creatures" shield must not swallow
                // damage it can't attribute to a creature.
                val sourceMatches = mod.sourceFilter == null || (
                    sourceId != null && groupShieldEvaluator.matches(
                        state,
                        state.projectedState,
                        sourceId,
                        mod.sourceFilter,
                        predicateContext
                    )
                    )
                sourceMatches
            }) {
            return state to 0
        }

        for (i in updatedEffects.indices) {
            if (remainingDamage <= 0) break
            val effect = updatedEffects[i]
            val mod = effect.effect.modification
            if (mod is SerializableModification.PreventNextDamage && targetId in effect.effect.affectedEntities) {
                // Source-specific shields (from CR 615.7 prevention distribution) only match their source
                if (mod.onlyFromSource != null && mod.onlyFromSource != sourceId) continue
                val prevented = minOf(mod.remainingAmount, remainingDamage)
                remainingDamage -= prevented
                val newRemaining = mod.remainingAmount - prevented
                if (newRemaining <= 0) {
                    toRemove.add(i)
                } else {
                    updatedEffects[i] = effect.copy(
                        effect = effect.effect.copy(
                            modification = SerializableModification.PreventNextDamage(newRemaining, mod.onlyFromSource)
                        )
                    )
                }
            }
        }

        // Check for creature-type-specific prevention shields (Circle of Solace)
        if (remainingDamage > 0 && sourceId != null) {
            val projected = state.projectedState
            val sourceSubtypes = projected.getSubtypes(sourceId).map { it.uppercase() }.toSet()
            val sourceCard = state.getEntity(sourceId)?.get<CardComponent>()
            if (sourceCard != null && sourceCard.isCreature) {
                for (i in updatedEffects.indices) {
                    if (remainingDamage <= 0) break
                    if (i in toRemove) continue
                    val effect = updatedEffects[i]
                    val mod = effect.effect.modification
                    if (mod is SerializableModification.PreventNextDamageFromCreatureType &&
                        targetId in effect.effect.affectedEntities &&
                        mod.creatureType.uppercase() in sourceSubtypes
                    ) {
                        // Prevent all damage from this instance and consume the shield
                        remainingDamage = 0
                        toRemove.add(i)
                    }
                }
            }
        }

        // Check for single-instance chosen-source prevention shields (Circle of Protection family:
        // "the next time a [quality] source of your choice would deal damage to you, prevent it").
        // Prevents the entire next instance from the matching source, then is consumed.
        if (remainingDamage > 0 && sourceId != null) {
            for (i in updatedEffects.indices) {
                if (remainingDamage <= 0) break
                if (i in toRemove) continue
                val effect = updatedEffects[i]
                val mod = effect.effect.modification
                if (mod is SerializableModification.PreventNextDamageInstanceFromSource &&
                    targetId in effect.effect.affectedEntities &&
                    mod.damageSourceId == sourceId
                ) {
                    // Dark Sphere prevents only half the instance, rounded down; the Circle of
                    // Protection family prevents all of it. Either way this is a *next instance*
                    // shield, so it is consumed even when it prevents nothing (a 1-damage hit
                    // halves to 0 and still spends the Sphere).
                    remainingDamage = if (mod.halveRoundedDown) remainingDamage - remainingDamage / 2 else 0
                    toRemove.add(i)
                }
            }
        }

        // Remove fully consumed shields in reverse order to maintain indices
        for (idx in toRemove.sortedDescending()) {
            updatedEffects.removeAt(idx)
        }

        var newState = state.copy(floatingEffects = updatedEffects)

        // Apply static damage reduction from permanents with ReplacementEffectSourceComponent
        remainingDamage = applyStaticDamageReduction(newState, targetId, remainingDamage, isCombatDamage, sourceId)

        return newState to remainingDamage
    }

    /**
     * Check if all damage from a specific source creature is prevented this turn.
     * Used by Chain of Silence and similar "prevent all damage creature would deal" effects.
     */
    fun isAllDamageFromSourcePrevented(state: GameState, sourceId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.PreventAllDamageDealtBy &&
                sourceId in floatingEffect.effect.affectedEntities
        }
    }

    /**
     * Check for damage redirection shields (Glarecaster, Zealous Inquisitor, Blood of the Martyr).
     *
     * Scans floating effects for RedirectNextDamage covering the entity.
     * If found, consumes (or decrements) the shield and returns the redirect target ID
     * and the amount to redirect.
     *
     * An **optional** shield ("you may have that damage dealt to you instead") only applies when its
     * controller has already said yes to *this* damage instance — the answer is recorded by
     * [OptionalDamageRedirect]'s pre-pass and consumed here. A declined or never-asked instance falls
     * through to the next covering shield, so declining Blood of the Martyr doesn't shut off a
     * mandatory Glarecaster underneath it.
     *
     * @param state The current game state
     * @param targetId The entity about to receive damage
     * @param damageAmount The amount of damage about to be dealt
     * @param sourceId The source of the damage — identifies the instance an optional shield was
     *   answered for; a null source can never match a recorded answer
     * @return Triple of (updated state with consumed/decremented shield, redirect target ID or null, amount to redirect)
     */
    fun checkDamageRedirection(
        state: GameState,
        targetId: EntityId,
        damageAmount: Int,
        inBatch: Boolean = false,
        sourceId: EntityId? = null
    ): Triple<GameState, EntityId?, Int> {
        var workingState = state
        var shieldIndex = -1
        for ((index, effect) in state.floatingEffects.withIndex()) {
            val modification = effect.effect.modification
            if (modification !is SerializableModification.RedirectNextDamage) continue
            if (!OptionalDamageRedirect.redirectShieldCovers(workingState, effect, modification, targetId)) continue
            if (!modification.optional) {
                shieldIndex = index
                break
            }
            // "You may…" — this instance redirects only if its controller already said so. The
            // answer is spent either way, so the same shield asks again for the next instance.
            val (consumedState, choice) = OptionalDamageRedirect.consume(
                workingState,
                OptionalDamageRedirect.choiceKey(effect.id, sourceId, targetId)
            )
            workingState = consumedState
            if (choice == true) {
                shieldIndex = index
                break
            }
        }
        if (shieldIndex == -1) return Triple(workingState, null, 0)

        val shield = workingState.floatingEffects[shieldIndex]
        val mod = shield.effect.modification as SerializableModification.RedirectNextDamage

        val redirectAmount = if (mod.amount != null) minOf(mod.amount, damageAmount) else damageAmount

        val updatedEffects = workingState.floatingEffects.toMutableList()
        if (mod.amount != null) {
            // Capacity shield (CR 615.7) — decrement; remove once the capacity is used up.
            val remaining = mod.amount - redirectAmount
            if (remaining <= 0) {
                updatedEffects.removeAt(shieldIndex)
            } else {
                updatedEffects[shieldIndex] = shield.copy(
                    effect = shield.effect.copy(modification = mod.copy(amount = remaining))
                )
            }
        } else {
            // Unlimited shield — when it is used up depends on its scope.
            when (mod.scope) {
                // One source's instance, then gone.
                RedirectScope.NEXT_INSTANCE -> updatedEffects.removeAt(shieldIndex)
                // All damage in one simultaneous moment (e.g. a whole combat damage step, CR 510.2).
                // Inside such a batch the shield must survive so it redirects every instance; the
                // caller consumes it once after the batch (see CombatDamageManager.consumeBatchRedirectShields).
                // Outside a batch a single instance *is* the whole moment, so consume it now.
                RedirectScope.NEXT_BATCH -> if (!inBatch) updatedEffects.removeAt(shieldIndex)
                // "All damage this turn" — never self-consumed; expires only with its duration.
                RedirectScope.CONTINUOUS -> { /* keep the shield */ }
            }
        }

        return Triple(workingState.copy(floatingEffects = updatedEffects), mod.redirectToId, redirectAmount)
    }

    /**
     * Find a static [RedirectDamage] replacement effect that applies to the in-flight
     * damage (Harsh Judgment). Scans battlefield permanents with a
     * [ReplacementEffectSourceComponent]; the first matching effect whose source hasn't
     * already applied this event ([appliedRedirects]) wins.
     *
     * Returns (redirectTargetId, redirectSourceEntityId) — the entity to redirect the
     * full amount to, plus the replacement source's id (so the caller can mark it applied
     * and prevent redirect loops). Returns (null, null) when nothing applies.
     */
    private fun findStaticDamageRedirect(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId,
        isCombatDamage: Boolean,
        appliedRedirects: Set<EntityId>
    ): Pair<EntityId?, EntityId?> {
        val projected = state.projectedState

        for (entityId in state.getBattlefield()) {
            if (entityId in appliedRedirects) continue
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is RedirectDamage) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.EventPattern.DamageEvent) continue

                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> isCombatDamage
                    is DamageType.NonCombat -> !isCombatDamage
                }
                if (!damageTypeMatches) continue
                if (!damageEvent.amount.matches(amount)) continue

                val sourceMatches = when (val source = damageEvent.source) {
                    is SourceFilter.Any -> true
                    is SourceFilter.Matching -> {
                        val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId, recipientId = targetId)
                        predicateEvaluator.matches(state, projected, sourceId, source.filter, context)
                    }
                    else -> false
                }
                if (!sourceMatches) continue

                val recipientMatches = when (val recipient = damageEvent.recipient) {
                    is RecipientFilter.Any -> true
                    is RecipientFilter.You -> targetId == sourceControllerId
                    is RecipientFilter.Self -> targetId == entityId
                    is RecipientFilter.Matching -> {
                        val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId)
                        predicateEvaluator.matches(state, projected, targetId, recipient.filter, context)
                    }
                    is RecipientFilter.CreatureYouControl -> {
                        projected.isCreature(targetId) && projected.getController(targetId) == sourceControllerId
                    }
                    else -> false
                }
                if (!recipientMatches) continue

                // Optional condition gate evaluated against the replacement *source* — e.g.
                // Martyrs of Korlis only redirects "as long as this creature is untapped".
                val gateCondition = effect.condition
                if (gateCondition != null) {
                    val gateContext = EffectContext(
                        sourceId = entityId,
                        controllerId = sourceControllerId,
                    )
                    if (!conditionEvaluator.evaluate(state, gateCondition, gateContext)) continue
                }

                val redirectTo = resolveRedirectTarget(state, effect.redirectTo, sourceId, entityId, targetId)
                if (redirectTo != null && redirectTo != targetId) {
                    return redirectTo to entityId
                }
            }
        }
        return null to null
    }

    /**
     * Resolve the [EffectTarget] of a static [RedirectDamage] to a concrete entity id.
     * [damageSourceId] is the source dealing the in-flight damage, [replacementOwnerId]
     * the permanent carrying the replacement, [originalTargetId] the original recipient.
     */
    private fun resolveRedirectTarget(
        state: GameState,
        target: EffectTarget,
        damageSourceId: EntityId,
        replacementOwnerId: EntityId,
        originalTargetId: EntityId
    ): EntityId? = when (target) {
        is EffectTarget.ControllerOfDamageSource ->
            state.getEntity(damageSourceId)?.get<ControllerComponent>()?.playerId
                ?: state.getEntity(damageSourceId)?.get<CardComponent>()?.ownerId
        is EffectTarget.Controller ->
            state.getEntity(replacementOwnerId)?.get<ControllerComponent>()?.playerId
        is EffectTarget.TargetController ->
            state.projectedState.getController(originalTargetId)
                ?: state.getEntity(originalTargetId)?.get<ControllerComponent>()?.playerId
                ?: state.getEntity(originalTargetId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent>()
                    ?.snapshot?.controllerId
        is EffectTarget.Self -> replacementOwnerId
        // Pariah-style redirect (With Great Power…): "all damage dealt to you is dealt to
        // enchanted creature instead." The replacement lives on the Aura/Equipment; the
        // recipient is whatever it's attached to.
        is EffectTarget.EnchantedCreature,
        is EffectTarget.EquippedCreature,
        is EffectTarget.EnchantedPermanent ->
            state.getEntity(replacementOwnerId)?.get<AttachedToComponent>()?.targetId
        else -> null
    }

    /**
     * Check for single-instance chosen-source prevention shields (Deflecting Palm, New Way Forward).
     *
     * Scans floating effects for [SerializableModification.PreventNextDamageFromSourceShield]
     * matching the damage source. If found, consumes the shield (one instance prevented) and emits a
     * [DamagePreventedEvent] carrying the shield's `linkId`. That event fires the shield's linked
     * "when damage is prevented this way, …" delayed triggered ability on the stack, which deals the
     * prevented amount / draws cards through the normal stack — so it can be responded to (CR 603).
     *
     * @param state The current game state
     * @param targetId The entity about to receive damage (the protected player — the shield's affected entity)
     * @param damageAmount The amount of damage about to be dealt (and thus prevented)
     * @param sourceId The entity dealing the damage
     * @return ExecutionResult if a shield matched (damage prevented, event emitted), null otherwise
     */
    fun checkDeflectDamageShield(
        state: GameState,
        targetId: EntityId,
        damageAmount: Int,
        sourceId: EntityId
    ): DeflectOutcome? {
        val sourceEntity = state.getEntity(sourceId)
        val originatingSourceId = sourceEntity
            ?.get<com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent>()?.sourceId
            ?: sourceEntity?.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()?.sourceId
            ?: sourceEntity?.get<com.wingedsheep.engine.state.components.stack.AbilityOnStackComponent>()?.sourceId
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            val mod = effect.effect.modification
            mod is SerializableModification.PreventNextDamageFromSourceShield &&
                (mod.damageSourceId == sourceId || mod.damageSourceId == originatingSourceId) &&
                (effect.effect.affectedEntities.isEmpty() || targetId in effect.effect.affectedEntities)
        }
        if (shieldIndex == -1) return null

        val shield = state.floatingEffects[shieldIndex]
        val mod = shield.effect.modification as SerializableModification.PreventNextDamageFromSourceShield

        // Consume the shield (one damage instance) and announce it so the linked delayed triggered
        // ability fires on the stack with the captured amount.
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        val newState = state.copy(floatingEffects = updatedEffects)
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name
        val event = DamagePreventedEvent(
            sourceId = sourceId,
            recipientId = targetId,
            amount = damageAmount,
            linkId = mod.linkId,
            sourceName = sourceName
        )
        // preventDamage = true (Deflecting Palm): damage is prevented — short-circuit. preventDamage
        // = false (Eye for an Eye): the reaction fires but the damage still proceeds.
        return if (mod.preventDamage) {
            DeflectOutcome.Prevented(EffectResult.success(newState, listOf(event)))
        } else {
            DeflectOutcome.Reflected(newState, listOf(event))
        }
    }

    /**
     * Check for "prevent all damage from a chosen source" shields (Samite Ministration).
     *
     * Scans floating effects for a [SerializableModification.PreventAllDamageFromSource] whose
     * `damageSourceId` matches [sourceId] and whose affected entities include [targetId]. The shield
     * is persistent (not consumed) — it prevents all damage from the chosen source for the turn.
     *
     * If the shield carries `gainLifeFromColors` and the source has one of those colors, the affected
     * player gains life equal to the prevented amount.
     *
     * @return EffectResult (damage fully prevented) if a matching shield exists, null otherwise
     */
    fun checkPreventFromSourceShield(
        state: GameState,
        targetId: EntityId,
        damageAmount: Int,
        sourceId: EntityId
    ): EffectResult? {
        val shield = state.floatingEffects.firstOrNull { effect ->
            val mod = effect.effect.modification
            mod is SerializableModification.PreventAllDamageFromSource &&
                mod.damageSourceId == sourceId &&
                targetId in effect.effect.affectedEntities
        } ?: return null

        val mod = shield.effect.modification as SerializableModification.PreventAllDamageFromSource

        // Damage is fully prevented. Optionally gain life when the source matches one of the colors.
        if (damageAmount <= 0 || mod.gainLifeFromColors.isEmpty()) {
            return EffectResult.success(state)
        }

        // Combine projected colors (battlefield permanents) with base card colors (spells on the stack).
        val sourceColors = state.projectedState.getColors(sourceId) +
            (state.getEntity(sourceId)?.get<CardComponent>()?.colors?.map { it.name } ?: emptyList())
        if (sourceColors.none { it in mod.gainLifeFromColors }) {
            return EffectResult.success(state)
        }

        // Life gain goes to the affected player (the protected "you"). The amount is the fixed
        // prevented damage, so the ModifyLifeGain pipeline must not touch it.
        val (newState, event) = gainLife(state, targetId, damageAmount, applyLifeGainModification = false)
        return EffectResult.success(newState, listOfNotNull(event))
    }

    /**
     * Whether the source of a damage instance matches a damage replacement's [SourceFilter].
     *
     * Shared by every `EventPattern.DamageEvent`-scoped replacement scan below (prevention,
     * doubling, flat/dynamic modification) so the three cannot drift apart — a filter supported by
     * one is supported by all. That drift was a real bug: [DoubleDamage] silently ignored
     * [SourceFilter.YouControl] and fell through to "no match", so Twinflame Tyrant never doubled
     * anything.
     *
     * @param hostId the permanent hosting the replacement effect
     * @param hostControllerId that permanent's *current* controller — the "you" in "a source you
     *   control" (see [replacementHostController])
     * @param recipientId the entity being damaged, exposed to source-relative predicates
     */
    private fun damageSourceMatches(
        state: GameState,
        projected: ProjectedState,
        filter: SourceFilter,
        sourceId: EntityId?,
        hostId: EntityId,
        hostControllerId: EntityId,
        recipientId: EntityId,
    ): Boolean = when (filter) {
        is SourceFilter.Any -> true
        is SourceFilter.Self -> sourceId != null && sourceId == hostId
        // "Enchanted creature" / "equipped creature" on the source side — damage dealt *by* the
        // permanent this replacement's host is attached to. Same lookup for both, mirroring how
        // [damageRecipientMatches] shares a branch for the RecipientFilter pair.
        is SourceFilter.EnchantedCreature, is SourceFilter.EquippedCreature -> {
            val attachedTo = state.getEntity(hostId)?.get<AttachedToComponent>()?.targetId
            sourceId != null && sourceId == attachedTo
        }
        // "A source you control" — any source (permanent, spell, or ability) whose controller is
        // this replacement's controller. Projected control for battlefield permanents, the base
        // ControllerComponent for spells and abilities on the stack, and last-known information for
        // a source that has left the battlefield (see [controllerOfObject]).
        is SourceFilter.YouControl ->
            sourceId != null && damageSourceController(state, sourceId, projected) == hostControllerId
        is SourceFilter.Matching -> {
            if (sourceId == null) false
            else {
                val context = PredicateContext(
                    controllerId = hostControllerId,
                    sourceId = hostId,
                    recipientId = recipientId,
                )
                predicateEvaluator.matches(state, projected, sourceId, filter.filter, context)
            }
        }
        else -> false
    }

    /**
     * Whether the recipient of a damage instance matches a damage replacement's [RecipientFilter].
     * Companion to [damageSourceMatches]; see that doc for why the matching is shared.
     *
     * @param hostId the permanent hosting the replacement effect
     * @param hostControllerId that permanent's current controller — the "you" the filters are
     *   relative to
     */
    private fun damageRecipientMatches(
        state: GameState,
        projected: ProjectedState,
        filter: RecipientFilter,
        targetId: EntityId,
        hostId: EntityId,
        hostControllerId: EntityId,
    ): Boolean {
        // Projected control for battlefield permanents, base control for anything else (a
        // planeswalker mid-zone-change, a stack object). Null for players.
        fun controllerOf(entityId: EntityId): EntityId? = projected.getController(entityId)
            ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

        return when (filter) {
            is RecipientFilter.Any -> true
            is RecipientFilter.Self -> targetId == hostId
            is RecipientFilter.You -> targetId == hostControllerId
            is RecipientFilter.Opponent -> targetId in state.turnOrder && targetId != hostControllerId
            is RecipientFilter.AnyPlayer -> targetId in state.turnOrder
            is RecipientFilter.AnyPlayerOrPlaneswalker ->
                targetId in state.turnOrder || projected.isPlaneswalker(targetId)
            is RecipientFilter.EnchantedCreature, is RecipientFilter.EquippedCreature -> {
                val attachedTo = state.getEntity(hostId)?.get<AttachedToComponent>()?.targetId
                targetId == attachedTo
            }
            is RecipientFilter.AnyCreature -> projected.isCreature(targetId)
            is RecipientFilter.CreatureYouControl ->
                projected.isCreature(targetId) && controllerOf(targetId) == hostControllerId
            is RecipientFilter.CreatureOpponentControls ->
                projected.isCreature(targetId) &&
                    controllerOf(targetId).let { it != null && it != hostControllerId }
            is RecipientFilter.AnyPermanent -> targetId in state.getBattlefield()
            is RecipientFilter.PermanentYouControl ->
                targetId in state.getBattlefield() && controllerOf(targetId) == hostControllerId
            // "An opponent or a permanent an opponent controls" (Twinflame Tyrant, Fated Firepower).
            is RecipientFilter.OpponentOrPermanentTheyControl ->
                if (targetId in state.turnOrder) targetId != hostControllerId
                else controllerOf(targetId).let { it != null && it != hostControllerId }
            is RecipientFilter.Matching -> {
                // sourceId is the permanent that owns the replacement (e.g. Camel), so
                // source-relative recipient predicates like InSameBandAsSource can resolve
                // "this creature and creatures banded with it".
                val context = PredicateContext(controllerId = hostControllerId, sourceId = hostId)
                predicateEvaluator.matches(state, projected, targetId, filter.filter, context)
            }
        }
    }

    /**
     * Whether a [DoubleDamage]'s source filter is attachment-scoped — "damage *equipped/enchanted
     * creature* would deal" (Mjölnir, Hammer of Thor).
     *
     * These are the one source shape that is a property of a *specific* creature rather than of the
     * damage's recipient, which is why [damageDoublersAffectingPlayer] excludes them and
     * [damageDoublersAffectingSource] owns them instead. No other [SourceFilter] paired with a
     * [DoubleDamage] in the catalog is source-specific — every one of them is
     * [SourceFilter.Matching] scoped by controller (Twinflame Tyrant, Gratuitous Violence, The
     * Rollercrusher Ride, Collective Inferno, Kuja, Neriv) — so "damage dealt to you can be doubled"
     * stays honest for those without a concrete source in hand. [SourceFilter.Self] would be the
     * next shape to need this treatment if a card ever pairs it with doubling.
     */
    private fun isAttachmentScopedSource(filter: SourceFilter): Boolean =
        filter is SourceFilter.EquippedCreature || filter is SourceFilter.EnchantedCreature

    /**
     * Battlefield permanents whose [DoubleDamage] replacement currently applies to damage dealt to
     * [playerId] — what the client turns into the player's "damage doubled" badge, the counterpart
     * of the prevention-shield badges.
     *
     * One entry per applicable replacement, because each applies once (CR 616.1): two Twinflame
     * Tyrants means two entries and quadrupled damage. Recipient-side only — the source-side filter
     * needs a concrete damage source to evaluate, so an entry means "damage dealt to this player
     * can be doubled by this permanent", not that every instance will be.
     *
     * The one exception is an attachment-scoped source ([isAttachmentScopedSource]): an unattached
     * Equipment doubles nothing at all, and once attached the doubling is a property of the creature
     * it is on, not of whoever might be damaged. Those are reported by
     * [damageDoublersAffectingSource] and badged on that creature instead — otherwise a Mjölnir
     * sitting unequipped on the battlefield warns *both* players that damage dealt to them is
     * doubled, which is false in both directions.
     */
    fun damageDoublersAffectingPlayer(state: GameState, playerId: EntityId): List<ActiveDamageDoubler> {
        val projected = state.projectedState
        val doublers = mutableListOf<ActiveDamageDoubler>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val hostControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is DoubleDamage) continue
                val damageEvent = effect.appliesTo
                if (damageEvent !is EventPattern.DamageEvent) continue
                if (isAttachmentScopedSource(damageEvent.source)) continue

                // Gated doublers (The Rollercrusher Ride's delirium) only warn while the gate holds.
                if (effect.restrictions.isNotEmpty()) {
                    val context = EffectContext(sourceId = entityId, controllerId = hostControllerId)
                    if (effect.restrictions.any { !conditionEvaluator.evaluate(state, it, context) }) continue
                }

                val matches = damageRecipientMatches(
                    state, projected, damageEvent.recipient, playerId,
                    hostId = entityId, hostControllerId = hostControllerId,
                )
                if (!matches) continue

                doublers.add(
                    ActiveDamageDoubler(
                        sourceId = entityId,
                        sourceName = container.get<CardComponent>()?.name ?: "A permanent",
                        damageType = damageEvent.damageType,
                    )
                )
            }
        }

        return doublers
    }

    /**
     * Battlefield permanents whose attachment-scoped [DoubleDamage] currently doubles damage dealt
     * *by* [sourceId] — the source-side counterpart of [damageDoublersAffectingPlayer], which the
     * client turns into a badge on the equipped/enchanted creature itself.
     *
     * Attachment is the whole condition: only permanents attached to [sourceId] are considered, so
     * the badge appears exactly when the doubling can actually apply and follows the Equipment as it
     * moves. One entry per applicable replacement, matching the once-each rule (CR 616.1) that
     * [damageDoublersAffectingPlayer] documents.
     *
     * Driven off the maintained [AttachmentsComponent] reverse index rather than a battlefield scan:
     * this runs from `ClientStateTransformer.buildCardActiveEffects` for *every* card on *every*
     * state push, so scanning would make the view path quadratic in battlefield size. The un-attached
     * case — overwhelmingly the common one — costs a single component lookup.
     *
     * Only the Equipment half is reachable from the current catalog; no card pairs [DoubleDamage]
     * with [SourceFilter.EnchantedCreature] yet, so the Aura half is generality carried by the shared
     * matcher rather than behaviour under test.
     */
    fun damageDoublersAffectingSource(state: GameState, sourceId: EntityId): List<ActiveDamageDoubler> {
        val attachedIds = state.getEntity(sourceId)?.get<AttachmentsComponent>()?.attachedIds.orEmpty()
        if (attachedIds.isEmpty()) return emptyList()

        val doublers = mutableListOf<ActiveDamageDoubler>()

        for (entityId in attachedIds) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val hostControllerId = replacementHostController(state, entityId) ?: continue
            // The forward link is what [damageSourceMatches] reads when the damage actually
            // resolves, so the badge agrees with the replacement rather than with the index alone.
            if (container.get<AttachedToComponent>()?.targetId != sourceId) continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is DoubleDamage) continue
                val damageEvent = effect.appliesTo
                if (damageEvent !is EventPattern.DamageEvent) continue
                if (!isAttachmentScopedSource(damageEvent.source)) continue

                // Gated doublers only badge while the gate holds — same rule as the player badge.
                if (effect.restrictions.isNotEmpty()) {
                    val context = EffectContext(sourceId = entityId, controllerId = hostControllerId)
                    if (effect.restrictions.any { !conditionEvaluator.evaluate(state, it, context) }) continue
                }

                doublers.add(
                    ActiveDamageDoubler(
                        sourceId = entityId,
                        sourceName = container.get<CardComponent>()?.name ?: "A permanent",
                        damageType = damageEvent.damageType,
                    )
                )
            }
        }

        return doublers
    }

    /**
     * Apply static damage reduction from permanents on the battlefield.
     *
     * Scans all battlefield entities for ReplacementEffectSourceComponent containing
     * PreventDamage effects, and reduces damage if the target matches the effect's filter.
     *
     * @param state The current game state
     * @param targetId The entity receiving damage
     * @param amount The incoming damage amount
     * @param isCombatDamage Whether this is combat damage (for DamageType filtering)
     * @param sourceId The entity dealing damage (for source-based prevention like Sandskin)
     * @return The reduced damage amount (minimum 0)
     */
    private fun applyStaticDamageReduction(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        isCombatDamage: Boolean = false,
        sourceId: EntityId? = null
    ): Int {
        if (amount <= 0) return 0

        var remainingDamage = amount
        val projected = state.projectedState

        for (entityId in state.getBattlefield()) {
            if (remainingDamage <= 0) break
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (remainingDamage <= 0) break
                if (effect !is PreventDamage) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.EventPattern.DamageEvent) continue

                // Check damage type filter (combat vs non-combat)
                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> isCombatDamage
                    is DamageType.NonCombat -> !isCombatDamage
                }
                if (!damageTypeMatches) continue

                // Amount threshold (Callous Giant: "3 or less"). Checked against the full
                // incoming would-be amount, not the partially-prevented remainder.
                if (!damageEvent.amount.matches(amount)) continue

                // Additional gating conditions evaluated against the source's controller
                // (Spirit of Resistance: "as long as you control a permanent of each color").
                if (effect.restrictions.isNotEmpty()) {
                    val context = EffectContext(
                        sourceId = entityId,
                        controllerId = sourceControllerId,
                    )
                    if (effect.restrictions.any { !conditionEvaluator.evaluate(state, it, context) }) continue
                }

                // Check if the damage source matches the source filter
                val sourceMatches = damageSourceMatches(
                    state, projected, damageEvent.source, sourceId,
                    hostId = entityId, hostControllerId = sourceControllerId, recipientId = targetId,
                )
                if (!sourceMatches) continue

                // Check if the target matches the recipient filter
                val recipientMatches = damageRecipientMatches(
                    state, projected, damageEvent.recipient, targetId,
                    hostId = entityId, hostControllerId = sourceControllerId,
                )

                if (recipientMatches) {
                    val preventAmount = effect.amount
                    val prevented = if (preventAmount == null) remainingDamage else minOf(preventAmount, remainingDamage)
                    remainingDamage -= prevented
                }
            }
        }

        return remainingDamage.coerceAtLeast(0)
    }

    /**
     * Apply static damage amplification from permanents on the battlefield.
     *
     * Scans all battlefield entities for ReplacementEffectSourceComponent containing
     * DoubleDamage effects, and doubles damage if the source and recipient match.
     * Per MTG rules, damage amplification applies before prevention.
     *
     * @param state The current game state
     * @param targetId The entity receiving damage
     * @param amount The incoming damage amount
     * @param sourceId The entity dealing damage
     * @return The amplified damage amount
     */
    fun applyStaticDamageAmplification(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?,
        isCombatDamage: Boolean = false
    ): Int {
        if (amount <= 0) return 0

        var amplifiedAmount = amount
        val projected = state.projectedState

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is DoubleDamage) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.EventPattern.DamageEvent) continue

                // Check damage type filter (combat vs non-combat) — The Rollercrusher Ride
                // only doubles noncombat damage.
                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> isCombatDamage
                    is DamageType.NonCombat -> !isCombatDamage
                }
                if (!damageTypeMatches) continue

                // Additional gating conditions evaluated against the source's controller
                // (The Rollercrusher Ride: delirium — "four or more card types … in your graveyard").
                if (effect.restrictions.isNotEmpty()) {
                    val context = EffectContext(
                        sourceId = entityId,
                        controllerId = sourceControllerId,
                    )
                    if (effect.restrictions.any { !conditionEvaluator.evaluate(state, it, context) }) continue
                }

                // Check if the damage source matches the source filter
                val sourceMatches = damageSourceMatches(
                    state, projected, damageEvent.source, sourceId,
                    hostId = entityId, hostControllerId = sourceControllerId, recipientId = targetId,
                )
                if (!sourceMatches) continue

                // Check if the target matches the recipient filter
                val recipientMatches = damageRecipientMatches(
                    state, projected, damageEvent.recipient, targetId,
                    hostId = entityId, hostControllerId = sourceControllerId,
                )
                if (!recipientMatches) continue

                // Each DoubleDamage source is its own replacement effect and applies once
                // (CR 616.1), so two Twinflame Tyrants quadruple rather than double.
                amplifiedAmount *= 2
            }
        }

        // Floating player-scoped damage-doubling (Lightning, Army of One's "Stagger"): a
        // duration-bounded [SerializableModification.DoubleDamageToPlayer] doubles all damage — combat
        // or noncombat, any source — dealt to the scoped player or to any permanent that player
        // controls. Each install is a separate replacement, so two of them on the same player quadruple
        // (each doubles once, CR 616). Independent of the source that created it (which may have left).
        for (floating in state.floatingEffects) {
            val mod = floating.effect.modification
            if (mod !is com.wingedsheep.engine.mechanics.layers.SerializableModification.DoubleDamageToPlayer) continue
            val recipientMatches = targetId == mod.playerId ||
                projected.getController(targetId) == mod.playerId
            if (!recipientMatches) continue
            amplifiedAmount *= 2
        }

        // Check for ModifyDamageAmount replacement effects (Valley Flamecaller)
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is ModifyDamageAmount) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.EventPattern.DamageEvent) continue

                // Check damage type filter (combat vs non-combat), exactly as the DoubleDamage loop
                // above does — combat damage reaches both loops through the same
                // applyStaticDamageAmplification(isCombatDamage = true). Without this, Hawkeye, Young
                // Avenger's "+X to *noncombat* damage" also pumped your creatures' combat damage.
                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> isCombatDamage
                    is DamageType.NonCombat -> !isCombatDamage
                }
                if (!damageTypeMatches) continue

                // Additional gating conditions, evaluated against the *replacement source's*
                // controller like the PreventDamage / DoubleDamage sites above — so Far Fortune,
                // End Boss's "Max speed — …" rider reads Far Fortune's controller's speed, not the
                // damaged opponent's.
                if (effect.restrictions.isNotEmpty()) {
                    val context = EffectContext(
                        sourceId = entityId,
                        controllerId = sourceControllerId,
                    )
                    if (effect.restrictions.any { !conditionEvaluator.evaluate(state, it, context) }) continue
                }

                // Check if the damage source matches the source filter
                val sourceMatches = damageSourceMatches(
                    state, projected, damageEvent.source, sourceId,
                    hostId = entityId, hostControllerId = sourceControllerId, recipientId = targetId,
                )
                if (!sourceMatches) continue

                // Check if the target matches the recipient filter
                val recipientMatches = damageRecipientMatches(
                    state, projected, damageEvent.recipient, targetId,
                    hostId = entityId, hostControllerId = sourceControllerId,
                )
                if (!recipientMatches) continue

                // Additive bonus: a dynamic amount (evaluated against the replacement's source
                // permanent — e.g. Fated Firepower's fire-counter count) when present, else the
                // flat modifier (Valley Flamecaller).
                amplifiedAmount += effect.dynamicModifier?.let { dyn ->
                    dynamicAmountEvaluator.evaluate(
                        state,
                        dyn,
                        EffectContext(sourceId = entityId, controllerId = sourceControllerId),
                        projected
                    )
                } ?: effect.modifier
            }
        }

        // Check player-level damage bonus components (e.g., The Flame of Keld Chapter III)
        if (sourceId != null) {
            for (playerId in state.turnOrder) {
                val playerContainer = state.getEntity(playerId) ?: continue
                val damageBonusComponent = playerContainer.get<DamageBonusComponent>() ?: continue

                // Check that the source is controlled by this player
                val sourceController = state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                if (sourceController != playerId) continue

                // Check if the source matches the source filter
                val sourceMatches = when (val filter = damageBonusComponent.sourceFilter) {
                    is SourceFilter.Any -> true
                    is SourceFilter.HasColor -> {
                        // Check projected state first (for battlefield permanents), fall back to base colors
                        // (for spells on the stack or other non-battlefield entities)
                        hasColorForSource(state, projected, sourceId, filter.color)
                    }
                    is SourceFilter.Matching -> {
                        val context = PredicateContext(controllerId = playerId)
                        predicateEvaluator.matches(state, projected, sourceId, filter.filter, context)
                    }
                    else -> false
                }
                if (!sourceMatches) continue

                amplifiedAmount += damageBonusComponent.bonusAmount
            }
        }

        // Check battlefield permanents for NoncombatDamageBonus static abilities (Artist's Talent Level 3)
        if (sourceId != null && !isCombatDamage) {
            val sourceController = projected.getController(sourceId)
                ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
            if (sourceController != null) {
                for (entityId in state.controlledBattlefield(sourceController)) {
                    val container = state.getEntity(entityId) ?: continue
                    val card = container.get<CardComponent>() ?: continue
                    val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                    val classLevel = container.get<ClassLevelComponent>()?.currentLevel

                    for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
                        if (ability !is NoncombatDamageBonus) continue

                        // Check target is an opponent or a permanent an opponent controls
                        val targetIsOpponent = targetId in state.turnOrder && targetId != sourceController
                        val targetController = state.getEntity(targetId)?.get<ControllerComponent>()?.playerId
                        val targetIsOpponentPermanent = targetController != null && targetController != sourceController

                        if (targetIsOpponent || targetIsOpponentPermanent) {
                            amplifiedAmount += ability.bonusAmount
                        }
                    }
                }
            }
        }

        // Turn-duration noncombat-damage amplification (Taii Wakeen, Perfect Shot): every source
        // the effect's controller controls deals +bonus noncombat damage to any permanent or
        // player this turn (CR 616). No opponent restriction — applies to the controller's own
        // permanents too. Multiple installs stack additively.
        if (sourceId != null && !isCombatDamage) {
            val sourceController = projected.getController(sourceId)
                ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
            if (sourceController != null) {
                for (floating in state.floatingEffects) {
                    val mod = floating.effect.modification
                    if (mod !is com.wingedsheep.engine.mechanics.layers.SerializableModification.AmplifyNoncombatDamage) continue
                    if (floating.controllerId != sourceController) continue
                    amplifiedAmount += mod.bonus
                }
            }
        }

        // Cap damage replacements (Divine Presence): clamp the would-be amount to a maximum.
        // Applied last so it caps the fully-amplified amount. Capping is a replacement, so
        // prevention shields still apply afterward to the capped amount.
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is CapDamage) continue
                if (amplifiedAmount <= effect.maxAmount) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.EventPattern.DamageEvent) continue

                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> isCombatDamage
                    is DamageType.NonCombat -> !isCombatDamage
                }
                if (!damageTypeMatches) continue
                if (!damageEvent.amount.matches(amplifiedAmount)) continue

                val sourceMatches = when (val source = damageEvent.source) {
                    is SourceFilter.Any -> true
                    is SourceFilter.Matching -> {
                        if (sourceId == null) false
                        else {
                            val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId, recipientId = targetId)
                            predicateEvaluator.matches(state, projected, sourceId, source.filter, context)
                        }
                    }
                    else -> false
                }
                if (!sourceMatches) continue

                val recipientMatches = when (val recipient = damageEvent.recipient) {
                    is RecipientFilter.Any -> true
                    is RecipientFilter.Self -> targetId == entityId
                    is RecipientFilter.Matching -> {
                        val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId)
                        predicateEvaluator.matches(state, projected, targetId, recipient.filter, context)
                    }
                    is RecipientFilter.CreatureYouControl -> {
                        projected.isCreature(targetId) && projected.getController(targetId) == sourceControllerId
                    }
                    else -> false
                }
                if (!recipientMatches) continue

                amplifiedAmount = effect.maxAmount
            }
        }

        // Minimum-damage replacements (Ojer Axonil, Deepest Might): raise the would-be amount to
        // a floor. The floor mirror of CapDamage — applied last, only to a positive would-be
        // amount (a source dealing 0 isn't dealing damage, so it isn't raised).
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is com.wingedsheep.sdk.scripting.SetMinimumDamage) continue
                if (amplifiedAmount <= 0) continue

                val floor = effect.dynamicMinimum?.let { dyn ->
                    dynamicAmountEvaluator.evaluate(
                        state, dyn,
                        EffectContext(sourceId = entityId, controllerId = sourceControllerId),
                        projected
                    )
                } ?: effect.minAmount
                if (amplifiedAmount >= floor) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.EventPattern.DamageEvent) continue

                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> isCombatDamage
                    is DamageType.NonCombat -> !isCombatDamage
                }
                if (!damageTypeMatches) continue
                if (!damageEvent.amount.matches(amplifiedAmount)) continue

                val sourceMatches = when (val source = damageEvent.source) {
                    is SourceFilter.Any -> true
                    is SourceFilter.Matching -> {
                        if (sourceId == null) false
                        else {
                            val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId, recipientId = targetId)
                            predicateEvaluator.matches(state, projected, sourceId, source.filter, context)
                        }
                    }
                    else -> false
                }
                if (!sourceMatches) continue

                val recipientMatches = when (val recipient = damageEvent.recipient) {
                    is RecipientFilter.Any -> true
                    is RecipientFilter.Opponent -> targetId in state.turnOrder && targetId != sourceControllerId
                    is RecipientFilter.Matching -> {
                        val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId)
                        predicateEvaluator.matches(state, projected, targetId, recipient.filter, context)
                    }
                    else -> false
                }
                if (!recipientMatches) continue

                amplifiedAmount = floor
            }
        }

        return amplifiedAmount
    }

    /**
     * Apply life-loss replacement effects (ModifyLifeLoss) to a life-loss amount.
     *
     * Scans the battlefield for permanents with a [ModifyLifeLoss] replacement effect
     * whose [GameEvent.LifeLossEvent] filter matches [losingPlayerId] and whose
     * [ModifyLifeLoss.restrictions] (evaluated against the source permanent's
     * controller) all hold, then applies `(amount * multiplier) + modifier`, clamped
     * to ≥ 0.
     *
     * Multiple matching effects are applied in iteration order. Used both by direct
     * life-loss effects (LoseLifeExecutor) and by damage that reduces a player's life
     * total (CR 120.3a).
     */
    fun applyStaticLifeLossModification(
        state: GameState,
        losingPlayerId: EntityId,
        amount: Int
    ): Int {
        if (amount <= 0) return 0

        var modifiedAmount = amount
        forEachLifeLossReplacement<ModifyLifeLoss>(state, losingPlayerId, { it.restrictions }) { effect ->
            modifiedAmount = (modifiedAmount * effect.multiplier) + effect.modifier
            if (modifiedAmount < 0) modifiedAmount = 0
        }
        return modifiedAmount
    }

    /**
     * Apply life-loss floor replacement effects ([LifeLossFloor]) so the resulting
     * life total does not fall below each effect's floor.
     *
     * Called only from the damage → life-loss pipeline (CR 120.3a); direct life-loss
     * effects ([com.wingedsheep.engine.handlers.effects.life.LoseLifeExecutor]) skip
     * this step, matching the printed ruling on Ali from Cairo: "This effect does
     * not apply to effects which reduce your life without doing damage."
     *
     * Multiple matching floors pick the strictest (highest resulting life total).
     */
    fun applyLifeLossFloors(
        state: GameState,
        losingPlayerId: EntityId,
        currentLife: Int,
        amount: Int
    ): Int {
        if (amount <= 0) return amount

        var modifiedAmount = amount
        forEachLifeLossReplacement<LifeLossFloor>(state, losingPlayerId, { it.restrictions }) { effect ->
            val maxLossAllowed = (currentLife - effect.floor).coerceAtLeast(0)
            if (modifiedAmount > maxLossAllowed) modifiedAmount = maxLossAllowed
        }
        return modifiedAmount
    }

    /**
     * Visit every battlefield replacement effect of type [T] whose `appliesTo`
     * is a [EventPattern.LifeLossEvent] matching [losingPlayerId] and whose
     * [restrictionsOf]-extracted conditions all hold against the source's controller.
     *
     * Shared scaffold for [applyStaticLifeLossModification] (`ModifyLifeLoss`)
     * and [applyLifeLossFloors] (`LifeLossFloor`); each function keeps the actual
     * arithmetic in its callback.
     */
    private inline fun <reified T : ReplacementEffect> forEachLifeLossReplacement(
        state: GameState,
        losingPlayerId: EntityId,
        restrictionsOf: (T) -> List<Condition>,
        action: (T) -> Unit
    ) {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is T) continue

                val pattern = effect.appliesTo
                if (pattern !is EventPattern.LifeLossEvent) continue

                val playerMatches = when (pattern.player) {
                    Player.Each, Player.Any -> true
                    Player.You -> losingPlayerId == sourceControllerId
                    // Not `!= sourceControllerId`: a teammate is not an opponent (CR 102.3).
                    Player.EachOpponent -> state.isOpponentOf(losingPlayerId, sourceControllerId)
                    else -> false
                }
                if (!playerMatches) continue

                val restrictions = restrictionsOf(effect)
                if (restrictions.isNotEmpty()) {
                    val context = EffectContext(
                        sourceId = entityId,
                        controllerId = sourceControllerId,
                    )
                    if (restrictions.any { !conditionEvaluator.evaluate(state, it, context) }) continue
                }

                action(effect)
            }
        }
    }

    /**
     * Check if a source entity has a specific color — checks projected state first,
     * then falls back to base CardComponent colors (for spells on the stack).
     */
    private fun hasColorForSource(
        state: GameState,
        projected: com.wingedsheep.engine.mechanics.layers.ProjectedState,
        sourceId: EntityId,
        color: com.wingedsheep.sdk.core.Color
    ): Boolean {
        if (projected.hasColor(sourceId, color)) return true
        val sourceEntity = state.getEntity(sourceId) ?: return false
        val card = sourceEntity.components[CardComponent::class.java] as? CardComponent ?: return false
        return card.colors.contains(color)
    }

    /**
     * Heal (CR 701.69a) all damage marked on [entityId] — "that permanent's controller removes all
     * marked damage from that permanent". Deliberately clears the whole [DamageComponent], including
     * the `deathtouchDamageReceived` flag: that flag only records damage *marked* by a deathtouch
     * source, and a heal removes exactly that. Any deathtouch damage being dealt in the same event
     * is re-stamped after the heal by the damage-marking code, so a deathtouch source still kills
     * (CR 704.5h).
     *
     * -1/-1 counters from a wither/infect source are NOT marked damage (CR 120.3d) and are untouched.
     *
     * This is the single home of the action: regeneration (CR 701.19a), the Pyramids destruction
     * replacement, the cleanup step (CR 514.2) and [applyHealOtherDamage] all route through it. The
     * battlefield-exit component strip in `ZoneMovementUtils` deliberately does *not* — that is a
     * whole-object reset (CR 400.7), not a heal.
     */
    fun healMarkedDamage(state: GameState, entityId: EntityId): GameState {
        val container = state.getEntity(entityId) ?: return state
        if (container.get<DamageComponent>() == null) return state
        return state.updateEntity(entityId) { it.without<DamageComponent>() }
    }

    /**
     * Apply [HealOtherDamage] — "instead that damage is dealt, but all other damage already dealt to
     * it is healed" (Wolverine, Fierce Fighter). The damage amount is untouched; the replacement's
     * only job is to wipe the damage marked *before* this event, so the recipient never accumulates
     * damage across separate damage events.
     *
     * **Caller contract:** call this immediately before the damage is marked, and at most once per
     * damage *event* — not once per instance. `CombatDamageManager.applyCombatDamage` documents why
     * and enforces it with a per-step set; the noncombat path deals one instance at a time and needs
     * no bookkeeping.
     *
     * Players, planeswalkers and battles never have marked damage, so this is a no-op on them.
     *
     * @return the state with marked damage healed if a matching replacement exists, else [state]
     */
    fun applyHealOtherDamage(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?,
        isCombatDamage: Boolean
    ): GameState {
        if (amount <= 0) return state
        // Fast path: nothing marked, nothing to heal — skip the battlefield scan. Not a correctness
        // guard; `healMarkedDamage` re-checks before touching the entity.
        if (state.getEntity(targetId)?.get<DamageComponent>() == null) return state
        val projected = state.projectedState

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val hostControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is HealOtherDamage) continue

                val damageEvent = effect.appliesTo as? EventPattern.DamageEvent ?: continue

                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> isCombatDamage
                    is DamageType.NonCombat -> !isCombatDamage
                }
                if (!damageTypeMatches) continue
                if (!damageEvent.amount.matches(amount)) continue

                if (!damageRecipientMatches(
                        state, projected, damageEvent.recipient, targetId, entityId, hostControllerId
                    )
                ) continue
                if (!damageSourceMatches(
                        state, projected, damageEvent.source, sourceId, entityId, hostControllerId, targetId
                    )
                ) continue

                return healMarkedDamage(state, targetId)
            }
        }
        return state
    }

    /**
     * Check for [ReplaceDamageWithCounters] replacement effects (Force Bubble, Anti-Venom,
     * Soul-Scar Mage).
     *
     * Scans the battlefield for permanents carrying a [ReplaceDamageWithCounters] whose
     * [com.wingedsheep.sdk.scripting.EventPattern.DamageEvent] matches this damage event —
     * recipient, source and damage type all have to agree — and, on the first match, replaces the
     * damage outright (CR 614.1a: the damage is never dealt) with counters on the permanent named
     * by [ReplaceDamageWithCounters.counterRecipient]. The first match wins because a replacement
     * gets one shot at an event (CR 614.5) and, after this one applies, the event is no longer a
     * damage event for any other to see.
     *
     * This is a *replacement*, not a prevention effect, so it deliberately runs even when the
     * damage can't be prevented (Soul-Scar Mage's own ruling says exactly that).
     *
     * @param state The current game state
     * @param targetId The entity receiving damage — a player or a permanent
     * @param amount The damage amount to replace
     * @param sourceId The entity dealing damage (matched against the pattern's source filter)
     * @param isCombatDamage Whether this is combat damage, matched against the pattern's damage
     *        type. Combat-damage callers must pass `true`, or a noncombat-only replacement
     *        (Soul-Scar Mage) would swallow combat damage as well.
     * @return ExecutionResult if replacement was applied, null if no replacement found
     */
    fun applyReplaceDamageWithCounters(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?,
        isCombatDamage: Boolean = false
    ): EffectResult? {
        if (amount <= 0) return null

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is ReplaceDamageWithCounters) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.EventPattern.DamageEvent) continue

                // Combat vs noncombat. Soul-Scar Mage only replaces noncombat damage, so a
                // replacement that names a type is skipped when this event is the other one.
                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> isCombatDamage
                    is DamageType.NonCombat -> !isCombatDamage
                }
                if (!damageTypeMatches) continue

                // "a source you control" (Soul-Scar Mage) / "this permanent" — shared with every
                // other damage-replacement scan so the supported filters cannot drift apart.
                val sourceMatches = damageSourceMatches(
                    state = state,
                    projected = state.projectedState,
                    filter = damageEvent.source,
                    sourceId = sourceId,
                    hostId = entityId,
                    hostControllerId = sourceControllerId,
                    recipientId = targetId,
                )
                if (!sourceMatches) continue

                // Check recipient filter
                val recipientMatches = when (val recipient = damageEvent.recipient) {
                    is RecipientFilter.You -> targetId == sourceControllerId
                    // "If damage would be dealt to <this creature>…" (Anti-Venom) — the damaged
                    // permanent is the replacement's own host; counters go on it (entityId).
                    is RecipientFilter.Self -> targetId == entityId
                    // "…to a creature an opponent controls" (Soul-Scar Mage). Reads the projected
                    // controller and the projected type, so a stolen creature and one that is only
                    // a creature because of a continuous effect are both judged correctly.
                    is RecipientFilter.CreatureOpponentControls ->
                        state.projectedState.isCreature(targetId) &&
                            state.projectedState.getController(targetId)
                                ?.let { it != sourceControllerId } == true
                    is RecipientFilter.Any -> true
                    else -> false
                }
                if (!recipientMatches) continue

                // Which permanent the counters land on. "That creature" (Soul-Scar Mage) is the
                // damaged permanent; every "this permanent" printing is the replacement's host.
                // A player has nowhere to put counters, so a DamagedPermanent replacement declines
                // rather than dropping the damage on the floor.
                val counterHolderId = when (effect.counterRecipient) {
                    DamageCounterRecipient.ReplacementHost -> entityId
                    DamageCounterRecipient.DamagedPermanent -> targetId
                }
                val counterHolder = state.getEntity(counterHolderId) ?: continue
                if (effect.counterRecipient == DamageCounterRecipient.DamagedPermanent &&
                    counterHolder.get<LifeTotalComponent>() != null
                ) continue

                // Match found — replace damage with counters
                val events = mutableListOf<EngineGameEvent>()
                var newState = state

                // Convert the printed counter name to its CounterType. `fromName` is the shared
                // parse: it knows the symbolic stat names ("+1/+1", "-1/-1") that `valueOf` alone
                // cannot reach.
                val counterType = CounterType.fromName(effect.counterType)
                    ?: CounterType.PLUS_ONE_PLUS_ONE

                val currentCounters = counterHolder.get<CountersComponent>() ?: CountersComponent()
                val updatedCounters = currentCounters.withAdded(counterType, amount)
                newState = newState.updateEntity(counterHolderId) { c ->
                    c.with(updatedCounters)
                }

                val entityName = counterHolder.get<CardComponent>()?.name ?: ""
                events.add(CountersAddedEvent(counterHolderId, effect.counterType, amount, entityName, placedBy = sourceControllerId))

                // Check sacrifice threshold (state-triggered ability approximation)
                val totalCounters = updatedCounters.getCount(counterType)
                val threshold = effect.sacrificeThreshold
                if (threshold != null && totalCounters >= threshold) {
                    newState = ZoneTransitionService.trackPermanentSacrifice(
                        newState, listOf(entityId), sourceControllerId
                    )
                    // Delegate zone movement to ZoneTransitionService for full cleanup
                    val transitionResult = ZoneTransitionService.moveToZone(
                        newState, entityId, Zone.GRAVEYARD
                    )
                    newState = transitionResult.state
                    events.add(
                        PermanentsSacrificedEvent(
                            sourceControllerId,
                            listOf(entityId),
                            listOf(entityName)
                        )
                    )
                    events.addAll(transitionResult.events)
                }

                return EffectResult.success(newState, events)
            }
        }

        return null
    }

    /**
     * Check for [ReplaceDamageWithMill] replacement effects (The Mindskinner).
     *
     * Scans the battlefield for permanents whose replacement effect prevents damage matching its
     * [ReplaceDamageWithMill.appliesTo] pattern and instead mills that many cards from each opponent
     * of the replacement's controller. [targetId] is the damage recipient (a player), [sourceId] the
     * damage source (required — the modelled cards scope to "a source you control"). Returns a
     * short-circuiting [EffectResult] when a replacement applies (so the original damage is replaced,
     * not dealt), or null when none matches.
     *
     * Mirrors [applyReplaceDamageWithCounters]; like it, damage-type filtering is not applied
     * (the modelled card replaces damage of any type).
     */
    fun applyReplaceDamageWithMill(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?
    ): EffectResult? {
        if (amount <= 0 || sourceId == null) return null
        val projected = state.projectedState

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = replacementHostController(state, entityId) ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is ReplaceDamageWithMill) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is EventPattern.DamageEvent) continue
                if (!damageEvent.amount.matches(amount)) continue

                val sourceMatches = when (val source = damageEvent.source) {
                    is SourceFilter.Any -> true
                    is SourceFilter.Matching -> {
                        val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId, recipientId = targetId)
                        predicateEvaluator.matches(state, projected, sourceId, source.filter, context)
                    }
                    else -> false
                }
                if (!sourceMatches) continue

                val recipientMatches = when (val recipient = damageEvent.recipient) {
                    is RecipientFilter.Any -> true
                    is RecipientFilter.Opponent -> targetId in state.turnOrder && targetId != sourceControllerId
                    is RecipientFilter.You -> targetId == sourceControllerId
                    is RecipientFilter.Matching -> {
                        val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId)
                        predicateEvaluator.matches(state, projected, targetId, recipient.filter, context)
                    }
                    else -> false
                }
                if (!recipientMatches) continue

                // Replace the damage: each opponent of the controller mills `amount` cards. Snapshot
                // the top cards before moving so a shrinking library doesn't re-read shifted slots.
                var newState = state
                val events = mutableListOf<EngineGameEvent>()
                for (opponentId in state.getOpponents(sourceControllerId)) {
                    val topCards = newState.getLibrary(opponentId).take(amount)
                    for (cardId in topCards) {
                        val result = ZoneTransitionService.moveToZone(newState, cardId, Zone.GRAVEYARD)
                        newState = result.state
                        events.addAll(result.events)
                    }
                }
                return EffectResult.success(newState, events)
            }
        }

        return null
    }
}
