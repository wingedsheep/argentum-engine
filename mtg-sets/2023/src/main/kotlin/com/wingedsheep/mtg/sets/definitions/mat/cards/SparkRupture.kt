package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spark Rupture — March of the Machine: The Aftermath #5
 * {2}{W} · Enchantment
 *
 * When this enchantment enters, draw a card.
 * Each planeswalker with one or more loyalty counters on it loses all abilities and is a creature
 * with power and toughness each equal to the number of loyalty counters on it.
 *
 * "Is a creature" replaces the planeswalker type (ruling: it's no longer a planeswalker), so the
 * static is one [CompositeStaticAbility] spanning Layer 4 (remove PLANESWALKER, add CREATURE),
 * Layer 6 (lose all abilities) and Layer 7b (base P/T = its loyalty counters, read per permanent
 * through [EffectTarget.AffectedEntity]). The bundle shares one CR 613.6 locked affected set, so the
 * planeswalker matched before Layer 4 keeps being sized and stripped after it stops being one.
 * Loyalty damage and the 0-loyalty SBA key off the projected planeswalker type, so the creature
 * takes marked damage; once its last loyalty counter is gone the filter stops matching and it
 * dies to the 0-loyalty SBA as a planeswalker again.
 */
val SparkRupture = card("Spark Rupture") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, draw a card.\n" +
        "Each planeswalker with one or more loyalty counters on it loses all abilities and is a " +
        "creature with power and toughness each equal to the number of loyalty counters on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    val loyaltyPlaneswalkers = GroupFilter(GameObjectFilter.Planeswalker.withCounter(CounterType.LOYALTY))
    val loyalty = DynamicAmounts.countersOn(EffectTarget.AffectedEntity, CounterType.LOYALTY)

    staticAbility {
        ability = CompositeStaticAbility(
            listOf(
                RemoveCardType("PLANESWALKER", filter = loyaltyPlaneswalkers),
                GrantCardType("CREATURE", filter = loyaltyPlaneswalkers),
                LoseAllAbilities(filter = loyaltyPlaneswalkers),
                SetBasePowerToughnessDynamicStatic(
                    power = loyalty,
                    toughness = loyalty,
                    filter = loyaltyPlaneswalkers
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a027a75f-4faf-4e96-9035-d1f622b9b607.jpg?1783916528"
        ruling(
            "2023-05-12",
            "A planeswalker that becomes a creature due to Spark Rupture is no longer a planeswalker. " +
                "It can no longer be attacked, and damage dealt to it won't cause it to lose loyalty counters."
        )
        ruling(
            "2023-05-12",
            "A planeswalker affected by Spark Rupture loses all abilities it has, not only loyalty " +
                "abilities. The resulting creature may gain abilities after Spark Rupture enters the battlefield."
        )
        ruling(
            "2023-05-12",
            "The resulting creature's power and toughness will change if the number of loyalty " +
                "counters on it changes."
        )
        ruling(
            "2023-05-12",
            "If a planeswalker-turned-creature has all loyalty counters removed from it, Spark " +
                "Rupture's effect will stop applying to it. It will momentarily return to being a " +
                "planeswalker, but as it has no loyalty counters, it will soon find itself in the graveyard."
        )
    }
}
