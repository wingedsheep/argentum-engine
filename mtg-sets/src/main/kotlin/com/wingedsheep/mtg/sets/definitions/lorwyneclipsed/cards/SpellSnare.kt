package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spell Snare
 * {U}
 * Instant
 * Counter target spell with mana value 2.
 */
val SpellSnare = card("Spell Snare") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell with mana value 2."

    spell {
        target = Targets.SpellWithManaValue(2)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Iris Compiet"
        flavorText = "Shadowmoor merrow swindle and steal not only for profit but for spite."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7551b61-656e-4f37-b9da-73174db983b7.jpg?1767659595"
    }
}
