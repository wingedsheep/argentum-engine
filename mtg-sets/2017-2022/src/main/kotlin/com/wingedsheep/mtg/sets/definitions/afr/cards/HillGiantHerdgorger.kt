package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hill Giant Herdgorger
 * {4}{G}{G}
 * Creature — Giant
 * 7/6
 * When this creature enters, you gain 3 life.
 */
val HillGiantHerdgorger = card("Hill Giant Herdgorger") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant"
    oracleText = "When this creature enters, you gain 3 life."
    power = 7
    toughness = 6
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "187"
        artist = "Chris Rahn"
        flavorText = "A shepherd's crook is useful for cleaning bits of wool out of the teeth."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b69315cc-c230-4704-9d66-411f624f3e49.jpg?1783926462"
    }
}
