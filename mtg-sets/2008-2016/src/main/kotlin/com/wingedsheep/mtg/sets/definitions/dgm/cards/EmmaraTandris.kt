package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Emmara Tandris
 * {5}{G}{W}
 * Legendary Creature — Elf Shaman
 * 5/7
 * Prevent all damage that would be dealt to creature tokens you control.
 *
 * One [PreventDamage] replacement whose recipient is a creature token Emmara's current controller
 * controls, re-read per damage instance; any source, combat or not.
 */
val EmmaraTandris = card("Emmara Tandris") {
    manaCost = "{5}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Shaman"
    power = 5
    toughness = 7
    oracleText = "Prevent all damage that would be dealt to creature tokens you control."

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(GameObjectFilter.Creature.token().youControl())
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7c91a0a-2f14-4131-8ca7-1d0046a8edd2.jpg?1783940029"
        ruling(
            "2013-04-15",
            "Emmara Tandris's ability prevents all damage that would be dealt to creature tokens you control, " +
                "not just combat damage."
        )
        ruling(
            "2013-04-15",
            "If Emmara Tandris is dealt lethal damage at the same time creature tokens you control would be " +
                "dealt damage, the damage that would be dealt to those tokens is prevented."
        )
    }
}
