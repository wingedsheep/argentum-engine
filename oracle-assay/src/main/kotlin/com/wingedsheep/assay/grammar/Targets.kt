package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetOpponentOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.targets.TargetPlayerOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Targeting, as the SDK splits it: a spell declares a [TargetRequirement] and its effect refers to
 * what was chosen through an [EffectTarget].
 *
 * The two halves are linked by a **name**, and the name is arbitrary — Ancestral Recall's golden
 * uses `"target"`, and any other string would behave identically. That makes it the one thing in a
 * parsed model that is not determined by the text, so the grammar always mints [SLOT] and the
 * differential renames both sides before comparing (see `Differential.normalizeSlotNames`). Two
 * models that differ only in what they called a slot are the same model, and neither the round trip
 * nor the differential should be able to see the difference.
 *
 * Almost nothing here is a [com.wingedsheep.assay.syntax.Phrase], and that is on purpose: a target
 * is not a line, and the phrasings that introduce one ("Target player draws…", "Destroy target
 * creature") are inseparable from the step that consumes it — English puts the verb and its object
 * in one clause, and so does the rule. What lives here is the vocabulary those rules share.
 *
 * [enchant] is the exception, and it is one because Magic makes it one: an Aura's attachment
 * restriction *is* a whole printed line with no verb in it, and the model it denotes is a bare
 * `TargetRequirement`. There is nowhere else for it to live.
 */
object Targets {

    /**
     * The canonical name linking a [TargetRequirement] to the [EffectTarget] that reads it.
     *
     * One name is enough while every rule takes at most one target. When a rule needs two, this
     * becomes a generator and the differential's renaming already handles the rest.
     */
    const val SLOT = "target"

    /**
     * The name of the [index]-th slot, for the rules that declare more than one.
     *
     * Slot 0 keeps the bare [SLOT] so every existing single-target rule is untouched, and the rest
     * are numbered. The names are as arbitrary as [SLOT] itself — the differential compares slots by
     * *position* — so all this has to do is be distinct, which is precisely what
     * [Steps.merge] refuses to invent when two clauses each declare one.
     */
    fun slot(index: Int): String = if (index == 0) SLOT else "$SLOT $index"

    /** …and the reference half, for an effect reading the [index]-th slot. */
    fun bound(index: Int): EffectTarget = EffectTarget.BoundVariable(slot(index))

    /** "target creature you control and target creature an opponent controls" — one of several. */
    fun permanent(filter: GameObjectFilter, index: Int): TargetRequirement =
        TargetPermanent(filter = TargetFilter(filter), id = slot(index))

    /**
     * "target player" — the requirement half.
     *
     * Constructed directly rather than through `dsl.Targets.Player`, which is the facade for this
     * shape but exposes no id: it is a `val` fixed at `TargetPlayer()`. A requirement that cannot be
     * named cannot be referred to, so an effect wanting `EffectTarget.BoundVariable` has to bypass
     * it. Worth an `id` parameter on the facade; noted rather than changed here.
     */
    fun player(): TargetRequirement = TargetPlayer(id = SLOT)

    /** "target opponent" — the requirement half, constructed directly for the reason [player] is. */
    fun opponent(): TargetRequirement = TargetOpponent(id = SLOT)

    /** "target opponent or planeswalker" — the modern damage-redirection wording (CR 115.7b). */
    fun opponentOrPlaneswalker(): TargetRequirement = TargetOpponentOrPlaneswalker(id = SLOT)

    /** …and its any-player sibling, which older burn spells print instead. */
    fun playerOrPlaneswalker(): TargetRequirement = TargetPlayerOrPlaneswalker(id = SLOT)

    /**
     * "any target" — the burn-spell requirement, covering any creature, player or planeswalker.
     *
     * Constructed directly for the same reason [player] is: `dsl.Targets.Any` is a `val` fixed at
     * `AnyTarget()`, and a requirement with no id cannot be referred to by the effect that reads it.
     */
    fun any(): TargetRequirement = AnyTarget(id = SLOT)

    /** …and the reference half, for the effect that acts on it. */
    fun bound(): EffectTarget = EffectTarget.BoundVariable(SLOT)

    /** True when [target] is a reference to the single slot this grammar mints. */
    fun isBound(target: EffectTarget): Boolean =
        target is EffectTarget.BoundVariable && target.name == SLOT

    /**
     * "target creature" — one permanent on the battlefield matching [filter].
     *
     * `TargetPermanent` and `TargetCreature` are both thin factories over the same [TargetObject]
     * with the same defaults, so the choice between them is a naming one and the model is identical
     * either way; the filter is what carries the meaning. Cards written by hand reach for whichever
     * reads better at the call site, and this has to equal both.
     *
     * [optional] is the whole of "**up to** one target creature": CR 601.2c lets a spell whose
     * requirement is optional be cast choosing no target at all, which the SDK spells as
     * `optional = true` leaving the count at one. It is a parameter here rather than a second
     * function because the two wordings denote the same requirement with one field flipped — see
     * [quantifiers], which is the table that flips it.
     */
    fun permanent(filter: GameObjectFilter, optional: Boolean = false): TargetRequirement =
        if (filter == GameObjectFilter.CreatureOrPlaneswalker) {
            TargetCreatureOrPlaneswalker(optional = optional, id = SLOT)
        } else {
            TargetPermanent(optional = optional, filter = TargetFilter(filter), id = SLOT)
        }

    /**
     * **"Target creature or planeswalker" has a requirement type of its own, and it is the one the
     * corpus writes.**
     *
     * Every other noun phrase in [Filters] becomes a filter inside a `TargetObject`, and this one
     * could too — `GameObjectFilter.CreatureOrPlaneswalker` is a perfectly good `Or` of two
     * predicates. But the SDK also ships [TargetCreatureOrPlaneswalker], a requirement carrying no
     * filter at all, and 34 hand-written cards use it against none that spell the `Or`. So the two
     * spellings are real and the corpus has already chosen; a rule that printed the other would be
     * inventing a house style, exactly as the each-player damage rule in [Steps] was. Broadside
     * Barrage, Sear, Hero's Downfall and Defibrillating Current are what the differential reported.
     *
     * Only the *bare* phrase maps: "target creature or planeswalker you control" carries a
     * controller predicate that this filterless requirement cannot hold, so it stays a
     * `TargetObject` and [permanentFilter]'s reconstruct-and-compare is what keeps that honest.
     */
    private val creatureOrPlaneswalker = GameObjectFilter.CreatureOrPlaneswalker

    /**
     * The inverse: the filter [requirement] restricts to, or null when it is anything else.
     *
     * Fail-closed on every field the grammar does not spell — a requirement that also carries
     * `excludeSelf`, a non-battlefield zone or a cross-zone union says something the printed phrase
     * "target creature" does not, and confirming it would claim a reading nobody performed. The
     * final equality check is what makes that exhaustive rather than a list of fields to remember.
     */
    fun permanentFilter(requirement: TargetRequirement): GameObjectFilter? =
        targetedFilter(requirement)?.takeIf { requirement == permanent(it) }

    /**
     * The filter [requirement] restricts to, read off **whatever else it carries** — the count, the
     * `optional` flag, a `dynamicMaxCount`.
     *
     * [permanentFilter]'s fail-closed reconstruct-and-compare is the right check for a rule that
     * spells exactly one requirement shape, and the wrong one for [quantifiers], whose rows spell one
     * shape each. So the two halves are separated: this reads the noun, and the rule that used it
     * compares the whole script it would have built. Nothing is lost — a requirement carrying a
     * field the sentence does not say still fails that comparison, one level up.
     */
    fun targetedFilter(requirement: TargetRequirement): GameObjectFilter? = when (requirement) {
        is TargetCreatureOrPlaneswalker -> creatureOrPlaneswalker
        is TargetObject -> requirement.filter.baseFilter
        else -> null
    }

    /**
     * "two target lands", "up to three target creatures" — one requirement admitting several
     * targets, and the shape [quantifiers]' plural rows are built on.
     *
     * `TargetCreature` rather than [permanent] for the reason that function's KDoc gives — the two
     * factories produce the same [TargetObject] — and without its `CreatureOrPlaneswalker` branch,
     * because that branch cannot arise here: [Filters] deliberately carries no plural for a compound
     * type phrase ("creatures **and** planeswalkers" swaps the conjunction), so no plural rule can
     * ever hand this that filter.
     */
    fun several(count: Int, filter: GameObjectFilter, optional: Boolean): TargetRequirement =
        TargetCreature(count = count, optional = optional, filter = TargetFilter(filter), id = SLOT)

    /**
     * "up to X target creatures" — the count is not a number in the text but the X the spell was
     * cast for, which the SDK spells as [TargetObject.dynamicMaxCount] rather than as a large
     * `count`.
     *
     * `optional = true` is not redundant beside `dynamicMaxCount`: the cap says nothing about the
     * minimum, so without it an X of zero would fizzle the cast.
     *
     * ### [DynamicAmount.XValue] and `CastX` are a *position*, not two spellings
     *
     * They are not a two-spellings-for-one-meaning pair to pick a majority from — the SDK draws a
     * semantic line between them and calls it load-bearing. [DynamicAmount.XValue] resolves from the
     * transient resolution context (`EffectContext.xValue`) and is populated only while the object
     * carrying it is resolving; `CastX` is the durable, object-scoped reading that rides onto the
     * permanent a spell leaves behind. So the right one follows from where the requirement sits:
     *
     *  - a **spell effect** — Doppelgang, Rot-Curse Rakshasa's renew, Icy Blast — resolves with that
     *    context live, so `XValue`.
     *  - a **triggered ability whose trigger carries the announced X** — cycling, "when you cast
     *    this spell" — also `XValue`, because `TriggerDetector` deliberately routes the announced
     *    `{X}` into the trigger's context for exactly this (Valor's Flagship reads it that way, and
     *    Rampaging War Mammoth is the line this rule wins).
     *  - a **trigger reading the X off the permanent afterwards** — Lost in the Maze's "When ~
     *    enters, tap X target creatures" — must be `CastX`, and `XValue` there is silently zero.
     *
     * This rule only ever lands in the first two: it is reached as a spell clause, and `Triggers`
     * lifts that clause's requirement into a trigger that carries the X. A row that could reach the
     * third would have to translate at the lift — the one place the position is known — and there is
     * no `DynamicAmount` at all for "the X of an arbitrary activated ability", so a rule wanting that
     * position needs new SDK vocabulary before it needs a template.
     *
     * ### Only the *bare* wording maps
     *
     * "…, where X is the number of verse counters on ~" defines X from the board rather than from the
     * cost, which is a different `DynamicAmount` behind a trailing clause [Amounts] owns.
     *
     * "Tap **X** target creatures" — no "up to" — is the other one, and it declines for a stronger
     * reason than a missing template: it means *exactly* X where this row means at most X. The corpus
     * has 40 such lines (Gridlock, Malicious Advice, Rats' Feast, Aether Tide) and models them with
     * this very requirement, because [TargetObject.minCount] is a plain `Int` that cannot take a
     * [DynamicAmount] — Icy Blast's KDoc records the approximation. Reading that wording here would
     * be a lossy normalization rather than a variant, and adding a rule for it would make two printed
     * forms denote one model, which is the redundant-reading class the gate holds at zero. It stays
     * declined until the SDK can tell the two requirements apart.
     */
    fun upToX(filter: GameObjectFilter): TargetRequirement = TargetCreature(
        optional = true,
        filter = TargetFilter(filter),
        id = SLOT,
        dynamicMaxCount = DynamicAmount.XValue,
    )

    /**
     * "any number of target creatures you control" — no upper bound at all, which the SDK spells as
     * a flag rather than as a large [several] count.
     *
     * `unlimited` implies a minimum of nothing and leaves `count` ignored, so this row carries
     * neither a number word nor an `optional`: "any number of" already says both halves of what
     * "up to two" needs two fields for. `Morph`'s face-down sweep is the requirement this equals,
     * built the same way before there was a table to put it in.
     */
    fun anyNumber(filter: GameObjectFilter): TargetRequirement =
        TargetCreature(unlimited = true, filter = TargetFilter(filter), id = SLOT)

    /** The marker a [Quantifier.prefix] spells a count with, and the slot name the rule binds. */
    const val COUNT_SLOT = "n"

    private const val COUNT_PLACEHOLDER = "{$COUNT_SLOT}"

    /**
     * A **target quantifier** — the words English prints in front of "target", and what they say
     * about the requirement that follows.
     *
     * ### Why this is a table and not a slot
     *
     * Every other prefix this grammar has factored became a *value* in a slot — the trigger join's
     * `Prefix`, the fronted duration, the step-trigger's `Phases`. This one cannot, and the reason
     * is the noun behind it: "up to one target **creature**" and "up to two target **creatures**"
     * disagree in number, [Filters] keeps its singular and plural as two separately-instantiated
     * cascades because English pluralization is a column and not a suffix rule, and a `{filter}`
     * slot is one phrase fixed at declaration time. A quantifier that could be slotted would have to
     * leave the noun's number undetermined, which is exactly the thing the round trip forbids.
     *
     * So the quantifier is a row and a rule using it is a **family** of rules, one per row. What
     * that buys is the same thing a slot would have: a verb cannot have one quantifier and lack
     * another, which is precisely the state the grammar was in — "tap up to three target creatures"
     * was written and "destroy up to three target creatures" was not, because each was a hand-copied
     * template rather than a row.
     *
     * ### The one axis
     *
     * [plural] carries two consequences, and they are the same fact stated twice: a plural
     * quantifier is exactly one that admits more than one target, so it draws its noun from
     * [Filters.plural] *and* its effect is written once per chosen target
     * (`ForEachTargetEffect` over `ContextTarget(0)`) rather than once against the requirement. A
     * singular quantifier — bare "target creature", or "up to one target creature", which caps at
     * one and merely permits none — keeps the [bound] reference every single-target rule uses. There
     * is no row where the two come apart, which is why it is one column.
     *
     * @param name how a rule built from this row identifies itself in a decline diagnostic.
     * @param prefix the words before "target", ending in the space that separates them from it, with
     *   [COUNT_PLACEHOLDER] where a number word is spelled. Empty for the bare noun phrase.
     * @param requirement the requirement the row denotes over a filter, for a count of `n`. Rows
     *   that spell no count ignore `n`, which is what makes a script carrying a count they cannot
     *   express fail the caller's reconstruct-and-compare instead of printing as if it had none.
     */
    class Quantifier internal constructor(
        val name: String,
        val prefix: String,
        val plural: Boolean,
        val requirement: (n: Int, filter: GameObjectFilter) -> TargetRequirement,
    ) {
        /** True when [prefix] spells a count, and a rule using this row therefore needs an `n` slot. */
        val counted: Boolean = COUNT_PLACEHOLDER in prefix

        /** [template] with the quantifier spliced in, for a rule declaring itself from this row. */
        fun splice(template: String): String = template.replace(QUANTIFIER_PLACEHOLDER, prefix)

        override fun toString(): String = name
    }

    /** The marker a quantified template reserves for the quantifier. Not a slot — a substitution. */
    const val QUANTIFIER_PLACEHOLDER = "{q}"

    /**
     * Every quantifier English prints in front of "target", as six rows.
     *
     * They are exhaustive over the *printed* forms, not over the SDK's fields: "one or two target
     * creatures" is a `minCount` below its `count` and is a seventh row nobody has needed
     * (`Combat.returnOneOrTwoTargets` still spells it whole), and "target creature an opponent
     * controls" is a filter rather than a quantifier. Anything else a requirement can carry — a
     * `sameController`, a `totalManaValueAtMost` — is a rider on the noun phrase and belongs to a
     * layer above this list, exactly as [Filters]' controller clause does.
     */
    val quantifiers: List<Quantifier> = listOf(
        Quantifier("target", prefix = "", plural = false) { _, filter -> permanent(filter) },
        Quantifier("up to one target", prefix = "up to one ", plural = false) { _, filter ->
            permanent(filter, optional = true)
        },
        Quantifier("several targets", prefix = "$COUNT_PLACEHOLDER ", plural = true) { n, filter ->
            several(n, filter, optional = false)
        },
        Quantifier("up to several targets", prefix = "up to $COUNT_PLACEHOLDER ", plural = true) { n, filter ->
            several(n, filter, optional = true)
        },
        Quantifier("up to X targets", prefix = "up to X ", plural = true) { _, filter -> upToX(filter) },
        Quantifier("any number of targets", prefix = "any number of ", plural = true) { _, filter ->
            anyNumber(filter)
        },
    )

    /**
     * The rows whose noun stays singular — bare "target creature" and "up to one target creature".
     *
     * **A family takes this list when its plural is a different sentence rather than a plural noun.**
     * Two sentences in [Steps] are like that, and both would print something English does not write if
     * they took the whole table:
     *
     *  - damage. "~ deals 3 damage to up to one target creature" is printed 11 times; "deals 3 damage
     *    to up to two target creatures" is printed never, because a damage verb over several targets
     *    is spelled "divided as you choose among …" and is a *different requirement*
     *    (`DivideDamage`), not this one with a plural noun.
     *  - counters. "Put a +1/+1 counter on up to one target creature" is printed 19 times; the plural
     *    is "put a +1/+1 counter on **each of** up to two target creatures" (7 lines), which is the
     *    distribute sentence and its own family.
     *
     * Handing those two the plural rows would not merely fail to win cards — it would read a
     * distribute model as a sentence that means something else, the reversible-but-wrong class this
     * module's fail-closed matching exists to catch. So the row set is part of what a family declares,
     * and the *reason* a family declares a subset is always that English changes the sentence rather
     * than the noun. Where it changes only the noun ([Steps.quantifiedPermanentSteps], the pump, the
     * grants), the family takes all six.
     */
    val singularQuantifiers: List<Quantifier> = quantifiers.filterNot { it.plural }

    /**
     * "Enchant creature" — an Aura's attachment restriction, and the whole of its printed line.
     *
     * ### Why this is not a keyword ability
     *
     * Enchant *is* a keyword ability in the Comprehensive Rules (702.5), and it is the largest
     * keyword-only decline family in the corpus at 1,289 cards — but `mtg-sdk` models it as
     * [com.wingedsheep.sdk.model.CardScript.auraTarget], a plain `TargetRequirement`, so there is no
     * `KeywordAbility` for [Keywords] to parse it into. That mismatch was Phase 1's first reported
     * finding; this rule is the answer to it, and it is a rule about a *target*, not about a keyword.
     *
     * Equip, the other half of that finding, is deliberately still absent. It looks like the same
     * shape and is not: `Equip {2}` lowers at authoring time into `CardDefinition.equipCost` *and* a
     * synthesized activated ability carrying its own timing, effect and target requirement — a
     * lowering to reproduce rather than a sentence to read, and one that reaches past `CardScript`
     * into a slot [CardFragment] does not model. Enchant needs none of that.
     *
     * The restriction is spelled through [permanent] like every other filtered target, so the whole
     * of [Filters] arrives with it: "Enchant creature you control" and "Enchant land" are already
     * rows in a list this rule slots rather than rules of their own.
     */
    val enchant: Phrase<TargetRequirement> = phrase("enchant {filter}", name = "enchant") {
        slot("filter", Filters.filter)
        build { permanent(it.value("filter")) }
        match { requirement -> permanentFilter(requirement)?.let { bind("filter" to it) } }
    }
}
