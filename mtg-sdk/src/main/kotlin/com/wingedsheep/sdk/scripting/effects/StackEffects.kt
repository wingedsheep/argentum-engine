package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.util.numberToWord
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

/**
 * Exile target spell on the stack (CR 718 "exile target spell" — Aven Interrupter).
 *
 * Distinct from [CounterEffect] with [CounterDestination.Exile]: this is **not** a counter.
 * It exiles the spell regardless of can't-be-countered (Aven Interrupter's ruling: "Spells that
 * can't be countered can still be exiled. They won't resolve."), and it fires no
 * "whenever a spell is countered" trigger. The spell still fails to resolve because it leaves
 * the stack. The target is the chosen spell ([com.wingedsheep.sdk.dsl.Targets.Spell] supplies
 * the requirement).
 *
 * @property makePlotted When true, the exiled card becomes *plotted* for its **owner** (CR 718.2):
 *   it gains the plotted designation and a permanent free-cast-on-a-later-turn permission. Pairs
 *   with [com.wingedsheep.sdk.scripting.effects.MakePlottedEffect]'s owner-controls semantics, but
 *   reads its subject from the stack instead of a gathered collection.
 * @property fixedAlternativeManaCost When non-null, the exiled card's **owner** may cast it from
 *   exile for this *fixed* mana cost (e.g. `{2}`) instead of its printed cost, for as long as it
 *   stays exiled — the spell-on-stack form of the **Airbend** keyword (Aang, Swift Savior:
 *   "airbend … target … spell"). Reuses `PlayWithFixedAlternativeManaCostComponent`, the same
 *   primitive the permanent [com.wingedsheep.sdk.dsl.Effects.Airbend] uses; mutually exclusive with
 *   [makePlotted]. Because this is "exile it", not "counter it", it works on spells that can't be
 *   countered (see [exileSpell]'s non-counter semantics).
 * @property emitAirbend When true, exiling the spell counts as an **airbend** (CR 701.65b): the
 *   executor records a [com.wingedsheep.sdk.core.BendType.AIR] bend for the controller and fires
 *   [com.wingedsheep.sdk.dsl.Triggers.YouBend], but only if the spell was actually exiled (a target
 *   that already left the stack exiles nothing → no bend). Set via [com.wingedsheep.sdk.dsl.Effects.AirbendSpell];
 *   left false for a plain non-airbend exile-spell (Aven Interrupter).
 * @property linkToSource When true, the exiled card is appended to the effect source's
 *   `LinkedExileComponent`, so a later ability of that same source can say "the exiled card" — the
 *   stack-side counterpart of [ExileLinkedToSourceEffect] and of `MoveCollection(linkToSource = true)`.
 *   Nothing returns automatically; the link is only a handle. **Spell Queller** pairs it with a
 *   leaves-the-battlefield trigger that gathers `CardSource.FromLinkedExile()` and grants the card's
 *   owner a free cast. The link survives the source's own zone change, so the leaves trigger still
 *   finds it.
 */
@SerialName("ExileTargetSpell")
@Serializable
data class ExileTargetSpellEffect(
    val makePlotted: Boolean = false,
    val fixedAlternativeManaCost: ManaCost? = null,
    val emitAirbend: Boolean = false,
    val linkToSource: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Exile target spell")
        if (makePlotted) append(". It becomes plotted")
        if (fixedAlternativeManaCost != null) {
            append(". Its owner may cast it for $fixedAlternativeManaCost rather than its mana cost")
        }
    }
}

/**
 * Exile every spell on the stack matching the controller scope.
 *
 * This is not a counter: spells that can't be countered are still exiled. [excludeSource]
 * supports the common "all other spells" wording when this effect is resolving from a spell.
 */
@SerialName("ExileSpellsOnStack")
@Serializable
data class ExileSpellsOnStackEffect(
    val opponentsOnly: Boolean = false,
    val excludeSource: Boolean = true,
) : Effect {
    override val description: String = buildString {
        append("Exile all ")
        if (excludeSource) append("other ")
        if (opponentsOnly) append("opponents' ")
        append("spells")
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

/**
 * The ability-strip sibling of [DestroySourceOfTargetedAbilityEffect]. If the targeted
 * activated or triggered ability on the stack has a source that is currently a permanent on the
 * battlefield, and (when [sourceCardTypes] is non-empty) that permanent's projected card types
 * include at least one of them, the source permanent **loses all abilities** for [duration].
 *
 * Reads the targeted stack entity from [com.wingedsheep.engine.handlers.EffectContext.targets]
 * (expects a [com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell]) and inspects its
 * `ActivatedAbilityOnStackComponent` / `TriggeredAbilityOnStackComponent` to find the source. If
 * the targeted stack object is a spell (no ability component), its source has already left the
 * battlefield, or the source's type doesn't match [sourceCardTypes], nothing happens.
 *
 * Compose with [CounterEffect]`(target = CounterTarget.Ability)` in a `CompositeEffect`, placing
 * this step **before** the counter so the source's ability-on-stack component is still readable
 * (the counter removes the ability from the stack). Used by Tishana's Tidebinder: "If an ability
 * of an artifact, creature, or planeswalker is countered this way, that permanent loses all
 * abilities for as long as this creature remains on the battlefield." — pair
 * `Duration.WhileSourceOnBattlefield` with `sourceCardTypes = {ARTIFACT, CREATURE, PLANESWALKER}`.
 *
 * @property duration How long the source permanent loses its abilities. The floating effect is
 *   keyed to the *effect's* source (e.g. Tishana), so [Duration.WhileSourceOnBattlefield] ends
 *   when that permanent leaves the battlefield.
 * @property sourceCardTypes Which card types the countered ability's source must have for the
 *   strip to apply (any of them). Empty means any permanent qualifies.
 */
@SerialName("RemoveAbilitiesFromSourceOfTargetedAbility")
@Serializable
data class RemoveAbilitiesFromSourceOfTargetedAbilityEffect(
    val duration: Duration = Duration.EndOfTurn,
    val sourceCardTypes: Set<CardType> = emptySet()
) : Effect {
    override val description: String = buildString {
        append("If an ability of ")
        if (sourceCardTypes.isEmpty()) {
            append("a permanent")
        } else {
            val names = sourceCardTypes.map { it.displayName.lowercase() }
            val list = when (names.size) {
                1 -> names[0]
                2 -> "${names[0]} or ${names[1]}"
                else -> names.dropLast(1).joinToString(", ") + ", or " + names.last()
            }
            val article = if (names.first().first() in "aeiou") "an" else "a"
            append("$article $list")
        }
        append(" is countered this way, that permanent loses all abilities ")
        append(duration.description.ifEmpty { "permanently" })
    }
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

    /**
     * The **self-contained verb phrase** for this cost — "pay {2}", "pay 2 life", "discard a card",
     * "sacrifice a Food", "get five poison counters".
     *
     * [description] is deliberately *not* that: for most variants it is only the object phrase
     * ("a card", "a Food"), because each of the three ward renderers supplies the verb itself
     * ("Ward—Discard " + description). That works only while a ward cost is rendered alone. A
     * disjunction ([Choice]) has to render each option *with its own verb* — "discard a card or
     * pay {2}" — so it joins [clause]s instead. Keep the two in sync when adding a variant: the
     * clause is what a player is asked to do, standing on its own, lowercase and unpunctuated.
     */
    val clause: String

    /**
     * Ward with a mana cost — e.g. Ward {1}.
     *
     * When [waterbend] is true the cost is a **Ward—Waterbend** (Avatar: The Last Airbender):
     * the {N} is still a normal mana cost, but while paying it the spell/ability's controller
     * may tap their untapped artifacts and creatures to help — each tapped permanent pays {1}
     * of the generic (a generic-only convoke+improvise, identical to the waterbend additional
     * cost / activated-ability waterbend). The waterbend taps are surfaced and applied through
     * the same machinery as those (`AlternativePaymentChoice.tapForGenericPermanents`).
     */
    @SerialName("WardCost.Mana")
    @Serializable
    data class Mana(val manaCost: String, val waterbend: Boolean = false) : WardCost {
        override val description: String =
            if (waterbend) "Waterbend $manaCost" else manaCost
        override val clause: String =
            if (waterbend) "waterbend $manaCost" else "pay $manaCost"
    }

    /** Ward with a life cost — e.g. Ward—Pay 2 life. */
    @SerialName("WardCost.Life")
    @Serializable
    data class Life(val amount: Int) : WardCost {
        override val description: String = "pay $amount life"
        override val clause: String = description
    }

    /**
     * Ward with a life cost whose amount is a game-state-dependent [DynamicAmount] — e.g.
     * Raubahn, Bull of Ala Mhigo's "Ward—Pay life equal to Raubahn's power"
     * ([com.wingedsheep.sdk.dsl.DynamicAmounts.sourcePower]). The amount is evaluated when the
     * ward triggered ability *resolves* (CR 702.21b), reading the source's power at that time,
     * or its last-known value if the source has left the battlefield (CR 113.7a) — both handled
     * by [com.wingedsheep.sdk.scripting.values.EntityReference.Source]'s last-known-information
     * fallback. The fixed-Int [Life] stays the common case; this variant covers only costs that
     * read live game state.
     */
    @SerialName("WardCost.DynamicLife")
    @Serializable
    data class DynamicLife(val amount: DynamicAmount) : WardCost {
        override val description: String = "pay life equal to ${amount.description}"
        override val clause: String = description
    }

    /**
     * Ward with a discard cost — e.g. Ward—Discard a card.
     *
     * When [filter] is non-null the discarded card(s) must match it — e.g.
     * Saruman of Many Colors' "Ward—Discard an enchantment, instant, or sorcery card."
     * The filter restricts both the can-pay eligibility check and the cards offered for
     * discard. A null filter means any card.
     */
    @SerialName("WardCost.Discard")
    @Serializable
    data class Discard(
        val count: Int = 1,
        val random: Boolean = false,
        val filter: GameObjectFilter? = null,
    ) : WardCost {
        override val description: String = buildString {
            if (filter != null) {
                if (count == 1) append("a ") else append("$count ")
                append(filter.description)
            } else {
                if (count == 1) append("a card") else append("$count cards")
            }
            if (random) append(" at random")
        }
        override val clause: String = "discard $description"
    }

    /**
     * Ward with a sacrifice cost — e.g. Ward—Sacrifice a Food, or
     * Ward—Sacrifice three nonland permanents (Valgavoth, Terror Eater) via [count].
     */
    @SerialName("WardCost.Sacrifice")
    @Serializable
    data class Sacrifice(val filter: GameObjectFilter, val count: Int = 1) : WardCost {
        override val description: String =
            if (count == 1) "a ${filter.description}" else "$count ${filter.description}s"
        override val clause: String = "sacrifice $description"
    }

    /**
     * Ward with a cost paid in **counters placed on the paying player** (CR 122.1 — a counter is a
     * marker placed on an object *or player*) — "Ward—Get five poison counters." (The Serpent
     * Society). [counterType] is a `Counters.*` symbol (`Counters.POISON`, `Counters.ENERGY`, …),
     * matching every other player-scoped counter surface in the SDK.
     *
     * Unlike every other ward cost this one has no affordability precondition: a player can always
     * get counters, so the payment is a plain yes/no and can never be "unpayable" the way an empty
     * hand or an empty board makes a discard or sacrifice unpayable. That asymmetry is exactly why
     * it is its own variant rather than a re-skin of [Life] — the payer is *receiving* a marker,
     * not spending a resource they must already hold.
     *
     * "Always payable" is an assumption, not a proof: it holds because nothing grants a *player*
     * `CANT_RECEIVE_COUNTERS` today. A Melira / Solemnity-shaped card would make the counter
     * placement a silent no-op and the ward would then be paid for free — revisit the can-pay
     * branch in `WardCounterEffectExecutor` if one ever lands.
     */
    @SerialName("WardCost.PlayerCounters")
    @Serializable
    data class PlayerCounters(val counterType: String, val amount: Int) : WardCost {
        override val description: String =
            if (amount == 1) "a $counterType counter" else "${numberToWord(amount)} $counterType counters"
        override val clause: String = "get $description"
    }

    /**
     * Ward—Collect evidence N (CR 701.59) — e.g. Axebane Ferox's "Ward—Collect evidence 4."
     *
     * The payment is the ordinary keyword action: exile any number of cards from your graveyard
     * with total mana value [amount] or greater. Because the constraint is a **sum** and not a
     * count, this is not expressible as a [Sacrifice]-style counted selection; the engine routes it
     * through the same `CollectEvidenceResolver` every other collect-evidence context uses, so the
     * reachability gate, the legal-selection rule and the exile can't drift from the activated-cost
     * and cast-cost forms.
     *
     * CR 701.59b fails closed here as everywhere else: a controller whose graveyard cannot reach
     * [amount] *can't choose to collect evidence*, so the spell or ability is countered without a
     * prompt rather than offering a payment they'd have to refuse.
     *
     * This is an **unlinked** cost — nothing on the card asks "was evidence collected", so it
     * stamps no `ChoiceSlot` (the linkage of CR 701.59c belongs only to the optional cast-cost
     * shape, `card { collectEvidence(n) }`).
     */
    @SerialName("WardCost.CollectEvidence")
    @Serializable
    data class CollectEvidence(val amount: Int) : WardCost {
        override val description: String = "collect evidence $amount"
        override val clause: String = description
    }

    /**
     * A ward cost made of two or more component costs that must *all* be paid — e.g.
     * "Ward—{2}, Pay 2 life." (Gisa, the Hellraiser). The components are paid one at a time
     * in order; declining or being unable to pay any one component counters the spell or
     * ability (CR 702.21a — a single ward cost whose payment is composed of multiple parts).
     *
     * Nesting another [Composite] inside [parts] is not supported (and not needed by any
     * printed card); keep [parts] a flat list of atomic ward costs.
     */
    @SerialName("WardCost.Composite")
    @Serializable
    data class Composite(val parts: List<WardCost>) : WardCost {
        override val description: String = parts.joinToString(", ") { it.description }
        override val clause: String = parts.joinToString(", ") { it.clause }
    }

    /**
     * A ward cost that is a **disjunction**: the paying player picks exactly *one* of [options] and
     * pays it — "Ward—Discard a card or pay {2}." (Titania, Rugged Rumbler). The OR to
     * [Composite]'s AND; the two are the only two ways a printed ward cost combines sub-costs, and
     * keeping them as sibling variants is what stops "or" from being smuggled in as an
     * ad-hoc third state on [Composite].
     *
     * Modelled on [com.wingedsheep.sdk.scripting.AdditionalCost.Choice] / `PayCost.Choice`, the
     * cost-vs-cost shape the other two cost vocabularies already use: options are themselves
     * fully-formed costs of the same vocabulary, only options the payer can actually pay are
     * offered, and declining is always available (declining a ward cost counters the spell or
     * ability, CR 702.21a). A mana option is just a [Mana] in the list — unlike
     * `AdditionalCost.OrPay`, where the mana leg has to fold into the spell's own mana cost at
     * cast time, a ward cost is paid on its own as the ward trigger resolves, so no leg is special.
     *
     * Nesting another [Choice] inside [options] is not supported (and not needed by any printed
     * card); keep [options] a flat list. A [Composite] option *is* supported — "or" over an
     * all-of group would be its natural use — and pays its parts in order.
     */
    @SerialName("WardCost.Choice")
    @Serializable
    data class Choice(val options: List<WardCost>) : WardCost {
        override val clause: String = options.joinToString(" or ") { it.clause }
        override val description: String = clause
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
    override val description: String = "Counter it unless its controller ${wardPaymentVerbPhrase(cost)}"
}

/**
 * The ward payment as a **third-person** verb phrase — "pays {2}", "discards a card" — for
 * [WardCounterEffect.description], whose subject is "its controller".
 *
 * Deliberately not [WardCost.clause]: that one is the bare imperative ("pay {2}") because it labels
 * an option in the payment picker. A [WardCost.Choice] joins these conjugated phrases rather than
 * its clauses, so a disjunction reads "…unless its controller discards a card or pays {2}" like
 * every other branch, instead of "…unless its controller discard a card or pay {2}".
 */
private fun wardPaymentVerbPhrase(cost: WardCost): String = when (cost) {
    is WardCost.Mana ->
        if (cost.waterbend) {
            "pays ${cost.manaCost} (they may tap artifacts and creatures to help; each pays for {1})"
        } else {
            "pays ${cost.manaCost}"
        }
    is WardCost.Life -> "pays ${cost.amount} life"
    is WardCost.DynamicLife -> "pays life equal to ${cost.amount.description}"
    is WardCost.Discard -> "discards ${cost.description}"
    is WardCost.Sacrifice -> "sacrifices ${cost.description}"
    is WardCost.CollectEvidence ->
        "exiles cards with total mana value ${cost.amount} or greater from their graveyard"
    is WardCost.PlayerCounters -> "gets ${cost.description}"
    is WardCost.Choice -> cost.options.joinToString(" or ") { wardPaymentVerbPhrase(it) }
    is WardCost.Composite -> "pays ${cost.description}"
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
    val removeLegendary: Boolean = false,
    /**
     * Keywords granted to the *token* the copy resolves into, when the copied spell is a permanent
     * spell (CR 707.10f — a copy of a permanent spell becomes a token as it resolves). Unlike
     * [keywordsForCopy] (which lasts only while the copy is a spell on the stack), these are baked
     * onto the resulting permanent for its whole life on the battlefield. Used for "the copy gains
     * haste" riders (Choreographed Sparks). Empty by default; ignored for non-permanent copies.
     */
    val addedTokenKeywords: Set<Keyword> = emptySet(),
    /**
     * If set, register a delayed trigger to sacrifice the token the copy resolves into at the
     * beginning of this step (the spell-copy sibling of
     * [CreateTokenCopyOfTargetEffect.sacrificeAtStep]). Models "the copy gains '… sacrifice this
     * token.'" (Choreographed Sparks). Ignored for non-permanent copies.
     */
    val sacrificeTokenAtStep: Step? = null,
    /**
     * When [sacrificeTokenAtStep] is set, gate the delayed sacrifice trigger to fire only on the
     * copy controller's turn — i.e. "at the beginning of *your* next end step" rather than the very
     * next end step of any player. Mirrors [CreateTokenCopyOfTargetEffect.sacrificeOnlyOnControllersTurn].
     */
    val sacrificeTokenOnlyOnControllersTurn: Boolean = false,
    /**
     * How many copies to create (CR 707.10 — each is an independent copy, and the controller may
     * choose new targets for each one separately). Defaults to a single copy. Pass a [DynamicAmount]
     * for "copy it for each …" clauses whose count is only known at resolution — Thousand-Year Storm
     * copies the triggering spell once per other instant or sorcery cast before it this turn. A
     * count of zero or less makes no copies at all.
     */
    val copies: DynamicAmount = DynamicAmount.Fixed(1)
) : Effect {
    override val description: String =
        if (copies == DynamicAmount.Fixed(1)) "Copy target spell"
        else "Copy target spell ${copies.description} times"
}

/**
 * Copy a spell once **for each other object it could target** (CR 707.10d), auto-assigning every
 * copy a distinct one of those objects as its target. Models the Zada family:
 *
 *  - Zada, Hedron Grinder — "copy it for each other creature you control that the spell could target"
 *  - Mirrorwing Dragon — "that player copies that spell for each other creature they control that
 *    the spell could target"
 *
 * This is the 707.10d shape, not the 707.10c one: **no player decision is involved.** Contrast
 * [CopyTargetSpellEffect] with a `copies` count, which makes N copies and pauses so the controller
 * *may choose* new targets for each — here both the number of copies and each copy's target fall out
 * of the board, so the copies go straight onto the stack.
 *
 * The candidate set is every object matching [candidates] that is a legal target for **every**
 * instance of the word "target" on the copied spell (707.10d: "if that player or object isn't a legal
 * target for each instance of the word *target*, a copy isn't created for that player or object"),
 * minus the objects the spell already targets — the "each **other** …" in the card text. Each copy is
 * put onto the stack with its object filling all of the spell's target slots.
 *
 * **Both [candidates] and control of the copies belong to the copied spell's controller, not to this
 * ability's controller.** That is what lets one effect express both wordings: Zada says "you control"
 * on a trigger only its own controller's casts fire, while Mirrorwing Dragon watches every seat and
 * says "**they** control" / "**that player** copies". So `candidates` written as
 * `GameObjectFilter.Creature.youControl()` reads as "creature the caster controls".
 *
 * A spell flagged "can't be copied" yields no copies.
 *
 * @property spell The spell to copy — [EffectTarget.TriggeringEntity] for the "copy that spell" wording.
 * @property candidates Which objects the copies are distributed over, one copy each.
 */
@SerialName("CopySpellForEachOtherPossibleTarget")
@Serializable
data class CopySpellForEachOtherPossibleTargetEffect(
    val spell: EffectTarget = EffectTarget.TriggeringEntity,
    val candidates: GameObjectFilter
) : Effect {
    override val description: String =
        "Copy that spell for each other ${candidates.description} it could target"

    override fun applyTextReplacement(replacer: TextReplacer): Effect =
        copy(candidates = candidates.applyTextReplacement(replacer))
}

/**
 * Copy each spell targeted by this effect (CR 707.10). One copy is created per
 * targeted spell on the stack; for each copy the controller may choose new targets.
 *
 * Models "Copy any number of target instant and/or sorcery spells. You may choose new
 * targets for the copies." (Display of Power). Unlike [CopyTargetSpellEffect], which
 * copies a single referenced target, this reads **every** spell target chosen for the
 * spell/ability (all [com.wingedsheep.sdk.scripting.targets.ChosenTarget.Spell] entries in
 * the resolution context) and copies them in turn, pausing per copy that has targets so
 * the controller can retarget it.
 *
 * Spells flagged "can't be copied" are skipped (no copy is created for them).
 */
@SerialName("CopyEachTargetSpell")
@Serializable
data class CopyEachTargetSpellEffect(
    /** Keywords (by enum name) granted to each copy while it remains a spell on the stack. */
    val keywordsForCopy: List<String> = emptyList(),
    /** When true, the Legendary supertype is stripped from each resulting copy (CR 707.10f). */
    val removeLegendary: Boolean = false
) : Effect {
    override val description: String = "Copy each target spell"
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
 * Copy target spell **or** ability on the stack, dispatching at resolution on the chosen
 * object's kind: an instant/sorcery spell copies via the spell-copy path; an activated or
 * triggered ability copies its ability-on-stack component. You may choose new targets for the
 * copy (CR 707.10c). Generalizes [CopyTargetSpellEffect] and [CopyTargetTriggeredAbilityEffect]
 * into the single "copy target spell or ability" clause (Return the Favor; the Fork/Twincast
 * family extended to abilities). The target requirement decides which stack-object kinds are
 * legal (e.g. [com.wingedsheep.sdk.dsl.Targets.InstantSorcerySpellOrAbility]); this effect copies
 * whichever one the player chose.
 *
 * [copies] is the number of copies to create (CR 707.10 — each is an independent instance created
 * on the stack). It defaults to a single copy (Return the Favor); a [DynamicAmount] such as
 * [DynamicAmount.XValue] models "copy … X times" (Gogo, Master of Mimicry: "{X}{X}, {T}: Copy
 * target activated or triggered ability you control X times"). When more than one copy is made and
 * the source ability has targets, the controller may choose new targets independently for every
 * copy. [copies] > 1 is honored on both branches — spells and abilities alike.
 *
 * @property target The effect target referencing the spell or ability to copy (typically ContextTarget(0))
 * @property copies How many copies to create (defaults to a single copy)
 */
@SerialName("CopyTargetSpellOrAbility")
@Serializable
data class CopyTargetSpellOrAbilityEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val copies: DynamicAmount = DynamicAmount.Fixed(1)
) : Effect {
    override val description: String =
        if (copies == DynamicAmount.Fixed(1)) "Copy target spell or ability"
        else "Copy target spell or ability ${copies.description} times"
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

/**
 * The next spell its controller casts this turn matching [spellFilter] can be cast without paying
 * its mana cost (CR 118.9 — "without paying its mana cost" is an alternative cost, so mandatory
 * additional costs still apply, and X is 0 per CR 107.3b because the free-cast action variant the
 * enumerator offers carries no X).
 *
 * One-shot rider, the same shape as [MakeNextSpellUncounterableEffect] and
 * [GrantNextSpellAffinityEffect]: it waits on the game state for the controller's next matching
 * cast, offers that cast the free-cast alternative, and is then consumed. **Consumption is by the
 * cast, not by the discount** — the printed text names "the next" matching spell, so a matching
 * spell cast for full price is that spell and spends the rider; a later one is not "the next".
 *
 * Unlike [com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost], which is a static ability
 * read off a permanent on the battlefield, this rider lives on the game state: it survives its
 * source leaving the battlefield (the ability has already resolved) and is not tied to the source
 * being a permanent at all.
 *
 * Used by World War Hulk chapter I ("The next red or green creature spell you cast this turn can
 * be cast without paying its mana cost.").
 *
 * @property spellFilter Which spell the rider waits for (defaults to any spell).
 */
@SerialName("GrantNextSpellFreeCast")
@Serializable
data class GrantNextSpellFreeCastEffect(
    val spellFilter: GameObjectFilter = GameObjectFilter.Any
) : Effect {
    override val description: String = buildString {
        append("The next ")
        if (spellFilter != GameObjectFilter.Any) append("${spellFilter.description} ")
        append("spell you cast this turn can be cast without paying its mana cost")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect =
        copy(spellFilter = spellFilter.applyTextReplacement(replacer))
}

/**
 * Grant the next [spellFilter] spell the controller casts this turn affinity for [forType] —
 * the same one-shot pending-rider shape as [MakeNextSpellUncounterableEffect], but the matched
 * spell costs {1} less to cast for each permanent of [forType] the controller has *at cast time*
 * (the reduction is dynamic, read by the cost calculator when the spell is cast).
 *
 * Don & Raph, Hard Science: "the next noncreature spell you cast this turn has affinity for artifacts."
 *
 * @property spellFilter Which spell the rider waits for (Don & Raph: noncreature spells).
 * @property forType The card type whose permanents reduce the cost (artifacts).
 */
@SerialName("GrantNextSpellAffinity")
@Serializable
data class GrantNextSpellAffinityEffect(
    val spellFilter: GameObjectFilter = GameObjectFilter.Noncreature,
    val forType: com.wingedsheep.sdk.core.CardType = com.wingedsheep.sdk.core.CardType.ARTIFACT
) : Effect {
    override val description: String =
        "The next ${spellFilter.description} spell you cast this turn has affinity for ${forType.displayName.lowercase()}s"

    override fun applyTextReplacement(replacer: TextReplacer): Effect =
        copy(spellFilter = spellFilter.applyTextReplacement(replacer))
}

/**
 * "Spells you cast this turn that match [spellFilter] cost {X} less to cast" — a turn-scoped,
 * controller-scoped generic cost reduction installed when this effect resolves.
 *
 * The *repeating* counterpart of [GrantNextSpellAffinityEffect]: that rider is consumed by the
 * first matching spell, this one applies to every matching spell for the rest of the turn.
 *
 * [amount] is evaluated **once, when this effect resolves**, and the resolved number is what the
 * cost calculator uses for the rest of the turn. That is what the Scion cycle's rulings require —
 * "the value of X is determined only once, at the time the ability resolves" — so life gained or
 * lost after activation does not change the discount. Use a static
 * [com.wingedsheep.sdk.scripting.ModifySpellCost] instead when the reduction should track board
 * state continuously.
 *
 * The reduction lives on the game state rather than on the source permanent, so it survives the
 * source leaving the battlefield (the ability has already resolved; its effect lasts the turn),
 * and it only reduces the generic portion of a cost (CR 601.2f) — never colored mana.
 *
 * Will, Scion of Peace: `ReduceSpellCostsThisTurnEffect(Filters.whiteOrBlue,
 * DynamicAmount.TurnTracking(Player.You, TurnTracker.LIFE_GAINED))`.
 *
 * @property spellFilter Which of the controller's spells are discounted.
 * @property amount How much generic mana to take off, resolved at execution time.
 */
@SerialName("ReduceSpellCostsThisTurn")
@Serializable
data class ReduceSpellCostsThisTurnEffect(
    val spellFilter: GameObjectFilter,
    val amount: com.wingedsheep.sdk.scripting.values.DynamicAmount,
) : Effect {
    override val description: String =
        "Spells you cast this turn that are ${spellFilter.description} cost {X} less to cast, " +
            "where X is ${amount.description}"

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
 * `onlyIfResolved` flag on the underlying AfterResolveDestinationComponent.
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

/**
 * Mark a spell on the stack so that, when it resolves, it is exiled instead of put into its
 * owner's graveyard and **becomes plotted** (CR 718). The plot designation and free-cast-on-a-
 * later-turn permission are granted only if the spell actually resolves into exile — if it is
 * countered or otherwise fails to resolve, it goes to the graveyard normally (sibling of
 * [MarkSpellExileWithCountersEffect], which carries the same `onlyIfResolved` semantics).
 *
 * Distinct from [ExileTargetSpellEffect]`(makePlotted = true)`: that one targets and removes a
 * spell from the stack *now* (a non-counter removal); this one lets the spell resolve fully and
 * only re-routes its post-resolution destination. Used by Lilah, Undefeated Slickshot: "exile
 * that spell instead of putting it into your graveyard as it resolves. If you do, it becomes
 * plotted."
 *
 * @property target The spell on the stack to mark (typically the triggering entity).
 */
@SerialName("MarkSpellPlotOnResolve")
@Serializable
data class MarkSpellPlotOnResolveEffect(
    val target: com.wingedsheep.sdk.scripting.targets.EffectTarget = com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity,
) : Effect {
    override val description: String =
        "Exile that spell instead of putting it into your graveyard as it resolves. " +
            "If you do, it becomes plotted"
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

/**
 * Return a single [target] — which may be a **spell on the stack** or a **permanent on the
 * battlefield** — to its owner's hand. This is the bounce counterpart to
 * [com.wingedsheep.sdk.scripting.effects.PutOnLibraryPositionOfChoiceEffect] (Swat Away),
 * which already handles the dual spell/permanent case for library placement.
 *
 * The executor resolves [target] to a single entity and dispatches:
 * - if the entity is a spell on the stack, it is removed from the stack and put into its
 *   owner's hand (it does not resolve) — like [ReturnSpellToOwnersHandEffect], this is **not**
 *   a counter (CR 701.27 / 701.5b), so "can't be countered" does not prevent it;
 * - otherwise it is treated as a permanent and bounced to its owner's hand.
 *
 * Used by cards whose single "target spell or nonland permanent" must go to hand regardless of
 * which it turns out to be (e.g. Press the Enemy). Pair with [TargetSpellOrPermanent].
 * If the target is no longer in a valid zone at resolution, the effect does nothing.
 */
@SerialName("ReturnSpellOrPermanentToOwnersHand")
@Serializable
data class ReturnSpellOrPermanentToOwnersHandEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Return target spell or permanent to its owner's hand"
}
