package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Wirewood Savage
 * {2}{G}
 * Creature — Elf
 * 2/2
 * Whenever a Beast enters the battlefield, you may draw a card.
 */
val WirewoodSavage = card("Wirewood Savage") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf"
    power = 2
    toughness = 2
    oracleText = "Whenever a Beast enters the battlefield, you may draw a card."

    triggeredAbility {
        trigger = TriggerSpec(
                ZoneChangeEvent(filter = GameObjectFilter.Creature.withSubtype(Subtype("Beast")), to = Zone.BATTLEFIELD),
                TriggerBinding.ANY
            )
        effect = MayEffect(DrawCardsEffect(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "304"
        artist = "DiTerlizzi"
        flavorText = "\"She is truly Wirewood's child now.\" —Elvish refugee"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99982622-98bc-45ae-8642-41cd543f32a8.jpg?1562931203"
    }
}
