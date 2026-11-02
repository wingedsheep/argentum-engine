package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Rogue's Gloves
 * {2}
 * Artifact — Equipment
 * Whenever equipped creature deals combat damage to a player, you may draw a card.
 * Equip {2}
 */
val RoguesGloves = card("Rogue's Gloves") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature deals combat damage to a player, you may draw a card.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Effects.DrawCards(1))
        description = "Whenever equipped creature deals combat damage to a player, you may draw a card."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Cyril Van Der Haegen"
        flavorText = "Professional pilferers prefer proper preparation."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22a35bd7-8f0f-421a-948d-810513e6b0a0.jpg?1783939156"
    }
}
