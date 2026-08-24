package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.assay.syntax.separated
import com.wingedsheep.assay.syntax.token
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Producing mana — "Add {G}.", "Add {C}{C}.", "Add {B} or {G}."
 *
 * The family exists because mana is the one effect Magic spells purely in symbols, and because the
 * *choice* form denotes several abilities rather than one effect with options. Both halves live
 * here so the word "add" is spelled in one file.
 *
 * ### `Add {B} or {G}.` is two abilities, and the SDK can say it two ways
 *
 * 165 hand-written cards spell a dual land's line as **two** `AddManaEffect` abilities sharing a
 * cost — Jungle Hollow's golden is two `CostTap` entries, one `BLACK` and one `GREEN` — and a much
 * smaller group spells it as one `AddManaOfChoiceEffect(ManaColorSet.Specific(...))`. Both work, and
 * the split is not arbitrary: every card in the second group carries a rider the first group cannot
 * express correctly ("Activate only once each turn" on two abilities would permit two activations),
 * which is a good reason for the type to exist and no reason for it to be a second spelling of the
 * plain case. The grammar therefore emits the majority form and never emits `ManaColorSet.Specific`,
 * the same treatment [Primitives.protectionScope] gives `ProtectionScope.Colors`. Registering both
 * would be genuine ambiguity — one text, two models — which the design says never to resolve by
 * picking one.
 *
 * Declining the minority form is also how the group polices its own membership. A rider is what
 * makes the line decline, so a card in the smaller group whose mana line *reads* has no rider and
 * therefore belongs in the majority. Spider Manifestation is the worked example: a bare
 * "{T}: Add {R} or {G}." that parsed, got compared, and diverged against its own golden. The card
 * was the bug, and it is now two abilities.
 *
 * A rule that denotes several things from one phrase is [Keywords.qualityRun]'s shape, which is why
 * [alternatives] hands back a list and [Activated] does the joining.
 */
object Mana {

    /**
     * "{G}", "{G}{G}", "{C}" — a repeated run of **one** mana symbol, as the effect it produces.
     *
     * One leaf rather than a symbol phrase plus a count, for the reason [Primitives.statModifiers]
     * is one leaf: the printed form repeats the symbol and the model holds a number, so neither
     * half can be written without seeing the other. A run of *different* symbols ("Add {W}{U}") is
     * a different effect the SDK spells as a composite, and this leaf declines it rather than
     * reading the first symbol and dropping the rest.
     *
     * The two halves are checked against each other by [token] itself, which re-reads what it
     * writes on every call — so an `AddManaEffect` carrying a restriction, a rider or a non-default
     * expiry prints "{G}", reads back as a plain one, compares unequal and refuses. Fail-closed by
     * construction rather than by a list of fields to remember.
     */
    val production: Phrase<Effect> = token(
        name = "mana symbols",
        pattern = Regex("""(?:\{[WUBRGC]})+"""),
        read = ::readProduction,
        write = ::writeProduction,
    )

    /**
     * "add {G}" — the clause, which is a spell effect in its own right (Dark Ritual).
     *
     * Periodless like every other clause in [Steps]: the full stop belongs to the sentence, not to
     * the verb phrase, which is what lets the same rule be a whole spell and the clause after an
     * activated ability's colon.
     */
    val addClause: Phrase<CardScript> = phrase("add {mana}", name = "add mana") {
        slot("mana", production)
        build { CardScript(spellEffect = it.value("mana")) }
        match { script ->
            val effect = script.spellEffect ?: return@match null
            if (script != CardScript(spellEffect = effect)) return@match null
            if (writeProduction(effect) == null) return@match null
            bind("mana" to effect)
        }
    }

    /**
     * "Add one mana of any color." — Blood Celebrant, and the whole any-colour family.
     *
     * The count is written as a *word* ("one mana", "two mana"), which is Oracle's convention for a
     * quantity of mana in prose as opposed to in symbols — so this takes [Cardinals.word] and the
     * singular is a rule of its own, the same split every counting rule in the grammar makes. There
     * is no plural of "mana", which is why only the noun before it changes.
     *
     * ### The plural spells "any **one** color", and reading only "any color" was a frozen word
     *
     * `AddAnyColorMana(n)` is *n* mana of a single colour the player picks, and above one that is
     * exactly what English has to say out loud: Oracle prints "add two mana of any **one** color" 63
     * times in the corpus against "add two mana of any color" 3 times. The rule spelled only the
     * second, so it read the three oddities and declined the sixty-three cards — the same defect
     * shape as [Filters]' controller layer sitting outside the mana-value qualifier, a modifier
     * frozen at the spelling whichever card came first happened to use.
     *
     * The two spellings are one rule via [com.wingedsheep.assay.syntax.PhraseBuilder.alsoSpelled],
     * with the common one canonical. The singular takes the pair the other way round — "add one mana
     * of any color" is what 625 cards print and "of any one color" is the single oddity — which is
     * why the canonical [template] is a parameter rather than a suffix rule.
     */
    private fun addAnyColour(
        template: String,
        name: String,
        fixed: Int?,
        alsoSpelledAs: String,
    ): Phrase<CardScript> {
        fun scriptFor(count: Int) = CardScript(spellEffect = Effects.AddAnyColorMana(count))
        return phrase(template, name = name) {
            if (fixed == null) slot("n", Cardinals.word)
            alsoSpelled(alsoSpelledAs, "$name (alternate)")
            build { scriptFor(fixed ?: it.int("n")) }
            match { script ->
                val amount = (script.spellEffect as? AddManaOfChoiceEffect)?.amount
                    ?.let { it as? DynamicAmount.Fixed }?.amount ?: return@match null
                if (fixed != null && amount != fixed) return@match null
                if (fixed == null && !Cardinals.spellable(amount)) return@match null
                if (script != scriptFor(amount)) return@match null
                bind("n" to amount)
            }
        }
    }

    /**
     * "Add three mana in any combination of {R} and/or {G}." — Goblin Clearcutter.
     *
     * A different effect from [addAnyColour] and not a restriction on it: the colours are an
     * enumerated *set* the player draws from freely, so the model carries both the amount and the
     * set. Two colours is what every printed card in this shape names, and the join is the same
     * "and/or" [Filters.anyColour] reads — so a third colour is a row rather than a rule, whenever
     * one appears.
     */
    private val addCombination: Phrase<CardScript> = run {
        fun scriptFor(count: Int, first: Color, second: Color) = CardScript(
            spellEffect = Effects.AddDynamicMana(DynamicAmount.Fixed(count), setOf(first, second))
        )
        phrase(
            "add {n} mana in any combination of {first} and/or {second}",
            name = "add mana in any combination",
        ) {
            slot("n", Cardinals.word)
            slot("first", Primitives.manaSymbolColor)
            slot("second", Primitives.manaSymbolColor)
            build { scriptFor(it.int("n"), it.value("first"), it.value("second")) }
            match { script ->
                val effect = script.spellEffect as? AddDynamicManaEffect ?: return@match null
                val amount = (effect.amountSource as? DynamicAmount.Fixed)?.amount ?: return@match null
                val colours = effect.allowedColors.toList()
                if (colours.size != 2 || !Cardinals.spellable(amount)) return@match null
                if (script != scriptFor(amount, colours[0], colours[1])) return@match null
                bind("n" to amount, "first" to colours[0], "second" to colours[1])
            }
        }
    }

    /**
     * "Add two mana in any combination of colors." — Chromatic Orrery, and the fourteen lines the
     * decline table keyed on "colors. Spend this …".
     *
     * The same effect [addCombination] builds with the colour set left implicit, which is what
     * `Effects.AddManaInAnyCombination`'s own default says it is: all five. A separate rule rather
     * than a colour-set slot over both, because "colors" is not a spelling of "{R} and/or {G}" — the
     * enumerated form names its colours and this one declines to, so there is nothing for a shared
     * slot to bind.
     */
    private val addAllColourCombination: Phrase<CardScript> = run {
        val everyColour = Color.entries.toSet()
        fun scriptFor(count: Int) = CardScript(
            spellEffect = Effects.AddDynamicMana(DynamicAmount.Fixed(count), everyColour)
        )
        phrase("add {n} mana in any combination of colors", name = "add mana in any combination of colours") {
            slot("n", Cardinals.word)
            build { scriptFor(it.int("n")) }
            match { script ->
                val effect = script.spellEffect as? AddDynamicManaEffect ?: return@match null
                val amount = (effect.amountSource as? DynamicAmount.Fixed)?.amount ?: return@match null
                if (effect.allowedColors != everyColour || !Cardinals.spellable(amount)) return@match null
                if (script != scriptFor(amount)) return@match null
                bind("n" to amount)
            }
        }
    }

    /**
     * Every "add …" clause a spell or an ability can print, other than the symbol form above.
     *
     * Declared after the rules it lists — object initializers run in declaration order, and a `val`
     * reaching a later one reads a null out of a half-initialized object.
     */
    val addClauses: List<Phrase<CardScript>> = listOf(
        addAnyColour(
            "add one mana of any color",
            "add one mana of any colour",
            fixed = 1,
            alsoSpelledAs = "add one mana of any one color",
        ),
        addAnyColour(
            "add {n} mana of any one color",
            "add several mana of any colour",
            fixed = null,
            alsoSpelledAs = "add {n} mana of any color",
        ),
        addCombination,
        addAllColourCombination,
    )

    /** "{B} or {G}" — exactly two, which is every dual land. */
    private val pair: Phrase<List<Effect>> = phrase("{first} or {second}", name = "two kinds of mana") {
        slot("first", production)
        slot("second", production)
        build { listOf(it.value("first"), it.value("second")) }
        match { effects -> effects.takeIf { it.size == 2 }?.let { bind("first" to it[0], "second" to it[1]) } }
    }

    /**
     * "{W}, {U}, or {B}" — three or more, with the Oxford comma the printed cards use. The same
     * two shapes [Primitives.scopeRun] is built from, over a different join word, and disjoint for
     * the same reason: [pair] takes exactly two and this takes at least three, so printing picks
     * the shape from the count rather than from a preference.
     */
    private val series: Phrase<List<Effect>> = phrase("{most}, or {last}", name = "three or more kinds of mana") {
        slot("most", separated("kinds of mana", production, ", ", min = 2))
        slot("last", production)
        build { it.value<List<Effect>>("most") + it.value<Effect>("last") }
        match { effects -> effects.takeIf { it.size >= 3 }?.let { bind("most" to it.dropLast(1), "last" to it.last()) } }
    }

    /** Two or more kinds of mana, joined the way printed Oracle text joins them. */
    private val alternatives: Phrase<List<Effect>> = oneOf("two or more kinds of mana", pair, series)

    /**
     * "Add {B} or {G}." — the sentence that denotes **several** mana effects, one per choice.
     *
     * Kept beside [added] rather than folded into it: one produces a `CardScript` because a single
     * mana effect is a spell effect a card can print on its own, and this produces a list of
     * effects because the choice form only ever appears as an activated ability's several
     * abilities. Their surface forms are disjoint — the join word is required here — so no text
     * reads both ways.
     */
    val addedAlternatives: Phrase<List<Effect>> = phrase("add {alternatives}.", name = "add one of several kinds of mana") {
        slot("alternatives", alternatives)
        build { it.value("alternatives") }
        match { effects ->
            if (effects.size < 2) return@match null
            if (effects.any { writeProduction(it) == null }) return@match null
            bind("alternatives" to effects)
        }
    }

    // -------------------------------------------------------------------------------------------
    // …and what the mana it produces may be spent on
    // -------------------------------------------------------------------------------------------

    /** Every "add …" clause, as the one slot a trailing spend restriction attaches behind. */
    private val anyAddClause: Phrase<CardScript> =
        oneOf("an add-mana clause", listOf(addClause) + addClauses)

    /**
     * "Add {G}. Spend this mana only to cast creature spells." — **one clause spanning two printed
     * sentences**, because the second one fills a field on the first one's effect.
     *
     * `restriction` is a field on all four `Add*ManaEffect`s, so the restriction sentence is not an
     * effect and has nowhere to go in a [Steps] run — a run member is a `CompositeEffect` element.
     * A rule spanning both sentences is the only shape that can hold it, which is
     * [Grammar.amplifyLine]'s argument one slot in: the fragment is the only place a line's two
     * contributions can meet, and here the clause is the only place a sentence pair's can.
     *
     * Being a clause rather than a rule in [Activated] is what makes it cheap. The same two sentences
     * are printed after an activated ability's colon, after a trigger's comma, after a loyalty
     * ability's "+1:", as a bare spell effect, and inside a granted ability's quotation marks — five
     * positions, all of which slot [Steps.step] and therefore inherit this without being told.
     *
     * The restriction is stripped before the inner clause is asked to print, which is what lets
     * every "add" rule stay unaware of the field: [Mana.production]'s leaf re-reads what it writes
     * and would refuse a restricted effect (see its note), and the any-colour and combination rules
     * refuse one by reconstructing the whole script. That fail-closed behaviour is now load-bearing
     * in both directions — it is what keeps a restricted effect from printing as a bare "Add {G}."
     */
    val restricted: Phrase<CardScript> =
        phrase("{add}. {spend}", name = "add mana with a spend restriction") {
            slot("add", anyAddClause)
            slot("spend", ManaSpending.restriction)
            build { bindings ->
                val inner = bindings.value<CardScript>("add")
                val effect = inner.spellEffect ?: return@build null
                if (inner != CardScript(spellEffect = effect)) return@build null
                withRestriction(effect, bindings.value("spend"))?.let { CardScript(spellEffect = it) }
            }
            match { script ->
                val effect = script.spellEffect ?: return@match null
                if (script != CardScript(spellEffect = effect)) return@match null
                val restriction = restrictionOf(effect) ?: return@match null
                val bare = withRestriction(effect, null) ?: return@match null
                bind("add" to CardScript(spellEffect = bare), "spend" to restriction)
            }
        }

    /**
     * "Add {U} or {R}. Spend this mana only to cast a noncreature spell." — The Emperor of Palamecia.
     *
     * [restricted] one shape over, for the reason [addedAlternatives] is beside [addClause]: the
     * choice form denotes several abilities rather than one effect, so the restriction lands on each
     * of them. It has to be *the same* restriction on all of them — the sentence is one sentence —
     * which is what `match` checks by reading one and reconstructing the rest.
     */
    val addedAlternativesRestricted: Phrase<List<Effect>> =
        phrase("add {alternatives}. {spend}.", name = "add one of several kinds of restricted mana") {
            slot("alternatives", alternatives)
            slot("spend", ManaSpending.restriction)
            build { bindings ->
                val restriction = bindings.value<ManaRestriction>("spend")
                val built = bindings.value<List<Effect>>("alternatives")
                    .map { withRestriction(it, restriction) }
                if (built.any { it == null }) null else built.filterNotNull()
            }
            match { effects ->
                if (effects.size < 2) return@match null
                val restriction = restrictionOf(effects.first()) ?: return@match null
                val bare = effects.map { effect ->
                    if (restrictionOf(effect) != restriction) return@match null
                    withRestriction(effect, null) ?: return@match null
                }
                if (bare.any { writeProduction(it) == null }) return@match null
                bind("alternatives" to bare, "spend" to restriction)
            }
        }

    /** The restriction [effect] carries, or null when it is not a mana effect or carries none. */
    private fun restrictionOf(effect: Effect): ManaRestriction? = when (effect) {
        is AddManaEffect -> effect.restriction
        is AddColorlessManaEffect -> effect.restriction
        is AddManaOfChoiceEffect -> effect.restriction
        is AddDynamicManaEffect -> effect.restriction
        else -> null
    }

    /**
     * [effect] with its restriction set to [restriction], or null when it has no such field.
     *
     * One function for both directions and for both of [restricted]'s halves, so "which effects have
     * a restriction" is answered in exactly one place. Nothing else on the effect is touched: a rider
     * set or a non-default expiry survives the round trip into the inner clause, which then refuses
     * to print it — the fail-closed path the field list would otherwise have to remember.
     */
    private fun withRestriction(effect: Effect, restriction: ManaRestriction?): Effect? = when (effect) {
        is AddManaEffect -> effect.copy(restriction = restriction)
        is AddColorlessManaEffect -> effect.copy(restriction = restriction)
        is AddManaOfChoiceEffect -> effect.copy(restriction = restriction)
        is AddDynamicManaEffect -> effect.copy(restriction = restriction)
        else -> null
    }

    // -------------------------------------------------------------------------------------------
    // The leaf's two halves
    // -------------------------------------------------------------------------------------------

    /**
     * A colourless run is [AddColorlessManaEffect] and a coloured one is [AddManaEffect] — two SDK
     * types for one printed shape, which is why this reads the symbol before it reads the count.
     */
    private fun readProduction(symbols: String): Effect? {
        val letters = symbols.filter { it in "WUBRGC" }
        val symbol = letters.firstOrNull() ?: return null
        if (letters.any { it != symbol }) return null
        val count = letters.length
        if (symbol == 'C') return Effects.AddColorlessMana(count)
        return Effects.AddMana(Color.fromSymbol(symbol) ?: return null, count)
    }

    private fun writeProduction(effect: Effect): String? = when (effect) {
        is AddManaEffect -> effect.amount.fixed()?.let { "{${effect.color.symbol}}".repeat(it) }
        is AddColorlessManaEffect -> effect.amount.fixed()?.let { "{C}".repeat(it) }
        else -> null
    }

    private fun DynamicAmount.fixed(): Int? = (this as? DynamicAmount.Fixed)?.amount?.takeIf { it >= 1 }
}
