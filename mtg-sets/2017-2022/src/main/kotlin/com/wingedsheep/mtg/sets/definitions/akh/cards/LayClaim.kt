package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lay Claim
 * {5}{U}{U}
 * Enchantment — Aura
 * Enchant permanent
 * You control enchanted permanent.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 */
val LayClaim = card("Lay Claim") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant permanent\nYou control enchanted permanent.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    auraTarget = TargetObject(filter = TargetFilter.Permanent)

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Chris Rallis"
        flavorText = "Initiates need worry only about the trials. Everything else is in the hands of the gods."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc0e741f-0448-4a95-9178-074356e50426.jpg?1783936519"
    }
}
