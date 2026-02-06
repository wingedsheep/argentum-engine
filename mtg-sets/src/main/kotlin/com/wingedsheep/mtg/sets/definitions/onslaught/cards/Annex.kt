package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent

/**
 * Annex
 * {2}{U}{U}
 * Enchantment — Aura
 * Enchant land
 * You control enchanted land.
 */
val Annex = card("Annex") {
    manaCost = "{2}{U}{U}"
    typeLine = "Enchantment — Aura"

    auraTarget = Targets.Land

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Andrew Robinson"
        flavorText = "Physical and political boundaries mean nothing in the midst of conflict."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0e2c4fb-3be7-4e65-8abe-89a65bf96967.jpg?1562929065"
    }
}
