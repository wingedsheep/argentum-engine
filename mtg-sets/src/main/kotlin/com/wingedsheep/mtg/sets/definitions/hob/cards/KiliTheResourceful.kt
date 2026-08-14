package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.storied
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.FreeFirstEquipEachTurn
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Kíli the Resourceful
 * {1}{W}
 * Legendary Creature — Dwarf Scout
 * 1/2
 *
 * Storied.
 * As long as you have an enduring story, you may pay {0} rather than pay the equip cost of the
 * first equip ability you activate each turn.
 * Whenever another Dwarf or Equipment you control enters, draw a card. This ability triggers only
 * once each turn.
 *
 * Dwarf and Equipment are both subtypes, so one union filter models the printed "or" without
 * double-triggering for an object that has both. `excludeSelf` carries "another"; Kíli otherwise
 * matches the Dwarf half of her own trigger.
 */
val KiliTheResourceful = card("Kíli the Resourceful") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dwarf Scout"
    oracleText = "Storied (If you control three or more artifacts, legendaries, and/or Sagas, you " +
        "have an enduring story for the rest of the game.)\n" +
        "As long as you have an enduring story, you may pay {0} rather than pay the equip cost " +
        "of the first equip ability you activate each turn.\n" +
        "Whenever another Dwarf or Equipment you control enters, draw a card. This ability " +
        "triggers only once each turn."
    power = 1
    toughness = 2

    storied()

    staticAbility {
        condition = Conditions.YouHaveEnduringStory
        ability = FreeFirstEquipEachTurn
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Any
                .withAnyOfSubtypes(listOf(Subtype.DWARF, Subtype.EQUIPMENT))
                .youControl(),
            binding = TriggerBinding.OTHER,
        )
        oncePerTurn = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Yuhong Ding"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/1805532f-6d99-47d0-9529-5f5831a7fdc8.jpg?1785496242"
    }
}
