package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Grassland Crusader
 * {5}{W}
 * Creature — Human Cleric Soldier
 * 2/4
 * {T}: Target Elf or Soldier creature gets +2/+2 until end of turn.
 */
val GrasslandCrusader = card("Grassland Crusader") {
    manaCost = "{5}{W}"
    typeLine = "Creature — Human Cleric Soldier"
    power = 2
    toughness = 4
    oracleText = "{T}: Target Elf or Soldier creature gets +2/+2 until end of turn."

    activatedAbility {
        cost = AbilityCost.Tap
        val t = target("target", TargetPermanent(
            filter = TargetFilter(
                GameObjectFilter.Creature.withSubtype("Elf") or GameObjectFilter.Creature.withSubtype("Soldier")
            )
        ))
        effect = ModifyStatsEffect(
            powerModifier = 2,
            toughnessModifier = 2,
            target = t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Mark Tedin"
        flavorText = "When the ground shakes, the Order is not far behind."
        imageUri = "https://cards.scryfall.io/large/front/c/1/c129f361-8769-4f9a-9745-eb5d0c085b88.jpg?1562940580"
    }
}
