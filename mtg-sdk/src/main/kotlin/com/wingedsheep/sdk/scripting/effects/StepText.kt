package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
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
 *  - **A battlefield gather** feeding the next step is that step's object: "Put a +1/+1 counter on
 *    each Frog … you control that entered the battlefield this turn", not "Look at … in your
 *    battlefield. Put … on each of those permanents".
 *
 * A run that doesn't match either shape exactly falls back to the per-step text, and steps with no
 * text of their own (the library-searched event marker) are skipped rather than joined as empty
 * sentences. [render] is the per-step text — `description` or a runtime description.
 */
internal fun describeSteps(effects: List<Effect>, render: (Effect) -> String): String {
    val sentences = mutableListOf<String>()
    var i = 0
    while (i < effects.size) {
        val fused = fuseSearch(effects, i) ?: fuseBattlefieldGather(effects, i, render)
        if (fused != null) {
            sentences += fused.first
            i += fused.second
            continue
        }
        render(effects[i]).takeIf { it.isNotBlank() }?.let { sentences += it }
        i++
    }
    return sentences.joinToString(". ")
}

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

/** A move that only relocates the cards: no face-down, counters, links, or partial filters. */
private fun MoveCollectionEffect.isPlainMove(): Boolean =
    order == CardOrder.Preserve && moveType == MoveType.Default && !linkToSource && !unlinkFromSource &&
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
