package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Jolene, Plundering Pugilist
 * {1}{R}{G}
 * Legendary Creature — Human Mercenary
 * 4/2
 * Whenever you attack with one or more creatures with power 4 or greater, create a Treasure token.
 * {1}{R}, Sacrifice a Treasure: Jolene deals 1 damage to any target.
 *
 * Modeling notes:
 * - The attack trigger is [Triggers.YouAttackWithFilter] over `Creature.powerAtLeast(4)`. It fires
 *   once per combat when at least one declared attacker has power ≥ 4 (evaluated under projected
 *   state, so pumps/anthems count). Jolene herself qualifies (4 power).
 * - The activated ability sacrifices a Treasure as part of its cost and deals 1 damage to any target.
 */
val JolenePlunderingPugilist = card("Jolene, Plundering Pugilist") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Mercenary"
    power = 4
    toughness = 2
    oracleText = "Whenever you attack with one or more creatures with power 4 or greater, create " +
        "a Treasure token.\n" +
        "{1}{R}, Sacrifice a Treasure: Jolene deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(GameObjectFilter.Creature.powerAtLeast(4))
        effect = Effects.CreateTreasure(1)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.Sacrifice(GameObjectFilter.Artifact.withSubtype("Treasure"))
        )
        val damageTarget = target("any target", Targets.Any)
        effect = Effects.DealDamage(1, damageTarget)
        description = "{1}{R}, Sacrifice a Treasure: Jolene deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Andreas Zafiratos"
        flavorText = "\"So, are we doing this the easy way or the fun way?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe30b5c8-4889-4350-bb1d-3e2a67d9dfb2.jpg?1712356118"
    }
}
