package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Divine Offering
 * {1}{W}
 * Instant
 *
 * Destroy target artifact. You gain life equal to its mana value.
 */
val DivineOffering = card("Divine Offering") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target artifact. You gain life equal to its mana value."

    spell {
        val artifact = target(TargetFilter.Artifact)
        effect = Effects.Destroy(artifact) then Effects.GainLife(DynamicAmounts.manaValueOf(artifact))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Jeff A. Menges"
        flavorText = "D'Haren stared at the twisted lump of metal that had been a prized artifact. The fight was getting ugly."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c78c2f3-2f40-48ad-9dc4-55d1fa399a56.jpg?1783948086"
    }
}
