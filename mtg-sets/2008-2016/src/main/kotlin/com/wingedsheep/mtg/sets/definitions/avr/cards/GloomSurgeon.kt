package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Gloom Surgeon — Avacyn Restored #104.
 *
 * A static [PreventDamage] replacement on itself for combat damage whose rest exiles "that many"
 * cards from the top of your library. "That many" counts the damage that would be dealt
 * ([DynamicAmounts.wouldBeDamage]), not what was prevented: per the ruling, combat damage that
 * can't be prevented (Malignus) is dealt and you still exile that many cards.
 */
val GloomSurgeon = card("Gloom Surgeon") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "If combat damage would be dealt to this creature, prevent that damage and exile that many " +
        "cards from the top of your library."

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.Self, damageType = DamageType.Combat),
            onPrevented = Patterns.Library.exileTop(DynamicAmounts.wouldBeDamage())
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Volkan Baǵa"
        flavorText = "He roams the streets of Havengul seeking the sick and delirious so that he may cure them of their lives."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b00c711-007d-4c85-9dd9-dd9d52f1649d.jpg?1783940698"
        ruling(
            "2012-05-01",
            "Combat damage that would be dealt to Gloom Surgeon will be prevented even if you can't exile that many cards from the top of your library."
        )
        ruling(
            "2012-05-01",
            "If damage that can't be prevented (such as the damage Malignus deals) is dealt to Gloom Surgeon, you'll still exile cards from your library."
        )
    }
}
