package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Goblin Clearcutter
 * {3}{R}
 * Creature — Goblin
 * 3/3
 * {T}, Sacrifice a Forest: Add three mana in any combination of {R} and/or {G}.
 */
val GoblinClearcutter = card("Goblin Clearcutter") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Goblin"
    power = 3
    toughness = 3
    oracleText = "{T}, Sacrifice a Forest: Add three mana in any combination of {R} and/or {G}."

    activatedAbility {
        cost = AbilityCost.Composite(
            listOf(
                AbilityCost.Tap,
                AbilityCost.Sacrifice(GameObjectFilter.Land.withSubtype("Forest"))
            )
        )
        effect = AddDynamicManaEffect(
            amountSource = DynamicAmount.Fixed(3),
            allowedColors = setOf(Color.RED, Color.GREEN)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Eric Peterson"
        flavorText = "\"Did you know that wood burns even better than rocks?\"\n—Toggo, goblin weaponsmith"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e07c0cae-852c-444c-8994-68a6d81b4cd4.jpg?1562940096"
    }
}
