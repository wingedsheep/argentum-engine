package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetEnchantedLandType

/**
 * Sea's Claim
 * {U}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land is an Island.
 */
val SeasClaim = card("Sea's Claim") {
    manaCost = "{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nEnchanted land is an Island."

    auraTarget = Targets.Land

    staticAbility {
        ability = SetEnchantedLandType("Island")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Alan Pollack"
        flavorText = "\"We shall wave our vengeance from shore to shore, drowning our enemies with the tide of our fury.\"\n—Ixidor, reality sculptor"
        imageUri = "https://cards.scryfall.io/large/front/f/b/fb652a5c-464e-4ba4-a4ab-1181be70cf7a.jpg?1562954583a "
    }
}
