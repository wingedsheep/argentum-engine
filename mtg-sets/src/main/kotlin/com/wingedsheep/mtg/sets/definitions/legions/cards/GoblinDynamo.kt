package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Goblin Dynamo
 * {5}{R}{R}
 * Creature — Goblin Mutant
 * 4/4
 * {T}: Goblin Dynamo deals 1 damage to any target.
 * {X}{R}, {T}, Sacrifice Goblin Dynamo: Goblin Dynamo deals X damage to any target.
 */
val GoblinDynamo = card("Goblin Dynamo") {
    manaCost = "{5}{R}{R}"
    typeLine = "Creature — Goblin Mutant"
    power = 4
    toughness = 4
    oracleText = "{T}: Goblin Dynamo deals 1 damage to any target.\n{X}{R}, {T}, Sacrifice Goblin Dynamo: Goblin Dynamo deals X damage to any target."

    // {T}: Goblin Dynamo deals 1 damage to any target.
    activatedAbility {
        cost = Costs.Tap
        val t = target("any target", AnyTarget())
        effect = DealDamageEffect(
            amount = DynamicAmount.Fixed(1),
            target = t
        )
    }

    // {X}{R}, {T}, Sacrifice Goblin Dynamo: Goblin Dynamo deals X damage to any target.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{X}{R}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        val t = target("any target", AnyTarget())
        effect = DealDamageEffect(
            amount = DynamicAmount.XValue,
            target = t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/9462cb4e-a38c-4a41-bad2-4ea3b22b0edb.jpg?1562924933"
    }
}
