package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dovin's Dismissal — Ravnica Allegiance #267 (planeswalker deck)
 * {2}{W}{U} · Instant
 *
 * Put up to one target tapped creature on top of its owner's library. You may search your library
 * and/or graveyard for a card named Dovin, Architect of Law, reveal it, and put it into your hand.
 * If you search your library this way, shuffle.
 *
 * The "up to one target tapped creature" slot is Peerless Ropemaster's optional tapped-creature
 * target; the search half is the planeswalker-deck tutor shared with Gideon's Battle Cry.
 */
val DovinsDismissal = card("Dovin's Dismissal") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Put up to one target tapped creature on top of its owner's library. You may search " +
        "your library and/or graveyard for a card named Dovin, Architect of Law, reveal it, and put it " +
        "into your hand. If you search your library this way, shuffle."

    spell {
        val victim = target(TargetFilter.TappedCreature, optional = true)
        effect = Effects.PutOnTopOfLibrary(victim) then Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Dovin, Architect of Law"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        inBooster = false
        artist = "Kieran Yanner"
        flavorText = "\"We're finished here.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6328e6b5-9dfb-4fd4-99ee-a1ffc2c707da.jpg?1783933609"
    }
}
