package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId

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
