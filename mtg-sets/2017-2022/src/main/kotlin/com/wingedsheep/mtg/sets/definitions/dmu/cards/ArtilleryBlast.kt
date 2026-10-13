package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Artillery Blast
 * {1}{W}
 * Instant
 * Domain — Artillery Blast deals X damage to target tapped creature, where X is 1 plus the number
 * of basic land types among lands you control.
 */
val ArtilleryBlast = card("Artillery Blast") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Domain — Artillery Blast deals X damage to target tapped creature, where X is 1 plus the number of basic land types among lands you control."

    spell {
        val victim = target(TargetFilter.TappedCreature)
        effect = Effects.DealDamage(DynamicAmounts.domain() + 1, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Julian Kok Joon Wen"
        flavorText = "To conserve ammunition, Jhoira modified ancient Thran artillery cannons to fire gouts of pure Shivan lava."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d4d7293-657f-48ac-931b-fb6a44128e6d.jpg?1783921370"
    }
}
