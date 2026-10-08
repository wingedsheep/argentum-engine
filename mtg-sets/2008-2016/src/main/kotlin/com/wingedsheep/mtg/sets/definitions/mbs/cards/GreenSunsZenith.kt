package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Green Sun's Zenith — Mirrodin Besieged #81 (canonical / earliest real printing, 2011)
 * {X}{G} · Sorcery
 *
 * Search your library for a green creature card with mana value X or less, put it onto the
 * battlefield, then shuffle. Shuffle Green Sun's Zenith into its owner's library.
 *
 * The search is the stock `Patterns.Library.searchLibrary` recipe pointed at the battlefield;
 * `count = 1` is how many cards are found, while the {X} paid lives in the *filter*
 * (`manaValueAtMostX()`, read off this spell's own cast at
 * resolution). Searching never compels a find (CR 701.23b), so X = 0 legitimately finds nothing —
 * except for Dryad Arbor, the one green creature card with mana value 0.
 *
 * The trailing clause is `selfShuffleIntoLibrary()`, which replaces this spell's CR 608.2n
 * destination. Note it fires only on resolution: countered or fizzled, the card goes to the
 * graveyard like any other spell. Of the five Zeniths this is the only one whose effect *pauses*
 * mid-resolution (for the library-search decision), so it is the cycle's one exercise of
 * `StackResolver`'s paused-resolve path.
 */
val GreenSunsZenith = card("Green Sun's Zenith") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a green creature card with mana value X or less, put it " +
        "onto the battlefield, then shuffle. Shuffle Green Sun's Zenith into its owner's library."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.withColor(Color.GREEN).manaValueAtMostX(),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
        )
        selfShuffleIntoLibrary()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "David Rapoza"
        flavorText = "As the green sun crowned, Phyrexian prophecies glowed on the Tree of Tales."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02335747-54e3-4827-ae19-4e362863da9b.jpg?1783941375"
        ruling(
            "2011-06-01",
            "If this spell doesn't resolve, none of its effects occur. In particular, it will go " +
                "to the graveyard rather than to its owner's library."
        )
        ruling(
            "2016-06-08",
            "If Green Sun's Zenith is countered, none of its effects will happen. Notably, it " +
                "will be put into its owner's graveyard rather than shuffled into its owner's library."
        )
        ruling(
            "2016-06-08",
            "In most cases, if you own Green Sun's Zenith and cast it, you'll shuffle your " +
                "library twice. In practice, shuffling once is sufficient, but effects that care " +
                "about you shuffling your library (like Psychogenic Probe, for example) will see " +
                "that you've shuffled twice."
        )
        ruling(
            "2016-06-08",
            "If you own Green Sun's Zenith, but an opponent casts it (due to Knowledge Pool's " +
                "effect, for example), that opponent searches their library for an appropriate " +
                "creature card, then shuffles that library. That opponent then shuffles Green " +
                "Sun's Zenith into your library. You won't shuffle any library in this case."
        )
    }
}
