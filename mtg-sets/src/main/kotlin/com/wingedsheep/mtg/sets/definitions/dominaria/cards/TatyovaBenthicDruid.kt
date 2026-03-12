package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Tatyova, Benthic Druid
 * {3}{G}{U}
 * Legendary Creature — Merfolk Druid
 * 3/3
 * Landfall — Whenever a land you control enters, you gain 1 life and draw a card.
 */
val TatyovaBenthicDruid = card("Tatyova, Benthic Druid") {
    manaCost = "{3}{G}{U}"
    typeLine = "Legendary Creature — Merfolk Druid"
    power = 3
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, you gain 1 life and draw a card."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Land.youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.GainLife(1) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Mathias Kollros"
        flavorText = "\"Yavimaya is one being—one vastness of rippling leaves, one deepness of roots, and one chatter of animals—of which I am one part.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93657aaa-7a0f-49ad-b026-6f79b3bd6768.jpg?1665822988"
    }
}
