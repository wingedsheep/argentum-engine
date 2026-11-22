package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Seraph of the Sword
 * {3}{W}
 * Creature — Angel
 * 3/3
 * Flying
 * Prevent all combat damage that would be dealt to this creature.
 */
val SeraphOfTheSword = card("Seraph of the Sword") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Prevent all combat damage that would be dealt to this creature."

    keywords(Keyword.FLYING)

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                damageType = DamageType.Combat
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Jaime Jones"
        flavorText = "\"Only when my sword lies idle shall I rejoice.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9789cac-5774-4f72-82c3-18f11f9d4a62.jpg?1783939941"
    }
}
