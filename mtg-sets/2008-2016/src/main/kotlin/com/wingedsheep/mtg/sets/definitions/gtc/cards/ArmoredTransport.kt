package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Armored Transport
 * {3}
 * Artifact Creature — Construct
 * 2/1
 * Prevent all combat damage that would be dealt to this creature by creatures blocking it.
 *
 * A static [PreventDamage] on combat damage to itself whose source filter is the live
 * `blockingSource()` pairing (CR 509) — the attacking-side mirror of Wall of Vapor. Damage from a
 * creature it blocks, or any noncombat damage, still gets through.
 */
val ArmoredTransport = card("Armored Transport") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 1
    oracleText = "Prevent all combat damage that would be dealt to this creature by creatures blocking it."

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                source = GameObjectFilter.Creature.blockingSource(),
                damageType = DamageType.Combat,
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "226"
        artist = "Cliff Childs"
        flavorText = "\"If only our jails were as secure.\"\n—Arrester Lavinia, Tenth Precinct"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8cf8f6c-217e-4d51-aa63-859c2d38d438.jpg?1783940093"
    }
}
