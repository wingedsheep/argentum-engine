package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dictate of Karametra
 * {3}{G}{G}
 * Enchantment
 * Flash
 * Whenever a player taps a land for mana, that player adds one mana of any type that land produced.
 *
 * The global Mana Flare shape (as Heartbeat of Spring): [AdditionalManaOnSourceTap] over every
 * land, any controller, with `color = null` so the bonus mirrors the type the land produced for
 * the tapping player.
 */
val DictateOfKarametra = card("Dictate of Karametra") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Flash\nWhenever a player taps a land for mana, that player adds one mana of any type that land produced."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land,
            color = null // mirror the produced type ("any type that land produced")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Noah Bradley"
        flavorText = "\"I refuse to let the folly of mortals endanger the home I made for them.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5114607-df8f-4e44-8ef5-b6af89964bbc.jpg?1783939415"
    }
}
