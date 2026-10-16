package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.ManaAbilitySourcesContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.ManaSourceTag
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * One mana payment that may spend only mana produced by sources matching a filter — "Spend only
 * mana produced by Treasures to cast it this way" (Security Rhox's
 * [com.wingedsheep.sdk.scripting.SelfAlternativeCost.manaSources]).
 *
 * [state] is the payment's view of the game, and every affordability check, validation and payment
 * for the restricted cast runs against it:
 *  - a [ManaAbilitySourcesContinuation] frame sits on its continuation stack, so the solver, its
 *    sacrifice/tap-permanents/explicit-activation extras and the mana-ability gate all refuse
 *    sources that don't match ([GameState.manaAbilitySourceAllowed]) — the same scope
 *    `WithManaAbilitySources` opens;
 *  - the player's floating pool holds only the units whose recorded source matched when the mana
 *    was made. Everything else — other sources' mana and mana with no recorded source — is set
 *    aside, untouched, and handed back by [restore].
 *
 * Read-only callers use [state] and drop it; a payment pays against [state] and then calls
 * [restore] on the result, which pops the frame and returns the set-aside mana to the pool.
 */
class ManaSourceRestrictedPayment private constructor(
    val state: GameState,
    private val playerId: EntityId,
    private val frame: ManaAbilitySourcesContinuation,
    private val setAside: ManaPoolComponent,
) {
    /** [paid] with the scope closed and the mana this payment couldn't touch back in the pool. */
    fun restore(paid: GameState): GameState {
        val stack = paid.continuationStack
        val index = stack.lastIndexOf(frame)
        val closed = if (index < 0) paid else paid.copy(continuationStack = stack.take(index) + stack.drop(index + 1))
        if (setAside.isEmpty) return closed
        return closed.updateEntity(playerId) { container ->
            val pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            // `plus` drops the added entries' spending obligations; these never left the player.
            container.with(pool.plus(setAside).copy(restrictedMana = pool.restrictedMana + setAside.restrictedMana))
        }
    }

    companion object {
        /**
         * Open the restricted payment for [playerId], spending only mana from sources matching
         * [sources]; [context] names the spell ([EffectContext.sourceId]) the filter is judged for.
         */
        fun open(
            state: GameState,
            playerId: EntityId,
            sources: GameObjectFilter,
            context: EffectContext,
            evaluator: PredicateEvaluator,
        ): ManaSourceRestrictedPayment {
            val scoped = context.copy(controllerId = playerId)
            val frame = ManaAbilitySourcesContinuation(playerId, sources, scoped)
            val predicateContext = PredicateContext.fromEffectContext(scoped)
            val pool = state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val (eligible, setAside) = pool.partitionBySource { tag ->
                evaluator.matchesSnapshot(state, tag.asSnapshot(), sources, predicateContext)
            }
            val view = state.copy(continuationStack = state.continuationStack + frame)
                .let { if (setAside.isEmpty) it else it.updateEntity(playerId) { c -> c.with(eligible) } }
            return ManaSourceRestrictedPayment(view, playerId, frame, setAside)
        }

        /** The source as it was when it produced the mana: the type line its tag recorded. */
        private fun ManaSourceTag.asSnapshot(): EntitySnapshot {
            val supers = if (isSnow) supertypes + Supertype.SNOW else supertypes
            val typeLine = TypeLine(supertypes = supers, cardTypes = cardTypes, subtypes = subtypes)
            return EntitySnapshot(
                entityId = sourceId,
                subtypes = subtypes.mapTo(mutableSetOf()) { it.value },
                supertypes = supers.mapTo(mutableSetOf()) { it.name },
                typeLine = typeLine,
            )
        }

        /**
         * Split the pool into the units [matches] admits and the rest. Only mana that remembers its
         * source can be admitted: an unrestricted unit through its [ManaUnit], a restricted entry
         * through its [com.wingedsheep.engine.state.components.player.RestrictedManaEntry.source].
         * The provenance counters of the admitted half are rebuilt from its units, the remainder
         * keeps the difference.
         */
        private fun ManaPoolComponent.partitionBySource(
            matches: (ManaSourceTag) -> Boolean,
        ): Pair<ManaPoolComponent, ManaPoolComponent> {
            val (unitsIn, unitsOut) = manaUnits.partition { matches(it.source) }
            val (restrictedIn, restrictedOut) = restrictedMana.partition { entry -> entry.source?.let(matches) == true }

            fun admitted(color: Color?): Int =
                minOf(unitsIn.count { it.color == color }, if (color == null) colorless else getAmount(color))
            fun admittedSnow(color: Color?): Int = minOf(
                unitsIn.count { it.color == color && it.source.isSnow },
                admitted(color),
                if (color == null) snowColorless else snowMana[color] ?: 0,
            )

            var eligible = ManaPoolComponent(
                white = admitted(Color.WHITE),
                blue = admitted(Color.BLUE),
                black = admitted(Color.BLACK),
                red = admitted(Color.RED),
                green = admitted(Color.GREEN),
                colorless = admitted(null),
                restrictedMana = restrictedIn,
                snowMana = Color.entries.associateWith { admittedSnow(it) }.filterValues { it > 0 },
                manaUnits = unitsIn,
                snowColorless = admittedSnow(null),
            )
            for (unit in unitsIn) eligible = eligible.withProvenance(unit.source, 1)
            // The pool's provenance counters are consumed proportionally, so they can sit below
            // what the surviving units would rebuild; never hand the admitted half more than the
            // pool had, and never leave the remainder negative.
            fun <K> capped(rebuilt: Map<K, Int>, original: Map<K, Int>): Map<K, Int> =
                rebuilt.mapValues { (k, n) -> minOf(n, original[k] ?: 0) }.filterValues { it > 0 }
            eligible = eligible.copy(
                manaBySubtype = capped(eligible.manaBySubtype, manaBySubtype),
                manaBySource = capped(eligible.manaBySource, manaBySource),
                manaByCardType = capped(eligible.manaByCardType, manaByCardType),
            )

            fun <K> minus(all: Map<K, Int>, taken: Map<K, Int>): Map<K, Int> =
                all.mapValues { (k, n) -> (n - (taken[k] ?: 0)).coerceAtLeast(0) }.filterValues { it > 0 }
            val rest = ManaPoolComponent(
                white = white - eligible.white,
                blue = blue - eligible.blue,
                black = black - eligible.black,
                red = red - eligible.red,
                green = green - eligible.green,
                colorless = colorless - eligible.colorless,
                restrictedMana = restrictedOut,
                manaBySubtype = minus(manaBySubtype, eligible.manaBySubtype),
                manaBySource = minus(manaBySource, eligible.manaBySource),
                manaByCardType = minus(manaByCardType, eligible.manaByCardType),
                snowMana = minus(snowMana, eligible.snowMana),
                manaUnits = unitsOut,
                snowColorless = snowColorless - eligible.snowColorless,
            )
            return eligible to rest
        }
    }
}
