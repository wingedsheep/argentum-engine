package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EffectVariable
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Composite Effects
// =============================================================================

/**
 * Multiple effects that happen together.
 *
 * By default, [description] and [runtimeDescription] concatenate each sub-effect's
 * own text with ". ". For cards whose pipeline produces a verbose or implementation-leaking
 * join (e.g., intermediate `StoreNumber` steps), supply [descriptionOverride] to render
 * a single hand-written sentence instead. Use `{0}`, `{1}`, ... placeholders paired with
 * [descriptionAmounts] to interpolate evaluated dynamic values at runtime — the static
 * [description] leaves placeholders literal.
 */
@SerialName("Composite")
@Serializable
data class CompositeEffect(
    val effects: List<Effect>,
    val stopOnError: Boolean = false,
    val descriptionOverride: String? = null,
    val descriptionAmounts: List<DynamicAmount> = emptyList()
) : Effect {
    override val description: String =
        descriptionOverride ?: effects.joinToString(". ") { it.description }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int): String {
        val template = descriptionOverride
            ?: return effects.joinToString(". ") { it.runtimeDescription(resolver) }
        var rendered = template
        descriptionAmounts.forEachIndexed { index, amount ->
            rendered = rendered.replace("{$index}", resolver(amount).toString())
        }
        return rendered
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var changed = false
        val newEffects = effects.map { val n = it.applyTextReplacement(replacer); if (n !== it) changed = true; n }
        val newAmounts = descriptionAmounts.map { val n = it.applyTextReplacement(replacer); if (n !== it) changed = true; n }
        return if (changed) copy(effects = newEffects, descriptionAmounts = newAmounts) else this
    }
}

/**
 * Optional effect wrapper - the player may choose to perform or skip this effect.
 * "You may draw a card" or "You may shuffle your library"
 *
 * Use this to compose optional parts of abilities rather than creating
 * specific optional variants of each effect.
 */
@SerialName("May")
@Serializable
data class MayEffect(
    val effect: Effect,
    val description_override: String? = null,
    val sourceRequiredZone: Zone? = null,
    val inlineOnTrigger: Boolean = false,
    /** Optional hint text shown below the prompt (e.g., keyword reminder text) */
    val hint: String? = null
) : Effect {
    override val description: String = description_override ?: "You may ${effect.description.lowercase()}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * Represents a single mode in a modal spell.
 *
 * Each mode can have its own targeting requirements, allowing cards like
 * Cryptic Command where different modes need different targets.
 *
 * Modes can also carry per-mode costs for cards like Feed the Cycle
 * ("forage or pay {B}") where different modes have different costs.
 *
 * @property effect The effect when this mode is chosen
 * @property targetRequirements Targets required for this specific mode
 * @property description Human-readable description of the mode
 * @property additionalManaCost Extra mana added to the spell's cost when this mode is chosen (e.g., "{B}")
 * @property additionalCosts Per-mode additional costs (e.g., Forage). When non-null, overrides card-level additional costs.
 */
@Serializable
data class Mode(
    val effect: Effect,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val description: String = effect.description,
    val additionalManaCost: String? = null,
    val additionalCosts: List<AdditionalCost>? = null
) {
    companion object {
        /**
         * Create a mode with no targeting.
         */
        fun noTarget(effect: Effect, description: String = effect.description): Mode =
            Mode(effect, emptyList(), description)

        /**
         * Create a mode with a single target.
         */
        fun withTarget(effect: Effect, target: TargetRequirement, description: String = effect.description): Mode =
            Mode(effect, listOf(target), description)
    }
}

/**
 * Modal spell effect - choose one or more of several modes.
 * "Choose one — [Mode A] or [Mode B]"
 * "Choose two — [Mode A], [Mode B], [Mode C], or [Mode D]"
 * "Choose one or both — [Mode A], [Mode B]"
 * "Choose one or more —" (Escalate / Spree style, combined with [allowRepeat] on Spree)
 *
 * Each mode can have its own targeting requirements, which are combined
 * based on which modes are chosen when the spell is cast.
 *
 * Example (Cryptic Command):
 * ```kotlin
 * ModalEffect(
 *     modes = listOf(
 *         Mode.withTarget(CounterSpellEffect, TargetSpell(), "Counter target spell"),
 *         Mode.withTarget(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Hand), TargetPermanent(), "Return target permanent to its owner's hand"),
 *         Mode.noTarget(TapAllCreaturesEffect(CreatureGroupFilter.OpponentsControl), "Tap all creatures your opponents control"),
 *         Mode.noTarget(DrawCardsEffect(1), "Draw a card")
 *     ),
 *     chooseCount = 2
 * )
 * ```
 *
 * @property modes List of possible modes to choose from
 * @property chooseCount Maximum number of modes to choose (default 1)
 * @property minChooseCount Minimum number of modes to choose. Defaults to [chooseCount]
 *           (i.e. "choose exactly N"). Set lower for "choose one or both" / "choose one or more" (rules 700.2).
 * @property allowRepeat If true, the same mode index may be chosen more than once
 *           (rules 700.2d — Escalate/Spree-style).
 */
@SerialName("Modal")
@Serializable
data class ModalEffect(
    val modes: List<Mode>,
    val chooseCount: Int = 1,
    val minChooseCount: Int = chooseCount,
    val allowRepeat: Boolean = false,
    /**
     * If true, when this spell's `AdditionalCost.BlightOrPay` cost was paid via the
     * blight path, the effective number of modes the player must choose becomes
     * `modes.size` (i.e. all modes). When the blight path was not paid, the regular
     * [chooseCount]/[minChooseCount] apply. Models the "Choose one. If this spell's
     * additional cost was paid, choose both instead." pattern (e.g., Pyrrhic Strike).
     */
    val chooseAllIfBlightPaid: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Choose ")
        when {
            minChooseCount < chooseCount && chooseCount == 2 && modes.size == 2 -> append("one or both")
            minChooseCount < chooseCount -> append("one or more")
            else -> when (chooseCount) {
                1 -> append("one")
                2 -> append("two")
                3 -> append("three")
                else -> append(chooseCount)
            }
        }
        append(" —\n")
        modes.forEachIndexed { index, mode ->
            append("• ")
            append(mode.description)
            if (index < modes.lastIndex) append("\n")
        }
        if (allowRepeat) {
            append("\nYou may choose the same mode more than once.")
        }
        if (chooseAllIfBlightPaid) {
            append("\nIf this spell's additional cost was paid, choose all instead.")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var anyChanged = false
        val newModes = modes.map { mode ->
            val newEffect = mode.effect.applyTextReplacement(replacer)
            var modeChanged = newEffect !== mode.effect
            val newTargetReqs = mode.targetRequirements.map { tr ->
                val n = tr.applyTextReplacement(replacer); if (n !== tr) modeChanged = true; n
            }
            if (modeChanged) anyChanged = true
            if (modeChanged) mode.copy(effect = newEffect, targetRequirements = newTargetReqs) else mode
        }
        return if (anyChanged) copy(modes = newModes) else this
    }

    companion object {
        /**
         * Create a simple modal effect with effects that have no targeting.
         * Backwards compatible with the old List<Effect> pattern.
         */
        fun simple(effects: List<Effect>, chooseCount: Int = 1): ModalEffect =
            ModalEffect(effects.map { Mode.noTarget(it) }, chooseCount)

        /**
         * Create a choose-one modal effect.
         */
        fun chooseOne(vararg modes: Mode): ModalEffect =
            ModalEffect(modes.toList(), 1)

        /**
         * Create a choose-two modal effect (Cryptic Command style).
         */
        fun chooseTwo(vararg modes: Mode): ModalEffect =
            ModalEffect(modes.toList(), 2)
    }
}

/**
 * Effect with an optional cost - "You may [cost]. If you do, [ifPaid]."
 *
 * This is the fundamental building block for optional effects like:
 * - "You may pay {2}. If you do, draw a card."
 * - "You may sacrifice a creature. If you do, deal 3 damage to any target."
 * - "You may discard a card. If you do, draw two cards."
 *
 * @property cost The optional cost the player may pay (e.g., PayLifeEffect, SacrificeEffect)
 * @property ifPaid The effect that happens if the player pays the cost
 * @property ifNotPaid Optional effect if the player doesn't pay (usually null)
 */
@SerialName("OptionalCost")
@Serializable
data class OptionalCostEffect(
    val cost: Effect,
    val ifPaid: Effect,
    val ifNotPaid: Effect? = null,
    val descriptionOverride: String? = null
) : Effect {
    override val description: String = descriptionOverride ?: buildString {
        append("You may ${cost.description.replaceFirstChar { it.lowercase() }}. ")
        append("If you do, ${ifPaid.description.replaceFirstChar { it.lowercase() }}")
        if (ifNotPaid != null) {
            append(". Otherwise, ${ifNotPaid.description.replaceFirstChar { it.lowercase() }}")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newCost = cost.applyTextReplacement(replacer)
        val newIfPaid = ifPaid.applyTextReplacement(replacer)
        val newIfNotPaid = ifNotPaid?.applyTextReplacement(replacer)
        return if (newCost !== cost || newIfPaid !== ifPaid || newIfNotPaid !== ifNotPaid)
            copy(cost = newCost, ifPaid = newIfPaid, ifNotPaid = newIfNotPaid) else this
    }
}

/**
 * Reflexive trigger - "When you do, [effect]."
 *
 * Used for abilities that trigger from the resolution of another effect.
 * Example: Heart-Piercer Manticore - "You may sacrifice another creature.
 *          When you do, deal damage equal to that creature's power."
 *
 * @property action The optional action (sacrifice, discard, etc.)
 * @property optional Whether the action is optional
 * @property reflexiveEffect The effect that happens "when you do"
 * @property reflexiveTargetRequirements Target requirements for the reflexive effect,
 *           selected AFTER the action completes (not when the trigger goes on the stack).
 *           Use this for cards like Wick's Patrol where the target depends on what the action did.
 */
@SerialName("ReflexiveTrigger")
@Serializable
data class ReflexiveTriggerEffect(
    val action: Effect,
    val optional: Boolean = true,
    val reflexiveEffect: Effect,
    val reflexiveTargetRequirements: List<TargetRequirement> = emptyList(),
    /** Optional hint text shown on the yes/no decision (e.g., keyword reminder text) */
    val hint: String? = null,
    /**
     * Optional override for the auto-generated description. Useful when the action or
     * reflexive effect is a pipeline whose composed description reads poorly (e.g.
     * blight expanded to its Gather/Select/AddCounters internals).
     */
    val descriptionOverride: String? = null
) : Effect {
    override val description: String = descriptionOverride ?: buildString {
        if (optional) append("You may ")
        append(action.description.replaceFirstChar { it.lowercase() })
        append(". When you do, ")
        append(reflexiveEffect.description.replaceFirstChar { it.lowercase() })
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAction = action.applyTextReplacement(replacer)
        val newReflexiveEffect = reflexiveEffect.applyTextReplacement(replacer)
        return if (newAction !== action || newReflexiveEffect !== reflexiveEffect)
            copy(action = newAction, reflexiveEffect = newReflexiveEffect) else this
    }
}

/**
 * Generic "unless" effect for punisher mechanics.
 * "Do [suffer], unless you [cost]."
 *
 * This is a unified effect that handles:
 * - "Sacrifice this unless you discard a land card" (Thundering Wurm)
 * - "Sacrifice this unless you sacrifice three Forests" (Primeval Force)
 * - Similar punisher-style effects
 *
 * @property cost The cost that can be paid to avoid the consequence
 * @property suffer The consequence if the cost is not paid
 * @property player Who must make the choice (defaults to controller)
 */
@SerialName("PayOrSuffer")
@Serializable
data class PayOrSufferEffect(
    val cost: PayCost,
    val suffer: Effect,
    val player: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = "${suffer.description} unless you ${cost.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newCost = cost.applyTextReplacement(replacer)
        val newSuffer = suffer.applyTextReplacement(replacer)
        return if (newCost !== cost || newSuffer !== suffer)
            copy(cost = newCost, suffer = newSuffer) else this
    }
}

/**
 * Create a delayed triggered ability that fires at a specific step.
 *
 * When executed, bakes any context-dependent target references (ContextTarget)
 * into concrete SpecificEntity references so the delayed trigger fires correctly
 * even after the original execution context is gone.
 *
 * Used for Astral Slide-style exile-until-end-step effects:
 * ```kotlin
 * CompositeEffect(listOf(
 *     MoveToZoneEffect(ContextTarget(0), Zone.EXILE),
 *     CreateDelayedTriggerEffect(
 *         step = Step.END,
 *         effect = MoveToZoneEffect(ContextTarget(0), Zone.BATTLEFIELD)
 *     )
 * ))
 * ```
 *
 * @param step The step at which the delayed trigger fires
 * @param effect The effect to execute when the trigger fires
 * @param fireOnlyOnControllersTurn If true, the delayed trigger only fires when the active player is the controller
 */
@SerialName("CreateDelayedTrigger")
@Serializable
data class CreateDelayedTriggerEffect(
    val step: Step? = null,
    val effect: Effect,
    val fireOnlyOnControllersTurn: Boolean = false,
    /**
     * Event-based trigger. When non-null, the delayed trigger fires whenever
     * an event matching this TriggerSpec occurs (scoped to [watchedTarget] if set),
     * and remains resident until [expiry] removes it. When null, the delayed
     * trigger is step-based (fires at the beginning of [step]).
     */
    val trigger: TriggerSpec? = null,
    /**
     * For event-based delayed triggers: the entity that scopes the trigger.
     * The trigger only fires for events sourced from this entity. Context
     * references (e.g. ContextTarget(0)) are baked into a concrete entity id
     * at creation time by CreateDelayedTriggerExecutor.
     */
    val watchedTarget: EffectTarget? = null,
    /**
     * For event-based delayed triggers: when the ability is removed.
     */
    val expiry: DelayedTriggerExpiry = DelayedTriggerExpiry.EndOfTurn,
    /**
     * If true, applies "your next end step" timing semantics: the trigger fires at
     * the next upcoming end step on the controller's turn. If the controller's
     * current-turn end step hasn't started yet, the trigger fires this turn; only if
     * it's already started (END or CLEANUP step on the controller's turn) is the
     * trigger deferred to the controller's following turn. Typically combined with
     * [fireOnlyOnControllersTurn] = true.
     */
    val onControllerNextTurn: Boolean = false
) : Effect {
    override val description: String = when {
        trigger != null -> "create a delayed trigger that fires on ${trigger.event::class.simpleName}"
        step != null -> "create a delayed trigger at the beginning of the next ${step.displayName}"
        else -> "create a delayed trigger"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * Expiry rule for an event-based delayed triggered ability.
 */
@Serializable
sealed interface DelayedTriggerExpiry {
    /** Remove the delayed trigger at the end of the current turn. */
    @SerialName("DelayedTriggerExpiry.EndOfTurn")
    @Serializable
    data object EndOfTurn : DelayedTriggerExpiry
}

/**
 * Effect that stores the result of executing an inner effect.
 *
 * This enables Oblivion Ring-style effects where the first trigger
 * needs to remember which card it exiled so the second trigger
 * can return it.
 *
 * @param effect The effect to execute
 * @param storeAs The variable to store the result in
 */
@SerialName("StoreResult")
@Serializable
data class StoreResultEffect(
    val effect: Effect,
    val storeAs: EffectVariable
) : Effect {
    override val description: String = "${effect.description} (stored as ${storeAs.name})"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * Effect that stores a count from the result of executing an effect.
 *
 * Used for variable-count effects like Scapeshift:
 * "Sacrifice any number of lands. Search for that many land cards."
 *
 * @param effect The effect to execute (typically a sacrifice or similar)
 * @param storeAs The count variable to store the number in
 */
@SerialName("StoreCount")
@Serializable
data class StoreCountEffect(
    val effect: Effect,
    val storeAs: EffectVariable.Count
) : Effect {
    override val description: String = "${effect.description} (count stored as ${storeAs.name})"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * Blight effect - "may blight N. If you do, [effect]"
 * Blight N means "put N -1/-1 counters on a creature you control".
 * This is an optional cost-gated effect used in triggered abilities.
 *
 * The player may choose a creature they control to blight. If they do,
 * the inner effect happens. If they don't (or can't), nothing happens.
 *
 * @property blightAmount Number of -1/-1 counters to place
 * @property innerEffect The effect that happens if the player blights
 * @property targetId The creature chosen to receive the counters (filled in during resolution)
 */
@SerialName("Blight")
@Serializable
data class BlightEffect(
    val blightAmount: Int,
    val innerEffect: Effect,
    val targetId: EntityId? = null
) : Effect {
    override val description: String = buildString {
        append("You may blight $blightAmount. If you do, ")
        append(innerEffect.description.replaceFirstChar { it.lowercase() })
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newInnerEffect = innerEffect.applyTextReplacement(replacer)
        return if (newInnerEffect !== innerEffect) copy(innerEffect = newInnerEffect) else this
    }
}

/**
 * "May tap another untapped creature you control. If you do, [effect]."
 * This is an optional cost-gated effect - the player may pay the cost to get the effect.
 *
 * @property innerEffect The effect that happens if the player pays the tap cost
 * @property targetId The creature chosen to tap (filled in during resolution)
 */
@SerialName("TapCreatureForEffect")
@Serializable
data class TapCreatureForEffectEffect(
    val innerEffect: Effect,
    val targetId: EntityId? = null
) : Effect {
    override val description: String = buildString {
        append("You may tap another untapped creature you control. If you do, ")
        append(innerEffect.description.replaceFirstChar { it.lowercase() })
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newInnerEffect = innerEffect.applyTextReplacement(replacer)
        return if (newInnerEffect !== innerEffect) copy(innerEffect = newInnerEffect) else this
    }
}

/**
 * "You may pay [manaCost]. If you do, [effect]."
 *
 * Optional mana payment during resolution. The controller may pay a mana cost
 * (auto-tapping lands if needed). If they pay, the inner effect is executed.
 * If they can't pay or decline, nothing happens.
 *
 * Example: Lightning Rift - "you may pay {1}. If you do, Lightning Rift deals 2 damage to any target."
 *
 * @property cost The mana cost the player may pay
 * @property effect The effect that happens if the player pays
 */
@SerialName("MayPayMana")
@Serializable
data class MayPayManaEffect(
    val cost: ManaCost,
    val effect: Effect
) : Effect {
    override val description: String = "You may pay $cost. If you do, ${effect.description.replaceFirstChar { it.lowercase() }}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * "You may pay {X}. If you do, [effect]."
 *
 * Presents the player with a number chooser (0 to max affordable mana).
 * If X > 0, pays X mana (auto-tapping lands) and executes the inner effect
 * with the chosen X value set in the effect context.
 *
 * Example: Decree of Justice cycling trigger - "you may pay {X}. If you do,
 * create X 1/1 white Soldier creature tokens."
 *
 * @property effect The effect that happens if the player pays (uses DynamicAmount.XValue)
 */
@SerialName("MayPayX")
@Serializable
data class MayPayXForEffect(
    val effect: Effect
) : Effect {
    override val description: String = "You may pay {X}. If you do, ${effect.description.replaceFirstChar { it.lowercase() }}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * "Any player may [cost]. If a player does, [consequence]."
 *
 * Each player in APNAP order gets the chance to pay the cost. As soon as
 * any player does, the consequence is executed and no further players are asked.
 * If no player pays, nothing happens.
 *
 * Example: Prowling Pangolin - "Any player may sacrifice two creatures.
 * If a player does, sacrifice Prowling Pangolin."
 *
 * @property cost The cost any player may choose to pay
 * @property consequence The effect that happens if a player pays
 */
@SerialName("AnyPlayerMayPay")
@Serializable
data class AnyPlayerMayPayEffect(
    val cost: PayCost,
    val consequence: Effect
) : Effect {
    override val description: String =
        "Any player may ${cost.description}. If a player does, ${consequence.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newCost = cost.applyTextReplacement(replacer)
        val newConsequence = consequence.applyTextReplacement(replacer)
        return if (newCost !== cost || newConsequence !== consequence)
            copy(cost = newCost, consequence = newConsequence) else this
    }
}

/**
 * Execute a list of sub-effects once for each target in the context.
 *
 * For each target, the sub-effects receive a context with only that one target
 * as ContextTarget(0), plus fresh storedCollections. This enables cards like Kaboom!
 * that repeat a pipeline per target.
 *
 * @property effects The sub-effects to execute for each target
 */
@SerialName("ForEachTarget")
@Serializable
data class ForEachTargetEffect(
    val effects: List<Effect>
) : Effect {
    override val description: String = buildString {
        append("For each target, ")
        append(effects.joinToString(". ") { it.description.replaceFirstChar { c -> c.lowercase() } })
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var changed = false
        val newEffects = effects.map { val n = it.applyTextReplacement(replacer); if (n !== it) changed = true; n }
        return if (changed) copy(effects = newEffects) else this
    }
}

/**
 * Execute a list of sub-effects once for each player matching the player selector.
 *
 * For each player, the sub-effects receive a context with `controllerId` set to
 * that player (so `Player.You` resolves to the current iteration's player),
 * plus fresh storedCollections. This enables cards like Winds of Change
 * that repeat a pipeline per player.
 *
 * @property players Which players to iterate over (e.g., Player.Each)
 * @property effects The sub-effects to execute for each player
 */
@SerialName("ForEachPlayer")
@Serializable
data class ForEachPlayerEffect(
    val players: Player,
    val effects: List<Effect>
) : Effect {
    override val description: String = buildString {
        append("For each player, ")
        append(effects.joinToString(". ") { it.description.replaceFirstChar { c -> c.lowercase() } })
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var changed = false
        val newEffects = effects.map { val n = it.applyTextReplacement(replacer); if (n !== it) changed = true; n }
        return if (changed) copy(effects = newEffects) else this
    }
}

/**
 * Flip a coin. Execute one effect if you win, another if you lose.
 * "Flip a coin. If you win the flip, [wonEffect]. If you lose the flip, [lostEffect]."
 *
 * Used for cards like Skittish Valesk, Goblin Bomb, Mana Clash, etc.
 *
 * @property wonEffect The effect that happens if the player wins the flip (null = nothing)
 * @property lostEffect The effect that happens if the player loses the flip (null = nothing)
 */
@SerialName("FlipCoin")
@Serializable
data class FlipCoinEffect(
    val wonEffect: Effect? = null,
    val lostEffect: Effect? = null
) : Effect {
    override val description: String = buildString {
        append("Flip a coin.")
        if (wonEffect != null) {
            append(" If you win the flip, ${wonEffect.description.replaceFirstChar { it.lowercase() }}.")
        }
        if (lostEffect != null) {
            append(" If you lose the flip, ${lostEffect.description.replaceFirstChar { it.lowercase() }}.")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newWon = wonEffect?.applyTextReplacement(replacer)
        val newLost = lostEffect?.applyTextReplacement(replacer)
        return if (newWon !== wonEffect || newLost !== lostEffect)
            copy(wonEffect = newWon, lostEffect = newLost) else this
    }
}

/**
 * Flip two coins. Execute different effects based on the combined outcome.
 * "Flip two coins. If both are heads, [bothHeadsEffect]. If both are tails, [bothTailsEffect]."
 *
 * Used for cards like Two-Headed Giant where two coins determine the outcome.
 *
 * @property bothHeadsEffect Effect if both coins are heads (null = nothing)
 * @property bothTailsEffect Effect if both coins are tails (null = nothing)
 * @property mixedEffect Effect if one head and one tail (null = nothing, most common)
 */
@SerialName("FlipTwoCoins")
@Serializable
data class FlipTwoCoinsEffect(
    val bothHeadsEffect: Effect? = null,
    val bothTailsEffect: Effect? = null,
    val mixedEffect: Effect? = null
) : Effect {
    override val description: String = buildString {
        append("Flip two coins.")
        if (bothHeadsEffect != null) {
            append(" If both coins come up heads, ${bothHeadsEffect.description.replaceFirstChar { it.lowercase() }}.")
        }
        if (bothTailsEffect != null) {
            append(" If both coins come up tails, ${bothTailsEffect.description.replaceFirstChar { it.lowercase() }}.")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newHeads = bothHeadsEffect?.applyTextReplacement(replacer)
        val newTails = bothTailsEffect?.applyTextReplacement(replacer)
        val newMixed = mixedEffect?.applyTextReplacement(replacer)
        return if (newHeads !== bothHeadsEffect || newTails !== bothTailsEffect || newMixed !== mixedEffect)
            copy(bothHeadsEffect = newHeads, bothTailsEffect = newTails, mixedEffect = newMixed) else this
    }
}

// =============================================================================
// Budget Modal (Pawprint / Season Cycle)
// =============================================================================

/**
 * Represents a single mode in a budget modal spell.
 *
 * Each mode has a cost (in "pawprints" or other currency) and an effect.
 * Players can select modes multiple times as long as the total cost doesn't
 * exceed the budget.
 *
 * @property cost The cost of this mode (e.g., 1, 2, or 3 pawprints)
 * @property effect The effect when this mode is chosen
 * @property description Human-readable description of the mode
 */
@Serializable
data class BudgetMode(
    val cost: Int,
    val effect: Effect,
    val description: String = effect.description
) {
    fun applyTextReplacement(replacer: TextReplacer): BudgetMode {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * Budget-based modal spell effect — "Choose up to N {P} worth of modes."
 *
 * Used by the Bloomburrow Season cycle (Season of Loss, Season of the Bold, etc.)
 * where each mode has a different cost and the player distributes a budget among them.
 * The same mode can be chosen multiple times. Modes execute in printed order.
 *
 * Example (Season of Loss):
 * ```kotlin
 * BudgetModalEffect(
 *     budget = 5,
 *     modes = listOf(
 *         BudgetMode(1, eachPlayerSacrificesCreature, "Each player sacrifices a creature"),
 *         BudgetMode(2, drawForDeadCreatures, "Draw a card for each creature that died this turn"),
 *         BudgetMode(3, opponentLosesLifeForGraveyard, "Each opponent loses X life")
 *     )
 * )
 * ```
 *
 * @property budget The total budget available (e.g., 5 pawprints)
 * @property modes The modes with their costs and effects
 */
@SerialName("BudgetModal")
@Serializable
data class BudgetModalEffect(
    val budget: Int,
    val modes: List<BudgetMode>
) : Effect {
    override val description: String = buildString {
        append("Choose up to $budget worth of modes:\n")
        modes.forEachIndexed { index, mode ->
            append("• (${mode.cost}) ${mode.description}")
            if (index < modes.lastIndex) append("\n")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var anyChanged = false
        val newModes = modes.map { mode ->
            val newMode = mode.applyTextReplacement(replacer)
            if (newMode !== mode) anyChanged = true
            newMode
        }
        return if (anyChanged) copy(modes = newModes) else this
    }
}

// =============================================================================
// Repeat Conditions
// =============================================================================

/**
 * Determines whether a [RepeatWhileEffect] should repeat after each body execution.
 *
 * Variants:
 * - [PlayerChooses]: Interactive — a player decides yes/no each iteration.
 * - [WhileCondition]: Synchronous — checks a game-state [Condition].
 */
@Serializable
sealed interface RepeatCondition {

    /**
     * A player decides each iteration whether to repeat.
     *
     * @property decider Which player makes the choice (resolved from EffectTarget)
     * @property prompt Text shown in the yes/no decision
     * @property yesText Label for the "repeat" button
     * @property noText Label for the "stop" button
     */
    @SerialName("PlayerChooses")
    @Serializable
    data class PlayerChooses(
        val decider: EffectTarget,
        val prompt: String,
        val yesText: String = "Repeat",
        val noText: String = "Stop"
    ) : RepeatCondition

    /**
     * Repeat while a game-state condition is true (checked after each body execution).
     *
     * @property condition The condition to evaluate
     */
    @SerialName("WhileCondition")
    @Serializable
    data class WhileCondition(
        val condition: Condition
    ) : RepeatCondition
}

/**
 * Repeat a body effect in a do-while loop controlled by a [RepeatCondition].
 *
 * The body executes at least once. After each iteration, the repeat condition
 * is evaluated:
 * - [RepeatCondition.PlayerChooses]: pauses for a yes/no decision
 * - [RepeatCondition.WhileCondition]: evaluates synchronously
 *
 * Example (Trade Secrets):
 * ```kotlin
 * RepeatWhileEffect(
 *     body = CompositeEffect(listOf(
 *         DrawCardsEffect(2, EffectTarget.ContextTarget(0)),
 *         DrawUpToEffect(4, EffectTarget.Controller)
 *     )),
 *     repeatCondition = RepeatCondition.PlayerChooses(
 *         decider = EffectTarget.ContextTarget(0),
 *         prompt = "Repeat the process? (You draw 2 cards, opponent draws up to 4)"
 *     )
 * )
 * ```
 *
 * @property body The effect to execute each iteration
 * @property repeatCondition Determines whether to repeat after each body execution
 */
@SerialName("RepeatWhile")
@Serializable
data class RepeatWhileEffect(
    val body: Effect,
    val repeatCondition: RepeatCondition
) : Effect {
    override val description: String = buildString {
        append(body.description)
        append(". Repeat this process")
        when (repeatCondition) {
            is RepeatCondition.PlayerChooses -> append(" as many times as ${repeatCondition.decider.description} chooses")
            is RepeatCondition.WhileCondition -> append(" while ${repeatCondition.condition.description}")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newBody = body.applyTextReplacement(replacer)
        return if (newBody !== body) copy(body = newBody) else this
    }
}

// =============================================================================
// Repeat Dynamic Times
// =============================================================================

/**
 * Repeat a body effect N times, where N is determined by a [DynamicAmount].
 *
 * The amount is evaluated once when the effect starts executing. Then the body
 * is executed that many times in sequence. If any iteration pauses for a decision,
 * remaining iterations resume via the standard [CompositeEffect] continuation.
 *
 * Example (Rottenmouth Viper - for each blight counter):
 * ```kotlin
 * RepeatDynamicTimesEffect(
 *     amount = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named("blight")),
 *     body = ForEachPlayerEffect(
 *         players = Player.EachOpponent,
 *         effects = listOf(chooseAction)
 *     )
 * )
 * ```
 *
 * @property amount How many times to repeat (evaluated once at execution start)
 * @property body The effect to execute each iteration
 */
@SerialName("RepeatDynamicTimes")
@Serializable
data class RepeatDynamicTimesEffect(
    val amount: DynamicAmount,
    val body: Effect
) : Effect {
    override val description: String = "Repeat ${body.description} a number of times"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newBody = body.applyTextReplacement(replacer)
        return if (newBody !== body) copy(body = newBody) else this
    }
}

// =============================================================================
// Choose Action (Player Chooses Between Effects)
// =============================================================================

/**
 * Check whether a choice is feasible for the choosing player.
 *
 * Used by [ChooseActionEffect] to filter out options the player cannot fulfill.
 * For example, "sacrifice a nonland permanent" is infeasible if the player controls none.
 */
@Serializable
sealed interface FeasibilityCheck {

    /**
     * The player controls at least [count] permanents matching [filter].
     */
    @SerialName("ControlsPermanentMatching")
    @Serializable
    data class ControlsPermanentMatching(
        val filter: GameObjectFilter,
        val count: Int = 1
    ) : FeasibilityCheck

    /**
     * The player has at least [count] cards in [zone] matching [filter].
     */
    @SerialName("HasCardsInZone")
    @Serializable
    data class HasCardsInZone(
        val zone: Zone,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1
    ) : FeasibilityCheck
}

/**
 * A labeled option in a [ChooseActionEffect].
 *
 * @property label Human-readable label shown in the UI (e.g., "Sacrifice a nonland permanent")
 * @property effect The effect executed when this option is chosen
 * @property feasibilityCheck Optional check — if non-null and the check fails, this option is hidden
 */
@Serializable
data class EffectChoice(
    val label: String,
    val effect: Effect,
    val feasibilityCheck: FeasibilityCheck? = null
) {
    fun applyTextReplacement(replacer: TextReplacer): EffectChoice {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * Present a player with labeled options and execute the chosen effect.
 *
 * This is a reusable "choose one action" pattern for punisher effects,
 * opponent choices, and any card where a player picks between distinct effects.
 *
 * Infeasible options (per [FeasibilityCheck]) are filtered out at execution time.
 * If only one option remains, it is auto-selected. If zero remain, nothing happens.
 *
 * Example (Thornplate Intimidator):
 * ```kotlin
 * ChooseActionEffect(
 *     choices = listOf(
 *         EffectChoice("Sacrifice a nonland permanent", ForceSacrificeEffect(...),
 *             FeasibilityCheck.ControlsPermanentMatching(GameObjectFilter.NonlandPermanent)),
 *         EffectChoice("Discard a card", discardPipeline,
 *             FeasibilityCheck.HasCardsInZone(Zone.HAND)),
 *         EffectChoice("Lose 3 life", LoseLifeEffect(3, target))
 *     ),
 *     player = EffectTarget.ContextTarget(0)  // opponent chooses
 * )
 * ```
 *
 * @property choices The labeled options to present
 * @property player Who makes the choice (defaults to controller)
 */
@SerialName("ChooseAction")
@Serializable
data class ChooseActionEffect(
    val choices: List<EffectChoice>,
    val player: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = buildString {
        append("Choose one —\n")
        choices.forEachIndexed { index, choice ->
            append("• ")
            append(choice.label)
            if (index < choices.lastIndex) append("\n")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var anyChanged = false
        val newChoices = choices.map { choice ->
            val newChoice = choice.applyTextReplacement(replacer)
            if (newChoice !== choice) anyChanged = true
            newChoice
        }
        return if (anyChanged) copy(choices = newChoices) else this
    }
}
