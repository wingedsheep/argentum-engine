package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ogre Siegebreaker (M20 #215)
 * {2}{B}{R}  Creature — Ogre Berserker  4/3
 *
 * {2}{B}{R}: Destroy target creature that was dealt damage this turn.
 */
val OgreSiegebreaker = card("Ogre Siegebreaker") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Ogre Berserker"
    oracleText = "{2}{B}{R}: Destroy target creature that was dealt damage this turn."
    power = 4
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{2}{B}{R}")
        val creature = target(TargetFilter.Creature.wasDealtDamageThisTurn())
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Zoltan Boros"
        flavorText = "\"What he doesn't burn, he smashes.\"\n—Captain Wellis, ogre wrangler"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0aba1c64-c552-4aeb-b669-da885c8c75b2.jpg"
    }
}
