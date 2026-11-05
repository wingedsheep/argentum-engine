package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Uncage the Menagerie — Hour of Devastation #137
 * {X}{G}{G} · Sorcery
 *
 * Search your library for up to X creature cards with different names that each have mana value
 * X, reveal them, put them into your hand, then shuffle.
 *
 * Shared Summons' pipeline with both bounds read off X: the gathered pool is the library's creature
 * cards with mana value exactly X (`manaValueEqualsDynamic(xValue)`), and `chooseUpTo(xValue)`
 * under [SelectionRestriction.OnePerCardName] caps the count. With X = 0 nothing can be chosen,
 * but the library is still searched and shuffled.
 */
val UncageTheMenagerie = card("Uncage the Menagerie") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to X creature cards with different names that each have " +
        "mana value X, reveal them, put them into your hand, then shuffle."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Creature.manaValueEqualsDynamic(DynamicAmounts.xValue())
                ),
                search = true
            )
            val found = chooseUpTo(
                DynamicAmounts.xValue(),
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for up to X creature cards with different names and mana value X"
            )
            toHand(found, revealed = true)
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "137"
        artist = "Simon Dominic"
        flavorText = "The destruction of Rhonas's monument released his collection upon the city."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc4aa918-53b9-4177-a859-f5a8eff09fe5.jpg?1783936013"
        ruling("2017-07-14", "If you cast Uncage the Menagerie with X as 0, you'll search your library and shuffle it, but you won't be able to find any cards.")
        ruling("2017-07-14", "For cards in your library with {X} in their mana costs, X is considered to be 0.")
    }
}
