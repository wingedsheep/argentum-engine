package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Conclave Tribunal
 * {3}{W}
 * Enchantment
 * Convoke
 * When this enchantment enters, exile target nonland permanent an opponent controls
 * until this enchantment leaves the battlefield.
 *
 * Paired ETB / LTB triggers linked through LinkedExileComponent, the Banishing Light shape.
 */
val ConclaveTribunal = card("Conclave Tribunal") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "When this enchantment enters, exile target nonland permanent an opponent controls until this enchantment leaves the battlefield."

    keywords(Keyword.CONVOKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d9fda5d-df8f-45d6-b8f0-33f903c4bf83.jpg?1783934204"
    }
}
