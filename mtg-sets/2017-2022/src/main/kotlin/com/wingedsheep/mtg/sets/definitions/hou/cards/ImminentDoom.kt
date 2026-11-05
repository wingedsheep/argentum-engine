package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Imminent Doom
 * {2}{R}
 * Enchantment
 * This enchantment enters with a doom counter on it.
 * Whenever you cast a spell with mana value equal to the number of doom counters on this
 * enchantment, this enchantment deals that much damage to any target. Then put a doom counter on
 * this enchantment.
 *
 * The cast trigger's spell filter compares the spell's mana value (X included) against the live
 * doom-counter count on this enchantment. The damage reads the triggering spell's mana value as
 * captured when the ability triggered — which equals the counter count at that moment — so a
 * second trigger stacked in response deals the same amount (ruling 2017-07-14), and countering the
 * spell doesn't change it.
 */
val ImminentDoom = card("Imminent Doom") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "This enchantment enters with a doom counter on it.\n" +
        "Whenever you cast a spell with mana value equal to the number of doom counters on this " +
        "enchantment, this enchantment deals that much damage to any target. Then put a doom counter " +
        "on this enchantment."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.DOOM,
            count = 1,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Any.manaValueEqualsDynamic(DynamicAmounts.countersOnSelf(CounterType.DOOM))
        )
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.triggeringSpellManaValue(), t) then
            Effects.AddCounters(CounterType.DOOM, 1, EffectTarget.Self)
        description = "Whenever you cast a spell with mana value equal to the number of doom counters " +
            "on this enchantment, this enchantment deals that much damage to any target. Then put a " +
            "doom counter on this enchantment."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a51f8207-485e-4a4e-aa62-8e0e69645a0e.jpg?1783936027"
        ruling(
            "2017-07-14",
            "Imminent Doom's triggered ability resolves before the spell that caused it to trigger. " +
                "The ability will resolve even if that spell is countered."
        )
        ruling(
            "2017-07-14",
            "If a spell has {X} in its mana cost, include the value chosen for that X when determining " +
                "the mana value of that spell."
        )
        ruling(
            "2017-07-14",
            "The amount of damage Imminent Doom's triggered ability deals is the number of counters it " +
                "had on it as the ability triggered. For example, if you cast Shock and then respond to " +
                "Imminent Doom's triggered ability with a second Shock, both abilities cause Imminent Doom " +
                "to deal 1 damage and Imminent Doom will end up with three doom counters on it."
        )
    }
}
