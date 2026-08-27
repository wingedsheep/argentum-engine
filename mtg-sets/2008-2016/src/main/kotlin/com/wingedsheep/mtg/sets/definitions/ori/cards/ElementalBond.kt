package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Elemental Bond
 * {2}{G}
 * Enchantment
 * Whenever a creature you control with power 3 or greater enters, draw a card.
 */
val ElementalBond = card("Elemental Bond") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control with power 3 or greater enters, draw a card."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().powerAtLeast(3),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "David Gaillet"
        flavorText = "\"I want to help Zendikar. Show me the way.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/554a8769-c840-4c9d-9959-b075c174457b.jpg?1783938323"

        ruling("2025-10-02", "The creature must have power 3 or greater as it enters, or Elemental Bond's ability won't trigger. Static abilities that raise (or lower) a creature's power are taken into account. However, you can't have a creature with power 2 or less enter and try to raise its power with a spell, an activated ability, or a triggered ability.")
    }
}
