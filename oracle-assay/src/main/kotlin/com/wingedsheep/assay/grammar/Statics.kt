package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Bindings
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions as SdkConditions
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantProtectionFromChosenColorToGroup
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.UntapDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Continuous abilities a permanent has just by being on the battlefield — `staticAbilities`, the
 * largest `CardScript` slot the grammar had never reached.
 *
 * The family opens on **auras**, because an aura is the card class where the whole card is two of
 * these sentences and nothing else: "Enchant creature" ([Targets.enchant]) plus one line saying what
 * the enchanted creature gets. Every later static family — lords, evasion grants, cost reduction,
 * "can't block" — lands in the same slot, so what this file buys is mostly the slot rather than the
 * thirty cards it completes today.
 *
 * ### The noun is in the text and not in the model
 *
 * `GroupFilter.attachedCreature()` is `GameObjectFilter.Permanent` scoped to `AttachedTo`: it says
 * *the thing this is attached to*, and nothing at all about that thing being a creature. So
 * "Enchanted creature has flying." and "Enchanted land has flying." denote the identical value, and
 * registering both would be genuine ambiguity — two printed forms, one model, nothing for the
 * printer to choose. Exactly one noun is spelled here, the one that is nearly all of the corpus, and
 * "Enchanted land" declines rather than being printed back as "Enchanted creature". That is the
 * fail-closed reading: a decline names the gap, a re-spelling would quietly change what the card
 * says.
 *
 * ### Why there is still no "Equipped creature …" rule, now that Equipment is read
 *
 * Same reason, one step further out — and the prediction this file made has since been carried out
 * rather than revised. An Equipment prints "Equipped creature gets +1/+1." for a value that is
 * *byte-identical* to the Aura's, because which word a card uses is a function of its type line: the
 * same class of printed-shape information as the self-reference noun ("this creature" vs "this
 * Equipment"). So it belongs to [com.wingedsheep.assay.normalize.Normalizer], and that is where it
 * went — `canonicalizeAttachmentNoun` abstracts "equipped creature" onto "enchanted creature" and
 * restores the printed word positionally, so every rule below reads both card classes and prints
 * each one back byte-exact. A second rule here would have left printing underdetermined between two
 * spellings of one model, which is the thing the module's second invariant forbids.
 *
 * ### No facade to build through
 *
 * The module's rule is that `build` goes through an SDK companion facade. Static abilities have
 * none — `dsl` publishes `Effects`, `Triggers`, `Costs` and `Conditions`, and hand-written cards
 * construct the static directly (`staticAbility { ability = ModifyStats(1, 2) }` on Holy Strength,
 * `GrantKeyword(Keyword.FLYING)` on Flight). The constructor *is* the curated surface here, exactly
 * as it is for [Replacements], and the missing `Statics` facade is a small SDK finding rather than
 * something this file should route around.
 *
 * One thing to know before reading a golden: **two SDK types share `@SerialName("ModifyStats")`** —
 * the [ModifyStats] static below and `ModifyStatsEffect`, the *effect* [Steps.pumpTargetPermanent]
 * prints as "Target creature gets +3/+3 until end of turn." They are in different polymorphic
 * hierarchies so nothing clashes, but a card's JSON shows both as `"type": "ModifyStats"`.
 */
object Statics {

    /**
     * "Enchanted creature gets +1/+2." — Holy Strength, and 37 more lines on cards already written.
     *
     * The `filter` argument is absent from the constructed value on purpose: the aura form **is**
     * [ModifyStats]'s default, which is why Holy Strength's golden carries no `filter` key either.
     * The equality against that reconstruction is what makes the omission safe — a lord's
     * "Creatures you control get +1/+1." is the same type with a real `GroupFilter`, and it refuses
     * to print here rather than printing a sentence about an aura.
     */
    private val attachedPump: Phrase<StaticAbility> =
        phrase("enchanted creature gets {mod}.", name = "enchanted creature gets") {
            slot("mod", Primitives.statModifiers)
            build {
                val (power, toughness) = it.value<Pair<Int, Int>>("mod")
                ModifyStats(power, toughness)
            }
            match { ability ->
                val stats = ability as? ModifyStats ?: return@match null
                if (stats != ModifyStats(stats.powerBonus, stats.toughnessBonus)) return@match null
                bind("mod" to (stats.powerBonus to stats.toughnessBonus))
            }
        }

    /**
     * "Enchanted creature has flying." — the granted-keyword static, slotting [Keywords.keyword]
     * whole so every parameterless keyword the grammar can spell arrives here for free.
     *
     * [GrantKeyword] holds its keyword as a `String`, which is wider than [Keyword]: the SDK also
     * uses the field for synthesized markers like `PROTECTION_FROM_BLACK` and `TOXIC_2` that no
     * enum constant names. Reading it back therefore has to find the constant rather than assume
     * one, and a value that names none declines — as does one whose keyword has no surface form in
     * [Keywords.keyword], since the slot's own printer refuses it.
     */
    private val attachedKeyword: Phrase<StaticAbility> =
        phrase("enchanted creature has {kw}.", name = "enchanted creature has a keyword") {
            slot("kw", Keywords.keyword)
            build { GrantKeyword(it.value<Keyword>("kw")) }
            match { ability ->
                val grant = ability as? GrantKeyword ?: return@match null
                val keyword = Keyword.entries.firstOrNull { it.name == grant.keyword } ?: return@match null
                if (grant != GrantKeyword(keyword)) return@match null
                bind("kw" to keyword)
            }
        }

    // ---------------------------------------------------------------------------------------
    // Combat restrictions — what a permanent may and may not do in combat
    // ---------------------------------------------------------------------------------------

    /**
     * Who a combat restriction is **about** — the one axis every member of the family shares.
     *
     * `mtg-sdk` factors this vocabulary the way English does. `CantBlock`, `CantBeBlocked`,
     * `CantBeBlockedBy`, `CantBeBlockedExceptBy`, `CantBeBlockedByMoreThan`,
     * `CantBeBlockedByFewerThan` and `CanOnlyBlockCreaturesWith` each carry the affected set as one
     * `GroupFilter` field and differ only in what they forbid, so a printed line is a subject plus a
     * restriction and the grammar is their **product**. It used to be one rule per sentence — five
     * rules reaching three of the twenty-odd combinations Oracle prints, and the three that happened
     * to be written were the three somebody had needed.
     *
     * Every combination below is printed. The three subjects, counted over "can't be blocked" alone:
     * the source 279 times, the attached permanent 25, a plural noun phrase 10. And they take
     * **disjoint model values** — `Scope.Self`, `Scope.AttachedTo`, and a battlefield scan which is
     * the only one [Filters] can produce — so printing is decided by the model rather than by the
     * alternation's order, the property every `oneOf` in this grammar is written to have.
     *
     * The one hole in the product is deliberate and is [Grammar.flagLine]'s: **the source's bare
     * "~ can't be blocked." is an `AbilityFlag`, not a static.** 19 hand-written cards spell it that
     * way against 6 that write `CantBeBlocked()`, so registering a source-scoped bare row here would
     * be a second rule for one text — `AMBIGUOUS` by construction. The attached and group subjects
     * have no such competitor: a card-level flag lands on the *Aura*, which is why Cloak of Mists,
     * Whispersilk Cloak and My Precious were carrying an evasion that did nothing.
     */
    private enum class Subject(val surface: String, val label: String) {
        SOURCE(Normalizer.SELF, "the source"),
        ATTACHED("enchanted creature", "the attached permanent"),
        GROUP("{group}", "a group"),
    }

    /** The `GroupFilter` a [Subject] denotes, reading its `{group}` slot where it has one. */
    private fun Subject.groupOf(bindings: Bindings): GroupFilter = when (this) {
        Subject.SOURCE -> GroupFilter.source()
        Subject.ATTACHED -> GroupFilter.attachedCreature()
        Subject.GROUP -> GroupFilter(bindings.value("group"))
    }

    /**
     * The slot bindings this subject needs in order to print [group], or null when [group] is a
     * value it does not spell.
     *
     * Fail-closed by round trip rather than by inspection: the candidate bindings go back through
     * [groupOf] and the result is compared, so an `excludeSelf`, an `excludeTarget`, a
     * `chosenSubtypeKey` or a `Scope.SoulbondPair` — every field a printed subject says nothing
     * about — refuses to print instead of being quietly dropped. That is the same check
     * [lordStatic] performs one field at a time, done once for the whole value.
     */
    private fun Subject.spelling(group: GroupFilter): List<Pair<String, Any?>>? {
        val pairs = if (this == Subject.GROUP) listOf("group" to group.baseFilter) else emptyList()
        return pairs.takeIf { groupOf(bind(*it.toTypedArray())) == group }
    }

    /**
     * One restriction over every subject that prints it — the family's shape.
     *
     * [clause] is the sentence *after* the subject and without its full stop, so the subject's own
     * spelling is the only thing that moves between the rows, and `{v}` inside it is the
     * restriction's variable part. Which is a different type per row — a blocker filter for three of
     * them and a count for two — so [parameter] is the slot phrase and the caller supplies the two
     * halves of its meaning, exactly as [lordStatic] takes them.
     *
     * **[parameter] is nullable because two restrictions have no variable part at all**, and that is
     * a property of their SDK types rather than of English: `CantBlock` and `CantBeBlocked` carry
     * nothing but the group, and `CantBeBlockedByMoreThan(1)` spells its count as the word "one"
     * inside a template English does not pluralize the same way ("more than one creature" against
     * "more than two creatures"). A slot whose value space is empty is not a slot; writing the shape
     * twice to avoid the null would be two copies of a fail-closed match that must not drift.
     *
     * The blocker filter is [Filters.plural] whole, so "creatures with flying", "black and/or red
     * creatures" and "creatures with power 2 or greater" are rows in a filter list rather than rules
     * here. Plural because that is the number English uses after "blocked by": a restriction names a
     * *class* of blocker, never one.
     */
    private fun <V> restriction(
        clause: String,
        name: String,
        parameter: Phrase<V>?,
        ability: (V?, GroupFilter) -> StaticAbility,
        read: (StaticAbility) -> Pair<V?, GroupFilter>?,
        subjects: List<Subject> = Subject.entries,
    ): List<Phrase<StaticAbility>> = subjects.map { subject ->
        phrase("${subject.surface} $clause.", name = "$name, ${subject.label}") {
            if (subject == Subject.GROUP) slot("group", Filters.plural)
            if (parameter != null) slot("v", parameter)
            build { bindings ->
                ability(parameter?.let { bindings.value<V>("v") }, subject.groupOf(bindings))
            }
            match { value ->
                val (parsed, group) = read(value) ?: return@match null
                val subjectBindings = subject.spelling(group) ?: return@match null
                if (value != ability(parsed, group)) return@match null
                val valueBinding = if (parameter != null) listOf("v" to parsed) else emptyList()
                bind(*(subjectBindings + valueBinding).toTypedArray())
            }
        }
    }

    /**
     * Every combat restriction, over every subject that prints it.
     *
     * The count rows spell their number as a *word with its noun* ("one creature", "two creatures")
     * rather than as a numeral, which is why they take [Cardinals.word] and why the singular is its
     * own row — the two differ in both halves. `CantBeBlockedByMoreThan(1)` also has an
     * `AbilityFlag` spelling (`CANT_BE_BLOCKED_BY_MORE_THAN_ONE`); the static is what 35
     * hand-written cards carry against 4 for the flag, so the static is what the grammar reads and
     * the flag is one more of the two-places-for-one-thing findings this file keeps recording.
     */
    private val combatRestrictions: List<Phrase<StaticAbility>> =
        // "Enchanted creature can't be blocked." (Cloak of Mists, Hot Soup), "Creatures you control
        // can't be blocked." (Jace, Arcane Strategist's static half, Detective of the Month). The
        // source is absent on purpose — see [Subject].
        restriction<Unit>(
            "can't be blocked",
            "can't be blocked",
            parameter = null,
            ability = { _, group -> CantBeBlocked(group) },
            read = { (it as? CantBeBlocked)?.let { r -> null to r.filter } },
            subjects = listOf(Subject.ATTACHED, Subject.GROUP),
        ) + restriction(
            "can't be blocked by {v}",
            "can't be blocked by",
            parameter = Filters.plural,
            ability = { blockers, group -> CantBeBlockedBy(blockers!!, group) },
            read = { (it as? CantBeBlockedBy)?.let { r -> r.blockerFilter to r.filter } },
        ) + restriction(
            "can't be blocked except by {v}",
            "can't be blocked except by",
            parameter = Filters.plural,
            ability = { blockers, group -> CantBeBlockedExceptBy(blockers!!, group) },
            read = { (it as? CantBeBlockedExceptBy)?.let { r -> r.blockerFilter to r.filter } },
        ) + restriction<Unit>(
            "can't be blocked by more than one creature",
            "can't be blocked by more than one creature",
            parameter = null,
            ability = { _, group -> CantBeBlockedByMoreThan(1, group) },
            read = {
                (it as? CantBeBlockedByMoreThan)?.takeIf { r -> r.maxBlockers == 1 }
                    ?.let { r -> null to r.filter }
            },
        ) + restriction(
            "can't be blocked by more than {v} creatures",
            "can't be blocked by more than several creatures",
            parameter = Cardinals.word,
            ability = { n, group -> CantBeBlockedByMoreThan(n!!, group) },
            read = {
                (it as? CantBeBlockedByMoreThan)
                    ?.takeIf { r -> r.maxBlockers >= 2 && Cardinals.spellable(r.maxBlockers) }
                    ?.let { r -> r.maxBlockers to r.filter }
            },
        ) + restriction(
            // "~ can't be blocked except by three or more creatures." — Troll of Khazad-dûm, and
            // menace generalized past two, which is exactly what `CantBeBlockedByFewerThan` says it
            // is. The two-blocker case is the keyword and is spelled by [Keywords]; a rule that also
            // printed `minBlockers = 2` would be a second spelling of menace, so the row's own
            // guard is what keeps them apart.
            "can't be blocked except by {v} or more creatures",
            "can't be blocked except by several or more creatures",
            parameter = Cardinals.word,
            ability = { n, group -> CantBeBlockedByFewerThan(n!!, group) },
            read = {
                (it as? CantBeBlockedByFewerThan)
                    ?.takeIf { r -> r.minBlockers >= 3 && Cardinals.spellable(r.minBlockers) }
                    ?.let { r -> r.minBlockers to r.filter }
            },
        ) + restriction<Unit>(
            // "Pirates can't block.", "Cowards can't block.", "Enchanted creature can't block."
            "can't block",
            "can't block",
            parameter = null,
            ability = { _, group -> CantBlock(group) },
            read = { (it as? CantBlock)?.let { r -> null to r.filter } },
        ) + restriction(
            // "Creatures with flying can block only creatures with flying." — Dense Canopy, the
            // group row; "Enchanted creature can block only creatures with flying." — Air Bladder.
            "can block only {v}",
            "can block only",
            parameter = Filters.plural,
            ability = { blockers, group -> CanOnlyBlockCreaturesWith(blockers!!, group) },
            read = { (it as? CanOnlyBlockCreaturesWith)?.let { r -> r.blockerFilter to r.filter } },
        )

    /**
     * "~ can't attack unless defending player controls an Island." — Deep-Sea Serpent, and the
     * island-walk-in-reverse family the older sets are full of.
     *
     * The condition is `SdkConditions.DefendingPlayerControlsLandType`, which is the SDK's own name for
     * exactly this sentence; the grammar reads the land type out of the `Exists` it lowers to rather
     * than modelling the condition itself, so the rule stays a sentence and not a second condition
     * vocabulary. The noun goes through [Filters.indefinite], which owns the article — English
     * derives it from the type's spelling and the model has nowhere to keep it.
     */
    private val cantAttackUnlessLandType: Phrase<StaticAbility> =
        phrase(
            "${Normalizer.SELF} can't attack unless defending player controls {land}.",
            name = "can't attack unless defending player controls a land type",
        ) {
            slot("land", Filters.indefinite)
            build { bindings ->
                landTypeOf(bindings.value("land"))
                    ?.let { CantAttackUnless(SdkConditions.DefendingPlayerControlsLandType(it)) }
            }
            match { value ->
                val restriction = value as? CantAttackUnless ?: return@match null
                val type = defendingPlayerLandType(restriction.condition) ?: return@match null
                if (value != CantAttackUnless(SdkConditions.DefendingPlayerControlsLandType(type))) return@match null
                bind("land" to GameObjectFilter.Land.withSubtype(type))
            }
        }

    /** The single subtype a filter names, or null when it names none or more than one. */
    private fun landTypeOf(filter: GameObjectFilter): String? =
        filter.cardPredicates.filterIsInstance<CardPredicate.HasSubtype>().singleOrNull()?.subtype?.value

    /** The land type a `DefendingPlayerControlsLandType` condition names, or null for any other. */
    private fun defendingPlayerLandType(condition: Condition): String? {
        val exists = condition as? Exists ?: return null
        val subtype = landTypeOf(exists.filter) ?: return null
        return subtype.takeIf { condition == SdkConditions.DefendingPlayerControlsLandType(it) }
    }

    // ---------------------------------------------------------------------------------------
    // Lords — a whole group of permanents, named by a filter
    // ---------------------------------------------------------------------------------------

    /**
     * "Sliver creatures get +1/+0.", "Cleric creatures have vigilance.", "All Slivers have
     * "{T}: Regenerate target Sliver."" — the lord shape, and the family every tribal set is made of.
     *
     * One shape, three members, because the three differ only in *what* the group is given: a
     * stat modifier, a keyword, or a whole activated ability. The affected set is
     * [Filters.plural] wrapped in a bare `GroupFilter`, so every noun phrase the grammar can spell
     * arrives here — "Sliver creatures", "creatures you control", "black creatures with flying" are
     * rows in a filter list rather than rules of their own.
     *
     * ### "All" is a spelling, not a meaning
     *
     * Oracle prints both "Cleric creatures have vigilance." and "All Sliver creatures get +1/+0."
     * for the same value: `GroupFilter(f)` says *every permanent matching f on the battlefield*, and
     * has no room for the word. The bare form is canonical because it is what the modern lord
     * templating uses and what the corpus overwhelmingly prints; the "All" form is an [alternate],
     * so those cards come back as a [com.wingedsheep.assay.gate.LineVerdict.VARIANT] — the reading
     * was right, only the spelling moved.
     *
     * Note what the shape deliberately cannot reach: `GroupFilter.source()` and
     * `GroupFilter.attachedCreature()` are *scoped* filters, not battlefield ones, so a lord rule
     * can never print an aura's line or a self-buff — the reconstruct-and-compare refuses them, and
     * [attachedPump] and the conditional rules below keep their own sentences.
     */
    private fun <V> lordStatic(
        verb: String,
        name: String,
        parameter: Phrase<V>,
        ability: (V, GroupFilter) -> StaticAbility,
        read: (StaticAbility) -> Pair<V, GroupFilter>?,
        // A quoted granted ability already ends in its own full stop, inside the quotation marks;
        // every other thing a lord gives out does not. The terminator is therefore the parameter's
        // business rather than the shape's, and this is the one place it shows.
        terminator: String = ".",
    ): List<Phrase<StaticAbility>> {
        fun rule(prefix: String, canonicalForm: Boolean, excludeSelf: Boolean): Phrase<StaticAbility> {
            val inner = phrase<StaticAbility>("$prefix{filter} $verb {v}$terminator", name = name) {
                slot("filter", Filters.plural)
                slot("v", parameter)
                build {
                    ability(it.value("v"), GroupFilter(it.value("filter"), excludeSelf = excludeSelf))
                }
                match { value ->
                    val (parsed, group) = read(value) ?: return@match null
                    if (group.excludeSelf != excludeSelf) return@match null
                    if (value != ability(parsed, group)) return@match null
                    bind("filter" to group.baseFilter, "v" to parsed)
                }
                canonical = canonicalForm
            }
            return if (canonicalForm) inner else alternate(inner)
        }
        return listOf(
            rule("", canonicalForm = true, excludeSelf = false),
            rule("all ", canonicalForm = false, excludeSelf = false),
            // "Other creatures you control get +0/+1." — Veteran Armorer, and every lord that leaves
            // itself out. "Other" is `GroupFilter.excludeSelf`, a field on the *iteration* rather
            // than on the noun, which is why it is a prefix here and not a [Filters] layer — the
            // same argument [Steps.otherGroupStep] makes on the effect side.
            rule("other ", canonicalForm = true, excludeSelf = true),
        )
    }

    /**
     * "All Slivers have protection from the chosen color." — Ward Sliver.
     *
     * A lord line with **no parameter at all**: the quality is the colour chosen as the source
     * entered, which `GrantProtectionFromChosenColorToGroup` names and no word in the sentence
     * varies. So it is a rule rather than a row of [lordStatic], whose whole shape is the slot the
     * verb takes.
     */
    private fun chosenColourProtection(prefix: String, canonicalForm: Boolean): Phrase<StaticAbility> {
        val inner = phrase<StaticAbility>(
            "$prefix{filter} have protection from the chosen color.",
            name = "a group has protection from the chosen colour",
        ) {
            slot("filter", Filters.plural)
            build { GrantProtectionFromChosenColorToGroup(GroupFilter(it.value("filter"))) }
            match { value ->
                val grant = value as? GrantProtectionFromChosenColorToGroup ?: return@match null
                if (value != GrantProtectionFromChosenColorToGroup(grant.filter)) return@match null
                bind("filter" to grant.filter.baseFilter)
            }
            canonical = canonicalForm
        }
        return if (canonicalForm) inner else alternate(inner)
    }

    // ---------------------------------------------------------------------------------------
    // Spell-affecting statics — the ones whose subject is a spell rather than a permanent
    // ---------------------------------------------------------------------------------------

    /** "Sliver spells can't be countered." — Root Sliver. */
    private val spellsCantBeCountered: Phrase<StaticAbility> =
        phrase("{filter} spells can't be countered.", name = "a spell type can't be countered") {
            slot("filter", Filters.subtypeOnly)
            build { GrantCantBeCountered(it.value("filter")) }
            match { value ->
                val grant = value as? GrantCantBeCountered ?: return@match null
                if (value != GrantCantBeCountered(grant.filter)) return@match null
                bind("filter" to grant.filter)
            }
        }

    /** "Any player may cast Sliver spells as though they had flash." — Quick Sliver. */
    private val spellsHaveFlash: Phrase<StaticAbility> =
        phrase(
            "any player may cast {filter} spells as though they had flash.",
            name = "a spell type may be cast as though it had flash",
        ) {
            slot("filter", Filters.subtypeOnly)
            build { GrantFlashToSpellType(it.value("filter")) }
            match { value ->
                val grant = value as? GrantFlashToSpellType ?: return@match null
                if (value != GrantFlashToSpellType(grant.filter)) return@match null
                bind("filter" to grant.filter)
            }
        }


    // ---------------------------------------------------------------------------------------
    // Conditional statics — "as long as …"
    // ---------------------------------------------------------------------------------------

    /**
     * "This creature gets +3/+3 as long as no opponent controls a creature." — Vexing Beetle, and
     * "As long as you control a Beast, this creature gets +2/+2 and has trample." — Skirk Outrider.
     *
     * The SDK wraps the whole static in a `ConditionalStaticAbility`, so the condition is a slot
     * around an ability rather than a field on one — which is why this is a wrapper family and not a
     * parameter on the rules above, exactly as [Steps.conditionalClause] is a wrapper rather than a
     * field.
     *
     * **Both printed orders exist and mean the same thing.** Oracle puts the clause after the effect
     * on some cards and in front on others, and the model has no room for which; the trailing form
     * is canonical because it is the commoner one, and the leading form is an [alternate].
     *
     * The affected set is the source in every card here, so it is a literal `GroupFilter.source()`
     * rather than a slot — a *conditional lord* is a different sentence with a noun phrase in it,
     * and it declines rather than being printed as one about this creature.
     */
    /**
     * What the conditional sentence says about the source: a stat change, a keyword run, or both.
     *
     * Three forms rather than three rules, for [Keywords.keywordRun]'s reason one level up — the
     * count of granted keywords moved into the slot, and once it had, "gets +2/+0 and has trample"
     * and "has flying and vigilance" were the same sentence with one clause missing. The forms take
     * disjoint printed templates *and* disjoint ability lists (a pump is always first, and
     * [KEYWORDS] has none), so nothing here is left for the printer to choose.
     */
    private enum class ConditionalForm(val pumps: Boolean, val grants: Boolean) {
        PUMP(pumps = true, grants = false),
        PUMP_AND_KEYWORDS(pumps = true, grants = true),
        KEYWORDS(pumps = false, grants = true),
    }

    private fun conditionalSelfStatic(
        leading: Boolean,
        form: ConditionalForm,
    ): Phrase<List<StaticAbility>> {
        fun abilitiesFor(
            modifiers: Pair<Int, Int>?,
            keywords: List<Keyword>,
            condition: Condition,
        ): List<StaticAbility> = listOfNotNull(
            modifiers?.let {
                ConditionalStaticAbility(ModifyStats(it.first, it.second, GroupFilter.source()), condition)
            },
        ) + keywords.map { ConditionalStaticAbility(GrantKeyword(it, GroupFilter.source()), condition) }

        val effect = buildString {
            append(Normalizer.SELF)
            if (form.pumps) append(" gets {mod}")
            if (form.grants) append(if (form.pumps) " and has {kws}" else " has {kws}")
        }
        val template =
            if (leading) "as long as {cond}, $effect." else "$effect as long as {cond}."
        val name = "the source" + (if (form.pumps) " gets" else "") +
            (if (form.grants) (if (form.pumps) " and has" else " has") else "") + " under a condition"

        val inner = phrase<List<StaticAbility>>(template, name = name) {
            slot("cond", Conditions.condition)
            if (form.pumps) slot("mod", Primitives.statModifiers)
            if (form.grants) slot("kws", Keywords.keywordRun)
            build { bindings ->
                abilitiesFor(
                    if (form.pumps) bindings.value<Pair<Int, Int>>("mod") else null,
                    if (form.grants) bindings.value("kws") else emptyList(),
                    bindings.value("cond"),
                )
            }
            match { abilities ->
                val first = abilities.firstOrNull() as? ConditionalStaticAbility ?: return@match null
                val modifiers = if (form.pumps) {
                    val stats = first.ability as? ModifyStats ?: return@match null
                    stats.powerBonus to stats.toughnessBonus
                } else {
                    null
                }
                val granted = abilities.drop(if (form.pumps) 1 else 0)
                if (form.grants == granted.isEmpty()) return@match null
                val keywords = granted.map { ability ->
                    val conditional = ability as? ConditionalStaticAbility ?: return@match null
                    val grant = conditional.ability as? GrantKeyword ?: return@match null
                    Keyword.entries.firstOrNull { it.name == grant.keyword } ?: return@match null
                }
                if (abilities != abilitiesFor(modifiers, keywords, first.condition)) return@match null
                bind("cond" to first.condition, "mod" to modifiers, "kws" to keywords)
            }
            canonical = !leading
        }
        return if (leading) alternate(inner) else inner
    }

    /**
     * "Creatures can't attack you unless their controller pays {2} for each creature they control
     * that's attacking you." — Windborn Muse, and the whole Propaganda family.
     *
     * One printed sentence, one SDK type, one variable: the tax per attacker. Everything else in the
     * sentence restates what `AttackTax` already means, which is why the rest is literal.
     */
    private val attackTax: Phrase<StaticAbility> = phrase(
        "creatures can't attack you unless their controller pays {cost} for each creature they " +
            "control that's attacking you.",
        name = "attack tax",
    ) {
        slot("cost", Primitives.manaCost)
        build { bindings ->
            Primitives.genericAmount(bindings.value("cost"))?.let { AttackTax(DynamicAmount.Fixed(it)) }
        }
        match { value ->
            val tax = value as? AttackTax ?: return@match null
            val amount = (tax.amountPerAttacker as? DynamicAmount.Fixed)?.amount ?: return@match null
            if (value != AttackTax(DynamicAmount.Fixed(amount))) return@match null
            bind("cost" to ManaCost.parse("{$amount}"))
        }
    }

    /**
     * "This creature gets +2/+2 for each face-down creature on the battlefield." — Primal Whisperer;
     * "~ gets +1/+0 for each artifact you control." — Nim Lasher, and twenty more.
     *
     * The dynamic sibling of [attachedPump]: the bonus is a multiple of a battlefield count rather
     * than a number, which the SDK spells as a different static type. Both numbers are in the text —
     * "+2/+2" is a `Multiply(count, 2)` in each half, "+1/+0" is the bare tally beside a `Fixed(0)` —
     * and `GrantDynamicStatsEffect` carries the halves separately, so [scaled] lowers each one on its
     * own. The rule used to require the two to *agree*, on the reasoning that a printed pair can only
     * spell one multiplier; "+1/+0" spells two, and eleven of the family's twenty-one lines are
     * asymmetric, so it read none of them.
     *
     * The clause after the noun phrase is [Amounts.scopes] rather than the literal it used to be.
     * Nim Lasher is what that literal cost: its line is this sentence with the *other* row of the
     * layer on the end, and it died on the full stop where the frozen " on the battlefield" was
     * expected. Note that the filter is scope-free here for [Amounts.Scope.narrowing]'s reason —
     * "for each artifact you control" says who controls them once, in the row, and the noun phrase
     * must not say it again.
     */
    private fun selfPumpPerCount(scope: Amounts.Scope): Phrase<StaticAbility> {
        fun abilityFor(mod: Pair<Int, Int>, counted: GameObjectFilter): StaticAbility {
            val count = DynamicAmount.AggregateBattlefield(scope.player, counted)
            return GrantDynamicStatsEffect(GroupFilter.source(), scaled(count, mod.first), scaled(count, mod.second))
        }
        return phrase(
            "${Normalizer.SELF} gets {mod} for each {counted}${scope.surface}.",
            name = "the source gets a multiple of a count of ${scope.where}",
        ) {
            slot("mod", Primitives.statModifiers)
            slot("counted", Filters.filter)
            build { bindings ->
                val counted = scope.narrowing(bindings.value("counted")) ?: return@build null
                val mod = bindings.value<Pair<Int, Int>>("mod")
                if (mod.first == 0 && mod.second == 0) return@build null
                abilityFor(mod, counted)
            }
            match { value ->
                val stats = value as? GrantDynamicStatsEffect ?: return@match null
                val aggregate = countedIn(stats.powerBonus) ?: countedIn(stats.toughnessBonus) ?: return@match null
                if (aggregate.player != scope.player) return@match null
                val power = multiplierOf(stats.powerBonus, aggregate) ?: return@match null
                val toughness = multiplierOf(stats.toughnessBonus, aggregate) ?: return@match null
                val mod = power to toughness
                if (value != abilityFor(mod, aggregate.filter)) return@match null
                val counted = scope.narrowing(aggregate.filter) ?: return@match null
                bind("mod" to mod, "counted" to counted)
            }
        }
    }

    /**
     * One half of a printed modifier pair as a [DynamicAmount] over [count].
     *
     * Three cases and not one, because the SDK spells the three numbers three ways and the corpus is
     * unanimous about which: "+1/+0" is the bare aggregate beside a `Fixed(0)` — Nim Lasher's golden,
     * and every other card in the family — while "+2/+2" is a `Multiply`. Writing `Multiply(count, 1)`
     * for the common half would be a model no hand-written card carries, which the differential would
     * report on every card this rule reads.
     */
    private fun scaled(count: DynamicAmount, multiplier: Int): DynamicAmount = when (multiplier) {
        0 -> DynamicAmount.Fixed(0)
        1 -> count
        else -> DynamicAmount.Multiply(count, multiplier)
    }

    /** The battlefield tally inside one half of a modifier pair, or null when that half is a constant. */
    private fun countedIn(amount: DynamicAmount): DynamicAmount.AggregateBattlefield? = when (amount) {
        is DynamicAmount.AggregateBattlefield -> amount
        is DynamicAmount.Multiply -> amount.amount as? DynamicAmount.AggregateBattlefield
        else -> null
    }

    /** [scaled]'s inverse against a known tally; null when this half is not that tally scaled at all. */
    private fun multiplierOf(amount: DynamicAmount, count: DynamicAmount.AggregateBattlefield): Int? = when {
        amount == DynamicAmount.Fixed(0) -> 0
        amount == count -> 1
        amount is DynamicAmount.Multiply && amount.amount == count -> amount.multiplier
        else -> null
    }

    val all: List<Phrase<StaticAbility>> = listOf(
        attachedPump,
        attachedKeyword,
        spellsCantBeCountered,
        spellsHaveFlash,
        attackTax,
        chosenColourProtection("", canonicalForm = true),
        chosenColourProtection("all ", canonicalForm = false),
        // "Untap all permanents you control during each other player's untap step." — Seedborn Muse.
        // A `data object`, so the whole sentence is one value and the rule is a constant.
        constant<StaticAbility>("untap all permanents you control during each other player's untap step.", UntapDuringOtherUntapSteps),
        // Goblin Goon's two lines. `Conditions.ControlMoreCreatures` compares your creature count
        // against your opponents' and says nothing about combat, so the printed noun — "defending
        // player" on the attack half, "attacking player" on the block half — is a fact about *which
        // sentence* the condition is in rather than about the condition. Registering it in
        // [Conditions] would give one value two printed forms and leave the printer to choose;
        // spelling it into each sentence keeps one form per model, and is the same argument
        // [Filters] makes about "enchanted creature" versus "equipped creature".
        constant<StaticAbility>(
            "${Normalizer.SELF} can't attack unless you control more creatures than defending player.",
            CantAttackUnless(SdkConditions.ControlMoreCreatures),
        ),
        constant<StaticAbility>(
            "${Normalizer.SELF} can't block unless you control more creatures than attacking player.",
            CantBlockUnless(SdkConditions.ControlMoreCreatures),
        ),
        cantAttackUnlessLandType,
    ) + combatRestrictions + Amounts.perScope(::selfPumpPerCount) + SpellCosts.all + lordStatic(
        "get", "a group gets",
        parameter = Primitives.statModifiers,
        ability = { (power, toughness), group -> ModifyStats(power, toughness, group) },
        read = { (it as? ModifyStats)?.let { s -> (s.powerBonus to s.toughnessBonus) to s.filter } },
    ) + lordStatic(
        "have", "a group has a keyword",
        parameter = Keywords.keyword,
        ability = { keyword, group -> GrantKeyword(keyword, group) },
        read = { ability ->
            val grant = ability as? GrantKeyword ?: return@lordStatic null
            Keyword.entries.firstOrNull { it.name == grant.keyword }?.let { it to grant.filter }
        },
    ) + lordStatic(
        // "All Slivers have "{T}: Regenerate target Sliver."" — a *whole activated ability* as the
        // thing granted, which is why [Activated.ability] is a slot here: the quoted text is the
        // same English an ability line prints, so the entire activated-ability grammar arrives with
        // one row and no verb is restated.
        "have", "a group has an activated ability",
        parameter = Activated.quoted,
        ability = { granted, group -> GrantActivatedAbility(granted, group) },
        read = { (it as? GrantActivatedAbility)?.let { g -> g.ability to g.filter } },
        terminator = "",
    ) + Granted.statics

    val static: Phrase<StaticAbility> = oneOf("a static ability", all)

    /** One static, lifted into the one-element list a line usually denotes. */
    private val single: Phrase<List<StaticAbility>> = phrase("{one}", name = "a static ability") {
        slot("one", static)
        build { listOf(it.value<StaticAbility>("one")) }
        match { it.singleOrNull()?.let { ability -> bind("one" to ability) } }
    }

    /**
     * "Enchanted creature gets +2/+2 and has flying.", "…gets +2/+0 and has first strike, vigilance,
     * trample, and haste." — a pump and **one static per granted keyword**, from one sentence.
     *
     * [Keywords.qualityRun] (CR 702.16g's joined protection) and [Mana.alternatives] ("{T}: Add {B}
     * or {G}." as two abilities sharing a cost) are the other rules that denote several models from
     * one phrase, and the answer is the same one each time: the rule denotes a *list*, and [single]
     * lifts the ordinary one-ability line into the same shape so nothing downstream has to know
     * which it got. What is emphatically not the answer is a compound SDK type meaning "pump and
     * grant" — the model is already right.
     *
     * The count of keywords is [Keywords.keywordRun]'s slot rather than the rule's, which is what
     * makes Sword of Vengeance's four the same rule as Holy Strength's one. Twenty-seven hand-written
     * cards print this sentence and all twenty-seven order the statics as the text does, pump then
     * grants in printed order, which is what makes the reconstruction below a comparison and not a
     * convention. A card carrying them the other way round declines rather than being reordered into
     * agreement.
     */
    private val pumpAndKeyword: Phrase<List<StaticAbility>> = run {
        fun abilitiesFor(modifiers: Pair<Int, Int>, keywords: List<Keyword>) =
            listOf<StaticAbility>(ModifyStats(modifiers.first, modifiers.second)) +
                keywords.map { GrantKeyword(it) }
        phrase("enchanted creature gets {mod} and has {kws}.", name = "enchanted creature gets and has") {
            slot("mod", Primitives.statModifiers)
            slot("kws", Keywords.keywordRun)
            build { abilitiesFor(it.value("mod"), it.value("kws")) }
            match { abilities ->
                val stats = abilities.firstOrNull() as? ModifyStats ?: return@match null
                val keywords = attachedKeywords(abilities.drop(1)) ?: return@match null
                val modifiers = stats.powerBonus to stats.toughnessBonus
                if (abilities != abilitiesFor(modifiers, keywords)) return@match null
                bind("mod" to modifiers, "kws" to keywords)
            }
        }
    }

    /**
     * "Enchanted creature has reach and vigilance." — one sentence, one static *per keyword*.
     *
     * The line-level twin of [attachedKeyword], and two rules rather than one because a run of one
     * is the single-ability rule reached through [single]: [Keywords.severalKeywords] therefore
     * starts at two, so the two rules take disjoint list sizes and printing is decided by the model.
     */
    private val attachedKeywordRun: Phrase<List<StaticAbility>> =
        phrase("enchanted creature has {kws}.", name = "enchanted creature has keywords") {
            slot("kws", Keywords.severalKeywords)
            build { it.value<List<Keyword>>("kws").map { keyword -> GrantKeyword(keyword) } }
            match { abilities ->
                if (abilities.size < 2) return@match null
                val keywords = attachedKeywords(abilities) ?: return@match null
                if (abilities != keywords.map { GrantKeyword(it) }) return@match null
                bind("kws" to keywords)
            }
        }

    /** The keywords a run of plain [GrantKeyword] statics names, or null if any is something else. */
    private fun attachedKeywords(abilities: List<StaticAbility>): List<Keyword>? {
        if (abilities.isEmpty()) return null
        return abilities.map { ability ->
            val grant = ability as? GrantKeyword ?: return null
            Keyword.entries.firstOrNull { it.name == grant.keyword } ?: return null
        }
    }

    /**
     * What one static-ability line denotes: usually one ability, and for the rules that spell a
     * keyword run, one per keyword — plus a pump where the sentence has one.
     *
     * The alternatives take disjoint list *shapes* — [single] is exactly one, [attachedKeywordRun]
     * two or more grants, [pumpAndKeyword] a pump first, and each [ConditionalForm] a distinct
     * combination of the two under a condition — so printing is decided by the model rather than by
     * the alternation's order, the property every `oneOf` in this grammar is written to have.
     */
    val line: Phrase<List<StaticAbility>> = oneOf(
        "static abilities",
        listOf(pumpAndKeyword, attachedKeywordRun) +
            ConditionalForm.entries.flatMap { form ->
                listOf(
                    conditionalSelfStatic(leading = false, form = form),
                    conditionalSelfStatic(leading = true, form = form),
                )
            } +
            single,
    )
}
