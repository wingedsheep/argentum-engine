package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Kavu Lair
 * {2}{G}
 * Enchantment
 * Whenever a creature with power 4 or greater enters, its controller draws a card.
 */
val KavuLair = card("Kavu Lair") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature with power 4 or greater enters, its controller draws a card."

    triggeredAbility {
        trigger = TriggerSpec(
            ZoneChangeEvent(filter = GameObjectFilter.Creature.powerAtLeast(4), to = Zone.BATTLEFIELD),
            TriggerBinding.ANY
        )
        controlledByTriggeringEntityController = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Chippy"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4581b53-23a0-4ca6-a77c-97d79e7a6570.jpg?1562944142"
    }
}
