package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Decimate {2}{R}{G}
 * Sorcery
 *
 * Destroy target artifact, target creature, target enchantment, and target land.
 * (You can't cast this spell unless you have legal choices for all its targets.)
 */
val Decimate = card("Decimate") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, target creature, target enchantment, and target land. " +
        "(You can't cast this spell unless you have legal choices for all its targets.)"

    spell {
        val artifact = target("target artifact", Targets.Artifact)
        val creature = target("target creature", Targets.Creature)
        val enchantment = target("target enchantment", Targets.Enchantment)
        val land = target("target land", Targets.Land)
        effect = Effects.Composite(
            Effects.Destroy(artifact),
            Effects.Destroy(creature),
            Effects.Destroy(enchantment),
            Effects.Destroy(land)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "251"
        artist = "Zoltan Boros"
        flavorText = "Anarchy comes in many forms: social, individual, Gruul . . ."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed38da33-c230-4eec-b7c7-3b0c5cdf727a.jpg?1721429459"
    }
}
