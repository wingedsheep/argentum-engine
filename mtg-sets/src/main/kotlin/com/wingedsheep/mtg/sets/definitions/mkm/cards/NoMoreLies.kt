package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * No More Lies
 * {W}{U}
 * Instant
 *
 * Counter target spell unless its controller pays {3}. If that spell is countered
 * this way, exile it instead of putting it into its owner's graveyard.
 */
val NoMoreLies = card("No More Lies") {
    manaCost = "{W}{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {3}. If that spell is countered this way, exile it instead of putting it into its owner's graveyard."

    spell {
        target = Targets.Spell
        effect = Effects.CounterUnlessDynamicPays(DynamicAmount.Fixed(3), exileOnCounter = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Liiga Smilshkalne"
        flavorText = "\"Lies fester and flourish in shadow. Our job is to bathe them in light.\"\n—Ezrim, Agency chief"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e0c695d-62f9-4805-9e2f-7032e8464136.jpg?1706242217"
    }
}
