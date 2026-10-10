package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Scapeshift — Morningtide #136
 * {2}{G}{G} · Sorcery
 *
 * Sacrifice any number of lands. Search your library for up to that many land cards, put them
 * onto the battlefield tapped, then shuffle.
 *
 * The sacrifice happens on resolution (not a cost): `SacrificeAnyNumber(Land)` records the
 * sacrificed lands, and the search's "up to that many" reads `permanentsSacrificedThisWay`
 * (Hew the Entwood's shape) before handing off to the standard library search.
 */
val Scapeshift = card("Scapeshift") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Sacrifice any number of lands. Search your library for up to that many land " +
        "cards, put them onto the battlefield tapped, then shuffle."

    spell {
        effect = Effects.SacrificeAnyNumber(GameObjectFilter.Land) then
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Land,
                count = DynamicAmounts.permanentsSacrificedThisWay(),
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Fred Fields"
        flavorText = "\"Changes far greater than the turning of the leaves await us at season's " +
            "end.\"\n—Colfenor, the Last Yew"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84829605-50eb-455d-a236-ebfa11e883c5.jpg?1783942776"
        ruling(
            "2018-07-13",
            "You sacrifice the lands as part of the resolution of Scapeshift. It isn't an " +
                "additional cost. If Scapeshift is countered, you won't sacrifice any lands."
        )
    }
}
