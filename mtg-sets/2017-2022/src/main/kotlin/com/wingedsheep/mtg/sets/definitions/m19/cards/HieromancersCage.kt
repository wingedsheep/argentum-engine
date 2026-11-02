package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hieromancer's Cage
 * {3}{W}
 * Enchantment
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield.
 */
val HieromancersCage = card("Hieromancer's Cage") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target nonland permanent an opponent controls until this enchantment leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(t)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Jason Rainville"
        flavorText = "Law protects but also contains."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/197743cd-249c-42ba-ac8d-027c088f8418.jpg?1783934606"
    }
}
