package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Repair and Recharge — The Brothers' War #24
 * {3}{W}{W} · Sorcery
 *
 * Return target artifact, enchantment, or planeswalker card from your graveyard to the battlefield.
 * Create a tapped Powerstone token.
 *
 * If the only target is illegal on resolution the whole spell doesn't resolve, so no Powerstone.
 */
val RepairAndRecharge = card("Repair and Recharge") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target artifact, enchantment, or planeswalker card from your graveyard to the " +
        "battlefield. Create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana " +
        "can't be spent to cast a nonartifact spell.\")"

    spell {
        val returned = target(
            TargetFilter(
                (GameObjectFilter.ArtifactOrEnchantment or GameObjectFilter.Planeswalker).ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.Move(returned, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.CreatePowerstone(1, tapped = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Ben Wootten"
        flavorText = "\"Throw the bodies out. They'll just weigh down the frame.\"\n—Rolf, Argivian mechanic"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04ff4a4b-4e86-41d1-a6b6-312aac0d3e1a.jpg?1783920124"
    }
}
