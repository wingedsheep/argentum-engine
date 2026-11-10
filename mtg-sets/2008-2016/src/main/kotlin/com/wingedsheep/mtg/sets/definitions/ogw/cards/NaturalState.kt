package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Natural State
 * {G}
 * Instant
 * Destroy target artifact or enchantment with mana value 3 or less.
 */
val NaturalState = card("Natural State") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or enchantment with mana value 3 or less."

    spell {
        val t = target(TargetFilter.ArtifactOrEnchantment.manaValueAtMost(3))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Volkan Baǵa"
        flavorText = "\"Killing the Eldrazi is not enough. We must erase all memory of them from this world.\"\n—Greenweaver Mina"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdd4bdd5-1673-4f22-b593-41df8ce95a97.jpg"
    }
}
