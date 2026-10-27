package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Flourishing Defenses
 * {4}{G}
 * Enchantment
 *
 * Whenever a -1/-1 counter is put on a creature, you may create a 1/1 green Elf Warrior creature token.
 *
 * "A -1/-1 counter" is the per-counter template: the ability triggers once for each counter, so a
 * creature entering with two -1/-1 counters (or a persist return) triggers it twice — `perCounter`.
 */
val FlourishingDefenses = card("Flourishing Defenses") {
    manaCost = "{4}{G}"
    typeLine = "Enchantment"
    oracleText = "Whenever a -1/-1 counter is put on a creature, you may create a 1/1 green Elf Warrior creature token."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature)
            .getsCounters(CounterType.MINUS_ONE_MINUS_ONE, perCounter = true)
        optional = true
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elf", "Warrior")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Dan Murayama Scott"
        flavorText = "Many horrors take the bait of brave elf volunteers walking alone, only to fall to the blades of rescuers lurking nearby."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/6223f5d6-83f1-454e-9cd5-aba157850430.jpg?1783942743"
    }
}
