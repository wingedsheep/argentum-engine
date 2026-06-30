package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Broken Wings
 * {2}{G}
 * Instant
 *
 * Destroy target artifact, enchantment, or creature with flying.
 *
 * Canonical printing: Zendikar Rising (ZNR) — the earliest real expansion printing.
 * Later reprints (KHM, SNC, DMU, CMM, FDN, DFT, …) contribute only a `Printing` row.
 */
val BrokenWings = card("Broken Wings") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact, enchantment, or creature with flying."

    spell {
        val target = target(
            "target artifact, enchantment, or creature with flying",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Artifact or
                        GameObjectFilter.Enchantment or
                        GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
                )
            )
        )
        effect = Effects.Destroy(target)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Ekaterina Burmak"
        flavorText = "Getting airborne with a kitesail is easy. It's everything afterward that takes focus, skill, and luck."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0fc2dfd-85b0-4add-be18-b39549235921.jpg?1782706246"
    }
}
