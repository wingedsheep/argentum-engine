package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.contextScopedReferenceIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Power/Toughness Modification Effects
// =============================================================================

/**
 * Modify power/toughness effect.
 * "Target creature gets +X/+Y until end of turn"
 *
 * Supports both fixed and dynamic amounts via [DynamicAmount].
 */
@SerialName("ModifyStats")
@Serializable
data class ModifyStatsEffect(
    val powerModifier: DynamicAmount,
    val toughnessModifier: DynamicAmount,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    constructor(powerModifier: Int, toughnessModifier: Int, target: EffectTarget, duration: Duration = Duration.EndOfTurn) :
        this(DynamicAmount.Fixed(powerModifier), DynamicAmount.Fixed(toughnessModifier), target, duration)

    override val description: String = buildString {
        append("${target.description} gets ")
        val pDesc = powerModifier.let {
            if (it is DynamicAmount.Fixed) {
                if (it.amount >= 0) "+${it.amount}" else "${it.amount}"
            } else {
                it.description
            }
        }
        val tDesc = toughnessModifier.let {
            if (it is DynamicAmount.Fixed) {
                if (it.amount >= 0) "+${it.amount}" else "${it.amount}"
            } else {
                it.description
            }
        }
        append("$pDesc/$tDesc")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int?): String = buildString {
        append("${target.description} gets ")
        // An amount the context can't determine yet — "double its power", read off a target that
        // hasn't been chosen — falls back to its own wording, exactly as [description] does for a
        // non-Fixed amount. Formatting the absent value as "+0" would read as a real +0/+0 pump.
        fun fmt(amount: DynamicAmount): String {
            val v = resolver(amount) ?: return amount.description
            return if (v >= 0) "+$v" else "$v"
        }
        append("${fmt(powerModifier)}/${fmt(toughnessModifier)}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newPower = powerModifier.applyTextReplacement(replacer)
        val newToughness = toughnessModifier.applyTextReplacement(replacer)
        return if (newPower !== powerModifier || newToughness !== toughnessModifier)
            copy(powerModifier = newPower, toughnessModifier = newToughness) else this
    }
}

/**
 * Set a creature's base power and/or toughness via a floating continuous effect at
 * Layer.POWER_TOUGHNESS, Sublayer.SET_VALUES (CR 613.4b, layer 7b).
 *
 * A `null` [power] or [toughness] leaves that stat unchanged, so this single atom expresses every
 * shape the engine needs:
 *  - power only ("change this creature's base power to target creature's power") — toughness null,
 *  - both ("has base power and toughness 2/2 until your next turn", Azure Beastbinder),
 *  - toughness only.
 * Both are [DynamicAmount] (the asymmetry the two predecessors carried — power-only was dynamic,
 * power-and-toughness was fixed Int — is gone), so either value can be read from game state.
 *
 * [reevaluateContinuously] picks *when* those `DynamicAmount`s are read — the headline difference
 * between two genuinely different Magic templates on the same layer-7b set, though not the only one
 * (see the three limits below):
 *  - `false` (default) — **snapshot**: the amounts are evaluated once as the effect resolves and the
 *    number is frozen for the duration. "Change this creature's base power to target creature's
 *    power" keeps the number it saw even if that other creature is later pumped or dies.
 *  - `true` — **re-evaluated**: the `DynamicAmount`s travel into the projection and are recomputed on
 *    every layer pass, so the stat tracks the game state. This is what an effect that hands a
 *    creature a *quoted static ability* needs — Ms. Marvel, Kamala Khan's "Until end of turn, this
 *    creature gains 'This creature's base power is equal to the number of cards in your hand.'"
 *    Note that a self-granted ability like that is **not** a characteristic-defining ability
 *    (CR 604.3a criteria 2 and 4: not printed on the card, and granted by the object to itself), so
 *    it belongs in layer 7b with the timestamp of the grant, not in layer 7a.
 * Either way the *affected set* is locked in at resolution (CR 611.2c) — only the number moves.
 *
 * Four limits on `reevaluateContinuously = true`, none of which apply to the snapshot mode. The
 * first three follow from the number being read by the layer projector rather than at resolution:
 *  - **Only projection-scoped `DynamicAmount`s are supported.** The projector re-evaluates the
 *    amount with just the source, its controller and the affected entity in scope, so anything
 *    reading the resolution context — `XValue`/`CastX`, `ContextProperty`, the pipeline's stored
 *    collections, or an `EntityReference`/`Player` naming a target, the triggering object or
 *    something sacrificed or tapped as a cost — has nothing to resolve against. This class's `init`
 *    rejects those via
 *    [com.wingedsheep.sdk.scripting.values.contextScopedReferenceIn] instead of letting them read as
 *    0 forever, so `SetBasePower(t, EntityProperty(Triggering, Power), reevaluateContinuously =
 *    true)` fails as the card is **loaded**, not silently at resolution. For X specifically,
 *    re-evaluation is also a rules error: CR 611.2d fixes a continuous effect's X on resolution.
 *    Counts, battlefield/zone aggregates, life totals, hand size and
 *    `EntityReference.Source`/`AffectedEntity` properties are all fine.
 *  - **"Your" means the *source's* controller**, not the affected creature's — the projector
 *    rebuilds the context from `sourceId`. That is right for a self-granted clause (Ms. Marvel:
 *    source and affected permanent are the same object, so it follows her controller even after a
 *    control change) and for a grant to a creature you control, but a re-evaluated grant handed to
 *    another player's creature would read the granting player's hand rather than the creature
 *    controller's. Keep the template to self-grants and creatures you control until an
 *    affected-entity-controller player reference exists.
 *  - **It applies only while the affected permanent is a creature** (CR 208.3a — the effect is still
 *    created, it just "doesn't do anything unless that permanent becomes a creature"). The gate is
 *    re-asked every projection pass, so a Vehicle crewed later in the turn picks the value up. The
 *    snapshot mode writes its number unconditionally.
 *  - **A quoted clause modelled this way is not an ability the creature actually has**, so
 *    `LoseAllAbilities` cannot strip it. In paper, "gains '…'" and Humility are both layer 6 and the
 *    later timestamp wins; here the grant is a layer-7b floating effect with nothing for layer 6 to
 *    remove, so it keeps applying. The two agree whenever the grant is the later effect — the
 *    common case, and Ms. Marvel's — and diverge only when the ability-removal arrives afterwards.
 *    Routing a runtime-granted static through `StaticAbilityHandler.lowerToContinuousEffectData`
 *    (as `BecomeArtifactExecutor` already does) is the shape that would close this; reach for it if
 *    a card ever needs to hand out a whole quoted static ability rather than one base-P/T clause.
 *
 * It is deliberately distinct from:
 *  - [ModifyStatsEffect] — a +N/+N *modifier* (layer 7c), not a set.
 *  - the `SetBasePowerToughness*Static` characteristic-defining abilities — applied for as long as
 *    a static ability printed on a permanent is active, not a floating effect with a duration.
 *
 * Reach it through the [com.wingedsheep.sdk.dsl.Effects] `SetBasePower` / `SetBaseToughness` /
 * `SetBasePowerAndToughness` facades rather than constructing it directly.
 *
 * @property target The creature whose base stats are being set
 * @property power The value to set base power to, or null to leave it
 * @property toughness The value to set base toughness to, or null to leave it
 * @property duration How long the effect lasts (typically Permanent for indefinite effects)
 * @property reevaluateContinuously Recompute [power]/[toughness] on every projection pass instead of
 *   snapshotting them at resolution
 */
@SerialName("SetBaseStats")
@Serializable
data class SetBaseStatsEffect(
    val target: EffectTarget,
    val power: DynamicAmount? = null,
    val toughness: DynamicAmount? = null,
    val duration: Duration = Duration.Permanent,
    val reevaluateContinuously: Boolean = false
) : Effect {
    init {
        // Load-time, not resolution-time: an amount the projector cannot re-evaluate is an
        // authoring mistake, and it should fail as the cardDef is built (CardDiscovery, the corpus
        // tests) rather than throw mid-game the first time this effect happens to resolve. Only
        // reached when the flag is set, so the JSON scan costs nothing for the common snapshot mode.
        if (reevaluateContinuously) {
            listOfNotNull(power, toughness).forEach { amount ->
                val offending = contextScopedReferenceIn(amount)
                require(offending == null) {
                    "SetBaseStatsEffect(reevaluateContinuously = true) cannot carry the " +
                        "context-scoped reference '$offending' in ${amount.description}: the " +
                        "projector re-evaluates the amount with only the source, its controller " +
                        "and the affected entity in scope, so target-, X-, triggering- and " +
                        "cost-scoped references resolve to nothing on every pass. Use the default " +
                        "snapshot mode (CR 611.2d requires X to be fixed on resolution anyway), " +
                        "or an amount computable from the source, the affected entity and global " +
                        "game state."
                }
            }
        }
    }

    override val description: String = buildString {
        when {
            power != null && toughness != null ->
                append("${target.description} has base power and toughness ${power.description}/${toughness.description}")
            power != null ->
                append("Change ${target.description}'s base power to ${power.description}")
            toughness != null ->
                append("Change ${target.description}'s base toughness to ${toughness.description}")
            else -> append("Set ${target.description}'s base stats")
        }
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newPower = power?.applyTextReplacement(replacer)
        val newToughness = toughness?.applyTextReplacement(replacer)
        return if (newPower !== power || newToughness !== toughness)
            copy(power = newPower, toughness = newToughness) else this
    }
}

/** Switch power and toughness after all other stat changes, for the chosen duration. */
@SerialName("SwitchPowerToughness")
@Serializable
data class SwitchPowerToughnessEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "Switch ${target.description}'s power and toughness" +
        if (duration.description.isNotEmpty()) " ${duration.description}" else ""
}
