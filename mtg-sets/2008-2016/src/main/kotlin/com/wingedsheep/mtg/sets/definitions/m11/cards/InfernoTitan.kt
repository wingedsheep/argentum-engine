package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Inferno Titan
 * {4}{R}{R}
 * Creature — Giant
 * 6/6
 *
 * {R}: This creature gets +1/+0 until end of turn.
 * Whenever this creature enters or attacks, it deals 3 damage divided as you choose among one,
 * two, or three targets.
 *
 * - "Enters **or** attacks" is two triggered abilities sharing one effect (the Sun Titan shape).
 * - Targets and the division are chosen as the trigger goes on the stack, each target getting at
 *   least 1 damage: [AnyTarget] (one to three) plus `Effects.DividedDamage` (cf. Gandalf, Spark
 *   Starter; Arc Lightning).
 */
val InfernoTitan = card("Inferno Titan") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 6
    toughness = 6
    oracleText = "{R}: This creature gets +1/+0 until end of turn.\n" +
        "Whenever this creature enters or attacks, it deals 3 damage divided as you choose among one, two, or three targets."

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    val damageDescription = "Whenever this creature enters or attacks, it deals 3 damage divided " +
        "as you choose among one, two, or three targets."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target = AnyTarget(count = 3, minCount = 1)
        effect = Effects.DividedDamage(total = 3, minTargets = 1, maxTargets = 3)
        description = damageDescription
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        target = AnyTarget(count = 3, minCount = 1)
        effect = Effects.DividedDamage(total = 3, minTargets = 1, maxTargets = 3)
        description = damageDescription
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "146"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1e4a028-6462-4373-9864-a8adfc78d52b.jpg?1783941804"
        ruling(
            "2010-08-15",
            "You divide the damage as you put Inferno Titan's triggered ability on the stack, not as it " +
                "resolves. Each target must be assigned at least 1 damage.",
        )
    }
}
