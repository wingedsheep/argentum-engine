package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Altar's Light — Mirrodin #1
 * {2}{W}{W} · Instant
 *
 * Exile target artifact or enchantment.
 *
 * Mirrodin's premium Disenchant: exile rather than destroy, so regeneration, indestructible, and
 * dies-triggers all miss it — at the cost of two extra mana over the usual {1}{W} rate.
 */
val AltarsLight = card("Altar's Light") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target artifact or enchantment."

    spell {
        val t = target("target artifact or enchantment", Targets.ArtifactOrEnchantment)
        effect = Effects.Exile(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Daren Bader"
        flavorText = "\"The altar does nothing; the device is crushed under the weight of its own " +
            "impurity.\"\n—Ushanti, leonin seer"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd037f62-7cef-4737-b575-942c5959f1ea.jpg?1783944565"
    }
}
