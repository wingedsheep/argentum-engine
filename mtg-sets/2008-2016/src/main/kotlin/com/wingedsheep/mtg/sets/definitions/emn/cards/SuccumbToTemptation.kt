package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Succumb to Temptation (Eldritch Moon #107)
 * {1}{B}{B}
 * Instant
 *
 * You draw two cards and you lose 2 life.
 */
val SuccumbToTemptation = card("Succumb to Temptation") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "You draw two cards and you lose 2 life."

    spell {
        effect = Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Winona Nelson"
        flavorText = "\"Restraint is for the meek.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2974e18b-be17-4059-9f29-7afe1d1a51eb.jpg"
    }
}
