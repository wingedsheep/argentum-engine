package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.util.numberToWord
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Player-facing wording for a library search, shared by the search pick's decision prompt
 * (`Patterns.Library.searchLibrary` and friends) and the search pipeline's rules text
 * ([describeSteps]) so the two never spell one search two ways.
 */
internal object SearchText {

    /** "a basic land card", "up to two creature cards", "creature cards (up to X)". */
    fun counted(filter: GameObjectFilter, count: DynamicAmount): String =
        when (val fixed = (count as? DynamicAmount.Fixed)?.amount) {
            1 -> noun(filter, plural = false).let { "${if (it.first().lowercaseChar() in "aeiou") "an" else "a"} $it" }
            null -> "${noun(filter, plural = true)} (up to ${count.description})"
            else -> "up to ${numberToWord(fixed)} ${noun(filter, plural = true)}"
        }

    /**
     * [filter]'s description as a card noun: "basic land card", "creature card with power 2 or
     * less", "card named Avarax" — the word "card" goes ahead of the first qualifying clause.
     */
    fun noun(filter: GameObjectFilter, plural: Boolean): String {
        val description = filter.description.trim()
        val qualifiers = listOf("with ", "named ", "that ", "whose ", "has ")
        val split = qualifiers.mapNotNull { q ->
            if (description.startsWith(q)) 0 else description.indexOf(" $q").takeIf { it >= 0 }
        }.minOrNull() ?: description.length
        val head = description.substring(0, split).trim().let { if (it == "card") "" else it.removeSuffix(" card") }
        val tail = description.substring(split).trim()
        return listOf(head, if (plural) "cards" else "card", tail).filter { it.isNotEmpty() }.joinToString(" ")
    }

    /** Where found cards go: "into your hand", "onto the battlefield tapped", "on top of your library". */
    fun destination(zone: Zone, placement: ZonePlacement): String? = when (zone) {
        Zone.HAND -> "into your hand"
        Zone.BATTLEFIELD -> if (placement == ZonePlacement.Tapped) "onto the battlefield tapped" else "onto the battlefield"
        Zone.GRAVEYARD -> "into your graveyard"
        Zone.LIBRARY -> if (placement == ZonePlacement.Top) "on top of your library" else null
        else -> null
    }

    /** The searched zones as a possessive phrase: "your library", "your graveyard, hand, and/or library". */
    fun zones(player: Player, zones: List<Zone>): String =
        "${player.possessive} " + zones.joinToString(" and/or ") { it.name.lowercase() }
}

/**
 * The rules text of a run of pipeline steps, joined into sentences.
 *
 * Each step describes itself, but some runs only read as English when described *together*:
 *
 *  - **A library search** — `gather(search = true)` → pick → move (→ shuffle) — is one sentence in
 *    Oracle ("Search your library for a basic Island, Swamp, or Mountain card, put it onto the
 *    battlefield tapped, then shuffle"), not four ("Look at …. Choose up to 1 of those cards. …").
 *  - **A discard** — gather a hand → its owner picks → move to their graveyard as a discard — is
 *    "Discard a card" / "Target player discards two cards", not "Look at cards in that player's
 *    hand. Choose exactly 2 of those cards. Put those cards into a graveyard" (which also reads as
 *    if *you* chose).
 *  - **A reveal from hand** — gather a hand → its owner picks → reveal the picks — is "Target
 *    player reveals two cards from their hand".
 *  - **A whole zone moved** — gather a graveyard → move it — is "Exile all cards from target
 *    player's graveyard"; **the targets moved** — gather the chosen targets → move them — is
 *    "Return each target to its owner's hand".
 *  - **A battlefield gather** feeding the next step is that step's object: "Put a +1/+1 counter on
 *    each Frog … you control that entered the battlefield this turn", not "Look at … in your
 *    battlefield. Put … on each of those permanents".
 *
 * Some steps also read differently after the step before them: a gate on the collection the
 * previous move recorded is "If you do, …"; a gate on a target the previous sentence named calls it
 * "that creature"; a player already named becomes "that player"; and a single picked card is
 * "that card", not "those cards".
 *
 * A run that doesn't match any shape exactly falls back to the per-step text, and steps with no
 * text of their own (the library-searched event marker) are skipped rather than joined as empty
 * sentences. [render] is the per-step text — `description` or a runtime description.
 */
internal fun describeSteps(effects: List<Effect>, render: (Effect) -> String): String {
    val sentences = mutableListOf<String>()
    val namedPlayers = mutableSetOf<Player>()
    var i = 0
    while (i < effects.size) {
        val fused = fuseSearch(effects, i)
            ?: fuseDiscard(effects, i, namedPlayers)
            ?: fuseRevealFromHand(effects, i, namedPlayers)
            ?: fuseZoneMove(effects, i)
            ?: fuseTargetsMove(effects, i)
            ?: fuseBattlefieldGather(effects, i, render)
        if (fused != null) {
            sentences += fused.first
            i += fused.second
            continue
        }
        val previous = effects.getOrNull(i - 1)
        val text = discardMoveText(effects[i], previous, namedPlayers)
            ?: ifYouDoText(effects[i], previous, render)
            ?: render(effects[i]).let { thatTarget(effects[i], it, sentences) }.let { singularPick(effects[i], previous, it) }
        text.takeIf { it.isNotBlank() }?.let { sentences += it }
        // A player whose zone a step looked at is "that player" in the steps after it.
        ((effects[i] as? GatherCardsEffect)?.source as? CardSource.FromZone)?.let { namedPlayers += it.player }
        i++
    }
    return sentences.joinToString(". ")
}

/** Who chooses for a step over [owner]'s cards is [owner] themself — the subject of "[owner] discards". */
private fun ownerChooses(owner: Player, chooser: Chooser): Boolean = when (chooser) {
    Chooser.Controller, Chooser.SourceController -> owner == Player.You || owner == Player.ControllerOfSource
    Chooser.ControllerOfSelection -> true
    Chooser.TargetPlayer -> owner == Player.TargetPlayer || owner == Player.TargetOpponent ||
        owner is Player.ContextPlayer || owner is Player.BoundVariable
    Chooser.TriggeringPlayer -> owner == Player.TriggeringPlayer
    Chooser.DefendingPlayer -> owner == Player.DefendingPlayer
    Chooser.ChosenOpponent -> owner == Player.ChosenOpponent
    Chooser.ControllerOfTriggeringEntity -> owner == Player.ControllerOfTriggeringEntity
    Chooser.Opponent, Chooser.ControllerOfTarget -> false
}

/** The sentence subject for [player] acting: null for "you" (an imperative), else "Target player" / "That player". */
private fun subject(player: Player, namedPlayers: Set<Player>): String? = when {
    player == Player.You || player == Player.ControllerOfSource -> null
    player in namedPlayers -> "That player"
    else -> player.description.replaceFirstChar { it.uppercaseChar() }
}

/** "Discard two cards" / "Target player discards two cards" — the verb agreeing with [subject]. */
private fun clause(subject: String?, verb: String, rest: String): String =
    if (subject == null) "${verb.replaceFirstChar { it.uppercaseChar() }} $rest" else "$subject ${verb}s $rest"

/** "your hand" for you, "their hand" for anyone else. */
private fun handOf(player: Player): String =
    if (player == Player.You || player == Player.ControllerOfSource) "your hand" else "their hand"

/** "a card", "two creature cards", "X cards", or null for a count that isn't a number or X. */
private fun countedCards(filter: GameObjectFilter, count: DynamicAmount): String? =
    when (val fixed = (count as? DynamicAmount.Fixed)?.amount) {
        1 -> SearchText.noun(filter, plural = false).let { "${if (it.first().lowercaseChar() in "aeiou") "an" else "a"} $it" }
        null -> if (count.description == "X") "X ${SearchText.noun(filter, plural = true)}" else null
        else -> "${numberToWord(fixed)} ${SearchText.noun(filter, plural = true)}"
    }

/**
 * "Target player reveals two cards from their hand" — a hand gathered, its owner picks, the picks
 * revealed (Thieving Sprite). The cards are named by the owner, so the owner is the subject.
 */
private fun fuseRevealFromHand(effects: List<Effect>, start: Int, namedPlayers: MutableSet<Player>): Pair<String, Int>? {
    val gather = effects[start] as? GatherCardsEffect ?: return null
    if (gather.search || gather.revealed) return null
    val source = gather.source as? CardSource.FromZone ?: return null
    if (source.zone != Zone.HAND || source.excludeSacrificedThisWay) return null
    val select = effects.getOrNull(start + 1) as? SelectFromCollectionEffect ?: return null
    if (select.from != gather.storeAs || !ownerChooses(source.player, select.chooser) ||
        select.filter != GameObjectFilter.Any || select.restrictions.isNotEmpty() || select.matchChosenCreatureType
    ) return null
    val count = (select.selection as? SelectionMode.ChooseExactly)?.count ?: return null
    val reveal = effects.getOrNull(start + 2) as? RevealCollectionEffect ?: return null
    if (reveal.from != select.storeSelected) return null

    val who = subject(source.player, namedPlayers)
    namedPlayers += source.player
    val hand = handOf(source.player)
    val text = countedCards(source.filter, count)?.let { clause(who, "reveal", "$it from $hand") }
        ?: clause(who, "reveal", "a number of ${SearchText.noun(source.filter, plural = true)} from $hand equal to ${count.description}")
    return text to 3
}

/**
 * "Exile all cards from target player's graveyard" — a whole non-battlefield zone gathered and
 * moved somewhere as one (Tormod's Crypt), with no pick in between.
 */
private fun fuseZoneMove(effects: List<Effect>, start: Int): Pair<String, Int>? {
    val gather = effects[start] as? GatherCardsEffect ?: return null
    if (gather.search || gather.revealed) return null
    val source = gather.source as? CardSource.FromZone ?: return null
    if (source.zone !in setOf(Zone.GRAVEYARD, Zone.HAND) || source.excludeSacrificedThisWay) return null
    val move = effects.getOrNull(start + 1) as? MoveCollectionEffect ?: return null
    if (move.from != gather.storeAs || move.revealed || !move.isPlainMove()) return null
    val to = move.destination as? CardDestination.ToZone ?: return null
    if (to.placement != ZonePlacement.Default || to.zone == source.zone) return null

    val cards = "all ${SearchText.noun(source.filter, plural = true)} from ${source.player.possessive} ${source.zone.name.lowercase()}"
    val returning = source.zone != Zone.HAND
    val text = when (to.zone) {
        Zone.EXILE -> "Exile $cards"
        Zone.HAND -> "${if (returning) "Return" else "Put"} $cards ${if (returning) "to" else "into"} ${to.player.possessive} hand"
        Zone.GRAVEYARD -> "Put $cards into ${to.player.possessive} graveyard"
        Zone.BATTLEFIELD -> "${if (returning) "Return" else "Put"} $cards ${if (returning) "to" else "onto"} the battlefield"
        else -> return null
    }
    return text to 2
}

/**
 * "Return each target to its owner's hand" — the chosen targets gathered and moved as one
 * (Mirkwood Nurturer), instead of "Look at the targets. Put those cards into a hand".
 */
private fun fuseTargetsMove(effects: List<Effect>, start: Int): Pair<String, Int>? {
    val gather = effects[start] as? GatherCardsEffect ?: return null
    if (gather.search || gather.revealed || gather.source != CardSource.ChosenTargets) return null
    val move = effects.getOrNull(start + 1) as? MoveCollectionEffect ?: return null
    if (move.from != gather.storeAs || move.revealed) return null
    val to = move.destination as? CardDestination.ToZone ?: return null
    val text = when {
        move.isPlainMove(MoveType.Destroy) && to.zone == Zone.GRAVEYARD -> "Destroy each target"
        move.isPlainMove(MoveType.Sacrifice) && to.zone == Zone.GRAVEYARD -> "Sacrifice each target"
        !move.isPlainMove() -> return null
        to.zone == Zone.HAND -> "Return each target to its owner's hand"
        to.zone == Zone.EXILE -> "Exile each target"
        to.zone == Zone.GRAVEYARD -> "Put each target into its owner's graveyard"
        to.zone == Zone.LIBRARY && to.placement == ZonePlacement.Top -> "Put each target on top of its owner's library"
        to.zone == Zone.LIBRARY && to.placement == ZonePlacement.Bottom -> "Put each target on the bottom of its owner's library"
        else -> return null
    }
    return text to 2
}

/**
 * A discard move on its own, after a pick: "That player discards that card" (Thieving Sprite),
 * not "Put those cards into a graveyard".
 */
private fun discardMoveText(step: Effect, previous: Effect?, namedPlayers: Set<Player>): String? {
    val move = step as? MoveCollectionEffect ?: return null
    if (move.revealed || !move.isPlainMove(MoveType.Discard)) return null
    val to = move.destination as? CardDestination.ToZone ?: return null
    if (to.zone != Zone.GRAVEYARD) return null
    return clause(subject(to.player, namedPlayers), "discard", if (isSinglePick(previous, move.from)) "that card" else "those cards")
}

/** [previous] picked exactly one card into [collection]. */
private fun isSinglePick(previous: Effect?, collection: String): Boolean {
    val select = previous as? SelectFromCollectionEffect ?: return false
    if (select.storeSelected != collection) return false
    return ((select.selection as? SelectionMode.ChooseExactly)?.count as? DynamicAmount.Fixed)?.amount == 1
}

/** A move of the one card just picked is a move of "that card". */
private fun singularPick(step: Effect, previous: Effect?, text: String): String {
    val move = step as? MoveCollectionEffect ?: return text
    return if (isSinglePick(previous, move.from)) text.replaceFirst("those cards", "that card") else text
}

/**
 * "If you do, put a +1/+1 counter on this creature" — a state gate on whether the move just before
 * it moved anything (`whenMatches(moved)` with no filter), not "If those cards contain a card, …".
 */
private fun ifYouDoText(step: Effect, previous: Effect?, render: (Effect) -> String): String? {
    val gated = step as? GatedEffect ?: return null
    if (gated.descriptionOverride != null) return null
    val condition = (gated.gate as? Gate.WhenCondition)?.condition as? CollectionContainsMatch ?: return null
    if (condition.filter != GameObjectFilter.Any) return null
    val move = previous as? MoveCollectionEffect ?: return null
    if (move.storeMovedAs == null || move.storeMovedAs != condition.collection) return null
    return buildString {
        append("If you do, ${render(gated.then).replaceFirstChar { it.lowercaseChar() }}")
        gated.otherwise?.let(render)?.takeIf { it.isNotBlank() }?.let {
            append(". If you don't, ${it.replaceFirstChar { c -> c.lowercaseChar() }}")
        }
    }
}

/**
 * A gate testing a target the earlier sentences already named calls it "that creature" (Little
 * Bear: "Untap another target creature you control. If that creature is a Bear creature, …"),
 * rather than repeating the whole target phrase as though it were a second target.
 */
private fun thatTarget(step: Effect, text: String, earlier: List<String>): String {
    val gated = step as? GatedEffect ?: return text
    val entity = ((gated.gate as? Gate.WhenCondition)?.condition as? EntityMatches)?.entity ?: return text
    if (entity !is EffectTarget.BoundVariable && entity !is EffectTarget.ContextTarget) return text
    val phrase = entity.description
    val noun = targetNoun(phrase) ?: return text
    if (earlier.none { phrase in it } || phrase !in text) return text
    return text.replaceFirst(phrase, "that $noun")
}

/** "another target creature you control" → "creature"; null when the phrase names no target noun. */
internal fun targetNoun(phrase: String): String? =
    phrase.substringAfter("target ", missingDelimiterValue = "").substringBefore(' ').takeIf { it.isNotEmpty() }

/** "Search your library for …, [reveal it,] put it …[, then shuffle]" — the sentence and steps consumed. */
private fun fuseSearch(effects: List<Effect>, start: Int): Pair<String, Int>? {
    val gather = effects[start] as? GatherCardsEffect ?: return null
    if (!gather.search || gather.revealed) return null
    val (player, zones, filter) = when (val source = gather.source) {
        is CardSource.FromZone -> Triple(source.player, listOf(source.zone), source.filter)
        is CardSource.FromMultipleZones -> Triple(source.player, source.zones, source.filter)
        else -> return null
    }
    if ((gather.source as? CardSource.FromZone)?.excludeSacrificedThisWay == true) return null
    val select = effects.getOrNull(start + 1) as? SelectFromCollectionEffect ?: return null
    if (select.from != gather.storeAs || select.chooser != Chooser.Controller ||
        select.filter != GameObjectFilter.Any || select.restrictions.isNotEmpty() || select.matchChosenCreatureType
    ) return null
    val count = (select.selection as? SelectionMode.ChooseUpTo)?.count ?: return null

    var consumed = 2
    var shuffleFirst = false
    val maybeShuffle = effects.getOrNull(start + consumed)
    if (maybeShuffle is ShuffleLibraryEffect && maybeShuffle.target == EffectTarget.Controller) {
        shuffleFirst = true
        consumed++
    }
    val move = effects.getOrNull(start + consumed) as? MoveCollectionEffect ?: return null
    if (move.from != select.storeSelected || !move.isPlainMove()) return null
    val toZone = move.destination as? CardDestination.ToZone ?: return null
    if (toZone.player != Player.You) return null
    val where = SearchText.destination(toZone.zone, toZone.placement) ?: return null
    consumed++
    var shuffleAfter = false
    if (!shuffleFirst) {
        val after = effects.getOrNull(start + consumed)
        if (after is ShuffleLibraryEffect && after.target == EffectTarget.Controller) {
            shuffleAfter = true
            consumed++
        }
    }
    // The searched-event marker has no text; fold it in so it can't split the sentence.
    if (effects.getOrNull(start + consumed) == EmitLibrarySearchedEventEffect) consumed++

    val single = (count as? DynamicAmount.Fixed)?.amount == 1
    val pronoun = if (single) "it" else "them"
    val text = buildString {
        append("Search ${SearchText.zones(player, zones)} for ${SearchText.counted(filter, count)}")
        if (shuffleFirst) {
            append(", then shuffle and put ${if (single) "that card" else "those cards"} $where")
        } else {
            if (move.revealed) append(", reveal $pronoun")
            append(", put $pronoun $where")
            if (shuffleAfter) append(", then shuffle")
        }
    }
    return text to consumed
}

/**
 * "Discard a card" / "Target player discards two cards" — `Patterns.Hand.discardCards` /
 * `discardAnyNumber` / a random discard: a hand gathered (optionally filtered), its owner picks,
 * the picks go to that player's graveyard as a discard. The discarding player is the subject; a
 * pick made by someone *other* than the hand's owner keeps the per-step text, since "X discards
 * …" would hide who chooses.
 */
private fun fuseDiscard(effects: List<Effect>, start: Int, namedPlayers: MutableSet<Player>): Pair<String, Int>? {
    val gather = effects[start] as? GatherCardsEffect ?: return null
    if (gather.search || gather.revealed) return null
    val source = gather.source as? CardSource.FromZone ?: return null
    if (source.zone != Zone.HAND || source.excludeSacrificedThisWay) return null
    val owner = source.player
    val select = effects.getOrNull(start + 1) as? SelectFromCollectionEffect ?: return null
    if (select.from != gather.storeAs || !ownerChooses(owner, select.chooser) ||
        select.filter != GameObjectFilter.Any || select.restrictions.isNotEmpty() || select.matchChosenCreatureType
    ) return null
    val move = effects.getOrNull(start + 2) as? MoveCollectionEffect ?: return null
    if (move.from != select.storeSelected || move.revealed || !move.isPlainMove(MoveType.Discard)) return null
    val toZone = move.destination as? CardDestination.ToZone ?: return null
    if (toZone.zone != Zone.GRAVEYARD || toZone.player != owner || toZone.placement != ZonePlacement.Default) return null

    val filter = source.filter
    fun counted(count: DynamicAmount): String = when (val fixed = (count as? DynamicAmount.Fixed)?.amount) {
        1 -> SearchText.noun(filter, plural = false).let { "${if (it.first().lowercaseChar() in "aeiou") "an" else "a"} $it" }
        null -> if (count.description == "X") "X ${SearchText.noun(filter, plural = true)}"
            else "a number of ${SearchText.noun(filter, plural = true)} equal to ${count.description}"
        else -> "${numberToWord(fixed)} ${SearchText.noun(filter, plural = true)}"
    }
    val what = when (val selection = select.selection) {
        is SelectionMode.ChooseExactly -> counted(selection.count)
        is SelectionMode.ChooseUpTo -> when ((selection.count as? DynamicAmount.Fixed)?.amount) {
            1 -> "up to one ${SearchText.noun(filter, plural = false)}"
            // A hand-sized sentinel cap ("up to 100") is "any number" in Oracle.
            in 11..Int.MAX_VALUE -> "any number of ${SearchText.noun(filter, plural = true)}"
            else -> "up to ${counted(selection.count)}"
        }
        is SelectionMode.Random -> "${counted(selection.count)} at random"
        SelectionMode.ChooseAnyNumber -> "any number of ${SearchText.noun(filter, plural = true)}"
        SelectionMode.All -> handOf(owner).takeIf { filter == GameObjectFilter.Any }
            ?: "all ${SearchText.noun(filter, plural = true)} in ${handOf(owner)}"
        SelectionMode.ChooseSpell -> return null
    }
    val who = subject(owner, namedPlayers)
    namedPlayers += owner
    return clause(who, "discard", what) to 3
}

/** A move that only relocates the cards: no face-down, counters, links, or partial filters. */
private fun MoveCollectionEffect.isPlainMove(expectedType: MoveType = MoveType.Default): Boolean =
    order == CardOrder.Preserve && moveType == expectedType && !linkToSource && !unlinkFromSource &&
        faceDown == null && !lookableInExile && !noRegenerate && !underOwnersControl && addCounterType == null &&
        !markEnteredViaSourceAbility && filter == null && attachTo == null

/**
 * A non-revealed gather of permanents on the battlefield, immediately consumed by a step that
 * calls them "those permanents": the gather's noun phrase takes the pronoun's place.
 */
private fun fuseBattlefieldGather(
    effects: List<Effect>,
    start: Int,
    render: (Effect) -> String
): Pair<String, Int>? {
    val gather = effects[start] as? GatherCardsEffect ?: return null
    if (gather.revealed || gather.search) return null
    val source = gather.source as? CardSource.FromZone ?: return null
    if (source.zone != Zone.BATTLEFIELD || source.excludeSacrificedThisWay) return null
    val next = effects.getOrNull(start + 1) ?: return null
    val text = render(next)
    val scope = controlScope(source.player) ?: return null
    val replaced = when {
        "each of those permanents" in text ->
            text.replace("each of those permanents", "each ${source.filter.permanentNounPhrase(plural = false, scope)}")
        "those permanents" in text ->
            text.replace("those permanents", "all ${source.filter.permanentNounPhrase(plural = true, scope)}")
        else -> return null
    }
    return replaced to 2
}

/** "you control" / "each opponent controls" — the scope a battlefield gather's player names. */
internal fun controlScope(player: Player): String? = when (player) {
    Player.You -> "you control"
    Player.Each -> ""
    else -> player.description.takeIf { it.isNotBlank() }?.let { "$it controls" }
}
