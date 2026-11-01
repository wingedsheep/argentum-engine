package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Widespread Brutality — War of the Spark #226 (canonical printing)
 * {1}{B}{R}{R}
 * Sorcery
 * Amass Zombies 2, then the Army you amassed deals damage equal to its power to each non-Army
 * creature.
 *
 * "The Army you amassed" is [EffectTarget.AmassedArmy] — the Army chosen to receive the counters,
 * recorded on the resolution pipeline by the Amass step. Chandra's Ignition's shape follows:
 * `ForEachInGroup` over every non-Army creature (the Army itself, and any other Army such as a
 * changeling, is spared), each dealt `powerOf(AmassedArmy)` with the Army as the damage source,
 * so protection from red doesn't stop the black Army's damage.
 */
val WidespreadBrutality = card("Widespread Brutality") {
    manaCost = "{1}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Amass Zombies 2, then the Army you amassed deals damage equal to its power to each " +
        "non-Army creature. (To amass Zombies 2, put two +1/+1 counters on an Army you control. It's " +
        "also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    spell {
        effect = Effects.Amass(2, "Zombie") then
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype("Army"))),
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.powerOf(EffectTarget.AmassedArmy),
                    target = EffectTarget.IterationEntity,
                    damageSource = EffectTarget.AmassedArmy,
                ),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97715d22-f432-4f67-b4ea-47b8fe6edca5.jpg?1783933385"
        ruling("2023-06-16", "In the rare case that you control multiple Army creatures (perhaps because you played a creature with changeling) while you amass Zombies, you choose which of your Army creatures to put the +1/+1 counters on. If that creature isn't a Zombie, it becomes a Zombie in addition to its other types.")
        ruling("2023-06-16", "Some cards refer to the \"amassed Army.\" That means the Army creature you chose to receive counters, even if no counters were placed on it for some reason.")
    }
}
