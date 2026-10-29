package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kaervek the Merciless {5}{B}{R} — Legendary Creature — Human Shaman 5/4
 *
 * Whenever an opponent casts a spell, Kaervek deals damage equal to that spell's mana value to any
 * target.
 *
 * The amount is the mana value captured on the SpellCastEvent, which counts each {X} as the
 * announced X (CR 202.3e) and ignores cost reductions / alternative costs.
 */
val KaervekTheMerciless = card("Kaervek the Merciless") {
    manaCost = "{5}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Shaman"
    power = 5
    toughness = 4
    oracleText = "Whenever an opponent casts a spell, Kaervek deals damage equal to that spell's " +
        "mana value to any target."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        val any = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.triggeringSpellManaValue(), any)
        description = "Whenever an opponent casts a spell, Kaervek deals damage equal to that " +
            "spell's mana value to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "rk post"
        flavorText = "\"Rats and jackals feast in his swath, but even they will not walk with him.\"\n—Mangara"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/444fa5a5-b5b0-4555-bd69-67c1c0cbf317.jpg?1783943201"
        ruling("2021-03-19", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2021-03-19", "Alternative costs, additional costs, and cost reductions don't change a spell's mana value. Its mana value is still based on its mana cost.")
        ruling("2021-03-19", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value.")
    }
}
