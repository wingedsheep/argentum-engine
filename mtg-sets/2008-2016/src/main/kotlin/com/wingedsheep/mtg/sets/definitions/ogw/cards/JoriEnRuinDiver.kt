package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jori En, Ruin Diver
 * {1}{U}{R}
 * Legendary Creature — Merfolk Wizard
 * 2/3
 * Whenever you cast your second spell each turn, draw a card.
 */
val JoriEnRuinDiver = card("Jori En, Ruin Diver") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "RU"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast your second spell each turn, draw a card."

    triggeredAbility {
        trigger = Triggers.you.castsNth(2)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Igor Kieryluk"
        flavorText = "\"Some relic hunters have given up exploring and gone to fight the Eldrazi. But I know our " +
            "work is more important than ever. The secrets we uncover could lead to the world's salvation.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/900d3af7-a03f-4173-8a8a-bd146dec9c0f.jpg"
    }
}
