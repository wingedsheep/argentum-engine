package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Adventurous Impulse
 * {G}
 * Sorcery
 * Look at the top three cards of your library. You may reveal a creature or land card
 * from among them and put it into your hand. Put the rest on the bottom of your library
 * in any order.
 */
val AdventurousImpulse = card("Adventurous Impulse") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library. You may reveal a creature or land card from among them and put it into your hand. Put the rest on the bottom of your library in any order."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(3)),
                name = "looked"
            )
            val (kept, rest) = chooseUpToSplit(
                1, from = looked,
                filter = GameObjectFilter.Creature or GameObjectFilter.Land,
                prompt = "You may reveal a creature or land card and put it into your hand",
                showAllCards = true,
                name = "kept",
                remainderName = "rest"
            )
            move(
                kept,
                destination = CardDestination.ToZone(Zone.HAND)
            )
            move(
                rest,
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Titus Lunter"
        flavorText = "\"Every odyssey begins with a single step.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f426c92c-6e71-49f0-9a91-0d529bf8c17d.jpg?1562745600"
    }
}
