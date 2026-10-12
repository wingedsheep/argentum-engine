package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Static Net
 * {3}{W}
 * Enchantment
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield.
 * When this enchantment enters, you gain 2 life and create a tapped Powerstone token.
 *
 * Banishing Light's exile-until-leaves shape (as Lassoed by the Law) plus a second, independent
 * ETB trigger for the life gain and the tapped Powerstone.
 */
val StaticNet = card("Static Net") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target nonland permanent an opponent controls " +
        "until this enchantment leaves the battlefield.\n" +
        "When this enchantment enters, you gain 2 life and create a tapped Powerstone token. " +
        "(It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(2) then Effects.CreatePowerstone(tapped = true)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5ab5cb30-3ced-4450-a3c4-b519f3762620.jpg?1783920121"
    }
}
