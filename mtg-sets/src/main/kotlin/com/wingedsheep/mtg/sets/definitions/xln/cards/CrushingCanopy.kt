package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crushing Canopy
 * {2}{G}
 * Instant
 *
 * Choose one —
 * • Destroy target creature with flying.
 * • Destroy target enchantment.
 *
 * Canonical printing is Ixalan (earliest real set); Innistrad: Crimson Vow gets a reprint row.
 */
val CrushingCanopy = card("Crushing Canopy") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Destroy target creature with flying.\n• Destroy target enchantment."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target creature with flying") {
                val t = target("target", TargetCreature(filter = TargetFilter.Creature.withKeyword(Keyword.FLYING)))
                effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
            }
            mode("Destroy target enchantment") {
                val t = target("target", TargetObject(filter = TargetFilter.Enchantment))
                effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Do not mistake your lofty vantage point for safety.\"\n—Shaper Tuvasa"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a66b0e45-e585-44f3-8d2b-e887330ba138.jpg?1783935728"
    }
}
