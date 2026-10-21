package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantHexproofToController

/**
 * Metropolis Reformer
 * {2}{W}
 * Creature — Angel Cleric
 * 2/3
 * Flying, vigilance
 * You have hexproof.
 * Whenever this creature is dealt damage, you gain that much life.
 */
val MetropolisReformer = card("Metropolis Reformer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Cleric"
    power = 2
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "You have hexproof.\n" +
        "Whenever this creature is dealt damage, you gain that much life."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = GrantHexproofToController
    }

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Ryan Pancoast"
        flavorText = "With their ancient enemy vanquished, the angels of New Capenna turned their eyes to their former captors: demons."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a61a3ba1-af6c-4ddc-aa82-34bfc30867d0.jpg?1783916528"
    }
}
