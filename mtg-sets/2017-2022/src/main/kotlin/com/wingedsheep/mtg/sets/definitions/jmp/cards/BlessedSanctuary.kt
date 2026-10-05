package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Blessed Sanctuary
 * {3}{W}{W}
 * Enchantment
 * Prevent all noncombat damage that would be dealt to you and creatures you control.
 * Whenever a nontoken creature you control enters, create a 2/2 white Unicorn creature token.
 *
 * The prevention is two continuous [PreventDamage] replacements restricted to
 * `DamageType.NonCombat` — one for the controller ([Recipient.You], Purity's shape) and one for
 * creatures you control ([Recipient.Object], Crystal Barricade's shape, without the "other").
 * Recipient filters are re-evaluated per damage instance against projected state, so creatures
 * that come under your control later are covered. Combat damage and life loss are not prevented.
 */
val BlessedSanctuary = card("Blessed Sanctuary") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Prevent all noncombat damage that would be dealt to you and creatures you control.\n" +
        "Whenever a nontoken creature you control enters, create a 2/2 white Unicorn creature token."

    // "Prevent all noncombat damage that would be dealt to you ..."
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.You,
                damageType = DamageType.NonCombat,
            ),
        ),
    )
    // "... and creatures you control."
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(GameObjectFilter.Creature.youControl()),
                damageType = DamageType.NonCombat,
            ),
        ),
    )

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl()).enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Unicorn"),
        )
        description = "Whenever a nontoken creature you control enters, create a 2/2 white Unicorn creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff80029e-650e-469d-8393-0edf7d9cd695.jpg?1783930511"
        ruling("2020-06-23", "Combat damage is the damage that's dealt automatically by attacking and blocking creatures. Any other damage is noncombat damage, even if it's dealt during a combat phase by an attacking or blocking creature.")
        ruling("2020-06-23", "Effects that cause you to lose life aren't prevented.")
    }
}
