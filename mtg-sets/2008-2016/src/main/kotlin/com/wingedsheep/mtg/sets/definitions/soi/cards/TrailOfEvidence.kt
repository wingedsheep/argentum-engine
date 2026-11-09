package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Trail of Evidence (Shadows over Innistrad #93)
 * {2}{U}
 * Enchantment
 *
 * Whenever you cast an instant or sorcery spell, investigate. (Create a Clue token. It's an artifact
 * with "{2}, Sacrifice this token: Draw a card.")
 */
val TrailOfEvidence = card("Trail of Evidence") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an instant or sorcery spell, investigate. (Create a Clue token. " +
        "It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.Investigate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Daniel Ljunggren"
        flavorText = "\"Every answer seems to lead to a more terrifying question.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b150797-ee8d-42f8-adcb-2f4b1783c074.jpg"
    }
}
