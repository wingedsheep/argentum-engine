package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Bindings
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.assay.syntax.separated
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.TurnTracker
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * "Create a 1/1 green Insect creature token." — the token clauses.
 *
 * One shape with five slots (the count, the stats, the colours, the creature types, the card types) plus an optional
 * granted run, and a *row* per printed variation, because English changes several words at once: the
 * article and the noun's number move with the count, and the keyword rider is a suffix the kernel's
 * fixed templates cannot make optional. Six rows out of two axes is what the axes cost; nothing
 * about the token's own description is written twice.
 *
 * ### The colour word is a run, not [Primitives.color]
 *
 * A token's colours are a `Set<Color>`, which [Primitives.color] cannot spell: "colorless" is the
 * empty set rather than a colour, and "1/1 blue and red Otter" is two. So the slot is an alternation
 * over the empty set, one colour, and [Keywords.keywordRun]'s shape over colours — four alternatives
 * taking disjoint set *sizes*, which is what leaves printing determined by the model.
 *
 * Printing a set needs an order the model does not carry, and Magic's is not arbitrary: Oracle text
 * lists colours in WUBRG order, and [Color]'s own declaration order is that order. So the printed run
 * is the set sorted by ordinal, and a card that happened to store its colours in another order still
 * compares equal — sets have no order to disagree about. A **pair** rotates that sequence rather than
 * starting at its head; see [pairOrder], which is where the corpus turned out to disagree with the
 * plain sort.
 *
 * ### `imageUri` is not in the text, and the differential already knows it
 *
 * `CreateTokenEffect` carries an `imageUri` that no printed word determines — it is art, chosen when
 * the card was authored. The rules here build without one, and `Folds.dropPresentation` drops the
 * field from both sides before comparing, alongside `descriptionOverride`, `message` and `prompt`:
 * a parser can never produce a URL, so a card that inlines one would otherwise diverge for ever over
 * its picture while agreeing about its token.
 *
 * ### "tapped" is an axis of the shape, not a rider
 *
 * "Create a **tapped** 2/2 black Zombie creature token." is one adjective in front of the stats and
 * `CreateTokenEffect.tapped` is one boolean, so it passes both halves of `AGENTS.md`'s
 * omissible-modifier test: Oracle prints the sentence without the word far more often than with it
 * (70 lines carry it), and the SDK has a distinct value for the version that does. It is therefore an
 * axis of [createToken] instantiated in both states rather than template text or a suffix — which is
 * also what keeps it multiplicative, since it crosses the count and the keyword run without being
 * told and "create **two** tapped 1/1 black Bat creature tokens **with flying**" is a cell of the
 * same product rather than a fourth rule.
 */
object Tokens {

    // ---------------------------------------------------------------------------------------
    // The colour run
    // ---------------------------------------------------------------------------------------

    /** WUBRG — [Color]'s declaration order, which is the order printed Oracle text uses. */
    private fun ordered(colours: Set<Color>): List<Color> = colours.sortedBy { it.ordinal }

    /**
     * …except for **two** colours, where the sequence is the same but the starting point is not.
     *
     * WUBRG is a cycle rather than a list, and a printed pair begins at whichever of its two colours
     * leaves the other within two steps *forward* around it. That produces "green and white" and
     * "black and green" — neither of which is WUBRG order — and it is not a house style: it is the
     * colour-pie adjacency Magic names its allied and enemy pairs by, so the allied pairs come out
     * one step apart (WU, UB, BR, RG, GW) and the enemy pairs two (WB, UR, BG, RW, GU). Exactly one
     * of the two orders can satisfy it, since the other is three or four steps, which is what makes
     * this a function of the set rather than a choice the printer makes.
     *
     * The corpus states it without exception: of 351 two-colour phrases in Oracle text, all ten
     * pairs appear and every one is spelled this way. A plain ordinal sort spells five of them
     * backwards — Exhibition Magician's "1/1 green and white Citizen" is the one that surfaced it,
     * on the day the modal band made a two-colour token line reachable.
     *
     * **Three or more is left at [ordered]**, and the reason is that the corpus does not yet settle
     * it: eleven phrases, and the two that name the same three colours disagree with each other
     * ("white, blue, and red" against "red, white, and blue"). Guessing a rotation from four
     * examples would be inventing a convention; the shards agree with WUBRG rotation and the wedges
     * are the open question, so it stays as it is until a line makes one reachable and says so.
     */
    private fun pairOrder(colours: Set<Color>): List<Color> {
        val (first, second) = ordered(colours)
        val forward = (second.ordinal - first.ordinal + Color.entries.size) % Color.entries.size
        return if (forward <= 2) listOf(first, second) else listOf(second, first)
    }

    private val colourPair: Phrase<Set<Color>> =
        phrase("{first} and {second}", name = "two colours") {
            slot("first", Primitives.color)
            slot("second", Primitives.color)
            build { setOf(it.value<Color>("first"), it.value<Color>("second")) }
            match { colours ->
                colours.takeIf { it.size == 2 }?.let {
                    val order = pairOrder(it)
                    bind("first" to order[0], "second" to order[1])
                }
            }
        }

    private val colourSeries: Phrase<Set<Color>> =
        phrase("{most}, and {last}", name = "three or more colours") {
            slot("most", separated("colours", Primitives.color, ", ", min = 2))
            slot("last", Primitives.color)
            build { (it.value<List<Color>>("most") + it.value<Color>("last")).toSet() }
            match { colours ->
                colours.takeIf { it.size >= 3 }?.let {
                    val order = ordered(it)
                    bind("most" to order.dropLast(1), "last" to order.last())
                }
            }
        }

    private val colours: Phrase<Set<Color>> = oneOf(
        "a token's colours",
        listOf(
            constant("colorless", emptySet()),
            phrase<Set<Color>>("{one}", name = "one colour") {
                slot("one", Primitives.color)
                build { setOf(it.value<Color>("one")) }
                match { it.singleOrNull()?.let { only -> bind("one" to only) } }
            },
            colourPair,
            colourSeries,
        ),
    )

    // ---------------------------------------------------------------------------------------
    // Created creature tokens
    // ---------------------------------------------------------------------------------------

    /**
     * A token's creature types — "Elf **Warrior**", "Kithkin **Soldier**", "Merfolk **Wizard**".
     *
     * A run rather than a slot, for the same reason [colours] is one: `CreateTokenEffect` holds a
     * `Set<String>`, and 476 cards print two words where 1,928 print one. A single-type slot could
     * only ever read the smaller half, and the two halves are not two constructs — the noun phrase
     * is one list whose length the card chooses, so it is [separated] with `min = 1` and the
     * one-word case stays the same parse it always was rather than becoming a second rule.
     *
     * ### Where the printed order comes from
     *
     * Colours are printed in WUBRG and [ordered] derives that from [Color]'s own declaration order,
     * so a `Set<Color>` prints deterministically without the model recording a sequence. **Creature
     * types have no such total order**: "Elf Warrior" is race-then-class, a convention of Magic's
     * style guide that the SDK publishes no data for, and inventing a race/class table here would be
     * exactly the "recovering information versus inventing it" line [Primitives.subtype] draws.
     *
     * So the order is taken from the set's own iteration, which is the one place the information
     * actually survives: `setOf("Elf", "Warrior")` is a `LinkedHashSet`, kotlinx decodes a JSON
     * array into one too, and this rule's own `build` inserts in printed order. Every path that
     * reaches [printedTypes] therefore carries the sequence the card was written with.
     *
     * What that costs is stated rather than hidden: two `CreateTokenEffect`s that are `==` — sets
     * have no order to disagree about — can print differently. Neither gate minds, and the
     * distinction is which one they ask. The touchstone starts from *text*, so it round-trips this
     * rule's own insertion order and is exact. The differential compares *models*, so a golden that
     * happens to spell `setOf("Warrior", "Elf")` still agrees. The one thing that would break is a
     * renderer printing a type line from a model nobody parsed, and that is Phase 4's problem to
     * solve with an ordered field, not this rule's to pre-empt with a guessed table.
     */
    private val typeRun: Phrase<List<Subtype>> =
        separated("creature types", Primitives.subtype, separator = " ", min = 1)

    /**
     * [typeRun]'s inverse: the types a token holds, in the order it holds them.
     *
     * Null on the empty set, which is a token with no creature type at all — a shape this sentence
     * cannot spell, since the template has no empty cell for the noun.
     */
    private fun printedTypes(types: Set<String>): List<Subtype>? =
        types.takeIf { it.isNotEmpty() }?.map { Subtype(it) }

    /**
     * The token's card types in front of "creature" — "1/1 colorless Thopter **artifact** creature
     * token", "1/1 white Glimmer **enchantment** creature token".
     *
     * A slot over the noun rather than a row per kind, because the word sits inside the noun phrase
     * and crosses every other axis — count, tapped, keyword rider, tally — without changing any of
     * them. `CreateTokenEffect` carries the two kinds as two booleans, so the slot's value is the
     * pair and the three constants take disjoint values; printing is decided by the model.
     *
     * "Enchantment artifact creature token" (one Oracle line) is left out: both booleans set is a
     * value the corpus prints once and in the opposite order to the type line's own, so the row
     * would be a convention invented from one example. It declines and is counted.
     */
    private data class TokenKind(val artifact: Boolean, val enchantment: Boolean)

    private val kind: Phrase<TokenKind> = oneOf(
        "a token's card types",
        constant("creature", TokenKind(artifact = false, enchantment = false)),
        constant("artifact creature", TokenKind(artifact = true, enchantment = false)),
        constant("enchantment creature", TokenKind(artifact = false, enchantment = true)),
    )

    /**
     * How many tokens a clause makes, as the two things that vary with it: the printed count and
     * the amount the model holds.
     *
     * A row rather than a slot because the noun's number changes with the count and the singular has
     * no number word at all — [Cardinals.word] starts at two for exactly that reason, so "a" and
     * "two" cannot be one slot without inventing a surface form for 1.
     */
    private class Count(
        val surface: String,
        val plural: Boolean,
        /** The count slot's phrase, for the counted form; null where the amount is fixed by the row. */
        val words: Phrase<Int>?,
        private val fixed: DynamicAmount?,
    ) {
        /** The amount a parse of this row denotes: the row's own, or the number word it just read. */
        fun amountFor(bindings: Bindings): DynamicAmount =
            fixed ?: DynamicAmount.Fixed(bindings.int("n"))

        /**
         * Is this the row that spells [amount]?
         *
         * The three rows are disjoint by construction — one is `Fixed(1)`, one is `XValue`, one is
         * any other spellable `Fixed` — so exactly one of them answers yes and printing is decided by
         * the model rather than by the list's order.
         */
        fun spells(amount: DynamicAmount): Boolean =
            if (words == null) amount == fixed
            else wordFor(amount)?.let(Cardinals::spellable) == true

        /** The number this row's slot would bind, or null on the rows that have no slot. */
        fun wordFor(amount: DynamicAmount): Int? =
            if (words == null) null else (amount as? DynamicAmount.Fixed)?.amount
    }

    private val counts: List<Count> = listOf(
        Count("a", plural = false, words = null, fixed = DynamicAmount.Fixed(1)),
        Count("{n}", plural = true, words = Cardinals.word, fixed = null),
        Count("X", plural = true, words = null, fixed = DynamicAmount.XValue),
    )

    /**
     * The shape: "create <count> P/T <colours> <type> creature token(s)[ with <keywords>]".
     *
     * The keyword rider builds the same list [Keywords.keywordRun] does everywhere else, into
     * `CreateTokenEffect.keywords` — one vocabulary for "gains flying and trample", "has flying and
     * trample" and "token with flying and trample", which is the whole reason that run is a shared
     * phrase rather than a rule inside one family.
     *
     * **Finding: `CreateTokenEffect.keywords` does not realize Decayed.** "Create a 2/2 black Zombie
     * creature token with decayed" reads into `keywords = [DECAYED]`, which is what CR 702.147 says
     * the token has — but the engine grants Decayed's "can't block" and its attack-sacrifice trigger
     * only off `CounterType.DECAYED` (`StateProjector`, `TriggerDetector`), so the keyword alone is
     * inert. Ghoulish Procession therefore writes `initialCounters = {decayed: 1}` and stands as the
     * differential's one divergence here. The card is the reading that *works* and the grammar is
     * the reading the text states; neither is wrong to change unilaterally, so the rule keeps saying
     * what the card says and the gap is reported. Fixing it is engine work: realize the keyword on a
     * created token the way the counter is realized.
     */
    private fun createToken(
        count: Count,
        keywords: Boolean,
        tapped: Boolean = false,
        suffix: String = "",
        suffixName: String = "",
        tally: Amounts.Scope? = null,
    ): Phrase<CardScript> {
        val noun = if (count.plural) "{kind} tokens" else "{kind} token"
        val rider = if (keywords) " with {kws}" else ""
        val entry = if (tapped) "tapped " else ""
        val counted = if (tally == null) "" else " for each {filter}${tally.surface}"
        val name = "create " + (if (count.plural) "tokens" else "a token") +
            (if (tapped) " tapped" else "") +
            (if (keywords) " with keywords" else "") + suffixName +
            (if (tally == null) "" else " per ${tally.where}")

        fun scriptFor(
            amount: DynamicAmount,
            power: Int,
            toughness: Int,
            colours: Set<Color>,
            types: List<Subtype>,
            granted: Set<Keyword>,
            kind: TokenKind,
        ) = CardScript(
            spellEffect = Effects.CreateToken(
                count = amount,
                power = power,
                toughness = toughness,
                colors = colours,
                creatureTypes = types.map { it.value }.toSet(),
                keywords = granted,
                tapped = tapped,
                artifactToken = kind.artifact,
                enchantmentToken = kind.enchantment,
            )
        )

        return phrase("create ${count.surface} $entry{p}/{t} {color} {types} $noun$rider$counted$suffix", name = name) {
            if (count.words != null) slot("n", count.words)
            slot("p", Primitives.cardinal)
            slot("t", Primitives.cardinal)
            slot("color", colours)
            slot("types", typeRun)
            slot("kind", kind)
            if (keywords) slot("kws", Keywords.keywordRun)
            if (tally != null) slot("filter", Filters.filter)
            build { bindings ->
                val granted = if (keywords) bindings.value<List<Keyword>>("kws").toSet() else emptySet()
                val amount = if (tally == null) {
                    count.amountFor(bindings)
                } else {
                    DynamicAmount.AggregateBattlefield(
                        tally.player,
                        tally.narrowing(bindings.value("filter")) ?: return@build null,
                    )
                }
                scriptFor(
                    amount,
                    bindings.int("p"),
                    bindings.int("t"),
                    bindings.value("color"),
                    bindings.value("types"),
                    granted,
                    bindings.value("kind"),
                )
            }
            match { script ->
                val token = script.spellEffect as? CreateTokenEffect ?: return@match null
                if (keywords == token.keywords.isEmpty()) return@match null
                if (token.tapped != tapped) return@match null
                val types = printedTypes(token.creatureTypes) ?: return@match null
                // The counted rows rebuild the amount from the two things the clause spells — the
                // player and the noun phrase — so an aggregate carrying anything else (a sum rather
                // than a count, an excluded self, a counter type) fails the compare below instead of
                // being printed as a plain "for each".
                val counting = if (tally == null) {
                    if (!count.spells(token.count)) return@match null
                    null
                } else {
                    val aggregate = token.count as? DynamicAmount.AggregateBattlefield ?: return@match null
                    if (aggregate.player != tally.player) return@match null
                    tally.narrowing(aggregate.filter) ?: return@match null
                }
                val amount = if (counting == null) {
                    token.count
                } else {
                    DynamicAmount.AggregateBattlefield(tally!!.player, counting)
                }
                if (script != scriptFor(
                        amount,
                        token.power,
                        token.toughness,
                        token.colors,
                        types,
                        token.keywords,
                        TokenKind(token.artifactToken, token.enchantmentToken),
                    )
                ) {
                    return@match null
                }
                bind(
                    "n" to count.wordFor(token.count),
                    "p" to token.power,
                    "t" to token.toughness,
                    "color" to token.colors,
                    "types" to types,
                    "kind" to TokenKind(token.artifactToken, token.enchantmentToken),
                    "kws" to token.keywords.sortedBy { it.ordinal },
                    "filter" to counting,
                )
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Predefined tokens
    // ---------------------------------------------------------------------------------------

    /**
     * "Create a Food token.", "Create two Treasure tokens." — the tokens the rules define once and
     * every card names by their noun alone.
     *
     * The SDK holds these as `CreatePredefinedTokenEffect(tokenType)` rather than as a spelled-out
     * token, so the noun *is* the model and the row list is the vocabulary. It is deliberately the
     * set of nouns the SDK publishes a facade for: a name with no facade would be a string this
     * grammar invented, which is the one thing a rule building through the facades must not do.
     *
     * **The token noun is a proper noun.** It stands mid-sentence where [Subtype] words also do, and
     * `SentenceCase` has already lowercased the line's first letter, so the templates are written
     * exactly as printed and the capital is real rather than restored.
     */
    /**
     * One predefined token noun, and the two facades the SDK gives it.
     *
     * [fixed] and [dynamic] are two overloads of one name that write two *different fields* —
     * `count: Int` and `dynamicCount: DynamicAmount` — so which one a rule calls is decided by the
     * shape of the count the sentence printed, not by preference. [dynamic] is null for the two
     * nouns the SDK publishes no dynamic overload for; a row that invented one would be building a
     * call this grammar made up, which is the thing "build goes through the facades" forbids.
     */
    private class Predefined(
        val tokenType: String,
        val fixed: (Int) -> Effect,
        val dynamic: ((DynamicAmount) -> Effect)? = null,
    )

    private val PREDEFINED: List<Predefined> = listOf(
        Predefined("Treasure", { Effects.CreateTreasure(count = it) }, { Effects.CreateTreasure(count = it) }),
        Predefined("Food", { Effects.CreateFood(count = it) }, { Effects.CreateFood(count = it) }),
        Predefined("Clue", { Effects.CreateClue(count = it) }, { Effects.CreateClue(count = it) }),
        Predefined("Blood", { Effects.CreateBlood(count = it) }, { Effects.CreateBlood(count = it) }),
        Predefined("Map", { Effects.CreateMapToken(count = it) }, { Effects.CreateMapToken(count = it) }),
        Predefined("Lander", { Effects.CreateLander(count = it) }),
        Predefined("Shard", { Effects.CreateShard(count = it) }),
    )

    private fun createPredefined(count: Count, token: Predefined): Phrase<CardScript> {
        val noun = if (count.plural) "tokens" else "token"
        fun scriptFor(amount: DynamicAmount): CardScript? {
            val effect = when (amount) {
                is DynamicAmount.Fixed -> token.fixed(amount.amount)
                else -> token.dynamic?.invoke(amount) ?: return null
            }
            return CardScript(spellEffect = effect)
        }
        return phrase(
            "create ${count.surface} ${token.tokenType} $noun",
            name = "create " +
                if (count.plural) "${token.tokenType} tokens" else "a ${token.tokenType} token",
        ) {
            if (count.words != null) slot("n", count.words)
            build { bindings -> scriptFor(count.amountFor(bindings)) }
            match { script ->
                val effect = script.spellEffect as? CreatePredefinedTokenEffect ?: return@match null
                if (effect.tokenType != token.tokenType) return@match null
                val amount = effect.dynamicCount ?: DynamicAmount.Fixed(effect.count)
                if (!count.spells(amount)) return@match null
                if (script != scriptFor(amount)) return@match null
                bind("n" to count.wordFor(amount))
            }
        }
    }

    /**
     * "Investigate." / "investigate twice." — the keyword action (CR 701.16a). Investigating
     * creates a Clue, but it is its own model (`InvestigateEffect`) rather than the Clue row's:
     * "whenever you investigate" (Erdwal Illuminator) watches the action, and a card that only
     * says "create a Clue token" has not investigated. So both spellings are canonical, each for
     * its own model.
     */
    private val investigate: List<Phrase<CardScript>> =
        listOf("investigate" to 1, "investigate twice" to 2).map { (text, times) ->
            val script = CardScript(spellEffect = Effects.Investigate(times))
            phrase<CardScript>(text, name = text) {
                build { script }
                match { if (it == script) bind() else null }
            }
        }

    /** One token clause, for the sentences that wrap it — see [Granted]. */
    val clause: Phrase<CardScript> get() = oneOf("a token clause", clauses)

    val clauses: List<Phrase<CardScript>> =
        counts.flatMap { count ->
            listOf(false, true).flatMap { tapped ->
                listOf(
                    createToken(count, keywords = false, tapped = tapped),
                    createToken(count, keywords = true, tapped = tapped),
                )
            }
        } +
            // Caller of the Claw. The tally is a *turn* tracker rather than a battlefield count, which
            // is why it is a row here and not one in [Amounts.count]: nothing about the phrase is a
            // noun the filter vocabulary could spell, and the whole clause names one tracked quantity.
            createToken(
                Count(
                    "a",
                    plural = false,
                    words = null,
                    fixed = DynamicAmount.TurnTracking(Player.You, TurnTracker.NONTOKEN_CREATURES_DIED),
                ),
                keywords = false,
                suffix = " for each nontoken creature put into your graveyard from the battlefield this turn",
                suffixName = " per creature that died this turn",
            ) +
            // "Create a 1/1 green Elf Warrior creature token **for each Elf you control**." —
            // Elvish Promenade, Beacon of Creation, Elvish Promenade's whole family. The count is
            // the noun phrase rather than a number word, so it goes through [Amounts.scopes] like
            // every other battlefield tally in the grammar and reaches all three of its rows with
            // one instantiation. The article stays singular: English counts the *kind* of token
            // once and lets the clause say how many, which is why this crosses the "a" row and no
            // other. `tapped` crosses it because a counted clause can still enter tapped, and the
            // keyword rider because "for each" sits between the noun and the rider in print.
            Amounts.perScope { scope ->
                createToken(counts.first(), keywords = false, tally = scope)
            } +
            Amounts.perScope { scope ->
                createToken(counts.first(), keywords = true, tally = scope)
            } +
            // "X Food tokens" is not printed — the predefined nouns take the article and the number
            // word only, so the X row is left out rather than written against nothing.
            PREDEFINED.flatMap { token ->
                counts.dropLast(1).map { createPredefined(it, token) }
            } +
            investigate

    /**
     * "Create that many Blood tokens." — Olivia's Attendants; "…create that many 1/1 green Elf
     * Warrior creature tokens." — Lathril, Tana, Living Hive, Rapacious One, and twenty more.
     *
     * ### Why this is a separate list and not a fourth row of [counts]
     *
     * "That many" is an **anaphor**: it names the quantity the sentence before it reported, and the
     * corpus prints it after at least six different antecedents — damage dealt, cards discarded,
     * creatures attacking, counters removed, cards milled, permanents sacrificed. The SDK spells
     * each as a different `ContextPropertyKey`, so a row inside [counts] would have to pick one and
     * would then read the other five into a value that evaluates to zero. That is the
     * reversible-but-wrong class in one phrase: it round-trips byte-for-byte and means a different
     * card.
     *
     * So the clause is scoped to the position where the antecedent is known, the way
     * [SelfSteps.triggering]'s anaphor is: [Steps.damageStep] is the cascade a damage trigger hands
     * its payoff, and this list is what that cascade adds. Every other antecedent is a further
     * instantiation the day its trigger family wants one, which is one line of `Steps` each.
     *
     * The general token rows come along because they are the same [createToken] shape with the same
     * count — the whole reason [Count] exists is that the amount and the printed word travel
     * together.
     */
    val damageClauses: List<Phrase<CardScript>> = run {
        val thatMany = Count(
            "that many",
            plural = true,
            words = null,
            fixed = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
        )
        listOf(false, true).flatMap { tapped ->
            listOf(
                createToken(thatMany, keywords = false, tapped = tapped),
                createToken(thatMany, keywords = true, tapped = tapped),
            )
        } + PREDEFINED.filter { it.dynamic != null }.map { createPredefined(thatMany, it) }
    }
}
