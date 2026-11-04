package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Everdawn Champion
 * {1}{W}{W}
 * Creature — Human Soldier
 * 2/2
 * Prevent all combat damage that would be dealt to this creature.
 *
 * A static prevention replacement effect on combat damage with [Recipient.Self] — the "dealt to"
 * half of Fog Bank. Damage is still assigned normally (it's prevented as it would be dealt).
 */
val EverdawnChampion = card("Everdawn Champion") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Prevent all combat damage that would be dealt to this creature."
    power = 2
    toughness = 2

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                damageType = DamageType.Combat
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Sara Winters"
        flavorText = "\"Undefeated in battle, she is the shining future our empire deserves.\"\n—Huatli"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84a74fa9-1338-40ea-95d9-0f4e78b85f60.jpg?1783935338"

        ruling(
            "2018-01-19",
            "Combat damage can be assigned to Everdawn Champion as normal, even though that damage will be " +
                "prevented. For example, if Everdawn Champion blocks a 4/4 creature with trample, the attacking " +
                "player may assign 2 of that creature's combat damage to the player or planeswalker it's attacking."
        )
    }
}
