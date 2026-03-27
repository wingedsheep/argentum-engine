package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Moonrise Cleric
 * {1}{W/B}{W/B}
 * Creature — Bat Cleric
 * 2/3
 *
 * Flying
 * Whenever this creature attacks, you gain 1 life.
 */
val MoonriseCleric = card("Moonrise Cleric") {
    manaCost = "{1}{W/B}{W/B}"
    typeLine = "Creature — Bat Cleric"
    power = 2
    toughness = 3
    oracleText = "Flying\nWhenever this creature attacks, you gain 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "226"
        artist = "Simon Dominic"
        flavorText = "\"The sunrisers have entrusted me with their life force. I intend to repay them bountifully.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35f2a71f-31e8-4b51-9dd4-51a5336b3b86.jpg?1721427155"
    }
}
