package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.SentenceCase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * **The top of the library** — how many cards you see, what you keep, and where the rest go.
 *
 * This is the cost band's lesson applied to a third SDK type, and the shape of the mistake it
 * undoes is the same one twice over. The SDK models every one of these sentences as *one pipeline* —
 * `GatherCards(TopOfLibrary(n))` → `SelectFromCollection(mode, filter)` → a `MoveCollection` per
 * half — and `Patterns.Library` publishes that pipeline as recipes whose *parameters are exactly the
 * words that vary*: `keepDestination`, `restDestination`, `restOrder`, `count`, `filter`. [Library]
 * was nevertheless reading it as one whole-sentence rule per printed card, each restating the recipe
 * with every parameter frozen — `lookAtTopAndKeep` could only spell hand-and-graveyard, so
 * "…and the rest on the bottom of your library in any order" was a different rule nobody had
 * written rather than a different *word*.
 *
 * So the family here is not a rule per sentence. It is three layers, and every sentence is a lift of
 * them:
 *
 * | Layer | What it spells | Vocabulary |
 * |---|---|---|
 * | how many | "the top card", "the top four cards", "the top X cards" | [topCards] |
 * | where one pile goes | "into your hand", "onto the battlefield", "on the bottom of your library" | [place] |
 * | …and in what order | "…in a random order", "…in any order" | [disposition] |
 *
 * The count layer is where the grammatical number lives: the noun agrees with the number inside the
 * same phrase, so "the top card" and "the top four cards" are one slot rather than two sentence
 * families, and nothing above it has to know which one it got.
 *
 * ### The order suffix owns exactly one field
 *
 * [disposition] is [Filters]' layering rule over a different value. `Preserve`, `Random` and
 * `ControllerChooses` are three disjoint values of one field, one printed suffix each (the empty
 * one included), and the layer strips precisely that field before delegating to [place]. A
 * combinator that could also print the destination would leave printing underdetermined the moment
 * two of them could express the same value; this cannot, because the split is on the field.
 *
 * ### The keep pile takes a [place] and the rest pile takes a [disposition], and that asymmetry is
 * the SDK's
 *
 * `lookAtTopAndKeep` exposes `restOrder` and no `keepOrder`, because the cards you keep go somewhere
 * one at a time and the ones you put back are a pile whose order the sentence has to say. Reading
 * the two slots as the same type would have let the grammar print "put one of them into your hand in
 * a random order", a sentence no card writes and the model cannot carry.
 *
 * ### The two layers the take sentence added
 *
 * "You may put a creature card from among them **onto the battlefield**" was written down here as an
 * SDK finding rather than a rule for one release: `lookAtTopRevealMatchingToHand` hardcoded the hand,
 * and hand-building the pipeline would have restated a recipe the SDK owns. It is now
 * `Patterns.Library.lookAtTopAndTakeMatching`, whose parameters are again exactly the words that
 * vary, and two more layers read them — [seen] for "look at" against "reveal", and [Take] for how
 * many of the pile you take and of what. See [takeMatchingRule].
 *
 * ### What is deliberately not here
 *
 * - **A second keep pile.** "You may put up to one land card from among them onto the battlefield
 *   tapped **and up to one Elf card from among them into your hand**" — Bounty of Skemfar — is two
 *   selections over one gather, which is a second `SelectFromCollection` the recipe has no room for.
 *   That is a different pipeline, not a longer sentence.
 * - **Somebody else's library.** Every rule here fixes `Player.You`, because the printed noun phrase
 *   for another player's library ("target opponent's", "each player's") is a *target* vocabulary and
 *   not a possessive word — [Library.lookAtOpponentTopAndBury] is what one costs today. The count
 *   layer takes no player slot for that reason; adding one is a rule that reaches every sentence
 *   here at once, which is the argument for doing it as its own change.
 */
object TopOfLibrary {

    // ---------------------------------------------------------------------------------------
    // Layer 1 — how many cards you see
    // ---------------------------------------------------------------------------------------

    /**
     * "the top card" / "the top four cards" / "the top X cards".
     *
     * **The noun is inside the phrase, which is what makes the number agree for free.** English
     * writes "the top *card*" for one and "the top four *cards*" for the rest, and that is not a
     * choice the sentences above could make correctly on their own — they would each need the split,
     * and one of them would eventually get it wrong. Keeping the noun here means a sentence writes
     * `{top}` and is right in both numbers.
     *
     * The three rows take disjoint values — one, [Cardinals.word]'s two-and-up, and `X` — so
     * printing is decided by the amount rather than by the alternation's order, and 1 cannot print
     * as "the top one cards" because [Cardinals.spellable] refuses it.
     */
    private val topCards: Phrase<DynamicAmount> = oneOf(
        "the top cards of a library",
        constant("the top card", DynamicAmount.Fixed(1)),
        phrase("the top {n} cards", name = "the top several cards") {
            slot("n", Cardinals.word)
            build { DynamicAmount.Fixed(it.int("n")) }
            match { amount ->
                val fixed = (amount as? DynamicAmount.Fixed)?.amount ?: return@match null
                if (!Cardinals.spellable(fixed)) return@match null
                bind("n" to fixed)
            }
        },
        constant("the top X cards", DynamicAmount.XValue),
    )

    /**
     * How many cards you see **and whether everyone else sees them** — "look at the top four cards
     * of your library" against "reveal the top four cards of your library".
     *
     * One printed verb, one `Boolean` on the gather step the count already comes off
     * ([GatherCardsEffect.revealed]), so this is [topCards] with one more field rather than a second
     * copy of every sentence below it. `Patterns.Library.lookAtTopAndKeep` has taken `revealed` as a
     * parameter all along and the grammar was calling it with the word frozen at "look at" — the
     * file's own lesson, one layer up from where it first landed.
     *
     * `Patterns.Library.revealTopPutAllMatchingToHand` spells the same reveal as a separate
     * `RevealCollectionEffect` step instead, which is a second SDK spelling of one fact and a
     * finding rather than something to fold here: folding would make the two recipes print the same
     * English from two different models. The hand-written corpus votes for the flag — Scout the
     * Borders, written from this very sentence, sets `revealed = true` on its gather.
     */
    private data class Seen(val count: DynamicAmount, val revealed: Boolean)

    /** "look at {top} of your library" / "reveal {top} of your library" — [Seen]'s two rows. */
    private val seen: Phrase<Seen> = oneOf(
        "the top cards of your library, looked at or revealed",
        phrase("look at {top} of your library", name = "look at the top cards of your library") {
            slot("top", topCards)
            build { Seen(it.value("top"), revealed = false) }
            match { if (it.revealed) null else bind("top" to it.count) }
        },
        phrase("reveal {top} of your library", name = "reveal the top cards of your library") {
            slot("top", topCards)
            build { Seen(it.value("top"), revealed = true) }
            match { if (it.revealed) bind("top" to it.count) else null }
        },
    )

    /** True where English would spell the pile as a plural — everything but a single named card. */
    private fun isPlural(count: DynamicAmount): Boolean = count != DynamicAmount.Fixed(1)

    /**
     * True when keeping [keep] of [count] leaves exactly one card behind — the fact that decides
     * "the other" against "the rest", and the line between [lookAtTopAndKeep] and
     * [lookAtTopAndKeepAllButOne]. A count the card works out at resolution can never be known to
     * leave one, so it is false there and the general rule owns it.
     */
    private fun leavesExactlyOne(count: DynamicAmount, keep: Int): Boolean =
        (count as? DynamicAmount.Fixed)?.amount?.minus(keep) == 1

    // ---------------------------------------------------------------------------------------
    // Layer 2 — where a pile goes, and in what order
    // ---------------------------------------------------------------------------------------

    /**
     * Where one pile ends up: "into your hand", "on the bottom of your library".
     *
     * A flat alternation over whole prepositional phrases rather than a preposition plus a zone,
     * because English does not derive the two from each other — a card goes *into* a hand, *onto*
     * the battlefield and *on* the bottom of a library, and a rule with the preposition as a slot
     * could print all nine combinations of which only three are English.
     */
    private val place: Phrase<CardDestination> = oneOf(
        "a place cards go",
        constant("into your hand", CardDestination.ToZone(Zone.HAND)),
        constant("into your graveyard", CardDestination.ToZone(Zone.GRAVEYARD)),
        constant("onto the battlefield", CardDestination.ToZone(Zone.BATTLEFIELD)),
        // "…onto the battlefield tapped", "…tapped and attacking". Two more rows rather than a
        // "tapped" suffix layer over the battlefield row, because `ZonePlacement` types them as
        // three disjoint values of one field and English writes them as three whole prepositional
        // phrases — the same argument the row above makes against a preposition slot.
        constant(
            "onto the battlefield tapped",
            CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
        ),
        constant(
            "onto the battlefield tapped and attacking",
            CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.TappedAndAttacking),
        ),
        constant(
            "on the bottom of your library",
            CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
        ),
        constant(
            "on top of your library",
            CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
        ),
        // "…and the rest on the bottom in a random order." — the library is elided once the
        // sentence has already said which one. One model, two real English spellings, so the full
        // one prints and this one parses; a card printing it comes back as a VARIANT.
        alternate(
            constant(
                "on the bottom",
                CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            ),
        ),
        alternate(
            constant("on top", CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)),
        ),
    )

    // ---------------------------------------------------------------------------------------
    // The two noun phrases that point back at the pile
    // ---------------------------------------------------------------------------------------

    /**
     * "of them" — how a card refers back to the pile it just gathered, when counting out of it.
     *
     * A slot carrying no value, because there is nothing in the model for it to carry: the pile has
     * already been named by the sentence before, and English's choice between "of them" and "of
     * those cards" is spelling. One prints, both parse, and a card writing the other comes back as a
     * VARIANT — which beats the decline it replaces and costs the model nothing.
     */
    private val ofThePile: Phrase<Unit> = oneOf(
        "the pile just looked at",
        constant("of them", Unit),
        alternate(constant("of those cards", Unit)),
    )

    /** "the rest" — the same referent as [ofThePile], as the subject of the disposition clause. */
    private val theRest: Phrase<Unit> = oneOf(
        "the cards not kept",
        constant("the rest", Unit),
        alternate(constant("the rest of them", Unit)),
        alternate(constant("the rest of those cards", Unit)),
        alternate(constant("the rest of the cards", Unit)),
        alternate(constant("the rest of the cards revealed this way", Unit)),
        alternate(constant("the rest of the revealed cards", Unit)),
    )

    /** A destination together with the order the cards arrive in — one `MoveCollectionEffect`. */
    private data class Disposition(val destination: CardDestination, val order: CardOrder)

    /** The order layer: one printed suffix per [CardOrder], the empty suffix included. */
    private fun ordered(suffix: String, order: CardOrder): Phrase<Disposition> =
        phrase("{place}$suffix", name = "a place${suffix.ifEmpty { " (in its printed order)" }}") {
            slot("place", place)
            build { Disposition(it.value("place"), order) }
            match { if (it.order == order) bind("place" to it.destination) else null }
        }

    /**
     * "into your graveyard", "on the bottom of your library in a random order".
     *
     * The three rows are disjoint on [CardOrder], which is the whole reason this is a layer rather
     * than a combinator: exactly one of them can express any given [Disposition], so the printer
     * never chooses.
     */
    private val disposition: Phrase<Disposition> = oneOf(
        "where the cards go",
        ordered("", CardOrder.Preserve),
        ordered(" in a random order", CardOrder.Random),
        ordered(" in any order", CardOrder.ControllerChooses),
    )

    // ---------------------------------------------------------------------------------------
    // Layer 3 — which of the cards you saw you take
    // ---------------------------------------------------------------------------------------

    /**
     * How many of the pile you take and what they have to be — one `SelectionMode` and one filter,
     * spelled as one phrase.
     *
     * **The option word lives in here, with the verb, because English fuses the two.** "You may put
     * **a** creature card", "put **up to two** permanent cards" and "put any number of Equipment
     * cards" are three spellings of one field: whether a card is taken at all is the *same* fact as
     * how many, and `SelectionMode` says so — `ChooseUpTo(1)` is exactly "you may take one". A layer
     * that spelled only the quantifier would have had to leave "you may" to the sentence above it,
     * and that sentence would then have had to decide the option word from a value it had handed
     * away. This is [topCards]' rule about the noun ("the top card" / "the top four cards") applied
     * to the verb phrase in front of it: the words that agree stay in one phrase.
     *
     * `ChooseExactly` is the fourth value and the only mandatory one, and it is the reason there are
     * two sentences below rather than one — see [takeMatchingRule].
     */
    private data class Take(val selection: SelectionMode, val filter: GameObjectFilter)

    /**
     * A [Take] row whose count is fixed by the row itself — everything but "up to *n*".
     *
     * [canonicalForm] is what splits one model's two printed spellings: "you may put any number of
     * Equipment cards" and "put any number of Equipment cards" are the same `ChooseAnyNumber`, so
     * one prints and the other only parses.
     */
    private fun takeRow(
        template: String,
        name: String,
        noun: Phrase<GameObjectFilter>,
        selection: SelectionMode,
        canonicalForm: Boolean = true,
    ): Phrase<Take> {
        val rule = phrase<Take>(template, name = name) {
            slot("f", noun)
            build { Take(selection, it.value("f")) }
            match { if (it.selection == selection) bind("f" to it.filter) else null }
            canonical = canonicalForm
        }
        return if (canonicalForm) rule else alternate(rule)
    }

    /**
     * "put up to two permanent cards", "you may put up to three enchantment cards" — the counted
     * [Take].
     *
     * [Cardinals.word] starts at two, which is what keeps this row disjoint from the "you may put a
     * creature card" row above it: `ChooseUpTo(1)` has exactly one canonical spelling and it is the
     * one with the article, so nothing can print as "up to one".
     */
    private fun takeUpToRow(template: String, name: String, canonicalForm: Boolean = true): Phrase<Take> {
        val rule = phrase<Take>(template, name = name) {
            slot("n", Cardinals.word)
            slot("f", Filters.pluralCards)
            build { Take(SelectionMode.ChooseUpTo(DynamicAmount.Fixed(it.int("n"))), it.value("f")) }
            match { take ->
                val upTo = take.selection as? SelectionMode.ChooseUpTo ?: return@match null
                val n = (upTo.count as? DynamicAmount.Fixed)?.amount ?: return@match null
                if (!Cardinals.spellable(n)) return@match null
                bind("n" to n, "f" to take.filter)
            }
            canonical = canonicalForm
        }
        return if (canonicalForm) rule else alternate(rule)
    }

    /**
     * The takes that are **optional in the printed verb** — "you may put a creature card", "you may
     * put any number of snow permanent cards".
     *
     * Disjoint from [countedTake] on the `SelectionMode` itself, which is what lets the two
     * sentences below print different word orders without either of them being able to print the
     * other's models.
     */
    private val optionalTake: Phrase<Take> = oneOf(
        "cards you may take from the pile",
        takeRow(
            "you may put {f}", "you may take one matching card",
            Filters.indefiniteCard, SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
        ),
        takeRow(
            "you may put any number of {f}", "you may take any number of matching cards",
            Filters.pluralCards, SelectionMode.ChooseAnyNumber,
        ),
        takeRow(
            "put any number of {f}", "take any number of matching cards",
            Filters.pluralCards, SelectionMode.ChooseAnyNumber, canonicalForm = false,
        ),
    )

    /** The takes whose printed verb is bare — "put a land card", "put up to two artifact cards". */
    private val countedTake: Phrase<Take> = oneOf(
        "cards you take from the pile, counted",
        takeRow(
            "put {f}", "take one matching card",
            Filters.indefiniteCard, SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
        ),
        takeUpToRow("put up to {n} {f}", "take up to several matching cards"),
        takeUpToRow(
            "you may put up to {n} {f}", "you may take up to several matching cards",
            canonicalForm = false,
        ),
    )

    // ---------------------------------------------------------------------------------------
    // Reading a pipeline back
    // ---------------------------------------------------------------------------------------

    /** The steps of a pipeline script, or null for anything that is not one. */
    private fun steps(script: CardScript): List<Effect>? =
        (script.spellEffect as? CompositeEffect)?.effects

    /**
     * How many cards a pipeline gathers off the top of *your* library.
     *
     * Null for another player's library and for a mill, which is [CardSource.TopOfLibrary]'s own
     * `isMill` flag: CR 701.17's "mill that many plus four instead" applies at the count site, so a
     * mill and an exile-from-the-top are different values even where the pipeline shape agrees, and
     * the SDK sets the flag on exactly one recipe. Reading the flag off rather than ignoring it is
     * what keeps this family from printing a mill as "exile the top two cards".
     */
    private fun gatheredCount(steps: List<Effect>): DynamicAmount? {
        val source = (steps.firstOrNull() as? GatherCardsEffect)?.source as? CardSource.TopOfLibrary
            ?: return null
        return source.count.takeIf { !source.isMill }
    }

    /** [gatheredCount] plus the gather's own reveal flag — the whole of what [seen] spells. */
    private fun gatheredSeen(steps: List<Effect>): Seen? {
        val gather = steps.firstOrNull() as? GatherCardsEffect ?: return null
        val count = gatheredCount(steps) ?: return null
        return Seen(count, gather.revealed)
    }

    /** The one selection step of a pipeline, or null when it has none or several. */
    private fun selection(steps: List<Effect>): SelectFromCollectionEffect? =
        steps.filterIsInstance<SelectFromCollectionEffect>().singleOrNull()

    /** The move step that empties [from], as the [Disposition] it denotes. */
    private fun movedTo(steps: List<Effect>, from: String): Disposition? =
        steps.filterIsInstance<MoveCollectionEffect>().singleOrNull { it.from == from }
            ?.let { Disposition(it.destination, it.order) }

    /**
     * The filter of the one **choice-free** partition step in a pipeline, or null when it has none.
     *
     * [selection] reads a `SelectFromCollection` — the player choosing — and this reads the step
     * that routes every matching card automatically. They are the two halves of the same question
     * ("which of the cards you saw end up where") and the grammar keeps them apart because English
     * does: "you may reveal a creature card **from among them**" is a choice and "put **all** land
     * cards revealed this way" is not.
     */
    private fun partition(steps: List<Effect>): GameObjectFilter? =
        steps.filterIsInstance<FilterCollectionEffect>().singleOrNull()
            ?.takeIf { it.collectionFilter == null }?.filter

    // ---------------------------------------------------------------------------------------
    // The sentences
    // ---------------------------------------------------------------------------------------

    /**
     * "Look at the top four cards of your library. You may reveal a creature card from among them
     * and put it into your hand. Put the rest on the bottom of your library in a random order." —
     * the single most-printed shape in the whole family, and `Patterns.Library`'s own
     * `lookAtTopRevealMatchingToHand`.
     *
     * Three printed sentences, one rule, for the reason [Library] states about every pipeline here:
     * the model's steps do not line up with the sentence boundaries. The second sentence is the
     * selection *and* one of the two moves, and the third is the other move — so splitting the text
     * would produce halves that denote nothing, and [Steps.sequence] correctly never tries.
     *
     * **The prompt is derived, not invented.** The facade requires one and no printed word
     * determines it, which is the class of field the differential already folds away
     * (`Folds.dropPresentation` drops `prompt` by name, on the SDK's own "the label a decision shows
     * its controller" wording). Deriving it from the filter the rule just read keeps two parses of
     * one line equal and keeps a compiled card's decision readable, and because both halves call the
     * same function the reconstruction below stays a comparison rather than an exception.
     */
    private val lookAtTopRevealMatchingToHand: Phrase<CardScript> = run {
        fun scriptFor(count: DynamicAmount, filter: GameObjectFilter, rest: Disposition): CardScript? {
            val noun = Filters.indefiniteCard.unparse(filter) ?: return null
            return CardScript(
                spellEffect = Patterns.Library.lookAtTopRevealMatchingToHand(
                    count = count,
                    filter = filter,
                    prompt = "You may reveal $noun from among them and put it into your hand",
                    restDestination = rest.destination,
                    restOrder = rest.order,
                ),
            )
        }
        phrase(
            "look at {top} of your library. you may reveal {filter} from among them and put " +
                "it into your hand. put the rest {rest}",
            name = "look at the top cards and take one matching card",
        ) {
            slot("top", topCards)
            slot("filter", Filters.indefiniteCard)
            slot("rest", disposition)
            build { scriptFor(it.value("top"), it.value("filter"), it.value("rest")) }
            match { script ->
                val steps = steps(script) ?: return@match null
                val count = gatheredCount(steps) ?: return@match null
                val filter = selection(steps)?.filter ?: return@match null
                val rest = movedTo(steps, "rest") ?: return@match null
                if (script != scriptFor(count, filter, rest)) return@match null
                bind("top" to count, "filter" to filter, "rest" to rest)
            }
        }
    }

    /**
     * "Look at the top five cards of your library. You may put a land card from among them onto the
     * battlefield tapped. Put the rest on the bottom of your library in a random order." — Elvish
     * Rejuvenator, Summoning Trap, Gather the Pack, Mayael the Anima, and the most-printed sentence
     * in this whole file's family.
     *
     * It is [lookAtTopRevealMatchingToHand] with the three words that rule freezes turned into
     * slots — the verb ([seen]), how many and of what ([Take]), and where they go ([place]) — which
     * is why the SDK grew `Patterns.Library.lookAtTopAndTakeMatching` under it and the older recipe
     * now delegates there. The two sentences stay apart on one field: this one moves the kept cards
     * without turning them face up, and that rule's does ("you may **reveal** a creature card … and
     * put **it** into your hand"), so exactly one of them can print any given script.
     *
     * ### Why two instantiations, and what decides the word order
     *
     * The two printed joins — "…into your hand. Put the rest into your graveyard." against "…into
     * your hand **and** the rest into your graveyard." — carry nothing the model can hold, so one of
     * them prints per script and the other only parses. Which one is canonical is **decided by the
     * `SelectionMode`**, measured over the corpus's 77 printings of the sentence:
     *
     * | Take | Separate sentences | Joined |
     * |---|---:|---:|
     * | "you may put a …" — `ChooseUpTo(1)` | **38** | 0 |
     * | "you may put any number of …" — `ChooseAnyNumber` | **9** | 5 |
     * | "put a …" — `ChooseExactly(1)` | 2 | **11** |
     * | "put up to n …" — `ChooseUpTo(n)` | 7 | 5 |
     *
     * The option word and the join move together: a take English spells with "you may" is written as
     * two sentences and a bare "put" is written as one. So the same split that gives [optionalTake]
     * and [countedTake] their disjoint halves of `SelectionMode` gives each of them its own
     * canonical order, and neither can print the other's models. This is [impulseRule]'s lesson
     * about durations, arriving at a third field.
     *
     * ### The prompt is the printed sentence
     *
     * The facade needs a decision label and no printed word supplies one — the class of field
     * `Folds.dropPresentation` drops by name. Rather than invent prose, both directions rebuild it
     * from the very layers the rule just read: `{take} from among them {keep}`, capitalized. Two
     * parses of one line therefore stay equal, and a compiled card's decision reads back as its own
     * Oracle text.
     */
    private fun takeMatchingRule(
        template: String,
        name: String,
        alsoSpelledTemplate: String,
        alsoSpelledName: String,
        take: Phrase<Take>,
    ): Phrase<CardScript> {
        fun scriptFor(pile: Seen, taken: Take, keep: CardDestination, rest: Disposition): CardScript? {
            val sentence = (take.unparse(taken) ?: return null) +
                " from among them " + (place.unparse(keep) ?: return null)
            return CardScript(
                spellEffect = Patterns.Library.lookAtTopAndTakeMatching(
                    count = pile.count,
                    filter = taken.filter,
                    prompt = SentenceCase.capitalize(sentence),
                    selection = taken.selection,
                    revealed = pile.revealed,
                    keepDestination = keep,
                    restDestination = rest.destination,
                    restOrder = rest.order,
                ),
            )
        }
        return phrase(template, name = name) {
            slot("seen", seen)
            slot("take", take)
            slot("keep", place)
            slot("remainder", theRest)
            slot("rest", disposition)
            alsoSpelled(alsoSpelledTemplate, name = alsoSpelledName)
            build { scriptFor(it.value("seen"), it.value("take"), it.value("keep"), it.value("rest")) }
            match { script ->
                val steps = steps(script) ?: return@match null
                val pile = gatheredSeen(steps) ?: return@match null
                val select = selection(steps) ?: return@match null
                val taken = Take(select.selection, select.filter)
                val keep = movedTo(steps, "kept") ?: return@match null
                val rest = movedTo(steps, "rest") ?: return@match null
                if (script != scriptFor(pile, taken, keep.destination, rest)) return@match null
                bind(
                    "seen" to pile,
                    "take" to taken,
                    "keep" to keep.destination,
                    "remainder" to Unit,
                    "rest" to rest,
                )
            }
        }
    }

    /** "…You may put a creature card from among them onto the battlefield. Put the rest …" */
    private val lookAtTopAndMayTakeMatching: Phrase<CardScript> = takeMatchingRule(
        template = "{seen}. {take} from among them {keep}. put {remainder} {rest}",
        name = "look at the top cards and take the matching ones you want",
        alsoSpelledTemplate = "{seen}. {take} from among them {keep} and {remainder} {rest}",
        alsoSpelledName = "look at the top cards and take the matching ones you want (joined)",
        take = optionalTake,
    )

    /** "…Put a land card from among them into your hand and the rest into your graveyard." */
    private val lookAtTopAndTakeMatchingCounted: Phrase<CardScript> = takeMatchingRule(
        template = "{seen}. {take} from among them {keep} and {remainder} {rest}",
        name = "look at the top cards and take a counted number of matching ones",
        alsoSpelledTemplate = "{seen}. {take} from among them {keep}. put {remainder} {rest}",
        alsoSpelledName = "look at the top cards and take a counted number of matching ones (split)",
        take = countedTake,
    )

    /**
     * "Look at the top seven cards of your library. Put two of them into your hand and the rest into
     * your graveyard." — the counted keep, with both destinations as slots.
     *
     * This rule was in [Library] with the two destinations frozen at hand and graveyard, which is
     * what made "…and the rest on the bottom of your library in any order" a missing rule rather
     * than a missing *word*. `Patterns.Library.lookAtTopAndKeep` has taken both as parameters all
     * along; the grammar simply was not reading it that way round.
     *
     * The kept pile takes a [place] and the remainder a [disposition] because the recipe has a
     * `restOrder` and no `keepOrder` — see the file KDoc.
     *
     * It refuses a remainder of exactly one, which [lookAtTopAndKeepAllButOne] owns. Without that
     * guard the two rules could both print Tower Geist and the alternation's *order* would decide
     * which — the configuration this module treats as a bug that has not surfaced yet.
     */
    private val lookAtTopAndKeep: Phrase<CardScript> = run {
        fun scriptFor(pile: Seen, keep: Int, to: CardDestination, rest: Disposition) = CardScript(
            spellEffect = Patterns.Library.lookAtTopAndKeep(
                count = pile.count,
                keepCount = DynamicAmount.Fixed(keep),
                keepDestination = to,
                restDestination = rest.destination,
                revealed = pile.revealed,
                restOrder = rest.order,
            ),
        )
        phrase(
            "{seen}. put {k} {pile} {keep} and {remainder} {rest}",
            name = "look at the top cards and keep some",
        ) {
            slot("seen", seen)
            slot("k", Cardinals.pronominal)
            slot("pile", ofThePile)
            slot("keep", place)
            slot("remainder", theRest)
            slot("rest", disposition)
            // "You look at the top four cards of your library, **then** put one of those cards into
            // your hand…" — Witness the Future. One sentence where the canonical form writes two,
            // and the same model: the recipe is a single pipeline either way, so the join is
            // spelling and belongs here rather than to [Steps]' clause run, which could not split a
            // gather from the selection that consumes it.
            alsoSpelled(
                "you {seen}, then put {k} {pile} {keep} and {remainder} {rest}",
                name = "look at the top cards and keep some (one sentence)",
            )
            build { bindings ->
                val pile = bindings.value<Seen>("seen")
                val keep = bindings.int("k")
                if (leavesExactlyOne(pile.count, keep)) return@build null
                scriptFor(pile, keep, bindings.value("keep"), bindings.value("rest"))
            }
            match { script ->
                val steps = steps(script) ?: return@match null
                val pile = gatheredSeen(steps) ?: return@match null
                val mode = selection(steps)?.selection as? SelectionMode.ChooseExactly ?: return@match null
                val keep = (mode.count as? DynamicAmount.Fixed)?.amount ?: return@match null
                if (!Cardinals.spellablePronominally(keep) || leavesExactlyOne(pile.count, keep)) return@match null
                val to = movedTo(steps, "kept")?.takeIf { it.order == CardOrder.Preserve } ?: return@match null
                val rest = movedTo(steps, "rest") ?: return@match null
                if (script != scriptFor(pile, keep, to.destination, rest)) return@match null
                bind(
                    "seen" to pile,
                    "k" to keep,
                    "pile" to Unit,
                    "keep" to to.destination,
                    "remainder" to Unit,
                    "rest" to rest,
                )
            }
        }
    }

    /**
     * "Look at the top two cards of your library. Put one of them into your hand and **the other**
     * into your graveyard." — Tower Geist.
     *
     * A row of its own rather than a spelling of "the rest", and the difference is decided by the
     * *model*: "the other" is what English writes when the remainder is exactly one card, which is
     * `count - keep == 1`. So the two rules take disjoint halves of the same value space and
     * printing stays determined — the same argument the singular and plural halves of [topCards]
     * make one layer down.
     *
     * It only reads a fixed count, because a remainder of exactly one is a fact you can only know
     * about numbers you have.
     */
    private val lookAtTopAndKeepAllButOne: Phrase<CardScript> = run {
        fun scriptFor(pile: Seen, keep: Int, to: CardDestination, rest: Disposition) = CardScript(
            spellEffect = Patterns.Library.lookAtTopAndKeep(
                count = pile.count,
                keepCount = DynamicAmount.Fixed(keep),
                keepDestination = to,
                restDestination = rest.destination,
                revealed = pile.revealed,
                restOrder = rest.order,
            ),
        )
        phrase(
            "{seen}. put {k} {pile} {keep} and the other {rest}",
            name = "look at the top cards and keep all but one",
        ) {
            slot("seen", seen)
            slot("k", Cardinals.pronominal)
            slot("pile", ofThePile)
            slot("keep", place)
            slot("rest", disposition)
            build { bindings ->
                val pile = bindings.value<Seen>("seen")
                val keep = bindings.int("k")
                if (!leavesExactlyOne(pile.count, keep)) return@build null
                if (!Cardinals.spellable((pile.count as DynamicAmount.Fixed).amount)) return@build null
                scriptFor(pile, keep, bindings.value("keep"), bindings.value("rest"))
            }
            match { script ->
                val steps = steps(script) ?: return@match null
                val pile = gatheredSeen(steps) ?: return@match null
                val count = (pile.count as? DynamicAmount.Fixed)?.amount ?: return@match null
                val mode = selection(steps)?.selection as? SelectionMode.ChooseExactly ?: return@match null
                val keep = (mode.count as? DynamicAmount.Fixed)?.amount ?: return@match null
                if (!leavesExactlyOne(pile.count, keep)) return@match null
                if (!Cardinals.spellable(count) || !Cardinals.spellablePronominally(keep)) return@match null
                val to = movedTo(steps, "kept")?.takeIf { it.order == CardOrder.Preserve } ?: return@match null
                val rest = movedTo(steps, "rest") ?: return@match null
                if (script != scriptFor(pile, keep, to.destination, rest)) return@match null
                bind(
                    "seen" to pile,
                    "k" to keep,
                    "pile" to Unit,
                    "keep" to to.destination,
                    "rest" to rest,
                )
            }
        }
    }

    /**
     * "Exile the top two cards of your library." — `Patterns.Library.exileTop`, and the shortest
     * sentence in the family.
     *
     * It is a whole clause on its own, unlike everything above it, because its pipeline has no
     * selection step: gather and move, nothing to decide, nothing for a second sentence to refer
     * back to. That is also why it is safe beside [impulse] below — the two recipes name their
     * collections differently (`exiled_top` against `impulseExiled`), so the model says which
     * sentence it came from and no line reads as both.
     */
    private val exileTop: Phrase<CardScript> = run {
        fun scriptFor(count: DynamicAmount) = CardScript(spellEffect = Patterns.Library.exileTop(count))
        phrase("exile {top} of your library", name = "exile the top cards of your library") {
            slot("top", topCards)
            build { scriptFor(it.value("top")) }
            match { script ->
                val count = gatheredCount(steps(script) ?: return@match null) ?: return@match null
                if (script != scriptFor(count)) return@match null
                bind("top" to count)
            }
        }
    }

    /**
     * "Exile the top card of your library. You may play that card this turn." — impulse draw, which
     * the SDK publishes as `Patterns.Exile.impulse` under that name.
     *
     * The **anaphor agrees with the count**, and that agreement is checked rather than spelled: a
     * `build` that reads "those cards" behind a count of one returns null, which is the kernel's own
     * "grammatical but denotes nothing". Two rows per duration split on the number would have been
     * the alternative, and the wrong one — the number is already decided by [topCards], so a second
     * place that decides it is a second place to get it wrong.
     *
     * The duration is a row rather than a slot, and the reason is the counting band's lesson about
     * word order arriving a second time. Every duration here is printed in **both** orders, so
     * neither is a minority to decline — but which one is *canonical* flips with the duration, and
     * it flips on the `MayPlayExpiry` itself:
     *
     * | Duration | Trailing | Fronted |
     * |---|---:|---:|
     * | `EndOfTurn` — "this turn" | **115** | 40 |
     * | `UntilEndOfNextTurn` | 16 | **59** |
     * | `UntilNextEndStep` | 5 | **8** |
     *
     * A single template with the duration as a slot would have to pick one order for all three and
     * would print the minority spelling for two of them. So each duration takes both orders as its
     * own pair of rows, the majority canonical and the other an `alternate` — two disjoint domains
     * of one field, exactly as damage's "equal to …" clause splits on the shape of its amount.
     * "For as long as it remains exiled" has no fronted spelling at all and is a single row.
     */
    private fun impulseRule(
        template: String,
        name: String,
        expiry: MayPlayExpiry,
        canonicalForm: Boolean = true,
    ): Phrase<CardScript> {
        fun scriptFor(count: DynamicAmount) =
            CardScript(spellEffect = Patterns.Exile.impulse(count, expiry))
        val rule = phrase<CardScript>(template, name = name) {
            slot("top", topCards)
            slot("cards", exiledCards)
            build { bindings ->
                val count = bindings.value<DynamicAmount>("top")
                if (bindings.value<Boolean>("cards") != isPlural(count)) return@build null
                scriptFor(count)
            }
            match { script ->
                val count = gatheredCount(steps(script) ?: return@match null) ?: return@match null
                if (script != scriptFor(count)) return@match null
                bind("top" to count, "cards" to isPlural(count))
            }
            canonical = canonicalForm
        }
        return if (canonicalForm) rule else alternate(rule)
    }

    /**
     * "that card" / "those cards" — what the second sentence of an [impulseRule] calls the pile the
     * first one exiled.
     *
     * The pronoun spellings are alternates rather than rows: "it" and "them" are the same two values
     * and the corpus prints the noun forms three times as often, so one prints and both parse.
     */
    private val exiledCards: Phrase<Boolean> = oneOf(
        "the cards just exiled",
        constant("that card", false),
        constant("those cards", true),
        alternate(constant("it", false)),
        alternate(constant("them", true)),
    )

    /**
     * "Reveal the top four cards of your library. Put all land cards revealed this way into your
     * hand and the rest into your graveyard." — Mulch, Beast Hunt, Chromescale Drake, and
     * `Patterns.Library`'s own `revealTopPutAllMatchingToHand`.
     *
     * The **mandatory** twin of [lookAtTopRevealMatchingToHand], and the pair is a good statement of
     * what this file means by a layer: the two sentences share the count layer and the disposition
     * layer whole, and differ in the one step between them. There the player *may* reveal up to one
     * matching card, which is a `SelectFromCollection`; here every matching card goes to hand with
     * no choice at all, which is a `FilterCollection` partition. English marks the difference with
     * "you may reveal **a**" against "put **all**", and the model marks it with two different step
     * types — so they are two rules over disjoint models rather than one rule with a quantifier slot.
     *
     * It follows that this rule needs **no derived prompt**: nothing pauses, so there is no decision
     * to label, and the field [lookAtTopRevealMatchingToHand] has to invent simply is not in the
     * recipe.
     *
     * The kept pile's destination is frozen at the hand because the facade freezes it, which is the
     * same SDK finding the file KDoc records against `lookAtTopRevealMatchingToHand`: "reveal the top
     * five cards, put all Dinosaur cards **onto the battlefield**" would need `keepDestination` as a
     * parameter, and adding one is `add-feature` work rather than a pipeline hand-built here.
     */
    private val revealTopPutAllMatchingToHand: Phrase<CardScript> = run {
        fun scriptFor(count: DynamicAmount, filter: GameObjectFilter, rest: Disposition) = CardScript(
            spellEffect = Patterns.Library.revealTopPutAllMatchingToHand(
                count = count,
                filter = filter,
                restDestination = rest.destination,
                restOrder = rest.order,
            ),
        )
        phrase(
            "reveal {top} of your library. put all {filter} revealed this way into your hand and " +
                "the rest {rest}",
            name = "reveal the top cards and take every matching card",
        ) {
            slot("top", topCards)
            slot("filter", Filters.pluralCards)
            slot("rest", disposition)
            build { scriptFor(it.value("top"), it.value("filter"), it.value("rest")) }
            match { script ->
                val steps = steps(script) ?: return@match null
                val count = gatheredCount(steps) ?: return@match null
                val filter = partition(steps) ?: return@match null
                val rest = movedTo(steps, "rest") ?: return@match null
                if (script != scriptFor(count, filter, rest)) return@match null
                bind("top" to count, "filter" to filter, "rest" to rest)
            }
        }
    }


    // ---------------------------------------------------------------------------------------
    // Mill — the top of a library, straight into a graveyard
    // ---------------------------------------------------------------------------------------

    /**
     * "Mill two cards." / "Target player mills two cards." — CR 701.17.
     *
     * A mill is this file's pipeline with the middle taken out: `GatherCards(TopOfLibrary(n))` and
     * one `MoveCollection` to the graveyard, no selection step and no second pile, which is why it
     * takes none of the [place] / [disposition] layers above and reads as a family of its own.
     * `Patterns.Library.mill` publishes exactly that recipe.
     *
     * **The `isMill` flag is why this cannot be spelled as a row of the exile family.** The gather
     * carries it, CR 701.17 applies "mill that many plus four instead" at the count site, and the
     * SDK therefore holds a mill and a same-shaped move apart even where the two pipelines otherwise
     * agree — see [exiledTopCount], which refuses a mill for that reason. A rule ignoring the flag
     * would print one as the other.
     *
     * **The miller is a template per row, not a slot**, for [Hand]'s stated reason: English spells
     * it as the sentence's subject and inflects the verb with it — "mill", "target player mills",
     * "each opponent mills" — so the three are three templates, each carrying its own requirement,
     * and the singular and plural counts are two rows apiece because the article and the noun both
     * change ([Steps]' split).
     *
     * "You may mill two cards." needs no row here. "Mill two cards" states no subject of its own, so
     * [Steps]' `you may {inner}` wrapper reaches it the moment this exists — which is what Crawling
     * Infestation's upkeep trigger reads.
     */
    private fun mill(
        template: String,
        name: String,
        count: Int?,
        target: EffectTarget,
        requirements: List<TargetRequirement>,
    ): Phrase<CardScript> {
        fun scriptFor(cards: Int) = CardScript(
            spellEffect = Patterns.Library.mill(cards, target),
            targetRequirements = requirements,
        )
        return phrase(template, name = name) {
            if (count == null) slot("n", Cardinals.word)
            build { bindings -> scriptFor(count ?: bindings.int("n")) }
            match { script ->
                val cards = count ?: milledCount(script) ?: return@match null
                if (count == null && !Cardinals.spellable(cards)) return@match null
                if (script != scriptFor(cards)) return@match null
                bind("n" to cards)
            }
        }
    }

    /** How many cards a `Patterns.Library.mill` pipeline moves, read off its gather step. */
    private fun milledCount(script: CardScript): Int? {
        val gather = (script.spellEffect as? CompositeEffect)?.effects?.firstOrNull()
            as? GatherCardsEffect ?: return null
        val source = gather.source as? CardSource.TopOfLibrary ?: return null
        if (!source.isMill) return null
        return (source.count as? DynamicAmount.Fixed)?.amount
    }

    private val millClauses: List<Phrase<CardScript>> = listOf(
        mill(
            "mill a card", "mill a card",
            count = 1, target = EffectTarget.Controller, requirements = emptyList(),
        ),
        mill(
            "mill {n} cards", "mill cards",
            count = null, target = EffectTarget.Controller, requirements = emptyList(),
        ),
        mill(
            "target player mills a card", "target player mills a card",
            count = 1, target = Targets.bound(), requirements = listOf(Targets.player()),
        ),
        mill(
            "target player mills {n} cards", "target player mills cards",
            count = null, target = Targets.bound(), requirements = listOf(Targets.player()),
        ),
        mill(
            "each opponent mills a card", "each opponent mills a card",
            count = 1, target = EffectTarget.PlayerRef(Player.EachOpponent), requirements = emptyList(),
        ),
        mill(
            "each opponent mills {n} cards", "each opponent mills cards",
            count = null, target = EffectTarget.PlayerRef(Player.EachOpponent), requirements = emptyList(),
        ),
    )

    val clauses: List<Phrase<CardScript>> = listOf(
        lookAtTopRevealMatchingToHand,
        lookAtTopAndMayTakeMatching,
        lookAtTopAndTakeMatchingCounted,
        revealTopPutAllMatchingToHand,
        lookAtTopAndKeepAllButOne,
        lookAtTopAndKeep,
        exileTop,
        // Each duration is printed in both orders, and **which order is canonical is decided by the
        // duration** — see [impulseRule]'s KDoc. Trailing for this turn, fronted for the two that
        // cross one.
        impulseRule(
            "exile {top} of your library. you may play {cards} this turn",
            "exile the top cards and play them this turn",
            MayPlayExpiry.EndOfTurn,
        ),
        impulseRule(
            "exile {top} of your library. until end of turn, you may play {cards}",
            "exile the top cards and play them until end of turn (fronted)",
            MayPlayExpiry.EndOfTurn,
            canonicalForm = false,
        ),
        impulseRule(
            "exile {top} of your library. until the end of your next turn, you may play {cards}",
            "exile the top cards and play them until the end of your next turn",
            MayPlayExpiry.UntilEndOfNextTurn,
        ),
        impulseRule(
            "exile {top} of your library. you may play {cards} until the end of your next turn",
            "exile the top cards and play them until the end of your next turn (trailing)",
            MayPlayExpiry.UntilEndOfNextTurn,
            canonicalForm = false,
        ),
        impulseRule(
            "exile {top} of your library. until your next end step, you may play {cards}",
            "exile the top cards and play them until your next end step",
            MayPlayExpiry.UntilNextEndStep,
        ),
        impulseRule(
            "exile {top} of your library. you may play {cards} until your next end step",
            "exile the top cards and play them until your next end step (trailing)",
            MayPlayExpiry.UntilNextEndStep,
            canonicalForm = false,
        ),
        // "…for as long as it remains exiled" — the SDK's own gloss on `MayPlayExpiry.Permanent`,
        // word for word. The trailing "it" is a literal rather than a second anaphor slot: the
        // clause is only ever printed about one card, and a slot there would let the two pronouns
        // in one sentence disagree.
        impulseRule(
            "exile {top} of your library. you may play {cards} for as long as it remains exiled",
            "exile the top card and play it for as long as it remains exiled",
            MayPlayExpiry.Permanent,
        ),
    ) + millClauses
}
