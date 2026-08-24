package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Clauses about a library — looking at the top of one, searching one, shuffling one.
 *
 * These are the first rules whose model is a **`Patterns` composition** rather than a single
 * effect: "Search your library for a card, then shuffle and put that card on top." is four pipeline
 * steps with string slot keys, and the SDK publishes the recipe as `Patterns.Library.searchLibrary`.
 * The rules therefore `build` through that facade and `match` by reconstructing it and comparing —
 * exactly the discipline the atomic rules follow, and the reason a card whose pipeline differs by a
 * single field declines instead of printing this sentence.
 *
 * That is also why the printed sentence and the model are so far apart here. The clause is one
 * English sentence and the value is a `CompositeEffect` of four steps, so nothing about the shape of
 * the model suggests where the sentence's boundaries are; only the recipe does. [Steps.sequence]
 * never decomposes one of these, because its own split is over the *outer* composite and none of
 * these inner steps has a clause rule of its own.
 *
 * **What is *searched* lives here; what comes off the **top** lives in [TopOfLibrary].** The split
 * is by pipeline rather than by topic: a search gathers `FromZone` by filter and shuffles, while a
 * look/reveal/exile gathers `TopOfLibrary` by count and decides where two piles go. They share this
 * file's discipline and nothing else, and keeping the top-of-library family separate is what let it
 * become a layered vocabulary instead of the one-rule-per-printed-card list it used to be — see that
 * file's KDoc for what the frozen destinations were costing.
 */
object Library {

    /**
     * "Look at the top three cards of your library, then put them back in any order." — Omen.
     *
     * The count is [Cardinals.word] rather than a numeral: Oracle spells a quantity of *cards* as a
     * word, the convention [Steps] takes both leaves for.
     */
    private val lookAtTopAndReorder: Phrase<CardScript> = run {
        fun scriptFor(count: Int) = CardScript(spellEffect = Patterns.Library.lookAtTopAndReorder(count))
        phrase(
            "look at the top {n} cards of your library, then put them back in any order",
            name = "look at the top cards and reorder",
        ) {
            slot("n", Cardinals.word)
            build { scriptFor(it.int("n")) }
            match { script ->
                val count = topOfLibraryCount(script) ?: return@match null
                if (!Cardinals.spellable(count) || script != scriptFor(count)) return@match null
                bind("n" to count)
            }
        }
    }

    /** The number of cards a `lookAtTopAndReorder` pipeline gathers, or null for any other value. */
    private fun topOfLibraryCount(script: CardScript): Int? {
        val gather = (script.spellEffect as? CompositeEffect)
            ?.effects?.firstOrNull() as? GatherCardsEffect ?: return null
        val source = gather.source as? CardSource.TopOfLibrary ?: return null
        return (source.count as? DynamicAmount.Fixed)?.amount
    }

    /**
     * "Search your library for a card, then shuffle and put that card on top." — Cruel Tutor.
     *
     * The destination is spelled by the template rather than by a slot, because each destination is
     * a different English sentence ("put it into your hand", "onto the battlefield") rather than a
     * different word in this one — the same argument [Steps.pumpTargetPermanent] makes about
     * durations. `filter` is `Any`, which is what "a card" means; a filtered search is a different
     * noun phrase and a rule this one does not claim.
     */
    private val searchForACardToTop: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                destination = SearchDestination.TOP_OF_LIBRARY,
            )
        )
        phrase(
            "search your library for a card, then shuffle and put that card on top",
            name = "search your library and put a card on top",
        ) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "You may shuffle." — the optional shuffle Omen ends on.
     *
     * `MayEffect` is the SDK's spelling of a player-chosen action inside a spell's effect. Note the
     * deliberate asymmetry with [Triggers]: on a *triggered ability* the same English lowers to the
     * ability's `optional` flag instead, because that is the field the hand-written cards set and
     * the one the trigger's own sentence introduces. Two SDK spellings of "you may", each canonical
     * in the sentence context that owns it, and [Triggers.abilityFor] is the lowering between them.
     */
    private val mayShuffle: Phrase<CardScript> = run {
        val script = CardScript(spellEffect = MayEffect(ShuffleLibraryEffect()))
        phrase("you may shuffle", name = "you may shuffle") {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "Look at the top five cards of target opponent's library. Put one of those cards into that
     * player's graveyard and the rest on top of their library in any order." — Cruel Fate.
     *
     * **Two printed sentences, one rule**, and the only rule in the grammar shaped that way. The
     * model is a four-step gather / select / move / move pipeline whose steps do not line up with
     * the sentence boundary at all — the second sentence is three of the four steps, and the first
     * step is what makes the second sentence's "those cards" mean anything. [Steps.sequence] splits
     * a line where the *model* is a composite of clause-sized effects; here it is not, so splitting
     * the text would produce two halves neither of which denotes anything.
     *
     * It unlocks one card, which the module's own rule says needs a stated reason: the reason is
     * that the alternative is not a smaller rule but a wrong one. The prompt labels are part of the
     * recipe rather than of the text, so they are reproduced from the card and compared by the
     * equality below like every other field.
     */
    private val lookAtOpponentTopAndBury: Phrase<CardScript> = run {
        fun scriptFor(count: Int) = CardScript(
            spellEffect = CompositeEffect(
                listOf(
                    GatherCardsEffect(
                        CardSource.TopOfLibrary(DynamicAmount.Fixed(count), Player.TargetOpponent),
                        storeAs = "looked",
                    ),
                    SelectFromCollectionEffect(
                        from = "looked",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                        storeSelected = "toGraveyard",
                        storeRemainder = "toTop",
                        selectedLabel = "Put in graveyard",
                        remainderLabel = "Put on top",
                    ),
                    MoveCollectionEffect(
                        from = "toGraveyard",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.TargetOpponent),
                    ),
                    MoveCollectionEffect(
                        from = "toTop",
                        destination = CardDestination.ToZone(Zone.LIBRARY, Player.TargetOpponent, ZonePlacement.Top),
                        order = CardOrder.ControllerChooses,
                    ),
                )
            ),
            targetRequirements = listOf(Targets.opponent()),
        )
        phrase(
            "look at the top {n} cards of target opponent's library. put one of those cards into " +
                "that player's graveyard and the rest on top of their library in any order",
            name = "look at an opponent's top cards and bury one",
        ) {
            slot("n", Cardinals.word)
            build { scriptFor(it.int("n")) }
            match { script ->
                val count = topOfLibraryCount(script) ?: return@match null
                if (!Cardinals.spellable(count) || script != scriptFor(count)) return@match null
                bind("n" to count)
            }
        }
    }

    /**
     * "Search your library for a Forest card, put that card onto the battlefield, then shuffle." —
     * the filtered searches, which are one recipe with one slot.
     *
     * `Patterns.Library.searchLibrary`'s other parameters are spelled by the *template* rather than
     * by slots, for the reason [Steps.pumpTargetPermanent] spells its duration: each destination and
     * each reveal is a different English sentence, not a different word in one. So the family is a
     * row per sentence over one shared shape, and the noun phrase goes through [Filters.indefinite]
     * so the article agrees with whatever type the card names.
     *
     * "…put **that card** onto the battlefield" and "…put **it** onto the battlefield" are two
     * printed forms of one recipe — Nature's Lore prints the first, Natural Order the second — so
     * one is canonical and the other parses without printing, and a card using the pronoun comes
     * back as a variant.
     */
    private fun search(
        template: String,
        name: String,
        canonicalForm: Boolean = true,
        destination: SearchDestination,
        reveal: Boolean = false,
    ): Phrase<CardScript> {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = Patterns.Library.searchLibrary(
                filter = filter,
                destination = destination,
                reveal = reveal,
            )
        )
        val rule = phrase<CardScript>(template, name = name) {
            slot("filter", Filters.indefiniteCard)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val filter = searchedFilter(script) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
            canonical = canonicalForm
        }
        return if (canonicalForm) rule else alternate(rule)
    }

    /** The filter a `searchLibrary` pipeline looks for, read off its gather step. */
    private fun searchedFilter(script: CardScript): GameObjectFilter? {
        val gather = (script.spellEffect as? CompositeEffect)?.effects?.firstOrNull() as? GatherCardsEffect
            ?: return null
        return (gather.source as? CardSource.FromZone)?.filter
    }

    /**
     * "If an opponent controls more lands than you, search your library for up to three Plains
     * cards, reveal them, put them into your hand, then shuffle." — Gift of Estates.
     *
     * The counted search, which is a different sentence from the singular one in every clause after
     * the noun: "up to three … cards, reveal **them**, put **them** into your hand". The condition in
     * front of it is [Steps]' conditional wrapper, so this rule is only the search half.
     */
    private val searchUpToNToHand: Phrase<CardScript> = run {
        fun scriptFor(count: Int, filter: GameObjectFilter) = CardScript(
            spellEffect = Patterns.Library.searchLibrary(
                filter = filter,
                count = count,
                destination = SearchDestination.HAND,
                reveal = true,
            )
        )
        phrase(
            "search your library for up to {n} {filter}, reveal them, put them into your hand, then shuffle",
            name = "search your library for several cards",
        ) {
            slot("n", Cardinals.word)
            // The card noun, not [Filters.plural]: Oracle inflects the head noun and leaves the type
            // phrase in front of it singular, so "creature cards" — never "creatures cards", which is
            // what this rule printed while the noun was in its own template.
            slot("filter", Filters.pluralCards)
            build { scriptFor(it.int("n"), it.value("filter")) }
            match { script ->
                val filter = searchedFilter(script) ?: return@match null
                val count = searchedCount(script) ?: return@match null
                if (!Cardinals.spellable(count) || script != scriptFor(count, filter)) return@match null
                bind("n" to count, "filter" to filter)
            }
        }
    }

    /** How many cards a `searchLibrary` pipeline selects, read off its select step. */
    private fun searchedCount(script: CardScript): Int? {
        val select = (script.spellEffect as? CompositeEffect)?.effects
            ?.filterIsInstance<SelectFromCollectionEffect>()?.firstOrNull() ?: return null
        val mode = select.selection as? SelectionMode.ChooseUpTo ?: return null
        return (mode.count as? DynamicAmount.Fixed)?.amount
    }

    /**
     * "Search your library for a Zombie card and a Swamp card, reveal them, put them into your
     * hand, then shuffle." — Corpse Harvester.
     *
     * Two searches in one sentence, and the model is two whole recipes in a row: only the *second*
     * shuffles, because the sentence's "then shuffle" is one shuffle at the end. That asymmetry is
     * the reason this is a rule rather than a [Steps.sequence] of two searches — neither half is the
     * sentence the singular rule spells.
     */
    private val searchForTwoCardsToHand: Phrase<CardScript> = run {
        fun scriptFor(first: GameObjectFilter, second: GameObjectFilter) = CardScript(
            spellEffect = Patterns.Library.searchLibrary(
                filter = first,
                reveal = true,
                shuffleAfter = false,
            ) then Patterns.Library.searchLibrary(
                filter = second,
                reveal = true,
                shuffleAfter = true,
            )
        )
        phrase(
            "search your library for {first} and {second}, reveal them, put them into " +
                "your hand, then shuffle",
            name = "search your library for two cards",
        ) {
            slot("first", Filters.indefiniteCard)
            slot("second", Filters.indefiniteCard)
            build { scriptFor(it.value("first"), it.value("second")) }
            match { script ->
                // `then` splices the *left* recipe's steps and appends the right one whole, so the
                // model is one flat run ending in the second recipe rather than a pair of them.
                // Reading it back that way is what keeps the reconstruction below a comparison.
                val parts = (script.spellEffect as? CompositeEffect)?.effects ?: return@match null
                if (parts.size < 2) return@match null
                val first = searchedFilterIn(CompositeEffect(parts.dropLast(1))) ?: return@match null
                val second = searchedFilterIn(parts.last()) ?: return@match null
                if (script != scriptFor(first, second)) return@match null
                bind("first" to first, "second" to second)
            }
        }
    }

    /**
     * "Search your graveyard, hand, and/or library for a card named Scion of Darkness and put it
     * onto the battlefield. If you search your library this way, shuffle." — Dark Supplicant.
     *
     * The zone list is a literal because `searchMultipleZones` takes an ordered list and Oracle
     * spells exactly one order for it; the second sentence restates what the recipe already does
     * when a library is among the zones, so it is a literal too. What varies is the *name*, which is
     * why [Primitives.cardName] exists.
     */
    private val searchZonesForNamedCard: Phrase<CardScript> = run {
        fun scriptFor(name: String) = CardScript(
            spellEffect = Patterns.Library.searchMultipleZones(
                zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                filter = GameObjectFilter.Any.named(name),
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
            )
        )
        phrase(
            "search your graveyard, hand, and/or library for a card named {name} and put it onto " +
                "the battlefield. if you search your library this way, shuffle",
            name = "search several zones for a named card",
        ) {
            slot("name", Primitives.cardName)
            build { scriptFor(it.text("name")) }
            match { script ->
                val gather = (script.spellEffect as? CompositeEffect)?.effects?.firstOrNull()
                    as? GatherCardsEffect ?: return@match null
                val filter = (gather.source as? CardSource.FromMultipleZones)?.filter ?: return@match null
                val named = filter.cardPredicates.filterIsInstance<CardPredicate.NameEquals>()
                    .singleOrNull()?.name ?: return@match null
                if (script != scriptFor(named)) return@match null
                bind("name" to named)
            }
        }
    }

    /** The filter one `searchLibrary` recipe looks for, given the recipe itself. */
    private fun searchedFilterIn(effect: Effect): GameObjectFilter? {
        val gather = (effect as? CompositeEffect)?.effects?.firstOrNull() as? GatherCardsEffect ?: return null
        return (gather.source as? CardSource.FromZone)?.filter
    }

    /**
     * "Reveal the top card of your library. If it's a Goblin permanent card, put it onto the
     * battlefield. Otherwise, put it into your graveyard." — Skirk Drill Sergeant.
     *
     * Three printed sentences and one recipe. The model is a gather with `revealed`, one selection
     * that splits the revealed card into a matching and a remaining slot, and a move per slot — so
     * "otherwise" is the *remainder* of a filter rather than a branch, and every sentence after the
     * first reads a slot the gather created. Splitting the text would produce halves that name
     * nothing.
     */
    private val revealTopAndSplit: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = CompositeEffect(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                        storeAs = "revealed",
                        revealed = true,
                    ),
                    SelectFromCollectionEffect(
                        from = "revealed",
                        selection = SelectionMode.All,
                        filter = filter,
                        storeSelected = "goblin",
                        storeRemainder = "nonGoblin",
                    ),
                    MoveCollectionEffect(from = "goblin", destination = CardDestination.ToZone(Zone.BATTLEFIELD)),
                    MoveCollectionEffect(from = "nonGoblin", destination = CardDestination.ToZone(Zone.GRAVEYARD)),
                )
            )
        )
        phrase(
            "reveal the top card of your library. if it's {filter}, put it onto the " +
                "battlefield. otherwise, put it into your graveyard",
            name = "reveal the top card and sort it",
        ) {
            slot("filter", Filters.indefiniteCard)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val steps = (script.spellEffect as? CompositeEffect)?.effects ?: return@match null
                val select = steps.getOrNull(1) as? SelectFromCollectionEffect ?: return@match null
                val filter = select.filter
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    val clauses: List<Phrase<CardScript>> = listOf(
        revealTopAndSplit,
        lookAtTopAndReorder,
        searchForACardToTop,
        searchUpToNToHand,
        mayShuffle,
        lookAtOpponentTopAndBury,
        search(
            "search your library for {filter}, put that card onto the battlefield, then shuffle",
            "search your library for a card to the battlefield",
            destination = SearchDestination.BATTLEFIELD,
        ),
        search(
            "search your library for {filter}, put it onto the battlefield, then shuffle",
            "search your library for a card to the battlefield (pronoun)",
            canonicalForm = false,
            destination = SearchDestination.BATTLEFIELD,
        ),
        search(
            "search your library for {filter}, reveal it, then shuffle and put that card on top",
            "search your library for a card, revealed, to the top",
            destination = SearchDestination.TOP_OF_LIBRARY,
            reveal = true,
        ),
        search(
            "search your library for {filter}, put it into your hand, then shuffle",
            "search your library for a card to your hand",
            destination = SearchDestination.HAND,
        ),
        search(
            "search your library for {filter}, reveal it, put it into your hand, then shuffle",
            "search your library for a card, revealed, to your hand",
            destination = SearchDestination.HAND,
            reveal = true,
        ),
        searchForTwoCardsToHand,
        searchZonesForNamedCard,
    )
}
