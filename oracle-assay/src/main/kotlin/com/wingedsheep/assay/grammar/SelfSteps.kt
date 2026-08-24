package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.RemoveKeywordEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clauses about the **source** — "it gets +2/+0 until end of turn.", "put it on top of its owner's
 * library.", "sacrifice it unless you discard a land card."
 *
 * Oracle's "it" here is the permanent whose ability this is, which is [EffectTarget.Self] and needs
 * no earlier sentence to introduce it — so unlike [Continuations] these are ordinary clauses that
 * can stand alone. The two anaphors are kept in separate vocabularies precisely because they point
 * at different things: "that creature" is the target the spell already chose, "it" is the source.
 *
 * Almost every card that prints one of these prints it inside a triggered ability ("When this
 * creature dies, put it on top of its owner's library"), and none of these rules knows that:
 * [Triggers] slots [Steps.step] whole, so the clause is the same clause wherever it lands.
 *
 * ### The third anaphor
 *
 * That last sentence holds everywhere but one place. A trigger whose event names a **filter**
 * mentions an object of its own, and English resolves "it" to the most recent mention: "Whenever a
 * Rat you control becomes blocked, **it** gets +2/+0" pumps the *Rat*, not the source. So the same
 * clause denotes [EffectTarget.TriggeringEntity] there and [EffectTarget.Self] everywhere else,
 * while the *name* still denotes the source in both.
 *
 * The vocabulary is therefore written once, as [retargetable], and instantiated per position rather
 * than copied — the members, their models and their fail-closed matches are one piece of code, and
 * only the subject's spelling and the target it denotes move. [Steps.step] takes the source reading
 * and [Steps.triggeredStep] takes both, which is what keeps one printed form per model: in the
 * filtered-trigger cascade the name reads the source and the pronoun reads the triggering entity, so
 * the two surfaces are disjoint and neither can print the other's sentence.
 */
object SelfSteps {

    /**
     * The clauses whose object can be *any* single permanent the sentence has already fixed.
     *
     * One shape, instantiated per anaphor position. [subject] is what stands in a `{self}` slot and
     * [target] is what the whole clause acts on; the two move together, which is the entire content
     * of the third-anaphor split described on this object.
     *
     * @param pronominal whether the members that spell the pronoun as a *literal* ("put **it** on
     *   top of its owner's library") are included. They have no subject slot, so [subject] cannot
     *   keep them apart: a position that reads the pronoun as something other than the source has to
     *   take them, and one that reads only the name has to leave them out, or one text would have
     *   two readings.
     * @param tag distinguishes the rule names across instantiations so an ambiguity diagnostic can
     *   still say which side it found.
     */
    fun retargetable(
        target: EffectTarget,
        subject: Phrase<Unit>,
        pronominal: Boolean,
        tag: String,
    ): List<Phrase<CardScript>> {
        val named = listOf(
            selfGets(target, subject, tag),
            selfGetsAndGains(target, subject, tag),
            selfGainsKeywords(target, subject, tag),
            selfLosesKeyword(target, subject, tag),
            move("untap {self}", "untap$tag", Effects.Untap(target), subject),
            move("regenerate {self}", "regenerate$tag", RegenerateEffect(target), subject),
        ) + putCounters(target, subject, tag) +
            // "{2}{U}: ~ can't be blocked this turn." — the durational evasion, whose whole family
            // lives in [Combat] beside the combat statics it is the spell-side sibling of. It is a
            // member here rather than a clause of its own because its object moves with every other
            // member's: 24 printed lines say it about the source, and being a clause is also what
            // lets "~ gets +1/+0 until end of turn and can't be blocked this turn." read as the two
            // clauses it is.
            Combat.restrictionClauses(target, subject, surface = "{self}", tag = tag)
        if (!pronominal) return named
        return named + listOf(
            putOnTop(target, tag),
            move(
                "shuffle it into its owner's library",
                "shuffle$tag into its library",
                Effects.Move(target, Zone.LIBRARY, ZonePlacement.Shuffled),
                subject = null,
            ),
            move("exile it", "exile$tag", Effects.Move(target, Zone.EXILE), subject = null),
            move(
                "return it to its owner's hand",
                "return$tag to its owner's hand",
                Effects.Move(target, Zone.HAND),
                subject = null,
            ),
            // "…return it to your hand." — Ghastly Remains. The same move: a card returning itself
            // goes to its owner's hand, and the owner of a card you are returning from your own
            // graveyard is you. Two printed forms, one model, so this one parses and never prints.
            alternate(
                move(
                    "return it to your hand",
                    "return$tag to your hand",
                    Effects.Move(target, Zone.HAND),
                    subject = null,
                )
            ),
        )
    }

    /**
     * "Put a +1/+1 counter on ~.", "Put two +1/+1 counters on it." — the counter verb aimed at the
     * source, and the single commonest effect shape in the whole hand-written corpus: 363 of the
     * 951 `AddCounters` a golden carries are exactly this one.
     *
     * The subject is a slot, so both spellings read and the name is what prints, the same treatment
     * [selfGets] gets. Being in [anaphoric] is what makes the pronoun safe: [Steps] drops this whole
     * list from every position after the first in a sequence, so once a clause has introduced a
     * target, "on it" is [Continuations]' to read and means that target. Registering the pronoun in
     * both places would be two readings of one text — the bug the differential caught on "Untap
     * target creature. It gets +2/+4", in a sentence where it would be just as invisible.
     *
     * Singular and plural are two rules over disjoint quantities for [Steps]' reason; everything
     * about why is written there, on the targeted twin of this pair.
     */
    private fun putCounters(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): List<Phrase<CardScript>> {
        fun scriptFor(kind: String, count: Int) =
            CardScript(spellEffect = Effects.AddCounters(kind, count, target))
        fun rule(template: String, name: String, quantity: Phrase<*>?) =
            phrase(template, name = name) {
                slot("kind", if (quantity == null) Primitives.singularCounterKind else Primitives.counterKind)
                if (quantity != null) slot("n", quantity)
                slot("self", subject)
                build { scriptFor(it.value("kind"), if (quantity == null) 1 else it.int("n")) }
                match { script ->
                    val (kind, count) =
                        Steps.countersAdded(script.spellEffect, target) ?: return@match null
                    if (quantity == null && count != 1) return@match null
                    if (quantity != null && !(count >= 2 && Cardinals.spellable(count))) return@match null
                    if (script != scriptFor(kind, count)) return@match null
                    bind("kind" to kind, "n" to count, "self" to Unit)
                }
            }
        // The count named by a trailing clause instead of by a number word. One rule, both of
        // Oracle's spellings, over the SDK's dynamic counter effect — and no bare-"X" row, for the
        // reason [Amounts.namesX] gives: this clause is one [Triggers] lifts, and the announced X is
        // silently zero anywhere it lands but a spell.
        fun dynamicScriptFor(kind: String, amount: DynamicAmount) =
            CardScript(spellEffect = Effects.AddDynamicCounters(kind, amount, target))
        val defined = phrase<CardScript>(
            "put X {kind} counters on {self}${Amounts.WHERE_X}",
            name = "put a counted number of counters on$tag",
        ) {
            definedByCount()
            slot("kind", Primitives.counterKind)
            slot("self", subject)
            slot("amount", Amounts.count)
            build {
                val amount = it.value<DynamicAmount>("amount")
                if (Amounts.namesX(amount)) dynamicScriptFor(it.value("kind"), amount) else null
            }
            match { script ->
                val (kind, amount) =
                    Steps.dynamicCountersAdded(script.spellEffect, target) ?: return@match null
                if (!Amounts.namesX(amount)) return@match null
                if (script != dynamicScriptFor(kind, amount)) return@match null
                bind("kind" to kind, "amount" to amount, "self" to Unit)
            }
        }
        return listOf(
            rule("put {kind} counter on {self}", "put a counter on$tag", null),
            rule("put {n} {kind} counters on {self}", "put counters on$tag", Cardinals.word),
            defined,
        )
    }

    /**
     * "This creature gets +1/+1 until end of turn." — firebreathing's effect clause, and Charging
     * Bandits' attack trigger spelled with the pronoun.
     *
     * The subject is a slot, so both of Oracle's spellings read and the noun is what prints. That
     * ordering is the corpus's: a card *naming* itself is how nearly every activated pump is
     * templated ("{R}: This creature gets +1/+0 until end of turn."), while the pronoun only appears
     * where an earlier clause in the same ability already named the source. Cards printing the
     * pronoun come back as a [com.wingedsheep.assay.gate.LineVerdict.VARIANT], which says the
     * reading was right and only the spelling moved.
     */
    private fun selfGets(target: EffectTarget, subject: Phrase<Unit>, tag: String): Phrase<CardScript> {
        fun scriptFor(modifiers: Pair<Int, Int>) = CardScript(
            spellEffect = Effects.ModifyStats(modifiers.first, modifiers.second, target)
        )
        return phrase("{self} gets {mod} until end of turn", name = "$tag gets".trim()) {
            frontedDuration()
            slot("self", subject)
            slot("mod", Primitives.statModifiers)
            build { scriptFor(it.value("mod")) }
            match { script ->
                val modifiers = Steps.fixedModifiers(script.spellEffect) ?: return@match null
                if (script != scriptFor(modifiers)) return@match null
                bind("self" to Unit, "mod" to modifiers)
            }
        }
    }

    /**
     * "This creature gets +2/+2 and gains trample until end of turn." — Clickslither, Glintwing
     * Invoker, Unstable Hulk.
     *
     * [Steps.pumpAndGrantTarget]'s source-side twin, and one rule for the same reason: the second
     * clause has no subject of its own in the text, and the model is a two-element composite over
     * one object. A [Steps.sequence] would need the second clause to name what it acts on.
     */
    private fun selfGetsAndGains(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): Phrase<CardScript> {
        fun scriptFor(modifiers: Pair<Int, Int>, keywords: List<Keyword>) = CardScript(
            spellEffect = Effects.Composite(
                listOf(Effects.ModifyStats(modifiers.first, modifiers.second, target)) +
                    keywords.map { Effects.GrantKeyword(it, target) }
            )
        )
        return phrase(
            "{self} gets {mod} and gains {kws} until end of turn",
            name = "$tag gets and gains".trim(),
        ) {
            frontedDuration()
            slot("self", subject)
            slot("mod", Primitives.statModifiers)
            slot("kws", Keywords.keywordRun)
            build { scriptFor(it.value("mod"), it.value("kws")) }
            match { script ->
                val effects = (script.spellEffect as? CompositeEffect)?.effects ?: return@match null
                val modifiers = Steps.fixedModifiers(effects.firstOrNull()) ?: return@match null
                val keywords = Steps.grantedKeywords(effects.drop(1)) ?: return@match null
                if (script != scriptFor(modifiers, keywords)) return@match null
                bind("self" to Unit, "mod" to modifiers, "kws" to keywords)
            }
        }
    }

    /**
     * "~ gains trample until end of turn.", "~ gains flying and shroud until end of turn." — Warped
     * Researcher and every card that grants itself a run.
     *
     * One rule over [Keywords.keywordRun] rather than one per count. It used to be a two-keyword
     * rule with no singular sibling, which is the shape a family looks like before it is one: the
     * count was in the rule instead of in the slot, so "gains trample" had nowhere to parse.
     */
    private fun selfGainsKeywords(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): Phrase<CardScript> {
        fun scriptFor(keywords: List<Keyword>) = CardScript(spellEffect = Steps.grants(keywords, target))
        return phrase(
            "{self} gains {kws} until end of turn",
            name = "$tag gains keywords".trim(),
        ) {
            frontedDuration()
            slot("self", subject)
            slot("kws", Keywords.keywordRun)
            build { scriptFor(it.value("kws")) }
            match { script ->
                val keywords = Steps.grantedKeywords(script.spellEffect) ?: return@match null
                if (script != scriptFor(keywords)) return@match null
                bind("self" to Unit, "kws" to keywords)
            }
        }
    }

    /** "~ loses flying until end of turn." — Swooping Talon, the grant rules' negation. */
    private fun selfLosesKeyword(
        target: EffectTarget,
        subject: Phrase<Unit>,
        tag: String,
    ): Phrase<CardScript> {
        fun scriptFor(keyword: Keyword) =
            CardScript(spellEffect = Effects.RemoveKeyword(keyword, target))
        return phrase("{self} loses {kw} until end of turn", name = "$tag loses a keyword".trim()) {
            frontedDuration()
            slot("self", subject)
            slot("kw", Keywords.keyword)
            build { scriptFor(it.value("kw")) }
            match { script ->
                val removal = script.spellEffect as? RemoveKeywordEffect ?: return@match null
                val keyword = Keyword.entries.firstOrNull { it.name == removal.keyword } ?: return@match null
                if (script != scriptFor(keyword)) return@match null
                bind("self" to Unit, "kw" to keyword)
            }
        }
    }

    /** "Put it on top of its owner's library." — Undying Beast's death trigger. */
    private fun putOnTop(target: EffectTarget, tag: String): Phrase<CardScript> {
        val script = CardScript(spellEffect = Effects.PutOnTopOfLibrary(target))
        return phrase(
            "put it on top of its owner's library",
            name = "put$tag on top of its library",
        ) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * The verbs whose object is one permanent and which carry nothing else — a move to a named zone,
     * an untap, a regeneration.
     *
     * The subject slot is optional because the older members spell the pronoun as a literal ("return
     * **it** to its owner's hand"), while the ones a card names itself in take a subject phrase.
     * Both are the same rule shape; only the printed subject differs. A `null` [subject] is what
     * [retargetable]'s `pronominal` flag gates on.
     */
    private fun move(
        template: String,
        name: String,
        effect: Effect,
        subject: Phrase<Unit>?,
    ): Phrase<CardScript> {
        val script = CardScript(spellEffect = effect)
        return phrase(template, name = name) {
            if (subject != null) slot("self", subject)
            build { script }
            match { if (it == script) bind("self" to Unit) else null }
        }
    }

    /**
     * "Sacrifice ~." — Ball Lightning's end step, and every other creature that pays for its
     * statistics by leaving.
     *
     * The bare sentence, and it declined until now because the four [sacrificeUnless] rules had the
     * *rider* written into their templates: "sacrifice ~" was only ever readable as the front of
     * "sacrifice ~ unless you pay {2}", so a card that printed the clause and stopped died on its
     * own full stop. An "unless" clause is something English adds to this sentence, not something
     * the sentence is made of, and a rule that cannot be read without its modifier is the shape
     * that puts a line in the `.` decline family.
     *
     * The model is the sacrifice with no cost in front of it — [SacrificeSelfEffect] alone rather
     * than the `PayOrSufferEffect` the riders build — so the two spellings denote different values
     * and neither can print the other's sentence.
     */
    private val sacrificeSelf: Phrase<CardScript> = run {
        val script = CardScript(spellEffect = SacrificeSelfEffect)
        phrase("sacrifice {self}", name = "sacrifice the source") {
            slot("self", Primitives.self)
            build { script }
            match { if (it == script) bind("self" to Unit) else null }
        }
    }

    /**
     * "Sacrifice ~ unless you pay {G}{G}." — Krosan Cloudscraper's upkeep tax.
     *
     * A row of the [sacrificeUnless] shape over a *mana* cost rather than a permanent one, which is
     * why it is written out: the cost has no noun phrase and therefore no article, so
     * [Filters.indefinite] has nothing to do and the slot is a bare mana symbol run.
     */
    private val sacrificeUnlessPay: Phrase<CardScript> = run {
        fun scriptFor(cost: ManaCost) = CardScript(
            spellEffect = PayOrSufferEffect(cost = Costs.pay.Mana(cost), suffer = SacrificeSelfEffect)
        )
        phrase("sacrifice {self} unless you pay {cost}", name = "sacrifice the source unless you pay") {
            slot("self", Primitives.self)
            slot("cost", Primitives.manaCost)
            build { scriptFor(it.value("cost")) }
            match { script ->
                val effect = script.spellEffect as? PayOrSufferEffect ?: return@match null
                val cost = ((effect.cost as? PayCost.Atom)?.atom as? CostAtom.Mana)?.cost ?: return@match null
                if (script != scriptFor(cost)) return@match null
                bind("self" to Unit, "cost" to cost)
            }
        }
    }

    /**
     * "Sacrifice it unless you discard a land card." — the Portal drawback, and one shape with two
     * costs.
     *
     * `PayOrSufferEffect` is the SDK's name for the whole sentence: a cost the controller may pay
     * and the thing that happens if they do not. The two members differ only in the [PayCost], which
     * is why the rule is a function of the cost's surface and both halves of the cost — the printed
     * noun phrase goes through [Filters.indefinite] so the article comes out right, and the cost is
     * reconstructed from the filter for the comparison.
     */
    private fun sacrificeUnless(
        template: String,
        name: String,
        // A discard names a *card* and a sacrifice names a permanent, which are two noun phrases
        // rather than one with a word appended; see [Filters.cardNoun].
        noun: Phrase<GameObjectFilter> = Filters.indefinite,
        cost: (GameObjectFilter) -> PayCost,
    ): Phrase<CardScript> {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = PayOrSufferEffect(cost = cost(filter), suffer = SacrificeSelfEffect)
        )
        return phrase(template, name = name) {
            slot("filter", noun)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val effect = script.spellEffect as? PayOrSufferEffect ?: return@match null
                val filter = paidFilter(effect.cost) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    /** The filter a one-atom pay cost names, or null when the cost is anything more complicated. */
    private fun paidFilter(cost: PayCost): GameObjectFilter? {
        val atom = (cost as? PayCost.Atom)?.atom ?: return null
        return when (atom) {
            is CostAtom.Discard -> atom.filter
            is CostAtom.Sacrifice -> atom.filter
            else -> null
        }
    }

    /**
     * "Sacrifice it unless you sacrifice three Forests." — the counted cost, Primeval Force's.
     *
     * A row of the [sacrificeUnless] shape would need the count in the cost *and* a plural noun in
     * the text, which changes both slots; the singular rules keep the noun singular and this one
     * carries the number, which is the same singular/plural split every counting rule here makes.
     */
    private val sacrificeUnlessCounted: Phrase<CardScript> = run {
        fun scriptFor(count: Int, filter: GameObjectFilter) = CardScript(
            spellEffect = PayOrSufferEffect(
                cost = Costs.pay.Sacrifice(filter, count = count),
                suffer = SacrificeSelfEffect,
            )
        )
        phrase(
            "sacrifice it unless you sacrifice {n} {filter}",
            name = "sacrifice the source unless you sacrifice several",
        ) {
            slot("n", Cardinals.word)
            slot("filter", Filters.plural)
            build { scriptFor(it.int("n"), it.value("filter")) }
            match { script ->
                val effect = script.spellEffect as? PayOrSufferEffect ?: return@match null
                val atom = (effect.cost as? PayCost.Atom)?.atom as? CostAtom.Sacrifice ?: return@match null
                if (!Cardinals.spellable(atom.count)) return@match null
                if (script != scriptFor(atom.count, atom.filter)) return@match null
                bind("n" to atom.count, "filter" to atom.filter)
            }
        }
    }

    /**
     * "Sacrifice it unless you sacrifice any number of creatures with total power 12 or greater." —
     * Phyrexian Dreadnought, and the third context `CostAtom` names.
     *
     * The whole of [VariableCosts] rather than a row of its own: a payable cost is the same payable
     * thing as an activation cost and an additional cost, so this slots the family the other two
     * slot. That is [Costs]' own argument one context further along, and it is why a verb this
     * sentence has never printed ("unless you tap any number of …") costs nothing to have — the
     * family is the type's product, and a context that can pay it can pay all of it.
     */
    private val sacrificeUnlessVariable: Phrase<CardScript> = run {
        fun scriptFor(atom: CostAtom) = CardScript(
            spellEffect = PayOrSufferEffect(cost = Costs.pay.Atom(atom), suffer = SacrificeSelfEffect)
        )
        phrase("sacrifice it unless you {atom}", name = "sacrifice the source unless you pay a chosen count") {
            slot("atom", VariableCosts.payAtoms)
            build { scriptFor(it.value("atom")) }
            match { script ->
                val effect = script.spellEffect as? PayOrSufferEffect ?: return@match null
                val atom = (effect.cost as? PayCost.Atom)?.atom as? CostAtom.VariablePermanents
                    ?: return@match null
                if (script != scriptFor(atom)) return@match null
                bind("atom" to atom)
            }
        }
    }

    /** "Sacrifice it unless you discard a card at random." — Pillaging Horde. */
    private val sacrificeUnlessRandomDiscard: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = PayOrSufferEffect(
                cost = Costs.pay.Discard(random = true),
                suffer = SacrificeSelfEffect,
            )
        )
        phrase(
            "sacrifice it unless you discard a card at random",
            name = "sacrifice the source unless you discard at random",
        ) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * The clauses that sacrifice the **source itself**, which no anaphor can move.
     *
     * `SacrificeSelfEffect` carries no [EffectTarget] at all — the SDK models "sacrifice this" as a
     * verb about the source rather than a verb with an object — so these are not members of
     * [retargetable] and a filtered trigger's "it" cannot reach them. That is the fail-closed
     * answer, not a gap worked around: a card that meant "sacrifice the creature that triggered
     * this" needs an effect the SDK does not have, so it declines and is counted.
     */
    private val sacrificesSource: List<Phrase<CardScript>> = listOf(
        sacrificeSelf,
        sacrificeUnlessPay,
        sacrificeUnlessCounted,
        sacrificeUnlessVariable,
        sacrificeUnlessRandomDiscard,
        sacrificeUnless(
            "sacrifice it unless you discard {filter}",
            "sacrifice the source unless you discard",
            noun = Filters.indefiniteCard,
        ) { Costs.pay.Discard(filter = it) },
        sacrificeUnless(
            "sacrifice it unless you sacrifice {filter}",
            "sacrifice the source unless you sacrifice",
        ) { Costs.pay.Sacrifice(it) },
    )

    /**
     * The clauses whose "it" is the **source** — what every position but a filtered trigger reads.
     *
     * Kept apart from the rest because English resolves an anaphor to the most recently mentioned
     * object: in "Whenever this creature attacks, it gets +2/+0" the only mention is the source, but
     * in "Untap target creature. It gets +2/+4 until end of turn." it is the target the first clause
     * introduced. So these rules are clauses in their own right and are *not* offered in a later
     * position of a sequence — [Continuations] owns "it" there. Registering them in both places
     * would be two readings of one text, which is ambiguity rather than a choice.
     */
    val anaphoric: List<Phrase<CardScript>> =
        retargetable(EffectTarget.Self, Primitives.self, pronominal = true, tag = " the source") +
            sacrificesSource

    /**
     * The same vocabulary inside a **filtered** trigger, where the two spellings come apart.
     *
     * The name still reads the source and the pronoun now reads the object the trigger's filter
     * matched, so the two instantiations have disjoint surfaces and disjoint models — one printed
     * form per model, with nothing for the printer to choose. See the third-anaphor section on this
     * object, and [Steps.triggeredStep] for the only cascade that takes this list.
     */
    val triggering: List<Phrase<CardScript>> =
        retargetable(EffectTarget.Self, Primitives.selfNamed, pronominal = false, tag = " the named source") +
            retargetable(
                EffectTarget.TriggeringEntity,
                Primitives.itPronoun,
                pronominal = true,
                tag = " the triggering permanent",
            ) +
            sacrificesSource

    /** Everything in this file that does not turn on the pronoun. Empty for now; the family is "it". */
    val clauses: List<Phrase<CardScript>> = emptyList()
}
