package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gutter Grime
 * {4}{G}
 * Enchantment
 * Whenever a nontoken creature you control dies, put a slime counter on this enchantment, then
 * create a green Ooze creature token with "This token's power and toughness are each equal to the
 * number of slime counters on Gutter Grime."
 *
 * Each Ooze is stamped with the Gutter Grime that made it (`stampCreator`), and its CDA reads that
 * one through [EffectTarget.TokenCreator]: with two Gutter Grimes each Ooze counts only its own, and
 * once its Gutter Grime leaves the battlefield the Ooze is 0/0.
 */
val GutterGrime = card("Gutter Grime") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken creature you control dies, put a slime counter on this enchantment, " +
        "then create a green Ooze creature token with \"This token's power and toughness are each equal to " +
        "the number of slime counters on Gutter Grime.\""

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl()).dies()
        effect = Effects.AddCounters(CounterType.SLIME, 1, EffectTarget.Self) then Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Ooze"),
            stampCreator = true,
            imageUri = "https://cards.scryfall.io/normal/front/1/f/1f22cfdb-15af-46b9-b5c9-d0c76d5b15d6.jpg?1783940881",
            staticAbilities = listOf(
                SetBasePowerToughnessDynamicStatic(
                    power = DynamicAmounts.countersOn(EffectTarget.TokenCreator, CounterType.SLIME),
                    toughness = DynamicAmounts.countersOn(EffectTarget.TokenCreator, CounterType.SLIME),
                    filter = GroupFilter.source(),
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Erica Yang"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9d007a2-163d-4e09-a70b-280a6fa3203b.jpg?1783940917"
    }
}
