package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Bindings
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.assay.syntax.separated
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * "Spend this mana only to cast creature spells." — what produced mana may be *spent* on, and the
 * top family in the tail ranking: 116 cards, 64 of them solely, over 118 lines.
 *
 * ### It is not a sentence, it is a field on the sentence before it
 *
 * Every printed instance is a second sentence following an "add …" clause, and what it denotes is
 * the `restriction` field on that clause's effect — one field on each of the four `Add*ManaEffect`s.
 * So it cannot be a clause in [Steps]' run: a run member is a `CompositeEffect` element, and this is
 * not an effect at all. The rule that reads it therefore spans **both** sentences, which is
 * [Mana.restricted]'s shape and [Grammar.amplifyLine]'s reason for existing one slot further out.
 *
 * Putting it on the *clause* rather than on the activated ability is what makes the family cheap:
 * `{T}: Add {G}. Spend this mana only to …`, `At the beginning of your first main phase, add {R}{R}.
 * Spend this mana only to …`, `+1: Add {R}{R}{R}. Spend this mana only to …`, a bare spell effect
 * and the same text inside a granted ability's quotation marks are all one rule, because they were
 * always one clause in five positions.
 *
 * ### The axis cannot be a filter, because `mtg-sdk` does not type it as one
 *
 * Every other "what does this apply to" family in this grammar slots [Filters]. This one cannot:
 * `ManaRestriction` is a **closed vocabulary of spend contexts**, not a `GameObjectFilter`, so a
 * filter slot would have nowhere to put its value. That is not a gap — a spend context is not a set
 * of objects. "Turn permanents face up", "unlock a door" and "activate equip abilities" name special
 * actions and cost payments that no object filter describes, and the two contexts that *are* about a
 * spell's characteristics are the two the SDK parameterizes: the card type and the subtype.
 *
 * So the grammar becomes the same product the SDK is:
 *
 *  - **the card type** × {spells, abilities, both} × negated → [ManaRestriction.CardTypeSpellsOrAbilitiesOnly]
 *  - **the subtype list** → [ManaRestriction.SubtypeSpellsOnly], and its spells-*and*-abilities
 *    sibling [ManaRestriction.SubtypeSpellsOrAbilitiesOnly]
 *  - **the mana-value floor** × {or with {X} in their cost} × {creature spells only} →
 *    [ManaRestriction.SpellsWithManaValueAtLeast]
 *  - and the atoms, which have no parameter at all
 *
 * with [ManaRestriction.AnyOf] as the join, and one negative sentence
 * ([ManaRestriction.CannotCastSpellsOtherThan]) that is a different sentence rather than a negated
 * spelling of this one.
 *
 * ### The declared empty cell
 *
 * `CardTypeSpellsOrAbilitiesOnly(CREATURE, allowSpells = true, allowAbilities = false)` and
 * [ManaRestriction.CreatureSpellsOnly] mean the same thing and would print the same words. All 15
 * hand-written cards that print "cast a creature spell" use the second, and no card uses the first,
 * so the card-type row **declares that cell empty**: the product covers every type but creature in
 * its spells-only column, and the atom covers creature. Registering both would be one text with two
 * models, which the design says never to resolve by preference. It is the same treatment
 * [Mana]'s note gives `ManaColorSet.Specific` and [Filters]'s gives the bare subtype.
 *
 * The same guard falls on the join: "cast artifact spells or activate abilities of artifacts" is
 * *one* atom with both booleans set, and an `AnyOf` of the two halves would print the identical
 * words. [joined] therefore refuses a pair the combined row can express, so the sentence has exactly
 * one reading and the ambiguity the touchstone counts stays at zero.
 *
 * ### Number is not in the model, so half the family is an alternate
 *
 * Oracle writes both "cast **a** creature spell" and "cast creature spell**s**" for the same value —
 * the printed number tracks how much mana the clause added, which the restriction does not carry.
 * Exactly one has to print, and the plural is canonical: it needs no article agreement, it is the
 * spelling the SDK's own `description` strings use, and the bare contexts ("activate abilities")
 * have no singular at all. A card printing the singular comes back as a
 * [com.wingedsheep.assay.gate.LineVerdict.VARIANT] — read correctly, spelled canonically.
 */
object ManaSpending {

    // -------------------------------------------------------------------------------------------
    // Articles — "an artifact spell" against "a creature spell"
    // -------------------------------------------------------------------------------------------

    /**
     * "a {noun}" / "an {noun}", with the article agreeing with whatever the slot prints.
     *
     * Two disjoint rules rather than one rule over an article slot, for [Filters.article]'s reason:
     * the article is not in the model, so the only thing that can decide it is the noun's own
     * printed first letter — which makes it a property of the pair rather than a value to bind.
     */
    private fun <T> articled(inner: Phrase<T>, name: String): Phrase<T> =
        oneOf(name, articleRule(inner, "a", name), articleRule(inner, "an", name))

    private fun <T> articleRule(inner: Phrase<T>, article: String, name: String): Phrase<T> =
        phrase("$article {noun}", name = "\"$article\" plus $name") {
            slot("noun", inner)
            build { bindings -> bindings.value<T>("noun").takeIf { articleFor(inner, it) == article } }
            match { value -> if (articleFor(inner, value) == article) bind("noun" to value) else null }
        }

    /** The article [inner] would print [value] with, or null when it cannot print it at all. */
    private fun <T> articleFor(inner: Phrase<T>, value: T): String? {
        val head = inner.unparse(value)?.firstOrNull()?.lowercaseChar() ?: return null
        return if (head in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
    }

    // -------------------------------------------------------------------------------------------
    // The card type
    // -------------------------------------------------------------------------------------------

    /**
     * The card types this family names, and the word Oracle spells each with.
     *
     * Enumerated rather than taken from `CardType.entries` because the word is not the enum's
     * `displayName` lowercased in every case that matters — and because a type nobody restricts mana
     * to is a row that would widen the product with no card behind it. Every type here appears in the
     * corpus in this position.
     */
    private val CARD_TYPES: List<Pair<CardType, String>> = listOf(
        CardType.ARTIFACT to "artifact",
        CardType.CREATURE to "creature",
        CardType.ENCHANTMENT to "enchantment",
        CardType.LAND to "land",
        CardType.PLANESWALKER to "planeswalker",
    )

    /** A card type and whether the sentence negated it — "artifact", "noncreature". */
    private data class TypeWord(val type: CardType, val negated: Boolean)

    /**
     * The adjective in front of "spell(s)" — the singular type word, or the same word with "non"
     * glued to its front.
     *
     * One leaf over both, because the negation is one bit of the value it produces rather than a
     * layer over a smaller one: `CardTypeSpellsOrAbilitiesOnly` has a `negated` field and nothing
     * else changes with it.
     */
    private val typeWord: Phrase<TypeWord> = oneOf(
        "a card type",
        CARD_TYPES.flatMap { (type, word) ->
            listOf(
                constant(word, TypeWord(type, negated = false)),
                constant("non$word", TypeWord(type, negated = true)),
            )
        },
    )

    /** …and the plural, which only the ability half needs: "abilities of artifact**s**". */
    private val pluralTypeWord: Phrase<CardType> = oneOf(
        "a card type (plural)",
        CARD_TYPES.map { (type, word) -> constant("${word}s", type) },
    )

    /** "artifact spells" — canonical — and "an artifact spell", which is the same value. */
    private val typeSpells: Phrase<TypeWord> = oneOf(
        "a card type's spells",
        phrase("{type} spells", name = "a card type's spells") {
            slot("type", typeWord)
            build { it.value("type") }
            match { bind("type" to it) }
        },
        alternate(
            phrase<TypeWord>("{type} spell", name = "a card type's spell") {
                slot("type", articled(typeWord, "a card type"))
                build { it.value("type") }
                canonical = false
            }
        ),
    )

    /**
     * "abilities of artifacts" — and the three other ways Oracle spells one card type's abilities.
     *
     * "Artifact source" and "artifact" are the same set by the printed ruling on Mishra's Workshop —
     * a source is any object with that card type in any zone — so the word "source" carries nothing
     * the model can hold, and the four spellings are one value.
     */
    private val typeAbilities: Phrase<CardType> = oneOf(
        "a card type's abilities",
        phrase("abilities of {type}", name = "a card type's abilities") {
            slot("type", pluralTypeWord)
            build { it.value("type") }
            match { bind("type" to it) }
        },
        alternate(
            phrase<CardType>("abilities of {type} sources", name = "a card type's sources' abilities") {
                slot("type", bare(typeWord))
                build { it.value("type") }
                canonical = false
            }
        ),
        alternate(
            phrase<CardType>("an ability of {type}", name = "one of a card type's abilities") {
                slot("type", articled(bare(typeWord), "a card type"))
                build { it.value("type") }
                canonical = false
            }
        ),
        alternate(
            phrase<CardType>("an ability of {type} source", name = "one of a card type's sources' abilities") {
                slot("type", articled(bare(typeWord), "a card type"))
                build { it.value("type") }
                canonical = false
            }
        ),
    )

    /** [typeWord] narrowed to its un-negated half: "abilities of nonartifacts" is not printed. */
    private fun bare(inner: Phrase<TypeWord>): Phrase<CardType> =
        phrase("{word}", name = "a card type") {
            slot("word", inner)
            build { it.value<TypeWord>("word").takeIf { word -> !word.negated }?.type }
            match { type -> bind("word" to TypeWord(type, negated = false)) }
        }

    // -------------------------------------------------------------------------------------------
    // The subtype
    // -------------------------------------------------------------------------------------------

    /**
     * "Dragon", "Mount or Vehicle", "Dwarf, Equipment, or Saga" — the subtypes one restriction names,
     * with the joins Oracle writes them with.
     *
     * The shape is [Mana.alternatives]': a pair takes exactly two and a series at least three, so
     * printing picks from the count rather than from a preference. The join *word* is the part the
     * model cannot hold — `SubtypeSpellsOnly` carries a `Set` — so "or" is canonical and "and" and
     * "and/or" parse without printing.
     */
    private val subtypeRun: Phrase<List<Subtype>> = run {
        val one = phrase<List<Subtype>>("{subtype}", name = "a subtype") {
            slot("subtype", Primitives.subtype)
            build { listOf(it.value<Subtype>("subtype")) }
            match { it.singleOrNull()?.let { only -> bind("subtype" to only) } }
        }
        fun pair(join: String, canonicalJoin: Boolean): Phrase<List<Subtype>> {
            val rule = phrase<List<Subtype>>("{first}$join{second}", name = "two subtypes") {
                slot("first", Primitives.subtype)
                slot("second", Primitives.subtype)
                build { listOf(it.value("first"), it.value("second")) }
                match { types ->
                    types.takeIf { it.size == 2 }?.let { bind("first" to it[0], "second" to it[1]) }
                }
                canonical = canonicalJoin
            }
            return if (canonicalJoin) rule else alternate(rule)
        }
        fun series(join: String, canonicalJoin: Boolean): Phrase<List<Subtype>> {
            val rule = phrase<List<Subtype>>("{most},$join{last}", name = "three or more subtypes") {
                slot("most", separated("subtypes", Primitives.subtype, ", ", min = 2))
                slot("last", Primitives.subtype)
                build { it.value<List<Subtype>>("most") + it.value<Subtype>("last") }
                match { types ->
                    types.takeIf { it.size >= 3 }?.let { bind("most" to it.dropLast(1), "last" to it.last()) }
                }
                canonical = canonicalJoin
            }
            return if (canonicalJoin) rule else alternate(rule)
        }
        oneOf(
            "one or more subtypes",
            one,
            pair(" or ", canonicalJoin = true),
            pair(" and ", canonicalJoin = false),
            pair(" and/or ", canonicalJoin = false),
            series(" or ", canonicalJoin = true),
            series(" and ", canonicalJoin = false),
            series(" and/or ", canonicalJoin = false),
        )
    }

    // -------------------------------------------------------------------------------------------
    // One spend context
    // -------------------------------------------------------------------------------------------

    /**
     * A context with no parameter: the whole printed phrase, in both numbers where English has two.
     *
     * [com.wingedsheep.assay.syntax.PhraseBuilder.alsoSpelled] rather than a sibling rule, because
     * the second spelling is the same rule with a word somewhere else — sharing the closures is what
     * stops the two halves drifting.
     */
    private fun atom(
        printed: String,
        value: ManaRestriction,
        vararg spellings: String,
    ): Phrase<ManaRestriction> = phrase(printed, name = printed) {
        build { value }
        match { if (it == value) Bindings.EMPTY else null }
        spellings.forEach { alsoSpelled(it, it) }
    }

    /**
     * The contexts that name no type at all.
     *
     * "Cast a creature spell" is here rather than in the card-type product for the reason the class
     * note gives; every other row is a context the product structurally cannot reach — a special
     * action (turn a permanent face up, unlock a door), a cost payment (an equip ability), or a
     * property of the *cast* rather than of the card (kicked, face-down, from exile).
     */
    private val atoms: List<Phrase<ManaRestriction>> = listOf(
        atom(
            "cast creature spells",
            ManaRestriction.CreatureSpellsOnly,
            "cast a creature spell",
        ),
        atom(
            "cast instant or sorcery spells",
            ManaRestriction.InstantOrSorceryOnly,
            "cast an instant or sorcery spell",
            "cast instant and sorcery spells",
            "cast instant and/or sorcery spells",
        ),
        atom(
            "cast legendary spells",
            ManaRestriction.LegendarySpellsOnly,
            "cast a legendary spell",
        ),
        atom(
            "cast kicked spells",
            ManaRestriction.KickedSpellsOnly,
            "cast a kicked spell",
        ),
        atom(
            "cast face-down spells",
            ManaRestriction.FaceDownSpellsOnly,
            "cast a face-down spell",
        ),
        atom("cast spells from exile", ManaRestriction.CastFromExileOnly),
        // Singular-only, because that is the only number Oracle prints it in: the clause names one
        // spell's origin rather than a class of card, so there is nothing to make plural.
        atom("cast a spell from anywhere other than your hand", ManaRestriction.CastFromNonHandOnly),
        atom(
            "activate abilities",
            ManaRestriction.AbilityActivationOnly,
            "activate an ability",
        ),
        atom(
            "activate equip abilities",
            ManaRestriction.EquipAbilityActivationOnly,
            "activate an equip ability",
        ),
        atom(
            "turn permanents face up",
            ManaRestriction.TurnPermanentsFaceUpOnly,
            "turn a permanent face up",
        ),
        atom(
            "unlock doors",
            ManaRestriction.UnlockDoorOnly,
            "unlock a door",
        ),
    )

    /** "cast artifact spells" — the product's spells-only column, creature excepted. */
    private val castTypeSpells: Phrase<ManaRestriction> =
        phrase("cast {spells}", name = "cast a card type's spells") {
            slot("spells", typeSpells)
            build { bindings -> spellsOnly(bindings.value("spells")) }
            match { value ->
                val restriction = value as? ManaRestriction.CardTypeSpellsOrAbilitiesOnly ?: return@match null
                val word = TypeWord(restriction.cardType, restriction.negated)
                if (spellsOnly(word) != restriction) return@match null
                bind("spells" to word)
            }
        }

    /**
     * The declared empty cell: creature spells with no negation are [ManaRestriction.CreatureSpellsOnly].
     *
     * Shared by both directions so the hole is one expression rather than two guards that could
     * disagree — the same reason [Activated.abilityFor] is one function.
     */
    private fun spellsOnly(word: TypeWord): ManaRestriction.CardTypeSpellsOrAbilitiesOnly? {
        if (word.type == CardType.CREATURE && !word.negated) return null
        return ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
            cardType = word.type,
            allowSpells = true,
            allowAbilities = false,
            negated = word.negated,
        )
    }

    /** "activate abilities of artifacts" — the product's abilities-only column. */
    private val activateTypeAbilities: Phrase<ManaRestriction> =
        phrase("activate {abilities}", name = "activate a card type's abilities") {
            slot("abilities", typeAbilities)
            build { bindings -> abilitiesOnly(bindings.value("abilities")) }
            match { value ->
                val restriction = value as? ManaRestriction.CardTypeSpellsOrAbilitiesOnly ?: return@match null
                if (abilitiesOnly(restriction.cardType) != restriction) return@match null
                bind("abilities" to restriction.cardType)
            }
        }

    private fun abilitiesOnly(type: CardType) = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
        cardType = type,
        allowSpells = false,
        allowAbilities = true,
    )

    /**
     * "cast artifact spells or activate abilities of artifacts" — the product's *both* column, and
     * the sub-family the corpus prints most often after the bare creature and instant-or-sorcery
     * rows.
     *
     * Two slots and an equality guard rather than one slot referenced twice: a repeated slot
     * reference binds the second reading over the first, so "cast artifact spells or activate
     * abilities of creatures" would read as a restriction about creatures and round-trip as one
     * about artifacts. Making the agreement a `build` condition is what refuses that line instead.
     */
    private val castOrActivateType: Phrase<ManaRestriction> =
        phrase("cast {spells} or activate {abilities}", name = "cast or activate one card type") {
            slot("spells", typeSpells)
            slot("abilities", typeAbilities)
            build { bindings -> spellsAndAbilities(bindings.value("spells"), bindings.value("abilities")) }
            match { value ->
                val restriction = value as? ManaRestriction.CardTypeSpellsOrAbilitiesOnly ?: return@match null
                val word = TypeWord(restriction.cardType, restriction.negated)
                if (spellsAndAbilities(word, restriction.cardType) != restriction) return@match null
                bind("spells" to word, "abilities" to restriction.cardType)
            }
        }

    private fun spellsAndAbilities(word: TypeWord, abilityType: CardType): ManaRestriction? {
        if (word.negated || word.type != abilityType) return null
        return ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
            cardType = word.type,
            allowSpells = true,
            allowAbilities = true,
        )
    }

    /** "cast Dragon spells" / "cast a Dragon spell" — one or more named subtypes, spells only. */
    private val castSubtypeSpells: Phrase<ManaRestriction> = oneOf(
        "cast a subtype's spells",
        phrase("cast {run} spells", name = "cast a subtype's spells") {
            slot("run", subtypeRun)
            build { ManaRestriction.SubtypeSpellsOnly(subtypeSet(it.value("run"))) }
            match { value ->
                val restriction = value as? ManaRestriction.SubtypeSpellsOnly ?: return@match null
                val run = restriction.subtypes.map(::Subtype)
                if (ManaRestriction.SubtypeSpellsOnly(subtypeSet(run)) != restriction) return@match null
                bind("run" to run)
            }
        },
        alternate(
            phrase<ManaRestriction>("cast {run} spell", name = "cast a subtype's spell") {
                slot("run", articled(subtypeRun, "one or more subtypes"))
                build { ManaRestriction.SubtypeSpellsOnly(subtypeSet(it.value("run"))) }
                canonical = false
            }
        ),
        // "cast a Dragon spell or an Omen spell" — the noun repeated instead of shared, which is a
        // *third* spelling of the same set rather than a member of [subtypeRun]: the run joins
        // subtypes and this joins whole noun phrases, so the article agrees with each one separately.
        // Maelstrom of the Spirit Dragon is the card, and its golden is the two-element set.
        alternate(
            phrase<ManaRestriction>("cast {first} spell or {second} spell", name = "cast either subtype's spell") {
                slot("first", articled(Primitives.subtype, "a subtype"))
                slot("second", articled(Primitives.subtype, "a subtype"))
                build { bindings ->
                    val first = bindings.value<Subtype>("first")
                    val second = bindings.value<Subtype>("second")
                    if (first == second) return@build null
                    ManaRestriction.SubtypeSpellsOnly(subtypeSet(listOf(first, second)))
                }
                canonical = false
            }
        ),
    )

    /** Insertion order is what prints, so it has to be the order the sentence named them in. */
    private fun subtypeSet(run: List<Subtype>): Set<String> = run.map { it.value }.toSet()

    /** "Dragon spells" — canonical — and "a Dragon spell", which names the same subtype. */
    private val subtypeSpells: Phrase<Subtype> = oneOf(
        "a subtype's spells",
        phrase("{subtype} spells", name = "a subtype's spells") {
            slot("subtype", Primitives.subtype)
            build { it.value("subtype") }
            match { bind("subtype" to it) }
        },
        alternate(
            phrase<Subtype>("{subtype} spell", name = "a subtype's spell") {
                slot("subtype", articled(Primitives.subtype, "a subtype"))
                build { it.value("subtype") }
                canonical = false
            }
        ),
    )

    /**
     * "abilities of Dragons" — and the three other ways Oracle spells one subtype's abilities, which
     * are [typeAbilities]' four spellings one vocabulary over.
     *
     * The plural is [Primitives.pluralSubtype] rather than an appended "s", which is the leaf that
     * knows `Elves` is `Elf` and `Plains` is `Plains`. Its one residue shows up here: that leaf's
     * pattern requires a trailing "s", so a type whose plural carries none — "activate abilities of
     * **Myr**" — declines. Widening the pattern is not this band's call, because it would give every
     * bare singular subtype in the grammar a second reading as a plural.
     */
    private val subtypeAbilities: Phrase<Subtype> = oneOf(
        "a subtype's abilities",
        phrase("abilities of {subtype}", name = "a subtype's abilities") {
            slot("subtype", Primitives.pluralSubtype)
            build { it.value("subtype") }
            match { bind("subtype" to it) }
        },
        alternate(
            phrase<Subtype>("abilities of {subtype} sources", name = "a subtype's sources' abilities") {
                slot("subtype", Primitives.subtype)
                build { it.value("subtype") }
                canonical = false
            }
        ),
        alternate(
            phrase<Subtype>("an ability of {subtype}", name = "one of a subtype's abilities") {
                slot("subtype", articled(Primitives.subtype, "a subtype"))
                build { it.value("subtype") }
                canonical = false
            }
        ),
        alternate(
            phrase<Subtype>("an ability of {subtype} source", name = "one of a subtype's sources' abilities") {
                slot("subtype", articled(Primitives.subtype, "a subtype"))
                build { it.value("subtype") }
                canonical = false
            }
        ),
    )


    /**
     * "cast Dragon spells or activate abilities of Dragons" — the subtype's spells-*and*-abilities
     * atom, which is [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] with a printed subtype rather
     * than the chosen one.
     *
     * One subtype only: the SDK atom holds one, and the corpus's two-subtype instance ("a Time Lord
     * or Alien spell or activate an ability of a Time Lord or Alien") therefore declines rather than
     * being read as the first of them.
     *
     * `creatureOnly` is deliberately unreachable from here. It exists for the chosen-type cards,
     * whose printed wording is "a **creature** spell of the chosen type", and no hand-written card
     * pairs it with a named subtype — so reading "cast Dragon creature spells" onto it would be a
     * model the corpus does not vouch for.
     */
    private val castOrActivateSubtype: Phrase<ManaRestriction> =
        phrase("cast {spells} or activate {abilities}", name = "cast or activate one subtype") {
            slot("spells", subtypeSpells)
            slot("abilities", subtypeAbilities)
            build { bindings ->
                val spells = bindings.value<Subtype>("spells")
                if (spells != bindings.value<Subtype>("abilities")) return@build null
                ManaRestriction.SubtypeSpellsOrAbilitiesOnly(spells.value)
            }
            match { value ->
                val restriction = value as? ManaRestriction.SubtypeSpellsOrAbilitiesOnly ?: return@match null
                if (restriction.creatureOnly) return@match null
                val subtype = Subtype(restriction.subtype)
                bind("spells" to subtype, "abilities" to subtype)
            }
            // Oracle joins the two halves with "and" about as often as with "or", and repeats the
            // "to" about as often as it elides it. Neither word is in the model — the atom says
            // *both*, whichever way the sentence said so.
            alsoSpelled("cast {spells} and activate {abilities}", "cast and activate one subtype")
            alsoSpelled("cast {spells} or to activate {abilities}", "cast or else activate one subtype")
            alsoSpelled("cast {spells} and to activate {abilities}", "cast and also activate one subtype")
        }

    /**
     * "cast spells with mana value 4 or greater" — and the two clauses the corpus prints beside it.
     *
     * A generator over the SDK's two booleans rather than four hand-written rules, because the
     * printed noun ("spells" against "creature spells") is what `creatureOnly` *is*, and it appears
     * twice in the longer form. Enumerating the product keeps the noun spelled once per rule.
     *
     * Only "or greater" — [ManaRestriction.SpellsWithManaValueAtLeast] is a floor and holds no
     * comparison, so the "or less" half of [ManaValues]' table has no field to land in here.
     */
    private val manaValueFloor: List<Phrase<ManaRestriction>> =
        listOf(false, true).flatMap { creatureOnly ->
            listOf(false, true).map { orX -> manaValueRow(creatureOnly, orX) }
        }

    private fun manaValueRow(creatureOnly: Boolean, orX: Boolean): Phrase<ManaRestriction> {
        val noun = if (creatureOnly) "creature spells" else "spells"
        val template = buildString {
            append("cast $noun with mana value {n} or greater")
            if (orX) append(" or $noun with {X} in their mana costs")
        }
        fun restrictionFor(floor: Int) = ManaRestriction.SpellsWithManaValueAtLeast(
            minManaValue = floor,
            orXInCost = orX,
            creatureOnly = creatureOnly,
        )
        return phrase(template, name = "cast spells above a mana value") {
            slot("n", Primitives.cardinal)
            build { restrictionFor(it.int("n")) }
            match { value ->
                val restriction = value as? ManaRestriction.SpellsWithManaValueAtLeast ?: return@match null
                if (restrictionFor(restriction.minManaValue) != restriction) return@match null
                bind("n" to restriction.minManaValue)
            }
        }
    }

    /** Everything one "to …" slot can hold. */
    private val context: Phrase<ManaRestriction> = oneOf(
        "a way to spend mana",
        atoms + manaValueFloor + listOf(
            castTypeSpells,
            activateTypeAbilities,
            castOrActivateType,
            castSubtypeSpells,
            castOrActivateSubtype,
        ),
    )

    // -------------------------------------------------------------------------------------------
    // The join
    // -------------------------------------------------------------------------------------------

    /**
     * "cast an enchantment spell, unlock a door, or turn a permanent face up" — several contexts,
     * which [ManaRestriction.AnyOf] holds as an ordered list.
     *
     * The shape is [subtypeRun]'s and [Mana.alternatives]', over a different vocabulary: a pair takes
     * exactly two and a series at least three, so the count decides which prints. English repeats the
     * "to" about half the time ("or **to** activate an ability") and that word is not in the model,
     * so it is an alternate.
     */
    private val joined: Phrase<ManaRestriction> = run {
        fun anyOf(parts: List<ManaRestriction>): ManaRestriction? {
            if (parts.size < 2) return null
            // No nesting: a printed sentence joins contexts, never joins of contexts.
            if (parts.any { it is ManaRestriction.AnyOf }) return null
            // The declared hole. "cast artifact spells or activate abilities of artifacts" is one
            // atom with both booleans set, so a join of its two halves would print those same words.
            if (foldsIntoOneRow(parts)) return null
            return ManaRestriction.AnyOf(parts)
        }

        fun partsOf(value: ManaRestriction, min: Int): List<ManaRestriction>? {
            val parts = (value as? ManaRestriction.AnyOf)?.restrictions ?: return null
            if (parts.size < min || anyOf(parts) != value) return null
            return parts
        }

        fun pair(join: String, canonicalJoin: Boolean): Phrase<ManaRestriction> {
            val rule = phrase<ManaRestriction>("{first}$join{second}", name = "two ways to spend mana") {
                slot("first", context)
                slot("second", context)
                build { anyOf(listOf(it.value("first"), it.value("second"))) }
                match { value ->
                    val parts = partsOf(value, min = 2)?.takeIf { it.size == 2 } ?: return@match null
                    bind("first" to parts[0], "second" to parts[1])
                }
                canonical = canonicalJoin
            }
            return if (canonicalJoin) rule else alternate(rule)
        }

        fun series(join: String, canonicalJoin: Boolean): Phrase<ManaRestriction> {
            val rule = phrase<ManaRestriction>("{most},$join{last}", name = "three or more ways to spend mana") {
                slot("most", separated("ways to spend mana", context, ", ", min = 2))
                slot("last", context)
                build { anyOf(it.value<List<ManaRestriction>>("most") + it.value<ManaRestriction>("last")) }
                match { value ->
                    val parts = partsOf(value, min = 3) ?: return@match null
                    bind("most" to parts.dropLast(1), "last" to parts.last())
                }
                canonical = canonicalJoin
            }
            return if (canonicalJoin) rule else alternate(rule)
        }

        oneOf(
            "several ways to spend mana",
            pair(" or ", canonicalJoin = true),
            pair(" or to ", canonicalJoin = false),
            pair(" and ", canonicalJoin = false),
            series(" or ", canonicalJoin = true),
            series(" or to ", canonicalJoin = false),
            series(" and ", canonicalJoin = false),
        )
    }

    /**
     * True when [parts] is the two halves of one [castOrActivateType] row.
     *
     * **[ManaRestriction.CreatureSpellsOnly] counts as the spells half**, and having to say so is the
     * declared empty cell being consistent: the atom stands in for the card-type row's creature cell
     * everywhere, so it stands in for it inside the join too. Gwenna, Eyes of Gaea is the card that
     * proved it — "cast creature spells or activate abilities of creature sources" read both as the
     * combined row and as `AnyOf(CreatureSpellsOnly, abilities of creatures)`, which is one text with
     * two models, the one thing the touchstone counts as a failure.
     */
    private fun foldsIntoOneRow(parts: List<ManaRestriction>): Boolean {
        if (parts.size != 2) return false
        val type = spellsHalf(parts[0]) ?: return false
        return abilitiesHalf(parts[1]) == type
    }

    /** The card type a restriction names, when it is that type's spells and nothing else. */
    private fun spellsHalf(restriction: ManaRestriction): CardType? = when (restriction) {
        is ManaRestriction.CreatureSpellsOnly -> CardType.CREATURE
        is ManaRestriction.CardTypeSpellsOrAbilitiesOnly ->
            restriction.cardType.takeIf { !restriction.negated && restriction.allowSpells && !restriction.allowAbilities }
        else -> null
    }

    /** …and when it is that type's abilities and nothing else. */
    private fun abilitiesHalf(restriction: ManaRestriction): CardType? =
        (restriction as? ManaRestriction.CardTypeSpellsOrAbilitiesOnly)
            ?.cardType
            ?.takeIf { !restriction.negated && !restriction.allowSpells && restriction.allowAbilities }

    // -------------------------------------------------------------------------------------------
    // The sentence
    // -------------------------------------------------------------------------------------------

    /** One context, or several joined. */
    private val contexts: Phrase<ManaRestriction> = oneOf("what this mana may be spent on", context, joined)

    /**
     * "Spend this mana only to cast creature spells." — and the one sentence that says it the other
     * way round.
     *
     * The negative is a **different sentence, not a negated spelling of this one**, and the SDK is
     * emphatic about why: every "spend only to …" clause is a whitelist that blocks each spend it
     * does not name, while "this mana can't be spent to cast a nonartifact spell" blocks exactly one
     * thing and leaves activating an ability, paying a ward cost and turning a permanent face up
     * legal. Both wordings exist over the same card types — "Spend this mana only to cast a
     * noncreature spell" (The Emperor of Palamecia) against "This mana can't be spent to cast a
     * nonartifact spell" (every Powerstone) — so the two sentences carry a distinction the words are
     * the only record of.
     *
     * Periodless, like every clause in [Steps]: the full stop belongs to the sentence [Mana.restricted]
     * puts it in.
     */
    val restriction: Phrase<ManaRestriction> = oneOf(
        "a mana spend restriction",
        phrase("spend this mana only to {contexts}", name = "a mana spend restriction") {
            slot("contexts", contexts)
            build { it.value("contexts") }
            match { value ->
                if (value is ManaRestriction.CannotCastSpellsOtherThan) return@match null
                bind("contexts" to value)
            }
        },
        phrase("this mana can't be spent to cast {spells}", name = "a mana spend prohibition") {
            slot("spells", typeSpells)
            build { bindings -> prohibition(bindings.value("spells")) }
            match { value ->
                val types = (value as? ManaRestriction.CannotCastSpellsOtherThan)?.cardTypes ?: return@match null
                val word = TypeWord(types.singleOrNull() ?: return@match null, negated = true)
                if (prohibition(word) != value) return@match null
                bind("spells" to word)
            }
        },
    )

    /**
     * "This mana can't be spent to cast a non**artifact** spell" — the negation is in the word, and
     * the model puts the *bare* type in the set, so the sentence only reads the negated spelling.
     */
    private fun prohibition(word: TypeWord): ManaRestriction? =
        if (!word.negated) null else ManaRestriction.CannotCastSpellsOtherThan(setOf(word.type))
}
