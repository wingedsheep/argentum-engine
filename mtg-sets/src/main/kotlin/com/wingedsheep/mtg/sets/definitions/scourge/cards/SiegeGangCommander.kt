package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Siege-Gang Commander
 * {3}{R}{R}
 * Creature — Goblin
 * 2/2
 * When Siege-Gang Commander enters the battlefield, create three 1/1 red Goblin creature tokens.
 * {1}{R}, Sacrifice a Goblin: Siege-Gang Commander deals 2 damage to any target.
 */
val SiegeGangCommander = card("Siege-Gang Commander") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 2
    oracleText = "When Siege-Gang Commander enters the battlefield, create three 1/1 red Goblin creature tokens.\n{1}{R}, Sacrifice a Goblin: Siege-Gang Commander deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            count = 3,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/e/9/e9577d3c-ee19-4b53-adac-b304287a066f.jpg?1561758362"
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Goblin"))
        )
        target = AnyTarget()
        effect = DealDamageEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Christopher Moeller"
        flavorText = "They poured from the Skirk Ridge like lava, burning and devouring everything in their path."
        imageUri = "https://cards.scryfall.io/large/front/9/2/92e78cec-aaf9-4fe8-887b-b7e356d63315.jpg"
    }
}
