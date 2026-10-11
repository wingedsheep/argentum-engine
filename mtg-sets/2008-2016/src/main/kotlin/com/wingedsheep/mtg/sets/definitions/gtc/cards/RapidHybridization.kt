package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rapid Hybridization
 * {U}
 * Instant
 *
 * Destroy target creature. It can't be regenerated. That creature's controller creates a 3/3
 * green Frog Lizard creature token.
 */
val RapidHybridization = card("Rapid Hybridization") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Destroy target creature. It can't be regenerated. That creature's controller creates a 3/3 green Frog Lizard creature token."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature, noRegenerate = true) then
            Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Frog", "Lizard"),
                controller = EffectTarget.TargetController,
                imageUri = "https://cards.scryfall.io/normal/front/b/9/b938d2e0-7e90-4f57-b363-5c68a6207d6c.jpg?1783940147"
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Jack Wang"
        flavorText = "\"We've merged your life essence with that of several creatures at once. You're welcome.\"\n—Speaker Trifon"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83557f55-f1ab-4995-9cc1-37be895a59db.jpg?1783940136"
        ruling("2013-07-01", "If Rapid Hybridization resolves and the creature isn't destroyed (perhaps because it has indestructible), its controller will still get the Frog Lizard token.")
        ruling("2013-01-24", "If the creature is an illegal target when Rapid Hybridization tries to resolve, it won't resolve and none of its effects will happen. No Frog Lizard token will be created.")
    }
}
