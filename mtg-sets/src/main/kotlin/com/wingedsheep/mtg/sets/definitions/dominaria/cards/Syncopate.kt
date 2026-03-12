package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Syncopate
 * {X}{U}
 * Instant
 * Counter target spell unless its controller pays {X}. If that spell is
 * countered this way, exile it instead of putting it into its owner's graveyard.
 */
val Syncopate = card("Syncopate") {
    manaCost = "{X}{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {X}. If that spell is countered this way, exile it instead of putting it into its owner's graveyard."

    spell {
        val spell = target("spell", Targets.Spell)
        effect = Effects.CounterUnlessDynamicPays(DynamicAmount.XValue, exileOnCounter = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Tommy Arnold"
        flavorText = "The fire spell stuttered and broke. Its pieces reached Teferi out of rhythm, meaningless."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f81739a5-35a7-4812-a7af-e1951bf5579c.jpg?1617884773"
    }
}
