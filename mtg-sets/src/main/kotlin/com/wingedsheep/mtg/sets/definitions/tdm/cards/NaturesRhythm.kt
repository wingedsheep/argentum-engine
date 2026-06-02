package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Nature's Rhythm — Tarkir: Dragonstorm #150
 * {X}{G}{G} · Sorcery
 *
 * Search your library for a creature card with mana value X or less, put it onto the
 * battlefield, then shuffle.
 * Harmonize {X}{G}{G}{G}{G} (You may cast this card from your graveyard for its harmonize
 * cost. You may tap a creature you control to reduce that cost by an amount of generic
 * mana equal to its power. Then exile this spell.)
 *
 * The Harmonize alternative cost is itself an {X} cost, and it is fully supported end to
 * end: `enumerateHarmonize` advertises `hasXCost`/`maxAffordableX` (folding in the best
 * single-creature tap reduction) so the client prompts for X, and tapping a creature
 * reduces the *generic* mana paid — including the {X} (which is generic, per the TDM
 * release notes) — while the chosen X that drives the "mana value X or less" search is
 * unchanged. See `CastSpellHandler.harmonizePaymentXValue`.
 */
val NaturesRhythm = card("Nature's Rhythm") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a creature card with mana value X or less, put it onto the battlefield, then shuffle.\n" +
        "Harmonize {X}{G}{G}{G}{G} (You may cast this card from your graveyard for its harmonize cost. " +
        "You may tap a creature you control to reduce that cost by an amount of generic mana equal to its power. Then exile this spell.)"

    spell {
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.Creature.manaValueAtMostX(),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.harmonize("{X}{G}{G}{G}{G}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Liiga Smilshkalne"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1397d904-c51d-451e-8505-7f3118acc1f6.jpg?1743204565"
    }
}
