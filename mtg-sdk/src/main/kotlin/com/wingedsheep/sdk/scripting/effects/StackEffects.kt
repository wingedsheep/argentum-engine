package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Stack Effects — Counter
// =============================================================================

/**
 * What kind of stack object to counter.
 */
@Serializable
sealed interface CounterTarget {
    /** Counter a spell (goes to graveyard/exile). */
    @SerialName("CounterTarget.Spell")
    @Serializable
    data object Spell : CounterTarget

    /** Counter an activated or triggered ability (removed from stack, no zone change). */
    @SerialName("CounterTarget.Ability")
    @Serializable
    data object Ability : CounterTarget

    /**
     * Counter a spell or an activated/triggered ability. The executor dispatches at
     * resolution: if the stack entity has a [com.wingedsheep.engine.state.components.stack.SpellOnStackComponent]
     * it is countered as a spell (subject to [CounterDestination]); otherwise it is
     * countered as an ability. Used by cards like Teferi's Response.
     */
    @SerialName("CounterTarget.SpellOrAbility")
    @Serializable
    data object SpellOrAbility : CounterTarget
}

/**
 * How the target of the counter is identified.
 */
@Serializable
sealed interface CounterTargetSource {
    /** Uses a chosen target from context.targets (normal targeting). */
    @SerialName("CounterTargetSource.Chosen")
    @Serializable
    data object Chosen : CounterTargetSource

    /** Uses context.triggeringEntityId (for triggered abilities like Decree of Silence). */
    @SerialName("CounterTargetSource.TriggeringEntity")
    @Serializable
    data object TriggeringEntity : CounterTargetSource
}

/**
 * Where the countered spell goes.
 */
@Serializable
sealed interface CounterDestination {
    /** Spell goes to owner's graveyard (default). */
    @SerialName("CounterDestination.Graveyard")
    @Serializable
    data object Graveyard : CounterDestination

    /**
     * Spell is exiled instead of going to graveyard.
     * @property grantFreeCast If true, the controller may cast the exiled card
     *   without paying its mana cost for as long as it remains exiled.
     */
    @SerialName("CounterDestination.Exile")
    @Serializable
    data class Exile(val grantFreeCast: Boolean = false) : CounterDestination
}

/**
 * Optional condition that allows the spell's controller to prevent countering.
 */
@Serializable
sealed interface CounterCondition {
    /** No condition — always counter. */
    @SerialName("CounterCondition.Always")
    @Serializable
    data object Always : CounterCondition

    /**
     * Counter unless controller pays a fixed mana cost.
     *
     * @property onPaid Optional effect run **only if** the spell's controller pays the
     *   cost (the "If they do, …" rider on cards like Divert Disaster). Executed with
     *   the original spell's caster (of the counter, not of the countered spell) as
     *   `controllerId`, so "you create a Lander token" targets the counter's caster.
     *   When the spell is countered instead, the rider does not run.
     */
    @SerialName("CounterCondition.UnlessPaysMana")
    @Serializable
    data class UnlessPaysMana(
        val cost: ManaCost,
        val onPaid: Effect? = null
    ) : CounterCondition

    /**
     * Counter unless controller pays a dynamic generic mana cost.
     *
     * @property onPaid See [UnlessPaysMana.onPaid].
     */
    @SerialName("CounterCondition.UnlessPaysDynamic")
    @Serializable
    data class UnlessPaysDynamic(
        val amount: DynamicAmount,
        val onPaid: Effect? = null
    ) : CounterCondition
}

/**
 * Unified counter effect for all counter-spell and counter-ability variations.
 *
 * Replaces the former CounterSpellEffect, CounterSpellWithFilterEffect,
 * CounterUnlessPaysEffect, CounterUnlessDynamicPaysEffect, CounterTriggeringSpellEffect,
 * CounterSpellToExileEffect, and CounterAbilityEffect with a single parametrized type.
 *
 * Examples:
 * - "Counter target spell" → CounterEffect()
 * - "Counter target creature spell" → CounterEffect(filter = creatureFilter)
 * - "Counter unless pays {2}" → CounterEffect(condition = UnlessPaysMana(ManaCost.parse("{2}")))
 * - "Counter that spell" → CounterEffect(targetSource = TriggeringEntity)
 * - "Counter and exile" → CounterEffect(counterDestination = Exile())
 * - "Counter target ability" → CounterEffect(target = Ability)
 */
@SerialName("Counter")
@Serializable
data class CounterEffect(
    val target: CounterTarget = CounterTarget.Spell,
    val targetSource: CounterTargetSource = CounterTargetSource.Chosen,
    val counterDestination: CounterDestination = CounterDestination.Graveyard,
    val condition: CounterCondition = CounterCondition.Always,
    val filter: TargetFilter? = null
) : Effect {
    override val description: String = buildString {
        when (target) {
            CounterTarget.Ability -> append("Counter target activated or triggered ability")
            CounterTarget.SpellOrAbility -> {
                if (filter != null) {
                    append("Counter target ${filter.baseFilter.description}")
                } else {
                    append("Counter target spell or ability")
                }
            }
            CounterTarget.Spell -> {
                when (condition) {
                    is CounterCondition.Always -> {
                        if (targetSource == CounterTargetSource.TriggeringEntity) {
                            append("Counter that spell")
                        } else if (filter != null) {
                            append("Counter target ${filter.baseFilter.description} spell")
                        } else {
                            append("Counter target spell")
                        }
                    }
                    is CounterCondition.UnlessPaysMana -> {
                        append("Counter target spell unless its controller pays ${condition.cost}")
                        condition.onPaid?.let { append(". If they do, ${it.description.replaceFirstChar(Char::lowercase)}") }
                    }
                    is CounterCondition.UnlessPaysDynamic -> {
                        append("Counter target spell unless its controller pays ${condition.amount.description}")
                        condition.onPaid?.let { append(". If they do, ${it.description.replaceFirstChar(Char::lowercase)}") }
                    }
                }
                when (val dest = counterDestination) {
                    CounterDestination.Graveyard -> {}
                    is CounterDestination.Exile -> {
                        if (condition is CounterCondition.UnlessPaysMana || condition is CounterCondition.UnlessPaysDynamic) {
                            append(". If countered, exile it")
                        } else {
                            append(". Exile it instead of putting it into its owner's graveyard")
                        }
                        if (dest.grantFreeCast) {
                            append(". You may cast that card without paying its mana cost for as long as it remains exiled")
                        }
                    }
                }
            }
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter?.applyTextReplacement(replacer)
        val newCondition = when (condition) {
            is CounterCondition.UnlessPaysMana -> {
                val newOnPaid = condition.onPaid?.applyTextReplacement(replacer)
                if (newOnPaid !== condition.onPaid) condition.copy(onPaid = newOnPaid) else condition
            }
            is CounterCondition.UnlessPaysDynamic -> {
                val newAmount = condition.amount.applyTextReplacement(replacer)
                val newOnPaid = condition.onPaid?.applyTextReplacement(replacer)
                if (newAmount !== condition.amount || newOnPaid !== condition.onPaid)
                    condition.copy(amount = newAmount, onPaid = newOnPaid) else condition
            }
            else -> condition
        }
        return if (newFilter !== filter || newCondition !== condition)
            copy(filter = newFilter, condition = newCondition) else this
    }
}

// =============================================================================
// Stack Effects — Counter All
// =============================================================================

/**
 * Counter every spell and/or ability on the stack matching the controller filter.
 * Non-targeted — resolves against whatever is on the stack at resolution time.
 *
 * Used by "mass counter" effects like Glen Elendra's Answer:
 * "Counter all spells your opponents control and all abilities your opponents control."
 *
 * If [storeCountAs] is set, the countered entity IDs are stored in the pipeline as
 * a named collection. Subsequent pipeline effects can read the count via
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.VariableReference] using the
 * key `"$storeCountAs" + "_count"` (e.g., "countered_count").
 *
 * @property spells Whether to counter spells on the stack
 * @property abilities Whether to counter activated/triggered abilities on the stack
 * @property opponentsOnly If true (default), only counter objects controlled by the
 *   effect source's opponents; if false, counter every matching object on the stack
 * @property storeCountAs Optional pipeline variable name to store the countered entity
 *   IDs under (for downstream `VariableReference("<name>_count")` usage)
 */
@SerialName("CounterAllOnStack")
@Serializable
data class CounterAllOnStackEffect(
    val spells: Boolean = true,
    val abilities: Boolean = true,
    val opponentsOnly: Boolean = true,
    val storeCountAs: String? = null
) : Effect {
    override val description: String = buildString {
        append("Counter all ")
        val parts = buildList {
            if (spells) add("spells")
            if (abilities) add("abilities")
        }
        append(parts.joinToString(" and "))
        if (opponentsOnly) append(" your opponents control")
    }
}

// =============================================================================
// Stack Effects — Destroy Ability Source
// =============================================================================

/**
 * Destroys the source permanent of the targeted activated or triggered ability on the
 * stack, if its source is currently a permanent on the battlefield. Reads the targeted
 * stack entity from [com.wingedsheep.engine.handlers.EffectContext.targets] (expects a
 * [com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell]) and inspects its
 * `ActivatedAbilityOnStackComponent` / `TriggeredAbilityOnStackComponent` to find the
 * source. If the targeted stack object is a spell (no ability component), nothing happens.
 *
 * Used by cards like Teferi's Response: "If a permanent's ability is countered this way,
 * destroy that permanent." Compose with [CounterEffect] in a `CompositeEffect` — place this
 * step *before* the counter so the source's component data is still present on the stack
 * entity when this effect runs. The permanent is still on the battlefield at this point
 * (ability sources don't have to be on the battlefield for the ability to resolve), so
 * destruction is straightforward.
 */
@SerialName("DestroySourceOfTargetedAbility")
@Serializable
data object DestroySourceOfTargetedAbilityEffect : Effect {
    override val description: String =
        "If a permanent's ability is countered this way, destroy that permanent"
}

// =============================================================================
// Stack Effects — Ward
// =============================================================================

/**
 * The cost a controller must pay to prevent a ward-triggered counter.
 * Mirrors the structure of [com.wingedsheep.sdk.scripting.KeywordAbility] ward
 * variants so static-granted ward and intrinsic ward share a single cost shape.
 */
@Serializable
sealed interface WardCost {
    /** Display string for prompts and oracle text generation. */
    val description: String

    /** Ward with a mana cost — e.g. Ward {1}. */
    @SerialName("WardCost.Mana")
    @Serializable
    data class Mana(val manaCost: String) : WardCost {
        override val description: String = manaCost
    }

    /** Ward with a life cost — e.g. Ward—Pay 2 life. */
    @SerialName("WardCost.Life")
    @Serializable
    data class Life(val amount: Int) : WardCost {
        override val description: String = "pay $amount life"
    }

    /** Ward with a discard cost — e.g. Ward—Discard a card. */
    @SerialName("WardCost.Discard")
    @Serializable
    data class Discard(val count: Int = 1, val random: Boolean = false) : WardCost {
        override val description: String = buildString {
            if (count == 1) append("a card") else append("$count cards")
            if (random) append(" at random")
        }
    }

    /** Ward with a sacrifice cost — e.g. Ward—Sacrifice a Food. */
    @SerialName("WardCost.Sacrifice")
    @Serializable
    data class Sacrifice(val filter: GameObjectFilter) : WardCost {
        override val description: String = "a ${filter.description}"
    }
}

/**
 * Counter the spell or ability that targeted this permanent unless its controller pays
 * the ward cost. Used by ward triggered abilities. The targeting source is identified
 * via context.targetingSourceEntityId (set by BecomesTargetEvent).
 *
 * Ward {1}        → WardCounterEffect(WardCost.Mana("{1}"))
 * Ward—Pay 2 life → WardCounterEffect(WardCost.Life(2))
 */
@SerialName("WardCounter")
@Serializable
data class WardCounterEffect(
    val cost: WardCost
) : Effect {
    override val description: String = when (cost) {
        is WardCost.Mana -> "Counter it unless its controller pays ${cost.manaCost}"
        is WardCost.Life -> "Counter it unless its controller pays ${cost.amount} life"
        is WardCost.Discard -> "Counter it unless its controller discards ${cost.description}"
        is WardCost.Sacrifice -> "Counter it unless its controller sacrifices ${cost.description}"
    }
}

// =============================================================================
// Stack Effects — Other
// =============================================================================

/**
 * Change the target of a spell that has exactly one target, and that target is a creature,
 * to another creature.
 * "If target spell has only one target and that target is a creature, change that spell's target to another creature."
 *
 * When [targetMustBeSource] is true, the spell's target must be the source of this effect
 * (e.g., Quicksilver Dragon: "If target spell has only one target and that target is this creature").
 */
@SerialName("ChangeSpellTarget")
@Serializable
data class ChangeSpellTargetEffect(
    val targetMustBeSource: Boolean = false
) : Effect {
    override val description: String = if (targetMustBeSource) {
        "Change target spell's target if it targets this creature"
    } else {
        "Change target spell's target to another creature"
    }
}

/**
 * Change the target of target spell or ability with a single target.
 * "Change the target of target spell or ability with a single target."
 *
 * Unlike ChangeSpellTargetEffect which only redirects creature targets to other creatures,
 * this effect works on any target type (creature, player, etc.) and can target
 * both spells and abilities on the stack.
 *
 * The spell or ability must have exactly one target. If it has zero or multiple targets,
 * the effect does nothing.
 */
@SerialName("ChangeTarget")
@Serializable
data object ChangeTargetEffect : Effect {
    override val description: String = "Change the target of target spell or ability with a single target"
}

/**
 * Reselect the target of the triggering spell or ability at random.
 * "If it has a single target, reselect its target at random."
 *
 * Uses context.triggeringEntityId to find the spell/ability on the stack.
 * If it has exactly one target, finds all legal targets and randomly picks one.
 * If it has zero or multiple targets, does nothing.
 */
@SerialName("ReselectTargetRandomly")
@Serializable
data object ReselectTargetRandomlyEffect : Effect {
    override val description: String = "Reselect its target at random"
}

/**
 * Selects which player may change a spell/ability's targets via [ChangeTriggeringObjectTargetsEffect].
 */
@Serializable
sealed interface RetargetChooser {
    /** The controller of the effect changes the targets (e.g., "you may change the target"). */
    @SerialName("RetargetChooser.Controller")
    @Serializable
    data object Controller : RetargetChooser

    /**
     * The owner of the single card in pipeline collection [collectionName] changes the targets —
     * e.g. the card left after `FilterCollection(GreatestManaValue)` over each player's revealed
     * top card (Psychic Battle). If the collection does not hold exactly one card (empty, or a tie
     * left several), there is no chooser and the retarget is skipped.
     */
    @SerialName("RetargetChooser.OwnerOfStored")
    @Serializable
    data class OwnerOfStored(val collectionName: String) : RetargetChooser
}

/**
 * The player named by [chooser] may change the target or targets of the triggering spell or
 * ability (`context.triggeringEntityId`). Resolve from a trigger that fires on the spell/ability
 * (e.g. [com.wingedsheep.sdk.scripting.EventPattern.TargetsChosenEvent]). The chooser may change all,
 * some, or none of the targets; new targets must be legal for the original spell/ability judged
 * from *its* controller's perspective (CR: same number, no illegal target, no target chosen twice).
 *
 * The non-random, player-chosen counterpart of [ReselectTargetRandomlyEffect]. When [chooser] is a
 * [RetargetChooser.StoredPlayer] that resolves to no player, the effect does nothing.
 */
@SerialName("ChangeTriggeringObjectTargets")
@Serializable
data class ChangeTriggeringObjectTargetsEffect(
    val chooser: RetargetChooser = RetargetChooser.Controller
) : Effect {
    override val description: String = "${
        when (chooser) {
            is RetargetChooser.Controller -> "You"
            is RetargetChooser.OwnerOfStored -> "The chosen player"
        }
    } may change the target or targets of the triggering spell or ability"
}

/**
 * Create copies of a spell on the stack (Storm mechanic).
 * "Copy it for each spell cast before it this turn. You may choose new targets for the copies."
 *
 * When resolved, creates [copyCount] copies of the spell on the stack. Each copy has the same
 * effect as the original. If the spell has targets, the controller may choose new targets for
 * each copy.
 *
 * @property copyCount Number of copies to create
 * @property spellEffect The effect of the original spell to copy
 * @property spellTargetRequirements Target requirements from the original spell (empty if untargeted)
 * @property spellName Name of the original spell for display
 */
@SerialName("StormCopy")
@Serializable
data class StormCopyEffect(
    val copyCount: Int,
    val spellEffect: Effect,
    val spellTargetRequirements: List<TargetRequirement> = emptyList(),
    val spellName: String
) : Effect {
    override val description: String = "Copy $spellName $copyCount time(s)"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var changed = false
        val newReqs = spellTargetRequirements.map {
            val n = it.applyTextReplacement(replacer); if (n !== it) changed = true; n
        }
        val newSpellEffect = spellEffect.applyTextReplacement(replacer)
        if (newSpellEffect !== spellEffect) changed = true
        return if (changed) copy(spellEffect = newSpellEffect, spellTargetRequirements = newReqs) else this
    }
}

/**
 * Copy target instant or sorcery spell on the stack.
 * "Copy target instant or sorcery spell. You may choose new targets for the copy."
 *
 * When resolved, reads the targeted spell's effect and target requirements from the stack,
 * then creates a copy. If the original spell has targets, the controller may choose new
 * targets for the copy.
 *
 * @property target The effect target referencing the spell to copy (typically ContextTarget(0))
 */
/**
 * Grant a keyword to a spell or ability on the stack until it leaves the stack.
 * Used for cards like Spinerock Tyrant: "those spells gain wither" — the granted
 * keyword applies for damage/source checks while the spell resolves, then
 * disappears with the spell.
 *
 * @property keyword The keyword to grant (enum name)
 * @property target The effect target referencing the spell on the stack
 *                  (typically [EffectTarget.TriggeringEntity] or [EffectTarget.ContextTarget])
 */
@SerialName("GrantKeywordToSpell")
@Serializable
data class GrantKeywordToSpellEffect(
    val keyword: String,
    val target: EffectTarget = EffectTarget.TriggeringEntity
) : Effect {
    constructor(keyword: Keyword, target: EffectTarget = EffectTarget.TriggeringEntity) :
        this(keyword.name, target)

    override val description: String = "${target.description} gains ${keyword.lowercase().replace('_', ' ')}"
}

@SerialName("CopyTargetSpell")
@Serializable
data class CopyTargetSpellEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    /**
     * Keywords (by enum name) granted to the copy on the stack while it remains a spell.
     * Each granted keyword is treated as if the spell itself had it for damage and
     * source-keyword checks (e.g., wither, lifelink). Empty by default.
     */
    val keywordsForCopy: List<String> = emptyList(),
    /**
     * When true, the Legendary supertype is removed from the copy's type line so the resulting
     * token is not legendary (e.g., Jackal, Genius Geneticist's "except the copy isn't legendary").
     * Applied to every copy regardless of which path (no-target, modal-with-targets, single-target
     * permanent or non-permanent) the executor takes.
     */
    val removeLegendary: Boolean = false
) : Effect {
    override val description: String = "Copy target spell"
}

/**
 * Copy target triggered ability on the stack.
 * "Copy target triggered ability. You may choose new targets for the copy."
 *
 * When resolved, reads the targeted triggered ability's effect and targets from the stack,
 * clones its TriggeredAbilityOnStackComponent onto a new entity, and pushes it onto the stack.
 * If the original ability has targets, the controller may choose new targets for the copy.
 *
 * @property target The effect target referencing the triggered ability to copy (typically ContextTarget(0))
 */
@SerialName("CopyTargetTriggeredAbility")
@Serializable
data class CopyTargetTriggeredAbilityEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Copy target triggered ability"
}

/**
 * When you next cast a spell matching [spellFilter] this turn, copy that spell.
 * You may choose new targets for the copies.
 *
 * Creates a pending spell copy entry on the game state. When the controller next casts
 * a spell matching [spellFilter] this turn, the engine creates [copies] copies of it.
 * Used by Howl of the Horde and similar effects.
 *
 * @property copies Number of copies to create when the next spell is cast
 * @property spellFilter Which spell to wait for (defaults to instant or sorcery)
 */
@SerialName("CopyNextSpellCast")
@Serializable
data class CopyNextSpellCastEffect(
    val copies: Int = 1,
    val spellFilter: GameObjectFilter = GameObjectFilter.InstantOrSorcery
) : Effect {
    override val description: String = if (copies == 1) {
        "When you next cast a ${spellFilter.description} spell this turn, copy that spell"
    } else {
        "When you next cast a ${spellFilter.description} spell this turn, copy that spell $copies times"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect =
        copy(spellFilter = spellFilter.applyTextReplacement(replacer))
}

/**
 * Until end of turn, whenever you cast a spell matching [spellFilter], copy it.
 * You may choose new targets for the copies.
 *
 * Creates a persistent pending spell copy entry on the game state. Unlike
 * CopyNextSpellCastEffect which is consumed after one use, this entry persists
 * for the rest of the turn, copying every matching spell cast.
 * Used by The Mirari Conjecture Chapter III and similar effects.
 *
 * @property copies Number of copies to create for each spell cast
 * @property spellFilter Which spells to copy (defaults to instant or sorcery)
 */
@SerialName("CopyEachSpellCast")
@Serializable
data class CopyEachSpellCastEffect(
    val copies: Int = 1,
    val spellFilter: GameObjectFilter = GameObjectFilter.InstantOrSorcery
) : Effect {
    override val description: String = if (copies == 1) {
        "Until end of turn, whenever you cast a ${spellFilter.description} spell, copy it"
    } else {
        "Until end of turn, whenever you cast a ${spellFilter.description} spell, copy it $copies times"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect =
        copy(spellFilter = spellFilter.applyTextReplacement(replacer))
}

/**
 * The next spell its controller casts this turn matching [spellFilter] can't be countered.
 *
 * One-shot rider: it attaches to the very next matching spell that player casts (stamping it
 * uncounterable for as long as it's on the stack) and is then consumed. Unlike
 * [com.wingedsheep.sdk.scripting.effects.GrantSpellsCantBeCounteredEffect], which protects
 * *every* matching spell cast for a whole duration, this protects only one spell. Mirrors the
 * pending-rider shape of [CopyNextSpellCastEffect] ("copy your next spell").
 *
 * Used by Mistrise Village ("{U}, {T}: The next spell you cast this turn can't be countered.").
 *
 * @property spellFilter Which spell the rider waits for (defaults to any spell).
 */
@SerialName("MakeNextSpellUncounterable")
@Serializable
data class MakeNextSpellUncounterableEffect(
    val spellFilter: GameObjectFilter = GameObjectFilter.Any
) : Effect {
    override val description: String =
        "The next ${spellFilter.description} spell you cast this turn can't be countered"

    override fun applyTextReplacement(replacer: TextReplacer): Effect =
        copy(spellFilter = spellFilter.applyTextReplacement(replacer))
}

// =============================================================================
// Stack Effects — Mark for Exile-After-Resolve with Counters
// =============================================================================

/**
 * Mark a spell on the stack so that, when it resolves, it is exiled with the
 * specified counters on it instead of being put into its owner's graveyard.
 *
 * Used by replacement effects that read like a triggered ability but actually
 * change the spell's resolution destination — for example Goliath Daydreamer's
 * "Whenever you cast an instant or sorcery spell from your hand, exile that
 * card with a dream counter on it instead of putting it into your graveyard
 * as it resolves."
 *
 * Per the ruling, if the spell is countered or otherwise fails to resolve, the
 * exile-with-counter does not happen — so this effect sets the
 * `onlyIfResolved` flag on the underlying ExileAfterResolveComponent.
 *
 * @property target The spell on the stack to mark (typically the triggering entity).
 * @property counterType Counter type string (see [com.wingedsheep.sdk.core.Counters]).
 * @property count How many counters of [counterType] to add when the spell exiles.
 */
@SerialName("MarkSpellExileWithCounters")
@Serializable
data class MarkSpellExileWithCountersEffect(
    val target: com.wingedsheep.sdk.scripting.targets.EffectTarget = com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity,
    val counterType: String = com.wingedsheep.sdk.core.Counters.PLUS_ONE_PLUS_ONE,
    val count: Int = 1
) : Effect {
    override val description: String = buildString {
        append("Exile that card with ")
        if (count == 1) append("a $counterType counter") else append("$count $counterType counters")
        append(" on it instead of putting it into your graveyard as it resolves")
    }
}

// =============================================================================
// Stack Effects — Return Spell to Hand
// =============================================================================

/**
 * Remove a target spell from the stack and put it into its owner's hand.
 *
 * This is **not** a counter (CR 701.27 / 701.5b). The spell does not resolve, but
 * "this spell can't be countered" does not prevent the move — only effects that
 * literally counter the spell are blocked. Used by cards like Hullbreaker Horror
 * ("Return target spell you don't control to its owner's hand") and the bounce
 * mode of Aetherize-style effects on the stack.
 *
 * The targeted spell is identified via [EffectTarget.ContextTarget] from
 * [com.wingedsheep.engine.handlers.EffectContext.targets]. If the spell is no
 * longer on the stack at resolution, the effect does nothing.
 */
@SerialName("ReturnSpellToOwnersHand")
@Serializable
data object ReturnSpellToOwnersHandEffect : Effect {
    override val description: String = "Return target spell to its owner's hand"
}
