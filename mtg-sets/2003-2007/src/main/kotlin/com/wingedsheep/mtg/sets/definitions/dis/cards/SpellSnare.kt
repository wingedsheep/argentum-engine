package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spell Snare
 * {U}
 * Instant
 * Counter target spell with mana value 2.
 */
val SpellSnare = card("Spell Snare") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell with mana value 2."

    spell {
        val spellWithManaValue = target(TargetFilter.SpellOnStack.manaValue(2))
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Hideaki Takamura"
        flavorText = "Every culture has its unlucky numbers. In a city where you're either alone, in a crowd, or being stabbed in the back, two is the worst number of all."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35554fdf-c70a-4baa-a35a-414caa9978be.jpg?1783943435"
    }
}
