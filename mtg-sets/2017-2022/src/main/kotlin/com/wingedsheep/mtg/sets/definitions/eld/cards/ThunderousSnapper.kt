package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thunderous Snapper
 * {G/U}{G/U}{G/U}{G/U}
 * Creature — Turtle Hydra
 * 4/4
 * Whenever you cast a spell with mana value 5 or greater, draw a card.
 */
val ThunderousSnapper = card("Thunderous Snapper") {
    manaCost = "{G/U}{G/U}{G/U}{G/U}"
    colorIdentity = "GU"
    typeLine = "Creature — Turtle Hydra"
    oracleText = "Whenever you cast a spell with mana value 5 or greater, draw a card."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.manaValueAtLeast(5))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Johann Bodin"
        flavorText = "While humans hear only a deafening roar, the fae hear music of breathtaking beauty."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9576d4f1-47bb-45e0-803c-93fa2cdacbe5.jpg"
    }
}
