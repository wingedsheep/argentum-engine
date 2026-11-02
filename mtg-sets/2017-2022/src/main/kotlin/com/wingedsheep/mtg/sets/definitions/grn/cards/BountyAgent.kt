package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bounty Agent
 * {1}{W}
 * Creature — Human Soldier
 * 2/2
 * Vigilance
 * {T}, Sacrifice this creature: Destroy target legendary permanent that's an artifact, creature, or enchantment.
 */
val BountyAgent = card("Bounty Agent") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n" +
        "{T}, Sacrifice this creature: Destroy target legendary permanent that's an artifact, creature, or enchantment."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val permanent = target(TargetFilter.ArtifactCreatureOrEnchantment.legendary())
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Randy Vargas"
        flavorText = "\"Lately I've seen the biggest bounties of my career—and for the biggest names.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e938121-5917-4b67-9014-74fca106a471.jpg?1783934205"
    }
}
