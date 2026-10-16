package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.ManaSourceTag
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.EntityId

/**
 * Tags mana added to a player's pool with its provenance — which source produced it and what
 * subtypes and card types that source had — so payoffs can later ask "which kind of source produced the mana spent
 * to cast this?".
 *
 * The [ManaPoolComponent.manaBySubtype] / [ManaPoolComponent.manaBySource] counters record, per
 * subtype and per producing source, how many mana units in the pool came from there. When mana is
 * spent for a spell, [com.wingedsheep.engine.handlers.actions.spell.CastPaymentProcessor] consumes
 * from those counters proportional to the unrestricted mana taken from the pool and records what was
 * consumed on the spell (`SpentManaProvenance`).
 *
 * Generalizes the old Treasure-only counter (Treasure is now just `manaBySubtype[Subtype.TREASURE]`)
 * and powers Alchemist's Talent level 3 ("if mana from a Treasure was spent"), Bat Colony ("a Bat
 * for each mana from a Cave spent to cast it"), and the LCI mana-source lands (Tecutlan / Barracks /
 * Myriad Pools — "cast … using mana produced by this land"). The set of mana-producing executors
 * that call into here is: [AddManaExecutor], [AddColorlessManaExecutor], [AddManaOfChoiceExecutor]
 * (both the immediate and the post-color-choice resumer paths).
 */
object ManaProvenanceTracker {

    /**
     * Snapshot what [sourceId] is right now. Subtypes come from projected state on the battlefield and the base [CardComponent.typeLine] otherwise —
     * the source may already be in the graveyard (a Treasure's `{T}, Sacrifice this` pays the cost
     * before the mana effect resolves), but the entity persists with its base type line intact.
     * Card types read the *projected* type line while the source is on the battlefield, so an
     * animated land's mana is mana from a creature, and fall back to the base type line once it
     * has left (a creature sacrificed for its own mana still made creature mana).
     */
    fun sourceTag(state: GameState, sourceId: EntityId): ManaSourceTag {
        val typeLine = state.getEntity(sourceId)?.get<CardComponent>()?.typeLine
        val projectedTypes = state.projectedState.getTypes(sourceId)
        val cardTypes = if (projectedTypes.isNotEmpty()) {
            CardType.entries.filterTo(mutableSetOf()) { it.name in projectedTypes }
        } else {
            typeLine?.cardTypes ?: emptySet()
        }
        val supertypes = if (projectedTypes.isNotEmpty()) Supertype.fromProjectedTypes(projectedTypes).toSet()
            else typeLine?.supertypes ?: emptySet()
        return ManaSourceTag(sourceId, if (projectedTypes.isNotEmpty()) state.projectedState.getSubtypes(sourceId).mapTo(mutableSetOf()) { com.wingedsheep.sdk.core.Subtype(it) } else typeLine?.subtypes?.toSet() ?: emptySet(), cardTypes, isSnowSource(state, sourceId), supertypes)
    }

    private fun sourceTag(state: GameState, sourceId: EntityId,
                          context: com.wingedsheep.engine.handlers.EffectContext): ManaSourceTag {
        if (state.projectedState.getTypes(sourceId).isNotEmpty()) return sourceTag(state, sourceId)
        val snapshot = context.lastKnownSourceSnapshot ?: return sourceTag(state, sourceId)
        return ManaSourceTag(sourceId,
            snapshot.subtypes.mapTo(mutableSetOf()) { com.wingedsheep.sdk.core.Subtype(it) },
            snapshot.typeLine?.cardTypes.orEmpty(),
            snapshot.supertypes.any { it.equals("Snow", ignoreCase = true) },
            snapshot.supertypes.mapNotNullTo(mutableSetOf()) { name ->
                Supertype.entries.find { it.name.equals(name, ignoreCase = true) } ?: Supertype.fromString(name)
            })
    }

    /**
     * Whether [sourceId] is a snow source (CR 205.4g), so its mana pays `{S}` (CR 107.4h). Reads the
     * projected supertypes while the source is on the battlefield — a permanent made snow by an
     * effect makes snow mana — and the base type line once it has left.
     */
    fun isSnowSource(state: GameState, sourceId: EntityId): Boolean =
        if (state.projectedState.getTypes(sourceId).isNotEmpty()) state.projectedState.isSnow(sourceId)
        else state.getEntity(sourceId)?.get<CardComponent>()?.typeLine?.supertypes?.contains(Supertype.SNOW) == true

    /**
     * Snow mana (CR 107.4h): when [sourceId] is a snow source, mark every unit its mana ability
     * just added to [tapperId]'s pool — the per-kind increase from [stateBeforeEffect] to [state] —
     * as snow, so a later `{S}` pip can be paid with it. Reads the pool delta rather than the
     * effect, so every mana executor (fixed, chosen, dynamic, dampened) is covered by this one call.
     */
    fun markSnowProduction(
        stateBeforeEffect: GameState,
        state: GameState,
        sourceId: EntityId?,
        tapperId: EntityId,
    ): GameState {
        if (sourceId == null) return state
        val before = stateBeforeEffect.getEntity(tapperId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        val after = state.getEntity(tapperId)?.get<ManaPoolComponent>() ?: return state
        val newUnits = after.manaUnits.toMutableList()
        before.manaUnits.forEach { newUnits.remove(it) }
        // The production tag retains projected snow status even after the source sacrifices itself.
        val producesSnow = newUnits.firstOrNull { it.source.sourceId == sourceId }?.source?.isSnow
            ?: isSnowSource(state, sourceId)
        if (!producesSnow) return state
        val marked = Color.entries.fold(after) { pool, color ->
            pool.markSnow(color, after.getAmount(color) - before.getAmount(color))
        }.markSnow(null, after.colorless - before.colorless)
        return if (marked == after) state else state.updateEntity(tapperId) { it.with(marked) }
    }

    /**
     * Increment the producing player's provenance counters when [sourceId] produced [amount]
     * unrestricted mana. A null [sourceId] contributes nothing.
     */
    fun tagAddedMana(state: GameState, playerId: EntityId, sourceId: EntityId?, amount: Int, color: Color?, context: com.wingedsheep.engine.handlers.EffectContext): GameState {
        if (amount <= 0 || sourceId == null) return state
        val tag = sourceTag(state, sourceId, context)
        return state.updateEntity(playerId) { container ->
            val pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(pool.withProvenance(tag, amount).copy(manaUnits = pool.manaUnits +
                List(amount) { com.wingedsheep.engine.mechanics.mana.ManaUnit(color, tag) }))
        }
    }

    /**
     * Tag the [amount] restricted entries [sourceId] just appended to the player's pool, so
     * restricted mana ("spend this mana only to cast a creature spell") carries its provenance
     * into the payment like unrestricted mana does.
     */
    fun tagAddedRestrictedMana(state: GameState, playerId: EntityId, sourceId: EntityId?, amount: Int, context: com.wingedsheep.engine.handlers.EffectContext): GameState {
        if (amount <= 0 || sourceId == null) return state
        val tag = sourceTag(state, sourceId, context)
        return state.updateEntity(playerId) { container ->
            val pool = container.get<ManaPoolComponent>() ?: return@updateEntity container
            container.with(pool.withRestrictedProvenance(tag, amount))
        }
    }
}
