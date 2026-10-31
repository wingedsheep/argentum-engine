package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scalding Cauldron
 * {1}
 * Artifact
 *
 * {3}, {T}, Sacrifice this artifact: It deals 3 damage to target creature.
 */
val ScaldingCauldron = card("Scalding Cauldron") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, {T}, Sacrifice this artifact: It deals 3 damage to target creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "229"
        artist = "Craig J Spearing"
        flavorText = "\"It's a witch's trick, this cauldron full of pain. I wouldn't dump it on any but the wicked.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/710d2782-2b23-441f-9890-6fa9c923b701.jpg"
    }
}
