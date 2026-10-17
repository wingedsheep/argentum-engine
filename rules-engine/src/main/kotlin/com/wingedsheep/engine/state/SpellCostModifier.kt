package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * A duration-bounded "spells [controllerId] casts [this turn | until your next turn] that match
 * [spellFilter] cost {[amount]} less (or, with [increase], more) to cast" modifier, installed by
 * [com.wingedsheep.sdk.scripting.effects.ModifySpellCostsEffect] (Will, Scion of Peace;
 * Rowan, Scion of War; Ral, Leyline Prodigy; Academy Loremaster's tax).
 *
 * The repeating counterpart of [PendingNextSpellAffinity]: that rider is consumed by the first
 * matching spell, this one keeps applying until its [duration] ends. Two differences drive the
 * shape:
 *
 * - **[amount] is already resolved.** The Scion rulings fix X at the moment the activated ability
 *   resolves, so the executor evaluates the [com.wingedsheep.sdk.scripting.values.DynamicAmount]
 *   once and stores the number. Re-reading it per cast would let life gained after activation
 *   inflate the discount.
 * - **It lives on the game state, not the source.** The ability has already resolved, so the
 *   discount must outlive the source leaving the battlefield.
 *
 * [Duration.EndOfTurn] entries are cleared at every turn boundary by
 * [com.wingedsheep.engine.core.TurnManager.startTurn]; [Duration.UntilYourNextTurn] entries survive
 * it and are removed after the untap step of [controllerId]'s next turn by
 * [com.wingedsheep.engine.core.CleanupPhaseManager.expireUntilYourNextTurnEffects].
 *
 * @property controllerId The player whose matching spells are modified — and whose next turn
 *   ends an [Duration.UntilYourNextTurn] discount.
 * @property spellFilter Which of that player's spells the discount applies to.
 * @property amount Generic mana taken off (or added to) each matching spell (CR 601.2f — colored
 *   mana is never changed, and the mana component is floored at {0}).
 * @property sourceId The entity that created this discount.
 * @property sourceName Human-readable name of the source.
 * @property duration How long the discount lasts: [Duration.EndOfTurn] or
 *   [Duration.UntilYourNextTurn].
 * @property increase `true` for a tax ("cost {N} more"), `false` for a discount.
 * @property nextSpellOnly `true` for a one-shot "the next spell you cast this turn costs {N} less"
 *   (Peerless Samurai): the first matching spell [controllerId] casts spends it, in
 *   [com.wingedsheep.engine.handlers.actions.spell.CastTriggers.consumeNextSpellRiders].
 */
@Serializable
data class SpellCostModifier(
    val controllerId: EntityId,
    val spellFilter: GameObjectFilter,
    val amount: Int,
    val sourceId: EntityId,
    val sourceName: String,
    val duration: Duration = Duration.EndOfTurn,
    val increase: Boolean = false,
    val nextSpellOnly: Boolean = false,
)
