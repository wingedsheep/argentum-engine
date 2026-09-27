package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kiku, Night's Flower
 * {B}{B}
 * Legendary Creature — Human Assassin
 * 1/1
 * {2}{B}{B}, {T}: Target creature deals damage to itself equal to its power.
 *
 * The target is both the damage source and the recipient, so its own abilities (lifelink,
 * deathtouch, infect) apply to the damage.
 */
val KikuNightsFlower = card("Kiku, Night's Flower") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Assassin"
    oracleText = "{2}{B}{B}, {T}: Target creature deals damage to itself equal to its power."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}{B}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(
            DynamicAmounts.powerOf(creature),
            creature,
            damageSource = creature
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Jim Murray"
        flavorText = "\"A wanderer has told me of an assassin in the Takenuma Swamp who uses her dark arts to animate her enemies' shadows against them. A wild tale, but it explains much.\"\n—Diary of Azusa"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07e18994-d08b-4a8e-abfb-b5531fd6f816.jpg?1783944312"
    }
}
