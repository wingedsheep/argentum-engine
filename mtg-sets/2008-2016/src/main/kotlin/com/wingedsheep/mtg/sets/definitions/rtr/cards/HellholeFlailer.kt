package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Hellhole Flailer — Return to Ravnica #167 (canonical printing)
 * {1}{B}{R} · Creature — Ogre Warrior · 3/2
 *
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 * {2}{B}{R}, Sacrifice this creature: It deals damage equal to its power to target player or
 * planeswalker.
 *
 * The creature is sacrificed as a cost, so its power is read from last-known information
 * (same shape as Blazing Bomb's Blow Up, covered by BlazingBombScenarioTest).
 */
val HellholeFlailer = card("Hellhole Flailer") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 2
    oracleText = "Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)\n{2}{B}{R}, Sacrifice this creature: It deals damage equal to its power to target player or planeswalker."

    unleash()

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}{R}"), Costs.SacrificeSelf)
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/4984a089-84af-4387-9a0d-819b119b5565.jpg?1783940338"
    }
}
