package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crushing Vines
 * {2}{G}
 * Instant
 * Choose one —
 * • Destroy target creature with flying.
 * • Destroy target artifact.
 */
val CrushingVines = card("Crushing Vines") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Destroy target creature with flying.\n• Destroy target artifact."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target creature with flying") {
                val t = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
                effect = Effects.Destroy(t)
            }
            mode("Destroy target artifact") {
                val t = target(TargetFilter.Artifact)
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Scott Chou"
        flavorText = "\"Your veil's curse won't stop me, deathmage. I have taken down countless night hunters and this dark plane is not big enough for you to hide.\"\n—Garruk Wildspeaker, to Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c59b3653-5a50-48f2-bcf1-ab305ef30902.jpg"
    }
}
