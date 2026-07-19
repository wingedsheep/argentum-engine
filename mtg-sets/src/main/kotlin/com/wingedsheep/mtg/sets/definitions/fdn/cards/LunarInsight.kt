package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Lunar Insight
 * {2}{U}
 * Sorcery
 *
 * Draw a card for each different mana value among nonland permanents you control.
 *
 * The count is the number of *distinct* mana values among your nonland permanents (two permanents
 * sharing a mana value count once) — [DynamicAmounts.distinctValues] over
 * [CardNumericProperty.MANA_VALUE]. A permanent with {X} in its cost has mana value 0 on the
 * battlefield (per ruling), so all such permanents fold into the single "0" value.
 */
val LunarInsight = card("Lunar Insight") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw a card for each different mana value among nonland permanents you control."

    spell {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.NonlandPermanent)
                .distinctValues(CardNumericProperty.MANA_VALUE)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Dan Murayama Scott"
        flavorText = "\"The night sky is generous with its secrets. We need only learn its language.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9a159f6-fecf-4bdd-b2f8-a9665a5cc32d.jpg?1783909117"
        ruling("2024-11-08", "If a permanent you control has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
    }
}
