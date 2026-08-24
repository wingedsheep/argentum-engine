package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Chambered Nautilus
 * {2}{U}
 * Creature — Nautilus Beast
 * 2 / 2
 *
 * Whenever this creature becomes blocked, you may draw a card.
 */
val ChamberedNautilus = card("Chambered Nautilus") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Nautilus Beast"
    oracleText = "Whenever this creature becomes blocked, you may draw a card."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        optional = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "John Matson"
        flavorText = "What's merely a home for the nautilus can become exquisite jewelry in the hands of Saprazzan artisans."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/860c613d-d031-4c2a-922b-39f4eec04e18.jpg"
    }
}
