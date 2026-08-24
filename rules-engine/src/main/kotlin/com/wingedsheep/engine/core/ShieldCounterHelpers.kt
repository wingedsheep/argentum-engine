package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Consume one shield counter from [entityId], the single mutation behind both halves of
 * CR 122.1c.
 *
 * One or more shield counters on a permanent create a **single** replacement effect and a
 * **single** prevention effect:
 *
 * - "If this permanent would be destroyed as the result of an effect, instead remove a shield
 *   counter from it."
 * - "If damage would be dealt to this permanent, prevent that damage and remove a shield counter
 *   from it."
 *
 * Both consume exactly **one** counter per event no matter how many are on the permanent, which is
 * why this atom is fixed at 1 rather than parameterized: a creature with three shield counters
 * survives three separate damage or destroy events, not one event three times over.
 *
 * This is the analogue of [untapOrConsumeStun]'s stun branch (CR 122.1d) — an inherent rule of the
 * counter wired at the chokepoints that can trigger it, not an ability of the permanent. Per the
 * official rulings a creature that loses all its abilities is still protected, and shield counters
 * are *not* keyword counters (deliberately absent from `StateProjector.KEYWORD_COUNTER_MAP`).
 *
 * The four chokepoints that reach this, two per half:
 * - The **prevention** half, both via [applyShieldCounterToDamage]:
 *   `DamageUtils.dealDamageToTarget` (noncombat/effect damage) and
 *   `CombatDamageManager.applyShieldCountersToCombatDamage` (combat damage, which marks itself in
 *   that class instead of routing through `dealDamageToTarget`).
 * - The **replacement** half, calling this directly: `ZoneMovementUtils.destroyPermanent` and
 *   `MoveCollectionExecutor`'s `MoveType.Destroy` branch. Deliberately *not* the lethal-damage
 *   state-based action (`LethalDamageCheck`): 122.1c replaces destruction "as the result of an
 *   **effect**", and the rulings confirm a creature with a shield counter still dies to the SBA
 *   when it has lethal damage marked on it or was dealt unpreventable damage by a deathtouch
 *   source.
 *
 * @return the updated state paired with the [CountersRemovedEvent] to emit, or `null` when
 *   [entityId] has no shield counter (so callers can fall through to the unreplaced behavior).
 */
fun consumeShieldCounter(state: GameState, entityId: EntityId): Pair<GameState, CountersRemovedEvent>? {
    val container = state.getEntity(entityId) ?: return null
    val counters = container.get<CountersComponent>() ?: return null
    if (counters.getCount(CounterType.SHIELD) <= 0) return null

    val newState = state.updateEntity(entityId) { c ->
        c.with(counters.withRemoved(CounterType.SHIELD, 1))
    }
    val event = CountersRemovedEvent(
        entityId,
        CounterType.SHIELD.name,
        1,
        container.get<CardComponent>()?.name ?: "Permanent"
    )
    return newState to event
}

/**
 * Outcome of a shield counter meeting an incoming damage instance — see [applyShieldCounterToDamage].
 *
 * @property state the state with the counter already removed
 * @property event the [CountersRemovedEvent] the caller must emit
 * @property damagePrevented whether the damage itself is prevented; false when prevention is
 *   switched off (Leyline of Punishment, Fear, Fire, Foes!), in which case the caller keeps dealing
 *   the damage but the counter is still gone
 */
data class ShieldedDamage(
    val state: GameState,
    val event: CountersRemovedEvent,
    val damagePrevented: Boolean,
)

/**
 * The prevention half of CR 122.1c: "If damage would be dealt to this permanent, prevent that damage
 * and remove a shield counter from it."
 *
 * Single home for the rule, shared by the two damage-application paths — `DamageUtils`
 * (noncombat/effect damage) and `CombatDamageManager.applyShieldCountersToCombatDamage` (combat
 * damage, which marks damage itself instead of routing through `DamageUtils.dealDamageToTarget`).
 *
 * Note the asymmetry the official rulings require: when damage *can't be prevented*, the damage is
 * still dealt **and** a shield counter is still removed. So the counter is consumed unconditionally
 * once one is present; only [ShieldedDamage.damagePrevented] is gated on [cantBePrevented].
 *
 * **Known ordering difference between the two paths.** CR 616.1 lets the affected permanent's
 * controller order competing replacement/prevention effects, so more than one order is legal — but
 * the two call sites do not currently pick the *same* one. `DamageUtils` consults redirection
 * (Glarecaster) and the damage-to-counters self-replacement (Anti-Venom) *before* the shield
 * counter, whereas the combat path spends the counter first, because it has to run over the whole
 * simultaneous batch (CR 510.2) before per-assignment replacements are reachable. A permanent
 * carrying both a shield counter and a Glarecaster shield therefore keeps the counter against a
 * Shock but spends it against a blocker. Both outcomes are legal orderings; unifying them would
 * mean giving up either the batch scoping or the redirect-first preference, so the difference is
 * recorded here rather than papered over.
 *
 * Players never carry shield counters (CR 122.1c is written for permanents), so callers may pass any
 * recipient; a player simply has no counters and gets `null`.
 *
 * @return `null` when [entityId] has no shield counter, so callers fall through to the normal
 *   damage-application path unchanged.
 */
fun applyShieldCounterToDamage(
    state: GameState,
    entityId: EntityId,
    cantBePrevented: Boolean,
): ShieldedDamage? {
    val (newState, event) = consumeShieldCounter(state, entityId) ?: return null
    return ShieldedDamage(newState, event, damagePrevented = !cantBePrevented)
}

/**
 * The printed sibling of [applyShieldCounterToDamage]: a
 * [com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter] on [entityId] itself — "If this
 * creature would be dealt damage, prevent that damage and remove a +1/+1 counter from it"
 * (Unbreathing Horde).
 *
 * Same shape as the shield-counter rule and wired at the same two chokepoints, with one deliberate
 * difference: the prevention is **not** gated on having a counter to remove. CR 122.1c's shield is
 * *made of* its counters, so no counter means no effect; Unbreathing Horde's is a printed ability
 * that prevents unconditionally and removes a counter if one is there. Hence this returns a
 * [ShieldedDamage] whose `event` is nullable, where the shield version returns `null` outright.
 *
 * Only self-recipient patterns are honoured — the printed line says "this creature", and a
 * card that shielded *other* permanents this way would need the scan to run over the battlefield
 * rather than over the damaged permanent.
 *
 * @return `null` when [entityId] carries no such replacement effect, so callers fall through to the
 *   normal damage-application path unchanged.
 */
fun applyPreventByRemovingCounterToDamage(
    state: GameState,
    entityId: EntityId,
    isCombatDamage: Boolean,
    cantBePrevented: Boolean,
): CounterSpendingShield? {
    val container = state.getEntity(entityId) ?: return null
    val effects = container.get<ReplacementEffectSourceComponent>()?.replacementEffects ?: return null
    val effect = effects.filterIsInstance<PreventDamageByRemovingCounter>().firstOrNull { candidate ->
        val pattern = candidate.appliesTo
        pattern is EventPattern.DamageEvent &&
            pattern.recipient == RecipientFilter.Self &&
            when (pattern.damageType) {
                is DamageType.Any -> true
                is DamageType.Combat -> isCombatDamage
                is DamageType.NonCombat -> !isCombatDamage
            }
    } ?: return null

    val counterType = counterTypeOf(effect.counterType) ?: return null
    val counters = container.get<CountersComponent>()
    val hasCounter = (counters?.getCount(counterType) ?: 0) > 0
    if (!hasCounter) {
        // Nothing to remove, but the damage is still prevented (the printed ruling).
        return CounterSpendingShield(state, event = null, damagePrevented = !cantBePrevented)
    }
    val newState = state.updateEntity(entityId) { c ->
        c.with(counters!!.withRemoved(counterType, 1))
    }
    val event = CountersRemovedEvent(
        entityId,
        counterType.name,
        1,
        container.get<CardComponent>()?.name ?: "Permanent"
    )
    return CounterSpendingShield(newState, event, damagePrevented = !cantBePrevented)
}

/**
 * Outcome of a printed prevent-and-remove-a-counter ability meeting an incoming damage instance.
 *
 * Unlike [ShieldedDamage] the [event] is nullable: the ability fires (and prevents) even when the
 * permanent has no counter of the named type left to remove.
 */
data class CounterSpendingShield(
    val state: GameState,
    val event: CountersRemovedEvent?,
    val damagePrevented: Boolean,
)

/**
 * The concrete [CounterType] a [CounterTypeFilter] names, or `null` for the filters that describe a
 * *set* of counter types rather than one ("any counter") — those can't say which counter to remove.
 */
private fun counterTypeOf(filter: CounterTypeFilter): CounterType? = when (filter) {
    is CounterTypeFilter.PlusOnePlusOne -> CounterType.PLUS_ONE_PLUS_ONE
    is CounterTypeFilter.MinusOneMinusOne -> CounterType.MINUS_ONE_MINUS_ONE
    is CounterTypeFilter.PlusOnePlusZero -> CounterType.PLUS_ONE_PLUS_ZERO
    is CounterTypeFilter.PlusZeroPlusOne -> CounterType.PLUS_ZERO_PLUS_ONE
    is CounterTypeFilter.MinusOneMinusZero -> CounterType.MINUS_ONE_MINUS_ZERO
    is CounterTypeFilter.MinusZeroMinusOne -> CounterType.MINUS_ZERO_MINUS_ONE
    is CounterTypeFilter.Loyalty -> CounterType.LOYALTY
    // Fails *closed*, unlike the enters-with resolver's +1/+1 fallback: an unknown counter name
    // here would silently spend the wrong counter, so the effect declines instead.
    is CounterTypeFilter.Named -> CounterType.entries.firstOrNull {
        it.name.equals(filter.name.uppercase().replace(' ', '_'), ignoreCase = true)
    }
    // "Any counter" cannot say which counter to remove.
    is CounterTypeFilter.Any -> null
}
