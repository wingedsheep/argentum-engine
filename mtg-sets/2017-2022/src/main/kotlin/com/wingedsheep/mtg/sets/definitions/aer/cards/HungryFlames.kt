package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hungry Flames
 * {2}{R}
 * Instant
 * Hungry Flames deals 3 damage to target creature and 2 damage to target player or planeswalker.
 */
val HungryFlames = card("Hungry Flames") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Hungry Flames deals 3 damage to target creature and 2 damage to target player or planeswalker."

    spell {
        val creature = target(TargetFilter.Creature)
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(3, creature) then Effects.DealDamage(2, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Izzy"
        flavorText = "In the hold of the *Heart of Kiran*, Chandra answered Dovin Baan's sabotage with an explosive retort."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ca23676-f36f-4266-ba4f-5e9ebf3adb57.jpg?1783936754"
        ruling("2017-02-09", "You can't cast Hungry Flames unless you target both a creature and a player. If one target is illegal as Hungry Flames resolves, the spell deals damage to the remaining legal target.")
    }
}
