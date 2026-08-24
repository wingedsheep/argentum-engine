package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.assay.syntax.separated
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Costs as SdkCosts

/**
 * What you pay — the clause before an activated ability's colon, and the clause an
 * "As an additional cost to cast this spell, …" line spells.
 *
 * ### One vocabulary, two contexts, because the SDK says so
 *
 * `CostAtom`'s own KDoc calls itself "the **one cost language**": a payable thing is declared once
 * and each *context* carries it through its own `Atom` wrapper — `AbilityCost.Atom` for an activated
 * ability, `AdditionalCost.Atom` for a spell's additional cost. This file is written the same way
 * round. [atoms] is a `Phrase<CostAtom>` — "discard a card", "sacrifice another creature", "remove
 * two spore counters from ~" — and the two contexts are two *lifts* of it, [cost] and [additional].
 * A row added here reaches both, which is the property the previous shape lacked: an additional cost
 * used to be a rule of its own that read "sacrifice a creature" and nothing else, while the
 * activation side read a different, longer list of the same English.
 *
 * ### What is *not* an atom: the costs only a permanent can pay
 *
 * "Sacrifice ~", "Exile ~", `{T}`, `{Q}`, "Exert ~" are their own `AbilityCost` cases rather than
 * atoms, and the reason is the same one `CostAtom` gives for keeping `excludeSelf` off the spell
 * side: *a spell being cast has no source permanent*. So [abilityOnly] holds them, [cost] offers
 * them alongside the lifted atoms, and [additional] does not offer them at all — which is not a gap
 * but the rule. A mana cost is in the same group for a different reason: an additional cost that is
 * mana is printed as part of the spell's own cost, never as this sentence.
 *
 * ### An atom, and a comma-joined run of them
 *
 * `{T}` is one cost, `{2}{B}, {T}, Sacrifice a Goblin` is three, and the SDK spells the second as
 * `AbilityCost.Composite` of the first kind. So this file is an **atom vocabulary** plus one run
 * rule, rather than a rule per whole cost shape: `{1}, {T}` stopped being a rule of its own the
 * moment a third atom appeared, and every future atom is a row in [atoms] that every multi-atom cost
 * gets for free.
 *
 * The single-atom case is the atom itself and **not** a one-element `Composite`, because that is
 * what hand-written cards carry — `AbilityCost.Composite` with one member is a value no card in the
 * corpus has. [cost] therefore takes disjoint models on its two alternatives and printing is decided
 * by the value rather than by the alternation's order.
 *
 * ### The ordering inside a composite is the printed one
 *
 * `{2}, {T}` is `Composite([Mana, Tap])` and not `Composite([Tap, Mana])`, because that is what
 * Cabal Coffers and every other hand-written card carries, and a `Composite` is a list rather than
 * a set. The run rule preserves printed order in both directions, which is the property that makes
 * it safe to generalize away from the enumerated pairs — reading it the other way round would
 * round-trip and disagree with every card, the reversible-but-wrong class.
 *
 * ### Capitalization is a *parameter* of the vocabulary, not an alternate over it
 *
 * A cost atom is the one clause Oracle capitalizes that is not a sentence start
 * ("Sacrifice a Goblin: …", "{T}, Sacrifice a Forest: …"), and
 * [com.wingedsheep.assay.syntax.SentenceCase] lowercases only the one at the line's start. That was
 * an [alternate] while costs lived in one position. They no longer do: in
 * "As an additional cost to cast this spell, **sacrifice** a creature." the lowercase spelling is
 * *canonical* and the capital would be a misprint. So [vocabulary] takes the leading word's
 * spelling as an argument and is instantiated twice; [atoms] pairs the two the way the activation
 * position needs them, and [additional] takes the lowercase instance alone. Writing it as one
 * template per row instantiated twice is what keeps the two halves from drifting — a row cannot
 * exist in one capitalization only.
 *
 * ### `{T}` is not a mana cost, and the SDK is what says so
 *
 * The tap rule and the mana rule can both be offered at the same offset without ambiguity because
 * `ManaCost.parse("{T}")` throws — a symbol the SDK's mana vocabulary has no place for makes
 * [Primitives.manaCost] decline rather than invent a reading. The two rules are therefore disjoint
 * by the SDK's own type rather than by an ordering in the alternation.
 */
object Costs {

    // -------------------------------------------------------------------------------------------
    // The shared atom vocabulary
    // -------------------------------------------------------------------------------------------

    /**
     * The `CostAtom` an SDK cost factory builds.
     *
     * Every row below constructs through `dsl.Costs` rather than through `CostAtom` directly, so the
     * facade stays the single place that decides what an atom's fields mean and a rule here cannot
     * drift from it — the same reason a card is required to. Unwrapping is total for the factories
     * used here, each of which returns an `AbilityCost.Atom`, and a facade that stopped doing so is
     * a bug worth failing loudly on rather than declining quietly.
     */
    private fun atomOf(cost: AbilityCost): CostAtom =
        requireNotNull((cost as? AbilityCost.Atom)?.atom) { "not an atom cost: $cost" }

    /**
     * The atom vocabulary in one capitalization.
     *
     * [lead] spells a row's leading word — `String::capitalized` for the activation position, the
     * identity for a mid-sentence one. Nothing else in a row differs between the two, which is why
     * this is a function returning the whole vocabulary rather than a per-row pairing helper.
     */
    private fun vocabulary(lead: (String) -> String): Phrase<CostAtom> {

        /**
         * "Sacrifice a Goblin", "Sacrifice a creature" — one permanent matching a filter.
         *
         * The article comes from [Filters.indefinite], which derives it from the noun's spelling in
         * both directions; the model has nowhere to keep it. `count` is checked against 1 here and
         * the plural rule below refuses 1, so the two take disjoint models and one printed form
         * exists per value.
         */
        val sacrificeFiltered = phrase("${lead("sacrifice")} {filter}", name = "sacrifice a permanent") {
            slot("filter", Filters.indefinite)
            build { atomOf(SdkCosts.Sacrifice(it.value("filter"))) }
            match { atom ->
                val sacrifice = atom as? CostAtom.Sacrifice ?: return@match null
                if (sacrifice.count != 1) return@match null
                if (atom != atomOf(SdkCosts.Sacrifice(sacrifice.filter))) return@match null
                bind("filter" to sacrifice.filter)
            }
        }

        /**
         * "Sacrifice another creature" — Nantuko Husk. The source is excluded from what may pay,
         * which the SDK carries as `excludeSelf` and Oracle spells as the word "another".
         *
         * No article: "another" is a determiner, so this slots the bare noun phrase where
         * [sacrificeFiltered] slots the one that carries its own "a"/"an".
         */
        val sacrificeAnother = phrase("${lead("sacrifice")} another {filter}", name = "sacrifice another permanent") {
            slot("filter", Filters.filter)
            build { atomOf(SdkCosts.SacrificeAnother(it.value("filter"))) }
            match { atom ->
                val sacrifice = atom as? CostAtom.Sacrifice ?: return@match null
                if (atom != atomOf(SdkCosts.SacrificeAnother(sacrifice.filter))) return@match null
                bind("filter" to sacrifice.filter)
            }
        }

        /** "Sacrifice three Clerics" — Dark Supplicant. The counted sibling, over a plural noun. */
        val sacrificeSeveral =
            phrase("${lead("sacrifice")} {n} {filter}", name = "sacrifice several permanents") {
                slot("n", Cardinals.word)
                slot("filter", Filters.plural)
                build { atomOf(SdkCosts.SacrificeMultiple(it.int("n"), it.value("filter"))) }
                match { atom ->
                    val sacrifice = atom as? CostAtom.Sacrifice ?: return@match null
                    if (!Cardinals.spellable(sacrifice.count)) return@match null
                    if (atom != atomOf(SdkCosts.SacrificeMultiple(sacrifice.count, sacrifice.filter))) {
                        return@match null
                    }
                    bind("n" to sacrifice.count, "filter" to sacrifice.filter)
                }
            }

        /**
         * "Discard a card", "Discard a creature card" — the single commonest non-mana cost in the
         * corpus, over one noun phrase.
         *
         * The unqualified form used to be a `constant` beside this rule, because [Filters] had no
         * noun for `GameObjectFilter.Any` — and `Costs.DiscardCard` is *definitionally*
         * `Discard(Any, 1)`, so the two rules printed one model the moment either could reach it.
         * The bare word is now the `Any` row of [Filters.indefiniteCard], which is where it belongs:
         * one printed form per model, and the qualified form gains every suffix the noun phrase can
         * carry.
         */
        val discardFiltered = phrase("${lead("discard")} {filter}", name = "discard a card of a kind") {
            slot("filter", Filters.indefiniteCard)
            build { atomOf(SdkCosts.Discard(it.value("filter"))) }
            match { atom ->
                val discard = atom as? CostAtom.Discard ?: return@match null
                if (discard.count != 1) return@match null
                if (atom != atomOf(SdkCosts.Discard(discard.filter))) return@match null
                bind("filter" to discard.filter)
            }
        }

        /** "Discard two cards" — the counted form, over the unqualified noun. */
        val discardSeveral = phrase("${lead("discard")} {n} cards", name = "discard several cards") {
            slot("n", Cardinals.word)
            build { atomOf(SdkCosts.Discard(count = it.int("n"))) }
            match { atom ->
                val discard = atom as? CostAtom.Discard ?: return@match null
                if (!Cardinals.spellable(discard.count)) return@match null
                if (atom != atomOf(SdkCosts.Discard(count = discard.count))) return@match null
                bind("n" to discard.count)
            }
        }

        /** "Discard a card at random" — Pillaging Horde. The payer chooses nothing. */
        val discardAtRandom =
            constant("${lead("discard")} a card at random", atomOf(SdkCosts.DiscardAtRandom(1)))

        /**
         * "Mill a card" — Deranged Assistant. No selection and no filter: the milled card is the top
         * of the library, so the count is the whole model.
         */
        val millCard = constant("${lead("mill")} a card", atomOf(SdkCosts.MillCard))

        val millSeveral = phrase("${lead("mill")} {n} cards", name = "mill several cards") {
            slot("n", Cardinals.word)
            build { atomOf(SdkCosts.Mill(it.int("n"))) }
            match { atom ->
                val mill = atom as? CostAtom.Mill ?: return@match null
                if (!Cardinals.spellable(mill.count)) return@match null
                if (atom != atomOf(SdkCosts.Mill(mill.count))) return@match null
                bind("n" to mill.count)
            }
        }

        /**
         * "Exile a creature card from your graveyard" — Cryptwailing, Deep Reconnaissance.
         *
         * The zone is a literal rather than a slot: the SDK's atom takes any `Zone`, but the only
         * one Oracle spells in a cost is the graveyard, and a slot over the whole enum would read
         * "exile a creature card from your battlefield". A second zone is a second row when a card
         * prints one.
         */
        val exileFromGraveyard =
            phrase("${lead("exile")} {filter} from your graveyard", name = "exile a card from your graveyard") {
                slot("filter", Filters.indefiniteCard)
                build { atomOf(SdkCosts.ExileFromGraveyard(1, it.value("filter"))) }
                match { atom ->
                    val exile = atom as? CostAtom.ExileFrom ?: return@match null
                    if (exile.count != 1) return@match null
                    if (atom != atomOf(SdkCosts.ExileFromGraveyard(1, exile.filter))) return@match null
                    bind("filter" to exile.filter)
                }
            }

        /**
         * "Exile two creature cards from your graveyard" — the counted form.
         *
         * The filter slot is the plural *card* noun, which inflects only its head: it is "two
         * *creature* cards", not "two creatures cards". [Filters.plural] belongs where the noun
         * phrase is the head; here "cards" is, and [Filters.pluralCards] owns the word.
         */
        val exileSeveralFromGraveyard = phrase(
            "${lead("exile")} {n} {filter} from your graveyard",
            name = "exile several cards from your graveyard",
        ) {
            slot("n", Cardinals.word)
            slot("filter", Filters.pluralCards)
            build { atomOf(SdkCosts.ExileFromGraveyard(it.int("n"), it.value("filter"))) }
            match { atom ->
                val exile = atom as? CostAtom.ExileFrom ?: return@match null
                if (!Cardinals.spellable(exile.count)) return@match null
                if (atom != atomOf(SdkCosts.ExileFromGraveyard(exile.count, exile.filter))) return@match null
                bind("n" to exile.count, "filter" to exile.filter)
            }
        }

        /**
         * "Tap an untapped Cleric you control" — Nova Cleric, and the singular the plural rule below
         * refuses.
         *
         * "Untapped" and "you control" are **literals** rather than parts of the noun phrase, for
         * the plural rule's reason: `CostAtom.TapPermanents` carries neither, so slotting them would
         * print a filter the atom cannot hold. The article is a literal too — it is fixed by
         * "untapped", not by the noun, so no rule has to derive it.
         */
        val tapPermanent = phrase(
            "${lead("tap")} an untapped {filter} you control",
            name = "tap a permanent",
        ) {
            slot("filter", Filters.filter)
            build { atomOf(SdkCosts.TapPermanents(1, it.value("filter"))) }
            match { atom ->
                val tap = atom as? CostAtom.TapPermanents ?: return@match null
                if (tap.count != 1) return@match null
                if (atom != atomOf(SdkCosts.TapPermanents(1, tap.filter))) return@match null
                bind("filter" to tap.filter)
            }
        }

        /** "Tap another untapped Rogue you control" — the source excluded, [sacrificeAnother]'s word. */
        val tapAnotherPermanent = phrase(
            "${lead("tap")} another untapped {filter} you control",
            name = "tap another permanent",
        ) {
            slot("filter", Filters.filter)
            build { atomOf(SdkCosts.TapAnotherPermanent(it.value("filter"))) }
            match { atom ->
                val tap = atom as? CostAtom.TapPermanents ?: return@match null
                if (atom != atomOf(SdkCosts.TapAnotherPermanent(tap.filter))) return@match null
                bind("filter" to tap.filter)
            }
        }

        /** "Tap two untapped Birds you control" — Crookclaw Elder, Keeper of the Nine Gales. */
        val tapPermanents = phrase(
            "${lead("tap")} {n} untapped {filter} you control",
            name = "tap several permanents",
        ) {
            slot("n", Cardinals.word)
            slot("filter", Filters.plural)
            build { atomOf(SdkCosts.TapPermanents(it.int("n"), it.value("filter"))) }
            match { atom ->
                val tap = atom as? CostAtom.TapPermanents ?: return@match null
                if (!Cardinals.spellable(tap.count)) return@match null
                if (atom != atomOf(SdkCosts.TapPermanents(tap.count, tap.filter))) return@match null
                bind("n" to tap.count, "filter" to tap.filter)
            }
        }

        /**
         * "Return a land you control to its owner's hand" — Flooded Shoreline's singular sibling.
         *
         * "You control" is a literal for [tapPermanent]'s reason — `CostAtom.ReturnToHand` is
         * defined over permanents you control and holds no controller predicate — while the article
         * is inside [Filters.indefinite], because here it *does* vary with the noun.
         */
        val returnToHand = phrase(
            "${lead("return")} {filter} you control to its owner's hand",
            name = "return a permanent to its owner's hand",
        ) {
            slot("filter", Filters.indefinite)
            build { atomOf(SdkCosts.ReturnToHand(it.value("filter"))) }
            match { atom ->
                val returned = atom as? CostAtom.ReturnToHand ?: return@match null
                if (returned.count != 1) return@match null
                if (atom != atomOf(SdkCosts.ReturnToHand(returned.filter))) return@match null
                bind("filter" to returned.filter)
            }
        }

        /**
         * "Remove a spore counter from ~", "Remove three spore counters from ~" — the counters come
         * off the ability's own source.
         *
         * `self = true` is what makes this the *direct* payment rather than a distribution across
         * matching permanents, and the printed "from ~" is exactly that distinction: a cost that
         * named a filter would print "from among … you control". So the two are different rows, and
         * only the self one is written here — the filter form is a sentence no card in the backlog
         * spells as a cost.
         *
         * The kind is [Primitives.singularCounterKind] in the singular, which carries its own
         * "a"/"an", and [Primitives.counterKind] in the counted forms, where the number supplies
         * the determiner.
         */
        val removeCounterFromSelf = phrase(
            "${lead("remove")} {kind} counter from ${Normalizer.SELF}",
            name = "remove a counter from this permanent",
        ) {
            slot("kind", Primitives.singularCounterKind)
            build { atomOf(SdkCosts.RemoveCounterFromSelf(it.text("kind"))) }
            match { atom ->
                val remove = atom as? CostAtom.RemoveCounters ?: return@match null
                val kind = remove.counterType ?: return@match null
                if (atom != atomOf(SdkCosts.RemoveCounterFromSelf(kind))) return@match null
                bind("kind" to kind)
            }
        }

        val removeCountersFromSelf = phrase(
            "${lead("remove")} {n} {kind} counters from ${Normalizer.SELF}",
            name = "remove several counters from this permanent",
        ) {
            slot("n", Cardinals.word)
            slot("kind", Primitives.counterKind)
            build { atomOf(SdkCosts.RemoveCounterFromSelf(it.text("kind"), it.int("n"))) }
            match { atom ->
                val remove = atom as? CostAtom.RemoveCounters ?: return@match null
                val kind = remove.counterType ?: return@match null
                val count = (remove.count as? DynamicAmount.Fixed)?.amount ?: return@match null
                if (!Cardinals.spellable(count)) return@match null
                if (atom != atomOf(SdkCosts.RemoveCounterFromSelf(kind, count))) return@match null
                bind("n" to count, "kind" to kind)
            }
        }

        /**
         * "Remove X charge counters from ~" — Calciform Pools. The count is the ability's X.
         *
         * ### Two printed forms, one cost value — a finding, and why it is an [alsoSpelled]
         *
         * "Remove **any number of** charge counters from ~" (the mana batteries, the storage lands,
         * Geistflame Reservoir) is the *same* `CostAtom.RemoveCounters(count = XValue, self = true)`
         * as "Remove **X** …": both are CR 601.2b counts announced by the payer as the ability is
         * activated, and the SDK has one value for them. What differs is only how the rest of the
         * ability refers back to the number — "Add X mana …" against "…for each charge counter
         * removed this way" — and that is a property of the *effect*, which is a different slot of
         * the script.
         *
         * So this is one rule with two surfaces rather than two rules: registering a sibling would
         * be a second printer for one model, which invariant 2 forbids and the ambiguity gate
         * catches. "Remove X" stays canonical because it is the majority and because it is the form
         * whose payoff sentence the grammar can already read; the batteries' spelling parses and is
         * reported as a normalized alternate, which is the honest verdict for a form the model
         * cannot distinguish.
         */
        val removeXCountersFromSelf = phrase(
            "${lead("remove")} X {kind} counters from ${Normalizer.SELF}",
            name = "remove X counters from this permanent",
        ) {
            alsoSpelled(
                "${lead("remove")} any number of {kind} counters from ${Normalizer.SELF}",
                name = "remove a chosen number of counters from this permanent",
            )
            slot("kind", Primitives.counterKind)
            build { atomOf(SdkCosts.RemoveXCounters(counterType = it.text("kind"), self = true)) }
            match { atom ->
                val remove = atom as? CostAtom.RemoveCounters ?: return@match null
                val kind = remove.counterType ?: return@match null
                if (remove.count != DynamicAmount.XValue) return@match null
                if (atom != atomOf(SdkCosts.RemoveXCounters(counterType = kind, self = true))) return@match null
                bind("kind" to kind)
            }
        }

        /** "Pay 1 life" — Blood Celebrant. A numeral, per Oracle's convention for quantities of life. */
        val payLife = phrase("${lead("pay")} {n} life", name = "pay life") {
            slot("n", Primitives.cardinal)
            build { atomOf(SdkCosts.PayLife(it.int("n"))) }
            match { atom ->
                val pay = atom as? CostAtom.PayLife ?: return@match null
                if (atom != atomOf(SdkCosts.PayLife(pay.amount))) return@match null
                bind("n" to pay.amount)
            }
        }

        return oneOf(
            "a cost atom",
            *VariableCosts.rows(lead).toTypedArray(),
            sacrificeFiltered,
            sacrificeAnother,
            sacrificeSeveral,
            discardFiltered,
            discardSeveral,
            discardAtRandom,
            millCard,
            millSeveral,
            exileFromGraveyard,
            exileSeveralFromGraveyard,
            tapPermanent,
            tapAnotherPermanent,
            tapPermanents,
            returnToHand,
            removeCounterFromSelf,
            removeCountersFromSelf,
            removeXCountersFromSelf,
            payLife,
        )
    }

    /** The vocabulary as Oracle prints it everywhere but a line's first word. */
    private val capitalized: Phrase<CostAtom> = vocabulary { it.replaceFirstChar { c -> c.uppercaseChar() } }

    /** …and as [com.wingedsheep.assay.syntax.SentenceCase] leaves it when it *is* that first word. */
    private val lowercased: Phrase<CostAtom> = vocabulary { it }

    /**
     * One shared cost atom, in the activation position.
     *
     * The lowercase instance is an [alternate] here — reachable only where the line-start pass put
     * it, never printed — which is what keeps one printed form per model. In [additional] the two
     * are the other way round.
     */
    val atoms: Phrase<CostAtom> = oneOf("a cost atom", capitalized, alternate(lowercased))

    // -------------------------------------------------------------------------------------------
    // Lifted into an activated ability's cost
    // -------------------------------------------------------------------------------------------

    /**
     * A cost rule in both the spelling Oracle prints and the one the line-start pass leaves behind.
     *
     * [rule] is a function of its leading word so that the two halves cannot drift: there is one
     * template, instantiated twice, and the lowercase instance can never print. The atom vocabulary
     * takes the same argument in bulk; this stays for the handful of rows that are not atoms.
     */
    private fun bothCases(word: String, name: String, rule: (String) -> Phrase<AbilityCost>): Phrase<AbilityCost> =
        oneOf(name, rule(word.replaceFirstChar { it.uppercaseChar() }), alternate(rule(word)))

    private val tap: Phrase<AbilityCost> = constant("{T}", AbilityCost.Tap)

    /** `{Q}`, the untap symbol — the mirror of `{T}`, and a symbol `ManaCost.parse` also rejects. */
    private val untap: Phrase<AbilityCost> = constant("{Q}", AbilityCost.Untap)

    private val mana: Phrase<AbilityCost> = phrase("{cost}", name = "a mana cost") {
        slot("cost", Primitives.manaCost)
        build { SdkCosts.Mana(it.value<ManaCost>("cost")) }
        match { cost -> manaCostOf(cost)?.let { bind("cost" to it) } }
    }

    /**
     * "Sacrifice this creature", "Exile this creature" — the source paying with itself.
     *
     * Their own `AbilityCost` cases rather than a `Sacrifice` over a source-scoped filter, which is
     * why they are constants here and not rows of the atom vocabulary — and why they are unreachable
     * from [additional], which has no source to pay with.
     */
    private val sacrificeSelf: Phrase<AbilityCost> =
        bothCases("sacrifice", "sacrifice this permanent") { verb ->
            constant("$verb ${Normalizer.SELF}", AbilityCost.SacrificeSelf)
        }

    private val exileSelf: Phrase<AbilityCost> =
        bothCases("exile", "exile this permanent") { verb ->
            constant("$verb ${Normalizer.SELF}", AbilityCost.ExileSelf)
        }

    /** "Exert ~" — CR 701.39. The source pays by not untapping, so it is a self cost too. */
    private val exertSelf: Phrase<AbilityCost> =
        bothCases("exert", "exert this permanent") { verb ->
            constant("$verb ${Normalizer.SELF}", AbilityCost.Exert)
        }

    /** The costs only a permanent can pay, plus the two that are symbols rather than sentences. */
    private val abilityOnly: Phrase<AbilityCost> = oneOf(
        "a permanent's own cost",
        tap,
        untap,
        mana,
        sacrificeSelf,
        exileSelf,
        exertSelf,
    )

    /** One cost atom, wrapped for the context that asks an activated ability's payer. */
    private val liftedAtom: Phrase<AbilityCost> = phrase("{atom}", name = "a shared cost") {
        slot("atom", atoms)
        build { AbilityCost.Atom(it.value("atom")) }
        match { cost -> (cost as? AbilityCost.Atom)?.let { bind("atom" to it.atom) } }
    }

    /** One cost. */
    private val atom: Phrase<AbilityCost> = oneOf("a cost", abilityOnly, liftedAtom)

    /** "{2}{B}, {T}, Sacrifice a Goblin" — two or more atoms, in the order the card prints them. */
    private val composite: Phrase<AbilityCost> = phrase("{atoms}", name = "several costs") {
        slot("atoms", separated("costs", atom, ", ", min = 2))
        build { SdkCosts.Composite(it.value<List<AbilityCost>>("atoms")) }
        match { cost ->
            val parts = (cost as? AbilityCost.Composite)?.costs ?: return@match null
            if (parts.size < 2 || cost != SdkCosts.Composite(parts)) return@match null
            bind("atoms" to parts)
        }
    }

    val cost: Phrase<AbilityCost> = oneOf("an activation cost", atom, composite)

    // -------------------------------------------------------------------------------------------
    // Lifted into a spell's additional cost
    // -------------------------------------------------------------------------------------------

    /**
     * One additional cost, mid-sentence — the payload of
     * [Restrictions.additionalCostLine].
     *
     * Only the lowercase vocabulary, and only the atoms: the sentence puts the clause after a comma,
     * so the capital would be a misprint, and a spell has no source permanent for [abilityOnly]'s
     * rows to name.
     */
    val additional: Phrase<AdditionalCost> = phrase("{atom}", name = "an additional cost") {
        slot("atom", lowercased)
        build { AdditionalCost.Atom(it.value("atom")) }
        match { cost -> (cost as? AdditionalCost.Atom)?.let { bind("atom" to it.atom) } }
    }

    private fun manaCostOf(cost: AbilityCost): ManaCost? =
        ((cost as? AbilityCost.Atom)?.atom as? CostAtom.Mana)?.cost
}
