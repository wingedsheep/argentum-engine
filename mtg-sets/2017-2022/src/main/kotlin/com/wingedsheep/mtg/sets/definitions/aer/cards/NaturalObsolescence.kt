package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Natural Obsolescence
 * {1}{G}
 * Instant
 *
 * Put target artifact on the bottom of its owner's library.
 */
val NaturalObsolescence = card("Natural Obsolescence") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put target artifact on the bottom of its owner's library."

    spell {
        val artifact = target(TargetFilter.Artifact)
        effect = Effects.PutOnBottomOfLibrary(artifact)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Eytan Zana"
        flavorText = "\"It is folly to believe your precious inventions will endure. All things return to the soil.\"\n—Nirav, Tirahar elder"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff693e03-4cc6-4903-8fc4-388d23c2f92f.jpg?1783936741"
    }
}
