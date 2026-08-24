package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * The noun phrase a spell acts on — "creature", "artifact or enchantment", "black creatures you
 * control", "creatures with power 2 or greater".
 *
 * The target of these rules is `mtg-sdk`'s [GameObjectFilter], which is a *bag of predicates*, and
 * that shape decides how the grammar has to be written. A predicate bag has no canonical spelling:
 * `Creature.youControl()` could in principle be printed by a rule for the type and a rule for the
 * controller in either order, and two rules that can each print part of one value are how a
 * bidirectional grammar goes underdetermined. So the rules here are **layered, not composed**: one
 * alternation spells the whole type phrase, and each layer around it owns exactly one field, strips
 * precisely that field, and delegates the rest inwards. Each layer also carries the layer below as a
 * pass-through alternative, which is what lets "black creatures you control" exist without a rule
 * that knows about both colour and control.
 *
 * ### The layers, innermost first
 *
 * | Layer | Owns | Surface |
 * |---|---|---|
 * | [typeNoun] | the whole predicate set of a named type | "creature", "nonbasic land", "Mountain" |
 * | [coloured] | the last [CardPredicate] when it is a colour one | "white creature", "nonblack creature" |
 * | [cardNoun] | nothing — the head noun the position prints | "creature **card**", "cards" |
 * | [controlledBy] | `controllerPredicate` | "creature you control" |
 * | the quality clauses | the last [CardPredicate] when it is a keyword, power or mana-value one | "creature you control with flying" |
 *
 * [cardNoun] is the one layer that owns no field, and its place in the list is the point: the layers
 * above it are modifiers English writes in *front* of the head noun and the layers below it are
 * clauses English writes *behind* it. A position that prints a head noun therefore splits the
 * cascade in exactly one place, and every suffix layer reaches card position without being told.
 * The mana-value suffix is a product rather than a single rule and lives in [ManaValues].
 *
 * The two *clause* layers are ordered by English alone. `controllerPredicate` is not a member of
 * `cardPredicates`, so the controller clause and the quality clauses commute in the model; Oracle
 * prints the controller first ("creatures **you control** with power 2 or greater") 158 times to 5,
 * so that is the canonical order and the reverse is an [alternate].
 *
 * The fluent builders these rules go through (`withColor`, `withKeyword`, `powerAtLeast`, …) all
 * **append** to `cardPredicates`, so the list is a stack and the outermost layer owns its top. That
 * is what makes "strip precisely the field I own" well-defined for a list-valued slot, and it is the
 * same order English uses: "white creature with flying" is built colour-then-keyword and printed
 * colour-then-keyword.
 *
 * ### Why the type list is enumerated
 *
 * "artifact or enchantment" is `Or([IsArtifact, IsEnchantment])` — an ordered list — while "artifact
 * creature" is two separate predicates. English does not distinguish them by shape, only by the
 * words, so deriving the surface from the predicates would need a theory of Magic's templating that
 * the SDK does not carry. Enumerating the printed forms keeps every rule provably invertible, and a
 * form nobody wrote down declines rather than being approximated — which is the point.
 *
 * ### Number is an axis, not a second vocabulary
 *
 * "Destroy target **creature**" and "Destroy all white **creatures**" name the same filters through
 * the same layers; only the type noun inflects. So the whole cascade is a function of grammatical
 * number and is instantiated twice — [filter] for the singular and [plural] for the plural — rather
 * than written twice. Everything outside the type noun ("you control", "with flying", "nonblack") is
 * number-invariant in Oracle-ese and is shared verbatim between the two.
 *
 * The plural of a *compound* type phrase is deliberately absent rather than derived: "artifact or
 * enchantment" pluralizes as "artifacts **and** enchantments", so the conjunction changes with the
 * number and nothing in the singular says so. Those rows carry no plural and decline in group
 * position, which names the gap instead of inventing a spelling.
 */
object Filters {

    /**
     * A type phrase and its plural, where English has one for it.
     *
     * Kept as a data row rather than as two lists so the two numbers of one type cannot drift apart,
     * and so a new type is one line in one place.
     */
    private data class TypeNoun(
        val singular: String,
        val plural: String?,
        val filter: GameObjectFilter,
    )

    private val TYPES: List<TypeNoun> = listOf(
        TypeNoun("creature", "creatures", GameObjectFilter.Creature),
        TypeNoun("artifact", "artifacts", GameObjectFilter.Artifact),
        TypeNoun("enchantment", "enchantments", GameObjectFilter.Enchantment),
        TypeNoun("land", "lands", GameObjectFilter.Land),
        TypeNoun("planeswalker", "planeswalkers", GameObjectFilter.Planeswalker),
        TypeNoun("permanent", "permanents", GameObjectFilter.Permanent),
        TypeNoun("nonland permanent", "nonland permanents", GameObjectFilter.NonlandPermanent),
        TypeNoun("noncreature permanent", "noncreature permanents", GameObjectFilter.NoncreaturePermanent),
        TypeNoun("basic land", "basic lands", GameObjectFilter.BasicLand),
        // The two card types that are never permanents. They appear in the same slot as the rest —
        // "target **sorcery** card in your graveyard", "search your library for an **instant**
        // card" — which is why they are rows here rather than a vocabulary of their own.
        TypeNoun("instant", "instants", GameObjectFilter.Instant),
        TypeNoun("sorcery", "sorceries", GameObjectFilter.Sorcery),
        TypeNoun("nonbasic land", "nonbasic lands", GameObjectFilter.NonbasicLand),
        // A bare quality with no noun of its own: "Noncreature spells cost {1} more to cast." puts
        // it in front of a word ("spells") that is not a permanent type, so the filter is the
        // adjective alone and does not inflect.
        TypeNoun("noncreature", "noncreature", GameObjectFilter.Noncreature),
        TypeNoun("artifact creature", "artifact creatures", GameObjectFilter.ArtifactCreature),
        TypeNoun("creature or planeswalker", null, GameObjectFilter.CreatureOrPlaneswalker),
        TypeNoun("creature or enchantment", null, GameObjectFilter.CreatureOrEnchantment),
        // The plural swaps the conjunction: "creature or land" becomes "creatures **and** lands",
        // which is why a plural is a column in this table rather than a suffix rule.
        TypeNoun("creature or land", "creatures and lands", GameObjectFilter.CreatureOrLand),
        TypeNoun("artifact or enchantment", null, GameObjectFilter.ArtifactOrEnchantment),
        TypeNoun("artifact or land", null, GameObjectFilter.ArtifactOrLand),
        TypeNoun("attacking creature", "attacking creatures", GameObjectFilter.Creature.attacking()),
        // A `StatePredicate.Or` of the two, which is one printed phrase and one value — the same
        // shape as the "artifact or enchantment" row above, and enumerated for the same reason.
        TypeNoun(
            "attacking or blocking creature",
            "attacking or blocking creatures",
            GameObjectFilter.Creature.attackingOrBlocking(),
        ),
        TypeNoun("face-down creature", "face-down creatures", GameObjectFilter.Creature.faceDown()),
        TypeNoun("blocking creature", "blocking creatures", GameObjectFilter.Creature.blocking()),
        TypeNoun("tapped creature", "tapped creatures", GameObjectFilter.Creature.tapped()),
        TypeNoun("untapped creature", "untapped creatures", GameObjectFilter.Creature.untapped()),
    )

    /**
     * The basic land types, which stand in a type noun's slot with no "land" after them — "for each
     * **Mountain** target opponent controls".
     *
     * Generated from the SDK's own list rather than enumerated, because the CR defines the five as a
     * closed set and the SDK publishes it; and built as `Land.withSubtype(…)` because that is the
     * shape every hand-written card uses for them, the basic land types being land types.
     *
     * Only the *basic* land types are here. A capitalized word is a subtype of some kind, but which
     * card type it implies is not recoverable from the word — "Goblin" is a creature, "Equipment" an
     * artifact, "Gate" a land — and the SDK publishes no list to rank against for the latter two.
     * Guessing would be the reversible-but-wrong class again, so the rest decline.
     */
    private val BASIC_LAND_TYPES: List<TypeNoun> = Subtype.ALL_BASIC_LAND_TYPES.map { type ->
        // "Plains" is its own plural — the one invariant among the five, and the same trap
        // `Primitives.pluralSubtype` exists for. Appending an "s" would spell a type that is not one.
        TypeNoun(type, if (type.endsWith("s")) type else "${type}s", GameObjectFilter.Land.withSubtype(Subtype(type)))
    }

    private fun typeNoun(plural: Boolean): Phrase<GameObjectFilter> = oneOf(
        if (plural) "a permanent type (plural)" else "a permanent type",
        (TYPES + BASIC_LAND_TYPES).mapNotNull { noun ->
            (if (plural) noun.plural else noun.singular)?.let { constant(it, noun.filter) }
        },
    )

    // ---------------------------------------------------------------------------------------
    // Subtypes — the tribal adjective, and the bare noun that stands for it
    // ---------------------------------------------------------------------------------------

    /**
     * "Sliver creature", "Goblin permanents" — a subtype in front of a type noun.
     *
     * The layer sits *inside* [colour] and the rest rather than outside them, because that is the
     * order both English and the predicate stack use: "black Sliver creature" builds the subtype
     * first and the colour on top, so the colour layer owns the top of the stack and this one owns
     * what is under it. Putting it further out would print "Sliver black creature".
     *
     * The subtype leaf is **ungated**: the card type comes from the noun this modifies, so nothing
     * is being guessed and there is no candidate to rank — unlike the bare form below, where the
     * word alone has to imply "creature". [Primitives.pluralSubtype]'s ranking exists for the
     * de-pluralization, which this layer never performs.
     *
     * The adjective stays **singular in both numbers** — "Sliver creature" and "Sliver creatures" —
     * because only the head noun inflects in English. This layer therefore takes no number
     * parameter, which is why it sits inside the cascade rather than being instantiated twice.
     */
    private fun subtyped(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("{subtype} {type}", name = name) {
            slot("subtype", Primitives.subtype)
            slot("type", inner)
            build { it.value<GameObjectFilter>("type").withSubtype(it.value<Subtype>("subtype")) }
            match { filter ->
                filter.stripTop<CardPredicate.HasSubtype>()
                    ?.let { (predicate, rest) -> bind("subtype" to predicate.subtype, "type" to rest) }
            }
        }

    /**
     * "Bird and/or Cleric permanent" — two subtypes, either of which qualifies.
     *
     * One `Or` predicate rather than two, exactly as [anyColour] reads the colour disjunction, and
     * "and/or" is the only join spelled for the same reason: "Bird or Cleric permanent" would be a
     * second printed form for one value with nothing for the printer to choose.
     */
    private fun anySubtype(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("{first} and/or {second} {type}", name = name) {
            slot("first", Primitives.subtype)
            slot("second", Primitives.subtype)
            slot("type", inner)
            build {
                it.value<GameObjectFilter>("type")
                    .withAnySubtype(it.value<Subtype>("first").value, it.value<Subtype>("second").value)
            }
            match { filter ->
                val (predicate, rest) = filter.stripTop<CardPredicate.Or>() ?: return@match null
                val subtypes = predicate.predicates.map {
                    (it as? CardPredicate.HasSubtype)?.subtype ?: return@match null
                }
                if (subtypes.size != 2) return@match null
                bind("first" to subtypes[0], "second" to subtypes[1], "type" to rest)
            }
        }

    /**
     * "non-Zombie creature" — the subtype layer's negation, which Oracle hyphenates where it writes
     * the colour negation as one word ("nonblack creature"). Two printed conventions for two
     * predicates, so they are two rules rather than one shape over a prefix.
     */
    private fun notSubtyped(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("non-{subtype} {type}", name = name) {
            slot("subtype", Primitives.subtype)
            slot("type", inner)
            build { it.value<GameObjectFilter>("type").notSubtype(it.value<Subtype>("subtype")) }
            match { filter ->
                filter.stripTop<CardPredicate.NotSubtype>()
                    ?.let { (predicate, rest) -> bind("subtype" to predicate.subtype, "type" to rest) }
            }
        }

    /**
     * "Slivers", "a Goblin", "target Sliver" — the subtype standing alone.
     *
     * **It builds `Permanent`, not `Creature`, and that is the rules' reading rather than a
     * convenience.** A bare creature-type noun names every *permanent* with the subtype; the
     * adjectival "Sliver creature" is what narrows it to creatures. Zombie Master is the card that
     * proves the distinction is deliberate rather than stylistic: its first line says "Other Zombie
     * **creatures** have swampwalk" and its second says "Other **Zombies** have …", and the ability
     * that second line grants is spelled "Regenerate this **permanent**".
     *
     * It denotes what "Sliver permanent" denotes, so registering it as a canonical rule would leave
     * printing underdetermined between two real English spellings of one value. It is therefore an
     * [alternate]: cards printing the bare noun read correctly and print back as the adjective form,
     * which is a `VARIANT` — the reading was right and only the spelling moved.
     *
     * Unlike [subtyped] this leaf **is** ranked against the SDK's creature-type list, because here
     * the word alone has to imply a type. A guess about a word the SDK does not name would be the
     * reversible-but-wrong class: "target Scion" would read as a creature type nothing in Magic has.
     *
     * ### History, because the measurement is the interesting part
     *
     * This read `Creature` for a long time, and the differential is what closed it. Flipping the
     * line alone took the count from 2 divergences to **104** — 103 hand-written cards spelled the
     * bare noun as a creature filter, and for nearly all of them the two select the same permanents,
     * which is exactly why it survived review for so long. The flip was therefore reverted twice
     * before it landed *with* its card migration, in that order: the cards first, then this line,
     * with the differential as the check at every step. Flipping first would have left 103
     * unexplained divergences, which is the gate lying about which side is wrong.
     *
     * The migration also named three gaps in the SDK's own vocabulary, each now a facade beside its
     * creature-scoped twin: `DynamicAmounts.permanentsWithSubtype`,
     * `Conditions.ControlPermanentOfType`, and `TargetFilter.PermanentInYourGraveyard`. That a
     * bare-noun reading had no way to be *written* is the finding this module exists to produce.
     */
    private fun bareSubtype(plural: Boolean, name: String): Phrase<GameObjectFilter> =
        alternate(
            phrase<GameObjectFilter>("{subtype}", name = name) {
                slot("subtype", if (plural) Primitives.pluralCreatureSubtype else Primitives.creatureSubtype)
                build { GameObjectFilter.Permanent.withSubtype(it.value<Subtype>("subtype")) }
                canonical = false
            }
        )

    /**
     * The two rules that exist only in **spell position** — the adjective in front of the word
     * "spells", where the sentence supplies the head noun.
     *
     * That position is genuinely a different one, not a spelling of the permanent noun phrase, and
     * both differences follow from one fact: **a spell on the stack is not a permanent.**
     *
     * - A bare subtype means `Any.withSubtype`, not [bareSubtype]'s `Permanent.withSubtype`. On the
     *   battlefield "Zombies" names Zombie *permanents* — the reading the differential took 104
     *   cards to settle; on the stack "Zombie spells" names cards, and a `IsPermanent` predicate
     *   there would be a narrower filter than the card says. So this is **canonical** where
     *   [bareSubtype] is an `alternate`: it is the only spelling of its value in this position,
     *   rather than the second spelling of a value the adjective form already prints.
     * - A bare colour has no noun to attach to. [colour] is a layer *around* a type phrase, and
     *   "Red spells you cast cost {1} less to cast." has no type phrase in it at all.
     *
     * Instantiating the cascade for the position is the same move [SelfSteps.retargetable] makes for
     * the anaphors, and for the same reason: the distinction exists in the sentence and nowhere in
     * the model, so no remap on the finished filter could recover it. Registering either rule in the
     * shared cascade instead would give one value two printed forms in permanent position.
     */
    private fun spellSubtype(suffix: String): Phrase<GameObjectFilter> =
        phrase("{subtype}", name = "a spell's subtype$suffix") {
            slot("subtype", Primitives.subtype)
            build { GameObjectFilter.Any.withSubtype(it.value<Subtype>("subtype")) }
            match { filter ->
                val subtype = (filter.cardPredicates.singleOrNull() as? CardPredicate.HasSubtype)?.subtype
                    ?: return@match null
                if (filter != GameObjectFilter.Any.withSubtype(subtype)) return@match null
                bind("subtype" to subtype)
            }
        }

    /** "Red spells you cast …" — see [spellSubtype]; a colour with no noun under it. */
    private fun spellColour(suffix: String): Phrase<GameObjectFilter> =
        phrase("{colour}", name = "a spell's colour$suffix") {
            slot("colour", Primitives.color)
            build { GameObjectFilter.Any.withColor(it.value<Color>("colour")) }
            match { filter ->
                val colour = (filter.cardPredicates.singleOrNull() as? CardPredicate.HasColor)?.color
                    ?: return@match null
                if (filter != GameObjectFilter.Any.withColor(colour)) return@match null
                bind("colour" to colour)
            }
        }

    // ---------------------------------------------------------------------------------------
    // The layers
    // ---------------------------------------------------------------------------------------

    /**
     * Strip the top of the predicate stack when it is the kind this layer owns.
     *
     * The pair is (the predicate, the filter without it) — the two halves a layer's `match` needs,
     * and the only place a layer is allowed to reach into `cardPredicates`.
     */
    private inline fun <reified P : CardPredicate> GameObjectFilter.stripTop(): Pair<P, GameObjectFilter>? {
        val top = cardPredicates.lastOrNull() as? P ?: return null
        return top to copy(cardPredicates = cardPredicates.dropLast(1))
    }

    /** "white creature" — one colour, as an adjective in front of the type noun. */
    private fun colour(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("{color} {type}", name = name) {
            slot("color", Primitives.color)
            slot("type", inner)
            build { it.value<GameObjectFilter>("type").withColor(it.value("color")) }
            match { filter ->
                filter.stripTop<CardPredicate.HasColor>()
                    ?.let { (predicate, rest) -> bind("color" to predicate.color, "type" to rest) }
            }
        }

    /** "nonblack creature" — the negated colour, which Oracle writes as one word. */
    private fun notColour(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("non{color} {type}", name = name) {
            slot("color", Primitives.color)
            slot("type", inner)
            build { it.value<GameObjectFilter>("type").notColor(it.value("color")) }
            match { filter ->
                filter.stripTop<CardPredicate.NotColor>()
                    ?.let { (predicate, rest) -> bind("color" to predicate.color, "type" to rest) }
            }
        }

    /**
     * "black and/or red creatures" — the disjunctive colour, which is one `Or` predicate rather
     * than two.
     *
     * "and/or" is the only join spelled here. "black or red creature" is a different printed form
     * for the same value and would be genuine ambiguity, so it declines; which of the two a card
     * prints is a templating choice the model has nowhere to keep.
     */
    private fun anyColour(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("{first} and/or {second} {type}", name = name) {
            slot("first", Primitives.color)
            slot("second", Primitives.color)
            slot("type", inner)
            build {
                it.value<GameObjectFilter>("type")
                    .withAnyColor(it.value("first"), it.value("second"))
            }
            match { filter ->
                val (predicate, rest) = filter.stripTop<CardPredicate.Or>() ?: return@match null
                val colours = predicate.predicates.map { (it as? CardPredicate.HasColor)?.color ?: return@match null }
                if (colours.size != 2) return@match null
                bind("first" to colours[0], "second" to colours[1], "type" to rest)
            }
        }

    /** "creatures with flying" — a keyword the members must have. */
    private fun withKeyword(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("{type} with {kw}", name = name) {
            slot("type", inner)
            slot("kw", Keywords.keyword)
            build { it.value<GameObjectFilter>("type").withKeyword(it.value<Keyword>("kw")) }
            match { filter ->
                filter.stripTop<CardPredicate.HasKeyword>()
                    ?.let { (predicate, rest) -> bind("type" to rest, "kw" to predicate.keyword) }
            }
        }

    /** "creatures without flying" — the keyword layer's negation. */
    private fun withoutKeyword(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("{type} without {kw}", name = name) {
            slot("type", inner)
            slot("kw", Keywords.keyword)
            build { it.value<GameObjectFilter>("type").withoutKeyword(it.value<Keyword>("kw")) }
            match { filter ->
                filter.stripTop<CardPredicate.NotKeyword>()
                    ?.let { (predicate, rest) -> bind("type" to rest, "kw" to predicate.keyword) }
            }
        }

    /** "creatures with power 2 or greater". */
    private fun withPowerAtLeast(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("{type} with power {n} or greater", name = name) {
            slot("type", inner)
            slot("n", Primitives.cardinal)
            build { it.value<GameObjectFilter>("type").powerAtLeast(it.int("n")) }
            match { filter ->
                filter.stripTop<CardPredicate.PowerAtLeast>()
                    ?.let { (predicate, rest) -> bind("type" to rest, "n" to predicate.min) }
            }
        }

    /** "creatures with power 2 or less". */
    private fun withPowerAtMost(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("{type} with power {n} or less", name = name) {
            slot("type", inner)
            slot("n", Primitives.cardinal)
            build { it.value<GameObjectFilter>("type").powerAtMost(it.int("n")) }
            match { filter ->
                filter.stripTop<CardPredicate.PowerAtMost>()
                    ?.let { (predicate, rest) -> bind("type" to rest, "n" to predicate.max) }
            }
        }

    /**
     * "nontoken Elf" — the token/nontoken layer.
     *
     * A prefix rather than a suffix, and a [CardPredicate] like the colour and keyword layers, so it
     * owns the top of the stack the same way they do. Oracle writes it as one word, which is why it
     * is a layer of its own and not a row in the type list: the noun it qualifies is still whatever
     * follows, subtype and all.
     */
    private fun nontoken(inner: Phrase<GameObjectFilter>, name: String): Phrase<GameObjectFilter> =
        phrase("nontoken {type}", name = name) {
            slot("type", inner)
            build { it.value<GameObjectFilter>("type").nontoken() }
            match { filter ->
                filter.stripTop<CardPredicate.IsNontoken>()?.let { (_, rest) -> bind("type" to rest) }
            }
        }

    /**
     * The word **"card"**, and the type phrase that qualifies it — "card", "creature card",
     * "green creature cards".
     *
     * A layer, and the layer that made this cascade reusable outside the battlefield. Every
     * card-position rule used to spell the noun in its *own* template — `"return target {filter} card
     * from your graveyard to your hand"` — which put the head noun in the **sentence** rather than in
     * the noun phrase. That is what froze the phrase at its type: a suffix layer attaches behind the
     * head noun, and there was no head noun here for it to attach behind. "Creature card with mana
     * value 3 or less" was therefore unreachable while "creature with mana value 3 or less" had been
     * readable since the counting band — one printed word apart, and a family near the top of the
     * tail ranking.
     *
     * The type phrase in front stays **singular in both numbers** ("creature cards", never "creatures
     * cards") because Oracle inflects only the head noun. So the card positions instantiate the
     * cascade with a singular type noun and pluralize *here*, which is why [nounPhrase] takes the
     * two numbers as separate arguments rather than threading one flag through.
     *
     * The unmodified row is a row rather than an omissible slot: `GameObjectFilter.Any` is exactly
     * what the bare word means, and the qualified row refuses it. One printed form per model, in
     * both directions — the same split this vocabulary has had since it was `pluralCards`.
     */
    private fun cardNoun(inner: Phrase<GameObjectFilter>, plural: Boolean, suffix: String): Phrase<GameObjectFilter> {
        val noun = if (plural) "cards" else "card"
        return oneOf(
            "a card$suffix",
            constant(noun, GameObjectFilter.Any),
            phrase("{type} $noun", name = "a card of a type$suffix") {
                slot("type", inner)
                build { it.value<GameObjectFilter>("type").takeIf { f -> f != GameObjectFilter.Any } }
                match { f -> if (f == GameObjectFilter.Any) null else bind("type" to f) }
            },
        )
    }

    /**
     * The controller clause, which is a suffix in English and a single field in the model — so it is
     * one rule per printed form, each stripping [GameObjectFilter.controllerPredicate] and handing
     * the rest back inwards.
     */
    private fun controlledBy(
        inner: Phrase<GameObjectFilter>,
        surface: String,
        predicate: ControllerPredicate,
        name: String,
    ): Phrase<GameObjectFilter> =
        phrase("{type} $surface", name = name) {
            slot("type", inner)
            build { it.value<GameObjectFilter>("type").copy(controllerPredicate = predicate) }
            match { filter ->
                if (filter.controllerPredicate != predicate) {
                    null
                } else {
                    bind("type" to filter.copy(controllerPredicate = null))
                }
            }
        }

    /**
     * The whole cascade, for one grammatical number.
     *
     * Each level carries the level below as its first alternative, so a filter that uses none of a
     * layer's vocabulary is printed by the layer that owns what it *does* use. Printing stays
     * determined by the model rather than by alternation order, because every rule's `match` tests
     * the exact field it owns and the type nouns are exact values.
     */
    private fun nounPhrase(
        plural: Boolean,
        spellPosition: Boolean = false,
        controlled: Boolean = true,
        card: Boolean = false,
    ): Phrase<GameObjectFilter> {
        val suffix = when {
            card && plural -> " (cards)"
            card -> " (card)"
            plural && !controlled -> " (plural, uncontrolled)"
            plural -> " (plural)"
            spellPosition -> " (of a spell)"
            else -> ""
        }
        // In card position only the head noun inflects, so the type phrase under it is always
        // singular; see [cardNoun].
        val named = typeNoun(plural && !card)
        val types = oneOf(
            "a permanent type or subtype$suffix",
            listOf(
                named,
                subtyped(named, "a permanent of a subtype$suffix"),
                notSubtyped(named, "a permanent of another subtype$suffix"),
                anySubtype(named, "a permanent of either subtype$suffix"),
                if (spellPosition) {
                    spellSubtype(suffix)
                } else {
                    bareSubtype(plural && !card, "a subtype standing alone$suffix")
                },
            ) + if (spellPosition) listOf(spellColour(suffix)) else emptyList(),
        )
        val counted = oneOf(
            "a permanent or token$suffix",
            types,
            nontoken(types, "a nontoken permanent$suffix"),
        )
        val coloured = oneOf(
            "a coloured permanent$suffix",
            counted,
            colour(counted, "a coloured permanent$suffix"),
            notColour(counted, "a permanent of another colour$suffix"),
            anyColour(counted, "a permanent of either colour$suffix"),
        )
        // The head noun, where the position prints one. Everything above this line is a modifier
        // English writes *in front* of the noun and everything below it is a clause English writes
        // *behind* the noun, which is the whole reason the insertion point is here and not at either
        // end of the cascade.
        val head = if (card) cardNoun(coloured, plural, suffix) else coloured
        // The quality clauses, over whatever noun phrase is handed in, and **without** the bare
        // pass-through. Keeping the pass-through out is what lets the minority word order below be an
        // alternate rather than a second reading of every unqualified noun.
        fun qualities(inner: Phrase<GameObjectFilter>, label: String) = listOf(
            withKeyword(inner, "a permanent with a keyword$label"),
            withoutKeyword(inner, "a permanent without a keyword$label"),
            withPowerAtLeast(inner, "a permanent with power at least$label"),
            withPowerAtMost(inner, "a permanent with power at most$label"),
            ManaValues.layer(inner, label),
        )

        fun byController(inner: Phrase<GameObjectFilter>, label: String) = listOf(
            controlledBy(inner, "you control", ControllerPredicate.ControlledByYou, "a permanent you control$label"),
            controlledBy(
                inner,
                "an opponent controls",
                ControllerPredicate.ControlledByOpponent,
                "a permanent an opponent controls$label",
            ),
        )

        if (!controlled) return oneOf("a qualified permanent$suffix", listOf(head) + qualities(head, suffix))

        // The controller clause sits **inside** the quality clauses, which is where Oracle puts it:
        // "creatures **you control** with power 2 or greater", "creature **an opponent controls** with
        // mana value 3 or less". Corpus-wide the controller-first order is printed 158 times against 5
        // for the other one, and this layer used to be the outermost — so the grammar read the five and
        // declined the hundred and fifty-eight. The predicate stack does not care either way:
        // `controllerPredicate` is its own field and not a member of `cardPredicates`, so the two
        // layers commute in the model and only the word order was ever at stake.
        val owned = oneOf("a permanent by controller$suffix", listOf(head) + byController(head, suffix))
        val canonical = oneOf("a qualified permanent$suffix", listOf(owned) + qualities(owned, suffix))
        // …and the fourteen cards that print the two clauses the other way round — "destroy target
        // creature with mana value 4 or less an opponent controls" (Darkstar Banisher, Silkwrap,
        // Arbor Colossus, …). Real English for the same value, so it parses and never prints: those
        // cards come back as a VARIANT rather than a decline. Its inner phrase is [qualities] without
        // the pass-through, so an unqualified "creature you control" has exactly one reading.
        val reversed = byController(
            oneOf("a permanent with a quality$suffix (unowned)", qualities(head, "$suffix (unowned)")),
            "$suffix (controller last)",
        ).map { alternate(it) }
        return oneOf("a permanent$suffix", listOf(canonical) + reversed)
    }

    /** A whole noun phrase in the singular — "creature", "nonblack attacking creature". */
    val filter: Phrase<GameObjectFilter> = nounPhrase(plural = false)

    /** …and in the plural — "creatures you control", "creatures with power 2 or greater". */
    val plural: Phrase<GameObjectFilter> = nounPhrase(plural = true)

    /**
     * …and in the plural with the controller clause **left to the sentence** — the subject of a
     * batch trigger, "one or more Treefolk you control attack".
     *
     * A third instantiation of the cascade rather than a use of [plural], for the same kind of
     * reason [spellQuality] is one: the position changes what an *absent* controller predicate
     * means. Every batched `EventPattern` in `mtg-sdk` folds a null `controllerPredicate` to "you
     * control" — `PermanentsEnteredEvent` and `CreaturesYouControlDiedEvent` say so in their KDoc
     * and their detectors do it, and `OneOrMoreDealCombatDamageToPlayerEvent` is scoped to the
     * observer before its filter is consulted at all. So the bare noun here does not mean "any
     * controller" the way it does on the battlefield, and a slot that printed "creatures you
     * control" from `ControlledByYou` would be a second spelling of what the event already says.
     * The controller clause is a word in each batch trigger's own surface, one row per scope, and
     * this vocabulary stops one layer below the one that owns the field.
     */
    val pluralSubject: Phrase<GameObjectFilter> = nounPhrase(plural = true, controlled = false)

    /**
     * "card", "creature card", "green creature card with mana value 3 or less" — the noun phrase in
     * **card position**: a library search, a graveyard return, a discard cost, a look-at-the-top.
     *
     * A fourth instantiation of the cascade, beside [plural], [pluralSubject] and [spellQuality],
     * and its reason is the plainest of the four: the printed head noun is different, so a suffix
     * clause attaches in a different place. See [cardNoun] for what that changes and why it could
     * not be a row inside [typeNoun].
     *
     * The controller layer is deliberately absent. An object in a graveyard or a library is *owned*,
     * not controlled, and every sentence here says which zone in its own words — "from **your**
     * graveyard", "in an opponent's graveyard" — so the field belongs to the sentence. The filters
     * these rules slot carry no `controllerPredicate` today for exactly that reason: `Graveyard`
     * strips it before binding the slot.
     */
    val cardNoun: Phrase<GameObjectFilter> = nounPhrase(plural = false, controlled = false, card = true)

    /** …and in the plural — "cards", "creature cards", the noun a zone-change batch names. */
    val pluralCards: Phrase<GameObjectFilter> = nounPhrase(plural = true, controlled = false, card = true)

    /**
     * …and in **spell position** — the adjective in "Zombie spells you cast", "Red spells",
     * "Noncreature spells". See [spellSubtype] for why this is an instantiation of the cascade
     * rather than a rule inside it.
     */
    val spellQuality: Phrase<GameObjectFilter> = nounPhrase(plural = false, spellPosition = true)

    /**
     * "a Forest", "an Island", "a creature" — a singular noun phrase with its indefinite article.
     *
     * The article is not in the model and never will be: English derives it from the *spelling* of
     * the word that follows, which the filter's printed form supplies. So both halves of both rules
     * derive it from the same function over [filter]'s own output, and the rule whose article
     * disagrees refuses in **both** directions — "an Forest" fails to parse for exactly the reason
     * it fails to print. That is what keeps one printed form per model with two alternatives in the
     * alternation, and it is why this is a pair of rules rather than a leaf: the noun inside is a
     * whole layered phrase, which a [com.wingedsheep.assay.syntax.token] cannot slot.
     */
    val indefinite: Phrase<GameObjectFilter> = oneOf(
        "a permanent with its article",
        article(filter, "a", "permanent"),
        article(filter, "an", "permanent"),
    )

    /**
     * "a creature card", "an artifact card with mana value 6 or greater" — [cardNoun] with its
     * article.
     *
     * The article derives from the printed form of [cardNoun] rather than of [filter], which is the
     * whole point of sharing the generator: the word the article agrees with is the first word of
     * the *noun phrase*, and in card position that is the type phrase inside "creature card" — while
     * for the bare row it is "card" itself. Deriving it from the type alone would have had nothing
     * to look at for "**a** card".
     */
    val indefiniteCard: Phrase<GameObjectFilter> = oneOf(
        "a card with its article",
        article(cardNoun, "a", "card"),
        article(cardNoun, "an", "card"),
    )

    /**
     * "Sliver" as a bare quality of a *card* — the noun in "Sliver spells can't be countered."
     *
     * Not a member of the cascade, and not [bareSubtype] either: this one carries **no card type at
     * all**, because a spell on the stack is not a permanent and the sentence names the subtype
     * alone. `GameObjectFilter.Any.withSubtype(…)` is what the hand-written cards use for it, which
     * is the difference from the battlefield nouns above that all imply "creature".
     */
    val subtypeOnly: Phrase<GameObjectFilter> = phrase("{subtype}", name = "a subtype") {
        slot("subtype", Primitives.subtype)
        build { GameObjectFilter.Any.withSubtype(it.value<Subtype>("subtype")) }
        match { filter ->
            val subtype = (filter.cardPredicates.singleOrNull() as? CardPredicate.HasSubtype)?.subtype
                ?: return@match null
            if (filter != GameObjectFilter.Any.withSubtype(subtype)) return@match null
            bind("subtype" to subtype)
        }
    }

    private fun article(noun: Phrase<GameObjectFilter>, article: String, position: String): Phrase<GameObjectFilter> =
        phrase("$article {type}", name = "\"$article\" plus a $position") {
            slot("type", noun)
            build { it.value<GameObjectFilter>("type").takeIf { f -> articleFor(noun, f) == article } }
            match { f -> if (articleFor(noun, f) == article) bind("type" to f) else null }
        }

    /** The article [noun] would print [f] with, or null when it cannot print it at all. */
    private fun articleFor(noun: Phrase<GameObjectFilter>, f: GameObjectFilter): String? {
        val head = noun.unparse(f)?.firstOrNull()?.lowercaseChar() ?: return null
        return if (head in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
    }
}
