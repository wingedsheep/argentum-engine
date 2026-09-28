package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.Serializable

/**
 * A triggered ability is an ability that fires when a specific condition is met.
 * It combines a trigger condition with an effect.
 *
 * Triggered abilities can optionally require targets. When a triggered ability
 * has a targetRequirement, the player must choose valid targets when the ability
 * goes on the stack. If no legal targets exist, the ability is removed from
 * the stack without resolving.
 */
@Serializable
data class TriggeredAbility(
    val id: AbilityId,
    val trigger: EventPattern,
    val binding: TriggerBinding = TriggerBinding.SELF,
    /**
     * What the ability does.
     *
     * **A printed "you may" lives here, as a [com.wingedsheep.sdk.scripting.effects.Gate.MayDecide]
     * around the effect** — there is no `optional` flag beside it. There used to be one, and it was
     * the same fact written twice: the engine read it and *built* this gate before the ability
     * reached the stack, so every game lowered one spelling into the other. The DSL still writes
     * `optional = true` (see `TriggeredAbilityBuilder.optional`); that is a shorthand the builder
     * lowers, not a second field on the model.
     */
    val effect: Effect,
    val targetRequirement: TargetRequirement? = null,
    /** Additional target requirements for multi-target triggered abilities (e.g., exchange control). */
    val additionalTargetRequirements: List<TargetRequirement> = emptyList(),
    /**
     * The branch taken when target selection produces nothing — a declined "up to N" slot, or a
     * mandatory slot with no legal target (CR 603.3d).
     *
     * Not the same position as a consent gate's `otherwise`, which is the branch taken when the
     * controller *declines* at resolution. A "you may … If you don't, …" ability carries its else
     * inside the gate; this field is the announcement-time one.
     */
    val elseEffect: Effect? = null,
    /**
     * The zones this ability's trigger condition functions in (CR 113.6b — "an ability that states
     * which zones it functions in functions only from those zones"). Defaults to the battlefield,
     * which CR 113.6 makes the rule for a permanent card's abilities.
     *
     * A *set* rather than a single zone because CR 113.6k allows one triggered ability to function
     * from several zones at once. Two shapes need it today: a graveyard/exile-resident ability
     * (`{GRAVEYARD}`, `{EXILE}` — Pyre Zombie, suspend, madness) and an *eminence* ability, which
     * functions from the command zone **and** the battlefield (`{BATTLEFIELD, COMMAND}` — Edgar
     * Markov). The detector scans each declared zone independently, so a card sitting in exactly
     * one of them fires exactly once.
     */
    val activeZones: Set<Zone> = setOf(Zone.BATTLEFIELD),
    /**
     * The intervening-"if" clause of CR 603.4 — an `if` that *immediately follows the trigger
     * event*: "When/Whenever/At [event], **if** [condition], [effect]."
     *
     * **It is checked twice.** Once when the trigger event occurs, and the ability triggers only
     * if it is true then; and again as the ability resolves, where a false condition removes the
     * ability from the stack and it does nothing. CR 603.4 calls the second check a mirror of the
     * check for legal targets, and [com.wingedsheep.engine.mechanics.stack.StackResolver] performs
     * it in the same place, emitting an `AbilityFizzledEvent`. The two are ordered: CR 608.2a runs
     * this check **before** CR 608.2b's target legality, so an ability whose condition has gone
     * false is removed from the stack whether or not its targets are still legal.
     *
     * The rule's own parenthetical is the boundary: *"the word 'if' has only its normal English
     * meaning anywhere else in the text of a card; this rule only applies to an 'if' that
     * immediately follows a trigger condition."* So an `if` printed **after** the effect —
     * "Whenever this creature attacks, create a token *if* you control a creature with power 4 or
     * greater" — is not this field. That ability triggers unconditionally and checks only as it
     * resolves, which is a [com.wingedsheep.sdk.dsl.Effects.If], not a
     * condition on the trigger.
     *
     * For a restriction on *when the ability triggers at all*, use [triggerRestriction]. The two
     * are separate fields rather than one flagged condition because there is no safe default: a
     * restriction re-checked on resolution fizzles abilities that should resolve, and an
     * intervening-"if" not re-checked resolves abilities that should do nothing.
     */
    val interveningIf: Condition? = null,
    /**
     * A CR 603.2 restriction on the ability's *trigger event*, checked when the trigger would fire
     * and **never again**. Distinct from [interveningIf], which CR 603.4 checks a second time.
     *
     * Three printed shapes land here:
     *
     *  - a **"while"** clause — "Whenever this creature attacks **while you control a Dinosaur**",
     *    "Whenever this creature attacks **while saddled**", "When this creature dies **while its
     *    power is 4 or greater**". The condition qualifies the event, not the resolution: a
     *    Dinosaur that leaves in response does not stop the pump.
     *  - a **"during"** clause or any other adverbial narrowing of the event — "Whenever you cast
     *    a spell **during an opponent's turn**", "Whenever a card leaves your graveyard **during
     *    your turn**".
     *  - a gate that no printed word states because it belongs to a **mechanic**: whether an
     *    Offspring cost was paid, which mode a card was cast for, whether a Spacecraft has reached
     *    its Station threshold, whether a Class has the level whose ability this is.
     */
    val triggerRestriction: Condition? = null,
    /** When true, the triggered ability is controlled by the triggering entity's controller
     * instead of the source permanent's controller. Used for cards like Death Match. */
    val controlledByTriggeringEntityController: Boolean = false,
    /** When true, this triggered ability triggers at most once each turn.
     * Used for cards like Scavenger's Talent: "This ability triggers only once each turn."
     *
     * This cap is spent by the **first trigger**, whether or not anything came of it: later
     * matching events in the same turn don't trigger at all. For the *other* printed rider,
     * "Do this only once each turn", use [effectOncePerTurn]. */
    val oncePerTurn: Boolean = false,
    /**
     * When true, this ability carries the printed rider "*Do this only once each turn*" (Jennifer
     * Walters // The Sensational She-Hulk, Baron Strucker, HYDRA Overlord).
     *
     * **CR 603.2h:** *"A triggered ability may have an instruction followed by 'Do this only once
     * each turn.' This ability triggers only if its source's controller has not yet taken the
     * indicated action that turn."* So the rider is a stateful **trigger condition keyed to the
     * action**, not a cap on how often the ability may be put on the stack:
     *
     *  - While the action is untaken, **every** matching event triggers its own instance — a
     *    multi-block puts one instance on the stack per damaged creature, one per Villain entering.
     *  - The choice is made as an instance *resolves* (Legolas, Counter of Kills ruling), and
     *    taking the action there spends the turn's single use.
     *  - Instances still on the stack afterwards **do nothing as they resolve**, and no further
     *    matching event triggers the ability for the rest of the turn (Nykthos Paragon / Riveteers
     *    Ascendancy rulings).
     *  - **Declining does not spend it** — the engine lowers this flag into a
     *    [com.wingedsheep.sdk.scripting.effects.Gate.OnceEachTurn] gate placed *inside* the consent
     *    gate, so only an action actually taken counts.
     *
     * The budget is per (source permanent, ability): two copies of the permanent each get their own.
     *
     * Do not model this wording with [oncePerTurn]: a trigger cap is spent by the first trigger
     * even when the player declines, which makes "decline down to the biggest damage number" (or
     * "pick which Villain connives") unreachable.
     *
     * **Keep the consent gate outermost or last — this is enforced.** The lowering looks for the
     * consent gate — a `Effects.May` / `mayPay` / `mayPayX` — at the top of [effect] or at the
     * **tail** of a `CompositeEffect`, which covers "do X, then you may Y" ("look at the top card
     * of your library. You may cast that card …", Planetarium of Wan Shi Tong). A "you may" sitting
     * anywhere else — mid-composite, or under some other wrapper — would leave the budget gate on
     * the outside, so declining would spend the turn's use. Rather than mis-place it silently the
     * lowering throws, and `EffectOncePerTurnLoweringTest` sweeps the whole card pool for the shape
     * so the failure lands at build time rather than mid-game.
     */
    val effectOncePerTurn: Boolean = false,
    /** When true, this triggered ability triggers at most once over the source permanent's
     * lifetime on the battlefield — a permanent (not per-turn) cap. Used for cards like
     * Acrobatic Cheerleader: "This ability triggers only once." Tracked by a component that,
     * unlike the [oncePerTurn] tracker, is NOT cleared at end of turn. */
    val triggersOnce: Boolean = false,
    /**
     * True for a *backup* ability (March of the Machine; CR 702.165). "Backup N" is an enters
     * trigger — put N +1/+1 counters on target creature; if that's another creature, it gains the
     * printed abilities below backup until end of turn — and the card composes that effect like
     * any other trigger. This flag is only the keyword marker, the triggered twin of
     * [ActivatedAbility.isBoast]: it travels with the ability onto the stack so a trigger can ask
     * whether *a backup ability* did the targeting (Mirror-Shield Hoplite's
     * [EventPattern.BecomesTargetEvent.backupAbilitiesOnly]). A copy of a backup ability is a
     * backup ability too (CR 707.2 — the copy copies the ability's text).
     */
    val isBackup: Boolean = false,
    /** Optional human-readable description that overrides the auto-generated one. */
    val descriptionOverride: String? = null
) : TextReplaceable<TriggeredAbility> {
    /**
     * Every condition checked *when the trigger event occurs* — both kinds, since CR 603.2 and
     * CR 603.4 agree on the first check and differ only on whether there is a second one. This is
     * what the trigger detector filters on; [interveningIf] alone is what the stack resolver
     * re-checks.
     *
     * It is a read-only derivation on purpose. The two halves are set separately, so a card that
     * means "if" can no longer reach the trigger-time-only path by writing to the name it used to
     * share with "while".
     */
    val triggerCondition: Condition?
        get() = when {
            interveningIf == null -> triggerRestriction
            triggerRestriction == null -> interveningIf
            else -> AllConditions(listOf(interveningIf, triggerRestriction))
        }

    /** All target requirements for this ability (primary + additional). */
    val allTargetRequirements: List<TargetRequirement>
        get() = listOfNotNull(targetRequirement) + additionalTargetRequirements

    val description: String
        get() = descriptionOverride ?: buildString {
            append(trigger.description)
            triggerCondition?.let {
                append(", ")
                append(it.description)
            }
            append(", ")
            if (targetRequirement != null) {
                append(targetRequirement.description)
                append(" ")
            }
            append(effect.description.replaceFirstChar { it.lowercase() })
            if (elseEffect != null) {
                append(". If you don't, ")
                append(elseEffect.description.replaceFirstChar { it.lowercase() })
            }
            append(".")
            if (effectOncePerTurn) append(" Do this only once each turn.")
        }

    /** Whether this triggered ability requires targets */
    val requiresTargets: Boolean
        get() = targetRequirement != null

    override fun applyTextReplacement(replacer: TextReplacer): TriggeredAbility {
        val newTrigger = trigger.applyTextReplacement(replacer)
        val newEffect = effect.applyTextReplacement(replacer)
        val newTargetReq = targetRequirement?.applyTextReplacement(replacer)
        var addlChanged = false
        val newAddlTargetReqs = additionalTargetRequirements.map {
            val n = it.applyTextReplacement(replacer)
            if (n !== it) addlChanged = true
            n
        }
        val newElseEffect = elseEffect?.applyTextReplacement(replacer)
        val newInterveningIf = interveningIf?.applyTextReplacement(replacer)
        val newRestriction = triggerRestriction?.applyTextReplacement(replacer)
        return if (newTrigger !== trigger || newEffect !== effect ||
                   newTargetReq !== targetRequirement || addlChanged ||
                   newElseEffect !== elseEffect || newInterveningIf !== interveningIf ||
                   newRestriction !== triggerRestriction)
            copy(trigger = newTrigger, effect = newEffect,
                 targetRequirement = newTargetReq,
                 additionalTargetRequirements = newAddlTargetReqs,
                 elseEffect = newElseEffect,
                 interveningIf = newInterveningIf,
                 triggerRestriction = newRestriction) else this
    }

    companion object {
        /**
         * @param id Minted from the card being built by default; code that synthesizes a trigger
         *   with no card around it (the engine's persist, delayed and saga-chapter triggers) names one.
         */
        fun create(
            trigger: EventPattern,
            binding: TriggerBinding = TriggerBinding.SELF,
            effect: Effect,
            targetRequirement: TargetRequirement? = null,
            additionalTargetRequirements: List<TargetRequirement> = emptyList(),
            elseEffect: Effect? = null,
            activeZones: Set<Zone> = setOf(Zone.BATTLEFIELD),
            interveningIf: Condition? = null,
            triggerRestriction: Condition? = null,
            controlledByTriggeringEntityController: Boolean = false,
            oncePerTurn: Boolean = false,
            effectOncePerTurn: Boolean = false,
            triggersOnce: Boolean = false,
            isBackup: Boolean = false,
            descriptionOverride: String? = null,
            id: AbilityId = AbilityId.next(),
        ): TriggeredAbility =
            TriggeredAbility(
                id = id,
                trigger = trigger,
                binding = binding,
                effect = effect,
                targetRequirement = targetRequirement,
                additionalTargetRequirements = additionalTargetRequirements,
                elseEffect = elseEffect,
                activeZones = activeZones,
                interveningIf = interveningIf,
                triggerRestriction = triggerRestriction,
                controlledByTriggeringEntityController = controlledByTriggeringEntityController,
                oncePerTurn = oncePerTurn,
                effectOncePerTurn = effectOncePerTurn,
                triggersOnce = triggersOnce,
                isBackup = isBackup,
                descriptionOverride = descriptionOverride
            )

        /**
         * [create] from a whole [TriggerSpec] — the form a card uses, since a card spells its trigger
         * with `Triggers.<subject>.<verb>()` and the subject already fixes the binding.
         */
        fun create(
            trigger: TriggerSpec,
            effect: Effect,
            targetRequirement: TargetRequirement? = null,
            additionalTargetRequirements: List<TargetRequirement> = emptyList(),
            elseEffect: Effect? = null,
            activeZones: Set<Zone> = setOf(Zone.BATTLEFIELD),
            interveningIf: Condition? = null,
            triggerRestriction: Condition? = null,
            controlledByTriggeringEntityController: Boolean = false,
            oncePerTurn: Boolean = false,
            effectOncePerTurn: Boolean = false,
            triggersOnce: Boolean = false,
            isBackup: Boolean = false,
            descriptionOverride: String? = null,
            id: AbilityId = AbilityId.next(),
        ): TriggeredAbility = create(
            trigger = trigger.event,
            binding = trigger.binding,
            effect = effect,
            targetRequirement = targetRequirement,
            additionalTargetRequirements = additionalTargetRequirements,
            elseEffect = elseEffect,
            activeZones = activeZones,
            interveningIf = interveningIf,
            triggerRestriction = triggerRestriction,
            controlledByTriggeringEntityController = controlledByTriggeringEntityController,
            oncePerTurn = oncePerTurn,
            effectOncePerTurn = effectOncePerTurn,
            triggersOnce = triggersOnce,
            isBackup = isBackup,
            descriptionOverride = descriptionOverride,
            id = id,
        )
    }
}
