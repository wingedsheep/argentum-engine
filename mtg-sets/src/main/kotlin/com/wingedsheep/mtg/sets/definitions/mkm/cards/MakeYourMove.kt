package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Make Your Move — Murders at Karlov Manor #22
 * {2}{W} · Instant
 *
 * Destroy target artifact, enchantment, or creature with power 4 or greater.
 *
 * The "power 4 or greater" restriction binds only to the creature branch, so the target is a
 * single battlefield filter built as `ArtifactOrEnchantment or Creature.powerAtLeast(4)` — a
 * 5/5 artifact creature qualifies twice over, a 2/2 creature not at all. `PowerAtLeast` reads
 * projected power, so a creature pumped past 4 by a lord or an Aura is a legal target and one
 * shrunk below 4 stops being one (and the spell fizzles if that was its only target).
 */
val MakeYourMove = card("Make Your Move") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target artifact, enchantment, or creature with power 4 or greater."

    spell {
        val t = target(
            "target artifact, enchantment, or creature with power 4 or greater",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.ArtifactOrEnchantment or GameObjectFilter.Creature.powerAtLeast(4)
                )
            )
        )
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Xabi Gaztelua"
        flavorText = "\"If you're thinking only one step ahead, you're already falling behind.\"\n—Teysa"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73475d29-2673-4614-86d3-404232426aa8.jpg?1783912922"
    }
}
