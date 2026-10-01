package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Minor Misstep
 * {U}
 * Instant
 * Counter target spell with mana value 1 or less.
 */
val MinorMisstep = card("Minor Misstep") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell with mana value 1 or less."
    spell {
        target(TargetFilter.SpellOnStack.manaValueAtMost(1))
        effect = Effects.CounterSpell()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Lorenzo Mastroianni"
        flavorText = "No matter how many times he saw it, Sarnvax always relished the look of dawning horror as his foes realized their mistake."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/360ca37b-5bbd-4923-a493-7674786a36af.jpg?1783918061"
    }
}
