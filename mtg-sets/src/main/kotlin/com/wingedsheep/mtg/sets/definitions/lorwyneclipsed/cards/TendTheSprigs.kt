package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Tend the Sprigs
 * {2}{G}
 * Sorcery
 *
 * Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 * Then if you control seven or more lands and/or Treefolk, create a 3/4 green Treefolk creature
 * token with reach.
 */
val TendTheSprigs = card("Tend the Sprigs") {
    manaCost = "{2}{G}"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, put it onto the battlefield tapped, " +
        "then shuffle. Then if you control seven or more lands and/or Treefolk, create a 3/4 " +
        "green Treefolk creature token with reach. (It can block creatures with flying.)"

    spell {
        val landsAndTreefolk =
            GameObjectFilter.Land or GameObjectFilter.Permanent.withSubtype(Subtype.TREEFOLK)

        effect = Effects.Composite(
            EffectPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = true
            ),
            ConditionalEffect(
                condition = Compare(
                    DynamicAmount.AggregateBattlefield(Player.You, landsAndTreefolk),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(7)
                ),
                effect = Effects.CreateToken(
                    power = 3,
                    toughness = 4,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Treefolk"),
                    keywords = setOf(Keyword.REACH),
                    imageUri = "https://cards.scryfall.io/normal/front/8/2/82e01706-ab45-4e52-9ee1-7070567234fd.jpg?1767955530"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Iris Compiet"
        flavorText = "Newgrowns need love, attention, and fertilizer."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/388f6d9d-bb9a-4a3d-93c5-701db194863c.jpg?1767658392"
    }
}
