package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Bindings
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions as SdkConditions
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantProtectionFromChosenColorToGroup
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.UntapDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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

    /**
     * "Enchanted creature doesn't untap during its controller's untap step." — Shackles, Claustrophobia,
     * Charmed Sleep, and the rest of the untap-lock Auras.
     *
     * The SDK grants the restriction as the `DOESNT_UNTAP` ability flag riding [GrantKeyword]'s string
     * field — the untap step skips any permanent whose projected keywords hold it — so this is
     * [attachedKeyword] with a sentence of its own instead of a keyword word. It is not a row in
     * [Keywords.keyword]: the flag has no printed keyword, so "has doesn't untap" would be a spelling
     * that exists nowhere, and the printer must never be able to produce it.
     */
    private val attachedDoesntUntap: Phrase<StaticAbility> = constant(
        "enchanted creature doesn't untap during its controller's untap step.",
        GrantKeyword(AbilityFlag.DOESNT_UNTAP.name),
    )

    /**
     * "This creature doesn't untap during your untap step if it attacked during your last turn." —
     * Goblin Rock Sled, and the counter-gated lands and creatures beside it.
     *
     * The conditional twin of the unconditional line [Grammar] reads as a card flag: once the lock
     * depends on a condition the SDK can no longer spell it as a property of the card, so it is a
     * `ConditionalStaticAbility` around the same `DOESNT_UNTAP` grant aimed at the source — the
     * shape [conditionalSelfStatic] builds for "has flying as long as …", with "if" where that one
     * says "as long as" because that is the word every one of these cards prints.
     */
    private val conditionalSelfDoesntUntap: Phrase<StaticAbility> = phrase(
        "${Normalizer.SELF} doesn't untap during your untap step if {cond}.",
        name = "the source doesn't untap under a condition",
    ) {
        slot("cond", Conditions.condition)
        build { ConditionalStaticAbility(GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.source()), it.value("cond")) }
        match { ability ->
            val conditional = ability as? ConditionalStaticAbility ?: return@match null
            if (conditional.ability != GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.source())) return@match null
            bind("cond" to conditional.condition)
        }
    }

    /**
     * `Enchanted creature has "Whenever this creature deals combat damage, create a Blood token."` —
     * Ceremonial Knife, and 149 more attachment lines that grant a **whole ability** instead of a
     * keyword.
     *
     * The attached-position twin of the `have "…"` row in [lordStatic]'s activated instantiation,
     * and written the same way: the quotes are the whole rule, and everything inside them is the
     * grammar an ability line already prints. So one row here inherits the entire triggered-ability
     * (and activated-ability) vocabulary rather than restating a verb.
     *
     * **Two rules over one printed shape, disjoint by what the quoted text can be.** A trigger
     * sentence opens with "whenever" / "when" / "at" and an activated ability has a cost before a
     * colon, so exactly one of [Triggers.trigger] and [Activated.quoted] can read any given
     * quotation and the printer is never left choosing. They *are* two SDK types —
     * `GrantTriggeredAbility` and `GrantActivatedAbility` — read by two different subsystems
     * (`TriggerDetector` and `LegalActionsCalculator`), so folding them into one rule would be
     * folding two models. The corpus splits 85 activated to 64 triggered on this line, which is why
     * neither is the real one with the other as an afterthought.
     *
     * Neither is a layer effect, so neither belongs in a `CompositeStaticAbility` beside the pump in
     * [pumpAndQuotedAbility] — the [Statics] header says why: only abilities projected through the
     * layer system are grouped, and these two are read elsewhere.
     *
     * ### The one thing inside the quotes this rule inherits and cannot fix
     *
     * `Normalizer.selfReferenceForms` abstracts **both** "this permanent" and the card's own printed
     * name to `~`, and says in its own KDoc that "the rules that read `~` must not treat it as
     * authoritative inside a quoted ability". That bites exactly once, in a *cost*: Basal Sliver's
     * "Sacrifice this permanent" means the Sliver holding the ability
     * (`AbilityCost.SacrificeSelf`), while Deconstruction Hammer's "Sacrifice Deconstruction
     * Hammer" means the granting Equipment (`AbilityCost.SacrificeGrantingPermanent`, CR 201.5a) —
     * two objects, one token, and nothing left in the normalized text to tell them apart.
     *
     * [Activated.quoted] reads the majority spelling and Deconstruction Hammer reports as a
     * differential divergence rather than being silently re-aimed. Remapping the cost by position
     * was tried and reverted in the same change: it fixed the Equipment and broke five Slivers,
     * which is the measurement that says the distinction is not a property of the position. The fix
     * belongs to `normalize/`, where the printed word still exists.
     */
    private val quotedTriggerGrant: Phrase<StaticAbility> =
        phrase("\"{ability}\"", name = "a quoted triggered ability grant") {
            slot("ability", Triggers.trigger)
            build { GrantTriggeredAbility(it.value("ability")) }
            match { ability ->
                val grant = ability as? GrantTriggeredAbility ?: return@match null
                if (grant != GrantTriggeredAbility(grant.ability)) return@match null
                bind("ability" to grant.ability)
            }
        }

    /**
     * [attachedQuotedTrigger]'s activated twin; see its KDoc for why the two are separate rules.
     *
     * The quotation marks live in [Activated.quoted] rather than in this template, because that rule
     * already owns them for the lord position and a second spelling of the same delimiters would be
     * a second reading of every granted activated ability.
     */
    private val quotedActivatedGrant: Phrase<StaticAbility> =
        phrase("{ability}", name = "a quoted activated ability grant") {
            slot("ability", Activated.quoted)
            build { GrantActivatedAbility(it.value("ability")) }
            match { ability ->
                val grant = ability as? GrantActivatedAbility ?: return@match null
                if (grant != GrantActivatedAbility(grant.ability)) return@match null
                bind("ability" to grant.ability)
            }
        }

    /** Either quoted grant, over one position; see [quotedTriggerGrant] for why they stay two rules. */
    private val quotedGrant: Phrase<StaticAbility> =
        oneOf("a quoted granted ability", quotedTriggerGrant, quotedActivatedGrant)

    /** `Enchanted creature has "…"` — the attachment position, over [quotedGrant]. */
    private val attachedQuotedAbility: Phrase<StaticAbility> =
        phrase("enchanted creature has {granted}", name = "enchanted creature has an ability") {
            slot("granted", quotedGrant)
            build { it.value<StaticAbility>("granted") }
            match { bind("granted" to it) }
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
     * The entity a [Subject]'s own pronoun denotes, or null where English has no pronoun for it.
     *
     * The singular subjects are one permanent, so "it" names them; a group is a set, and Oracle
     * writes a clause about a set's members with a relative clause rather than a pronoun ("creatures
     * that are attacking alone"), which is a different sentence over a different model — a filter
     * predicate rather than a condition. Returning null is what keeps [attackingAloneEvasion] from
     * printing an "it" for a plural subject.
     */
    private fun Subject.pronounEntity(): EffectTarget? = when (this) {
        Subject.SOURCE -> EffectTarget.Self
        Subject.ATTACHED -> EffectTarget.EnchantedPermanent
        Subject.GROUP -> null
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
        ) + Subject.entries.mapNotNull(::attackingAloneEvasion)

    /**
     * "~ can't be blocked as long as it's attacking alone." — Gutter Skulker and five others;
     * "Enchanted creature can't be blocked as long as it's attacking alone." — Gutter Shortcut, the
     * Aura the same card's disturb cast puts on the stack, and Aerie Auxiliary.
     *
     * **This is not the conditional wrapper the band declined to write.** That one slots
     * [Conditions.condition] after any restriction, and its probe finished 2 of 29 lines because the
     * payload was the condition vocabulary rather than the restriction — see [Subject]'s band notes.
     * The clause here is not a row of that vocabulary at all: its subject is a *pronoun back to the
     * restriction's own subject*, so there is nothing free to slot, and the whole sentence is two
     * rows over the [Subject] table this family already has. The two are reachable from disjoint
     * surfaces ("as long as it's attacking alone" against "as long as {cond}"), so writing the
     * general wrapper later does not collide with this — it would have to exclude the pronoun, which
     * `Conditions` cannot spell.
     *
     * The affected group and the condition's entity are the *same* permanent said twice, which is
     * how the SDK spells it: `CantBeBlocked` carries the group and `ConditionalStaticAbility` carries
     * an `EntityMatches` over the role. So the two halves are derived from one [Subject] and the
     * `match` rebuilds both, which is what stops the rule printing "it" for a sentence whose
     * condition is about some other permanent.
     *
     * "Attacking alone" is CR 506.5, and it is a `StatePredicate` rather than a condition type —
     * `GameObjectFilter.Any.attackingAlone()` is the whole of what the clause says.
     */
    private fun attackingAloneEvasion(subject: Subject): Phrase<StaticAbility>? {
        val entity = subject.pronounEntity() ?: return null
        val filter = GameObjectFilter.Any.attackingAlone()
        fun ability(group: GroupFilter): StaticAbility =
            ConditionalStaticAbility(CantBeBlocked(group), EntityMatches(entity, filter))
        return phrase(
            "${subject.surface} can't be blocked as long as it's attacking alone.",
            name = "can't be blocked while attacking alone, ${subject.label}",
        ) {
            build { bindings -> ability(subject.groupOf(bindings)) }
            match { value ->
                val conditional = value as? ConditionalStaticAbility ?: return@match null
                val group = (conditional.ability as? CantBeBlocked)?.filter ?: return@match null
                val bindings = subject.spelling(group) ?: return@match null
                if (value != ability(group)) return@match null
                bind(*bindings.toTypedArray())
            }
        }
    }

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
    // Assigning combat damage by toughness — the Doran family
    // ---------------------------------------------------------------------------------------

    /**
     * "Each creature you control assigns combat damage equal to its toughness rather than its
     * power." — Assault Formation and Huatli, the Sun's Heart; "Each creature assigns …" — Doran,
     * the Siege Tower; and with the qualifier, "Each creature you control **with toughness greater
     * than its power** assigns …" — Bedrock Tortoise, Tapestry Warden, Ancient Lumberknot.
     *
     * The family is two rules and one flag, and the flag is why it is two rules rather than a slot:
     * `onlyWhenToughnessGreaterThanPower` is a `Boolean`, and English spells its two values in two
     * different *places* — set, it is a clause inside the noun phrase; clear, it is nothing at all.
     * A slot has one position, and an empty surface for the common value would give the printer a
     * choice between "each creature you control assigns" and "each creature you control  assigns".
     * So the boolean is a row, which is the same answer [restriction] gives its parameterless
     * members for the same reason.
     *
     * **The subject is a distributive singular**, and that is the one thing about this family that
     * is not shared with any other group static in this file. Every lord and every combat
     * restriction prints its group as a plural noun phrase ("creatures you control get +1/+1"), so
     * they slot [Filters.plural]; Oracle prints *this* sentence as "each creature you control …
     * assigns … **its** toughness", agreeing the verb and the pronoun with the singular, because
     * the rule it states is about one creature at a time. Same `GroupFilter` value, different
     * number on the noun — so the slot is [Filters.filter] and the "each" is a literal.
     *
     * The reconstruct-and-compare is what keeps `GroupFilter`'s other fields honest: a `Scope.Self`
     * or an `excludeSelf` that this sentence says nothing about refuses to print rather than being
     * silently dropped, and [attachedDamageByToughness] owns the one scope that does have a
     * printed form.
     */
    private fun damageByToughness(qualified: Boolean): Phrase<StaticAbility> {
        val qualifier = if (qualified) " with toughness greater than its power" else ""
        fun abilityFor(filter: GameObjectFilter) =
            AssignDamageEqualToToughness(GroupFilter(filter), qualified)
        return phrase(
            "each {group}$qualifier assigns combat damage equal to its toughness rather than its power.",
            name = if (qualified) "a group assigns damage by toughness where it exceeds power"
            else "a group assigns damage by toughness",
        ) {
            slot("group", Filters.filter)
            build { abilityFor(it.value("group")) }
            match { value ->
                val assign = value as? AssignDamageEqualToToughness ?: return@match null
                if (assign.onlyWhenToughnessGreaterThanPower != qualified) return@match null
                val base = assign.filter.baseFilter
                if (value != abilityFor(base)) return@match null
                bind("group" to base)
            }
        }
    }

    /**
     * "As long as equipped creature's toughness is greater than its power, it assigns combat damage
     * equal to its toughness rather than its power." — Bark of Doran, and the same value
     * [damageByToughness] builds with the group scoped to the attachment.
     *
     * A `constant` rather than a row of the pair above, because the attached subject does not merely
     * change the noun: it moves the qualifier out of the noun phrase and to the front of the
     * sentence as an "as long as" clause, and turns the subject of the main clause into "it". There
     * is no filter left to slot — `GroupFilter.attachedCreature()` is the whole value, and it is
     * [AssignDamageEqualToToughness]'s own default — so the sentence denotes one model and the rule
     * is the sentence. The unqualified attached form is not printed by any card and is not
     * registered: an unprinted row is a second spelling waiting to be chosen from.
     */
    private val attachedDamageByToughness: Phrase<StaticAbility> = constant(
        "as long as enchanted creature's toughness is greater than its power, it assigns combat " +
            "damage equal to its toughness rather than its power.",
        AssignDamageEqualToToughness(),
    )

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
     *
     * ### Why the *result* is a type parameter too
     *
     * `A` is what the sentence denotes, and for three of the four callers it is one `StaticAbility`.
     * [lordPumpAndKeyword] is the fourth and denotes a **list** — "get +2/+2 and have trample" is a
     * Layer 7c pump plus a Layer 6 grant, two abilities from one sentence, exactly as
     * [pumpAndKeyword] reads the attached version. Generalising the result is what lets that rule
     * inherit all three prefix rows ("", "all ", "other ") and the reconstruct-and-compare rather
     * than restating them; the alternative was a second three-row generator that would drift from
     * this one the first time either was edited.
     */
    private fun <V, A> lordStatic(
        verb: String,
        name: String,
        parameter: Phrase<V>,
        ability: (V, GroupFilter) -> A,
        read: (A) -> Pair<V, GroupFilter>?,
        // A quoted granted ability already ends in its own full stop, inside the quotation marks;
        // every other thing a lord gives out does not. The terminator is therefore the parameter's
        // business rather than the shape's, and this is the one place it shows.
        terminator: String = ".",
    ): List<Phrase<A>> {
        fun rule(prefix: String, canonicalForm: Boolean, excludeSelf: Boolean, chosenType: Boolean): Phrase<A> {
            val key = if (chosenType) CHOSEN_TYPE_KEY else null
            val qualifier = if (chosenType) " of the chosen type" else ""
            val inner = phrase<A>("$prefix{filter}$qualifier $verb {v}$terminator", name = name) {
                slot("filter", Filters.plural)
                slot("v", parameter)
                build {
                    ability(
                        it.value("v"),
                        GroupFilter(it.value("filter"), excludeSelf = excludeSelf, chosenSubtypeKey = key),
                    )
                }
                match { value ->
                    val (parsed, group) = read(value) ?: return@match null
                    if (group.excludeSelf != excludeSelf) return@match null
                    if (group.chosenSubtypeKey != key) return@match null
                    if (value != ability(parsed, group)) return@match null
                    bind("filter" to group.baseFilter, "v" to parsed)
                }
                canonical = canonicalForm
            }
            return if (canonicalForm) inner else alternate(inner)
        }
        // "Creatures you control of the chosen type get +1/+1." — Etchings of the Chosen, Cover of
        // Darkness, and every lord over the creature type its source chose as it entered. The
        // qualifier is `GroupFilter.chosenSubtypeKey`, a field on the group rather than a predicate
        // on the noun, which is why it is a row of this product and not a [Filters] layer: the
        // object-filter spelling of the same words, `CardPredicate.HasChosenSubtype`, is what a
        // trigger or a target names, and a layer would hand the lord a second printer for one
        // sentence. Only the default key is spelled — a pipeline's own stored choice (Walking
        // Desecration) has a key the text does not name, and refuses to print.
        return listOf(false, true).flatMap { chosenType ->
            listOf(
                rule("", canonicalForm = true, excludeSelf = false, chosenType = chosenType),
                rule("all ", canonicalForm = false, excludeSelf = false, chosenType = chosenType),
                // "Other creatures you control get +0/+1." — Veteran Armorer, and every lord that leaves
                // itself out. "Other" is `GroupFilter.excludeSelf`, a field on the *iteration* rather
                // than on the noun, which is why it is a prefix here and not a [Filters] layer — the
                // same argument [Steps.otherGroupStep] makes on the effect side.
                rule("other ", canonicalForm = true, excludeSelf = true, chosenType = chosenType),
            )
        }
    }

    /** `GroupFilter.ChosenSubtypeCreatures`' default key — the choice the source made as it entered. */
    private const val CHOSEN_TYPE_KEY = "chosenCreatureType"

    /**
     * What a multi-layer lord gives its group: a base power and toughness, optionally some keywords,
     * and a creature type — the three halves of [compositeLord]'s sentence, held together so the
     * shape's single `{v}` slot can carry them.
     */
    private data class Bodied(
        val stats: Pair<Int, Int>,
        val keywords: List<Keyword>,
        val subtype: Subtype,
    )

    /**
     * "Creatures you control have base power and toughness 6/6 and are Oozes in addition to their
     * other types." — March of the World Ooze, Kudo, Raised by Giants, and Sigarda's Summons with a
     * keyword clause in the middle.
     *
     * The one lord row whose value is a [CompositeStaticAbility], and the reason is CR 613.6 rather
     * than a wish to keep the sentence together: the printed ability applies in Layer 4
     * (the added creature type), Layer 6 (the keywords) and Layer 7b (the base P/T), and an effect
     * that applies in several layers locks its affected set as it *first* starts to apply. Three
     * separate statics would each re-resolve their own set as the layers are walked, so they are a
     * different — and wrong — card. All four hand-written cards in the family spell it this way.
     *
     * That makes this rule the grammar's first producer of `CompositeStaticAbility`, which is why it
     * is one rule rather than three rows of the plain lord shape: the composite is the value, not a
     * convenience.
     *
     * **Two spellings, disjoint by whether the keyword clause is there.** The bare form joins its two
     * halves with "and"; the keyword form takes a comma before "have" and another before the final
     * "and", which is Oracle's Oxford comma over three clauses rather than two. A single template
     * with an optional middle would print the wrong punctuation for one of them, so they are two
     * templates over one [Bodied] and the empty keyword list is what chooses.
     *
     * The subtype prints plural ("are Angels") and the model holds it singular, which is
     * [Primitives.pluralCreatureSubtype]'s whole job — the same split every plural noun in [Filters]
     * takes.
     */
    private val compositeLord: List<Phrase<StaticAbility>> = run {
        fun abilitiesFor(body: Bodied, group: GroupFilter): StaticAbility =
            CompositeStaticAbility(
                listOf<StaticAbility>(SetBasePowerToughnessStatic(body.stats.first, body.stats.second, group)) +
                    body.keywords.map { GrantKeyword(it, group) } +
                    GrantSubtype(body.subtype.value, group),
            )

        fun read(ability: StaticAbility): Pair<Bodied, GroupFilter>? {
            val parts = (ability as? CompositeStaticAbility)?.abilities ?: return null
            if (parts.size < 2) return null
            val stats = parts.first() as? SetBasePowerToughnessStatic ?: return null
            val subtype = parts.last() as? GrantSubtype ?: return null
            val keywords = mutableListOf<Keyword>()
            for (part in parts.drop(1).dropLast(1)) {
                val grant = part as? GrantKeyword ?: return null
                keywords += Keyword.entries.firstOrNull { it.name == grant.keyword } ?: return null
            }
            val body = Bodied(stats.power to stats.toughness, keywords, Subtype(subtype.subtype))
            return body to stats.filter
        }

        fun parameter(withKeywords: Boolean): Phrase<Bodied> {
            val template = if (withKeywords) {
                "base power and toughness {p}/{t}, have {kws}, and are {subtype} in addition to their other types"
            } else {
                "base power and toughness {p}/{t} and are {subtype} in addition to their other types"
            }
            return phrase(template, name = "a body and a type") {
                slot("p", Primitives.cardinal)
                slot("t", Primitives.cardinal)
                if (withKeywords) slot("kws", Keywords.keywordRun)
                slot("subtype", Primitives.pluralCreatureSubtype)
                build {
                    val keywords = if (withKeywords) it.value<List<Keyword>>("kws") else emptyList()
                    Bodied(it.int("p") to it.int("t"), keywords, it.value("subtype"))
                }
                match { body ->
                    if (body.keywords.isEmpty() == withKeywords) return@match null
                    if (withKeywords) {
                        bind("p" to body.stats.first, "t" to body.stats.second, "kws" to body.keywords, "subtype" to body.subtype)
                    } else {
                        bind("p" to body.stats.first, "t" to body.stats.second, "subtype" to body.subtype)
                    }
                }
            }
        }

        listOf(false, true).flatMap { withKeywords ->
            lordStatic(
                "have", "a group has a body and a type",
                parameter = parameter(withKeywords),
                ability = { body, group -> abilitiesFor(body, group) },
                read = ::read,
            )
        }
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
     * and `GrantDynamicStats` carries the halves separately, so [scaled] lowers each one on its
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
            return GrantDynamicStats(GroupFilter.source(), scaled(count, mod.first), scaled(count, mod.second))
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
                val stats = value as? GrantDynamicStats ?: return@match null
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

    /** See [Amounts.scaled] — this family was its first caller and now shares it with [Replacements]. */
    private fun scaled(count: DynamicAmount, multiplier: Int): DynamicAmount =
        Amounts.scaled(count, multiplier)

    /** The battlefield tally inside one half of a modifier pair, or null when that half is a constant. */
    private fun countedIn(amount: DynamicAmount): DynamicAmount.AggregateBattlefield? = when (amount) {
        is DynamicAmount.AggregateBattlefield -> amount
        is DynamicAmount.Multiply -> amount.amount as? DynamicAmount.AggregateBattlefield
        else -> null
    }

    /** [scaled]'s inverse against a known tally; null when this half is not that tally scaled at all. */
    private fun multiplierOf(amount: DynamicAmount, count: DynamicAmount.AggregateBattlefield): Int? =
        Amounts.multiplierOf(amount, count)

    val all: List<Phrase<StaticAbility>> = listOf(
        attachedPump,
        attachedKeyword,
        attachedDoesntUntap,
        conditionalSelfDoesntUntap,
        attachedQuotedAbility,
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
        damageByToughness(qualified = false),
        damageByToughness(qualified = true),
        attachedDamageByToughness,
    ) + combatRestrictions + compositeLord + Amounts.perScope(::selfPumpPerCount) + SpellCosts.all + lordStatic<Pair<Int, Int>, StaticAbility>(
        "get", "a group gets",
        parameter = Primitives.statModifiers,
        ability = { (power, toughness), group -> ModifyStats(power, toughness, group) },
        read = { (it as? ModifyStats)?.let { s -> (s.powerBonus to s.toughnessBonus) to s.filter } },
    ) + lordStatic<Keyword, StaticAbility>(
        "have", "a group has a keyword",
        parameter = Keywords.keyword,
        ability = { keyword, group -> GrantKeyword(keyword, group) },
        read = { ability ->
            val grant = ability as? GrantKeyword ?: return@lordStatic null
            Keyword.entries.firstOrNull { it.name == grant.keyword }?.let { it to grant.filter }
        },
    ) + lordStatic<ActivatedAbility, StaticAbility>(
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
     * "Other Giant creatures you control get +2/+2 and have trample." — Sunrise Sovereign, Goblin
     * King, and 251 more cards: the tribal lord that hands out a body *and* a keyword.
     *
     * The line-level twin of [pumpAndKeyword], and the same model for the same reason — the pump
     * applies in Layer 7c and each grant in Layer 6, so one printed sentence denotes a *list* of
     * top-level statics rather than a compound "pump and grant" SDK type. What differs is only the
     * subject: [pumpAndKeyword]'s is the attachment, this one's is [Filters.plural] inside a
     * `GroupFilter`, so it arrives as a [lordStatic] parameter and inherits that generator's three
     * prefix rows — including "other ", which is the row Oracle prints for almost every lord.
     *
     * The keyword count is [Keywords.keywordRun]'s slot, so "get +1/+1 and have flying and haste"
     * is the same rule as "get +2/+2 and have trample". The abilities are reconstructed and
     * compared, pump first then the grants in printed order, so a card carrying them the other way
     * round declines rather than being reordered into agreement — the rule [pumpAndKeyword] states
     * and 27 hand-written cards obey.
     *
     * ### The keyword clause lives in the parameter, not the verb
     *
     * [lordStatic]'s shape is `{filter} <verb> {v}<terminator>`, so the whole of "+2/+2 and have
     * trample" is one `{v}`. That is what keeps the "and have" join out of the generator, where it
     * would have to be optional for the three callers that do not print it, and it is why this rule
     * needs no template of its own.
     */
    private val lordPumpAndKeyword: List<Phrase<List<StaticAbility>>> = run {
        fun abilitiesFor(body: PumpAndKeywords, group: GroupFilter): List<StaticAbility> =
            listOf<StaticAbility>(ModifyStats(body.stats.first, body.stats.second, group)) +
                body.keywords.map { GrantKeyword(it, group) }

        fun read(abilities: List<StaticAbility>): Pair<PumpAndKeywords, GroupFilter>? {
            if (abilities.size < 2) return null
            val stats = abilities.first() as? ModifyStats ?: return null
            val keywords = mutableListOf<Keyword>()
            for (part in abilities.drop(1)) {
                val grant = part as? GrantKeyword ?: return null
                if (grant.filter != stats.filter) return null
                keywords += Keyword.entries.firstOrNull { it.name == grant.keyword } ?: return null
            }
            return PumpAndKeywords(stats.powerBonus to stats.toughnessBonus, keywords) to stats.filter
        }

        val parameter: Phrase<PumpAndKeywords> =
            phrase("{mod} and have {kws}", name = "a body and keywords") {
                slot("mod", Primitives.statModifiers)
                slot("kws", Keywords.keywordRun)
                build { PumpAndKeywords(it.value("mod"), it.value("kws")) }
                match { body -> bind("mod" to body.stats, "kws" to body.keywords) }
            }

        lordStatic(
            "get", "a group gets and has",
            parameter = parameter,
            ability = ::abilitiesFor,
            read = ::read,
        )
    }

    /** What [lordPumpAndKeyword]'s single `{v}` slot carries: the pump, and the keywords beside it. */
    private data class PumpAndKeywords(val stats: Pair<Int, Int>, val keywords: List<Keyword>)

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
     * `Enchanted creature gets +1/+0 and has "Whenever this creature deals combat damage, create a
     * Blood token."` — Ceremonial Knife, and 37 more.
     *
     * [pumpAndKeyword]'s shape with [quotedGrant] where the keyword run goes, and a *list* for the
     * same reason: one printed sentence, two SDK abilities, because the pump is a Layer 7c
     * continuous effect and the grant is read by a different subsystem entirely. They are two
     * top-level statics rather than a `CompositeStaticAbility`, which is the SDK's grouping for
     * abilities that all go through the layer system (CR 613.6) and not a general "these were one
     * sentence" wrapper.
     *
     * The pump comes first because the sentence does, and 38 hand-written cards agree; a card
     * carrying them the other way round declines rather than being reordered into agreement.
     */
    private val pumpAndQuotedAbility: Phrase<List<StaticAbility>> = run {
        fun abilitiesFor(modifiers: Pair<Int, Int>, granted: StaticAbility) =
            listOf<StaticAbility>(ModifyStats(modifiers.first, modifiers.second), granted)
        phrase("enchanted creature gets {mod} and has {granted}", name = "enchanted creature gets and has an ability") {
            slot("mod", Primitives.statModifiers)
            slot("granted", quotedGrant)
            build { abilitiesFor(it.value("mod"), it.value("granted")) }
            match { abilities ->
                if (abilities.size != 2) return@match null
                val stats = abilities[0] as? ModifyStats ?: return@match null
                val modifiers = stats.powerBonus to stats.toughnessBonus
                if (abilities != abilitiesFor(modifiers, abilities[1])) return@match null
                bind("mod" to modifiers, "granted" to abilities[1])
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

    /**
     * "Enchanted creature can't attack or block." — Pacifism, Arrest's combat half, Compulsory Rest,
     * and the rest of the white Aura removal; "~ can't attack or block unless it has an even number
     * of counters on it." — Sab-Sunen, and the source-scoped drawback creatures beside it (whose
     * conditions, Sab-Sunen's included, still decline inside [Conditions.condition]).
     *
     * One sentence, **two statics**: the SDK has no joint "can't attack or block" restriction, and
     * the hand-written cards carry `CantAttack` then `CantBlock` over the same group — the attack
     * and block declarations read them separately (CR 508.1 and 509.1). So it is a line rule, like
     * [attachedKeywordRun], rather than a [restriction] row, and it reuses [Subject] whole: the
     * attached, source and group subjects all print, and the round trip through [spelling] is what
     * refuses a group this sentence cannot say. The pair is compared in printed order, so a card
     * carrying the block half first declines rather than being reordered into agreement.
     *
     * The "unless" form is source-only on purpose. Its condition comes from [Conditions.condition],
     * whose "it" is the source; under "enchanted creature" the same pronoun would mean the Aura's
     * host, and the condition vocabulary has no way to say so. Goblin Goon's split pair stays two
     * constants above — its two halves print different nouns, so they are not one sentence.
     */
    private val cantAttackOrBlock: List<Phrase<List<StaticAbility>>> = Subject.entries.map { subject ->
        fun abilitiesFor(group: GroupFilter) = listOf<StaticAbility>(CantAttack(group), CantBlock(group))
        phrase("${subject.surface} can't attack or block.", name = "can't attack or block, ${subject.label}") {
            if (subject == Subject.GROUP) slot("group", Filters.plural)
            build { abilitiesFor(subject.groupOf(it)) }
            match { abilities ->
                val group = (abilities.firstOrNull() as? CantAttack)?.filter ?: return@match null
                val bindings = subject.spelling(group) ?: return@match null
                if (abilities != abilitiesFor(group)) return@match null
                bind(*bindings.toTypedArray())
            }
        }
    }

    /** "~ can't attack or block unless {cond}." — see [cantAttackOrBlock] for why only the source. */
    private val cantAttackOrBlockUnless: Phrase<List<StaticAbility>> = run {
        fun abilitiesFor(condition: Condition) =
            listOf<StaticAbility>(CantAttackUnless(condition), CantBlockUnless(condition))
        phrase(
            "${Normalizer.SELF} can't attack or block unless {cond}.",
            name = "can't attack or block unless a condition",
        ) {
            slot("cond", Conditions.condition)
            build { abilitiesFor(it.value("cond")) }
            match { abilities ->
                val condition = (abilities.firstOrNull() as? CantAttackUnless)?.condition ?: return@match null
                if (abilities != abilitiesFor(condition)) return@match null
                bind("cond" to condition)
            }
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
     *
     * [lordPumpAndKeyword] has [pumpAndKeyword]'s list shape and is separated from it by the
     * *filter* rather than by the shape: one carries `GroupFilter.attachedCreature()`, the other a
     * battlefield group, and each reconstructs-and-compares against its own, so neither can print
     * the other's model. The two are also disjoint in text — an attachment's subject conjugates
     * singular ("gets … and has"), a group's plural ("get … and have").
     */
    val line: Phrase<List<StaticAbility>> = oneOf(
        "static abilities",
        listOf(pumpAndKeyword, pumpAndQuotedAbility, attachedKeywordRun, cantAttackOrBlockUnless) +
            lordPumpAndKeyword + cantAttackOrBlock +
            ConditionalForm.entries.flatMap { form ->
                listOf(
                    conditionalSelfStatic(leading = false, form = form),
                    conditionalSelfStatic(leading = true, form = form),
                )
            } +
            single,
    )
}
