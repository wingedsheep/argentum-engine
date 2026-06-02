package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Byway Barterer {2}{R}
 * Creature — Raccoon Rogue
 * 3/3
 *
 * Menace
 * Whenever you expend 4, you may discard your hand. If you do, draw two cards.
 */
val BywayBarterer = card("Byway Barterer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Raccoon Rogue"
    power = 3
    toughness = 3
    oracleText = "Menace\nWhenever you expend 4, you may discard your hand. If you do, draw two cards."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.Expend(4)
        effect = MayEffect(
            HandPatterns.discardHand().then(Effects.DrawCards(2))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Ryan Pancoast"
        flavorText = "\"This trinket has saved my hide more times than I can count. I couldn't possibly let it go for less than say . . . four acorns.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f41fc718-641b-4f32-a8c1-3e5591a05bf8.jpg?1721426591"
        ruling("2024-07-26", "You may choose to discard your hand even if your hand contains zero cards.")
    }
}
